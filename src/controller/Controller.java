package controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.Session;
import model.User;
import network.NetworkHelloReceiver;
import network.NetworkReceiver;
import network.NetworkSender;
import network.NetworkSlideSender;
import view.View;
import events.Ack;
import events.AckEvent;
import events.GenericEvent;
import events.GoTo;
import events.Join;
import events.Nack;
import events.NewLeader;
import events.RequestToJoin;
import events.SlidePart;
import events.SlidePartData;

public class Controller implements Observer{

	private Session session; 
	private NetworkSlideSender slideSender;
	private NetworkSender networkSender;
	private NetworkReceiver networkReceiver;
	private NetworkHelloReceiver networkHelloReceiver;
	private View view;
	
	private Map<GenericEvent, List<User>> ackedEvent;
	
	
	private List<SlidePartData> tempArray;
	private int currentSessionNumber=-1;

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Controller(Session session) {
		this.session = session;
		view = new View(session); 
		try {
			slideSender = new NetworkSlideSender(session);
			networkSender = new NetworkSender(session);
			networkReceiver = new NetworkReceiver(session, this);
			ackedEvent = new HashMap<GenericEvent, List<User>>();
			if(session.isLeader()){
				networkHelloReceiver = new NetworkHelloReceiver(session);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Controller(){
		try {
			slideSender = new NetworkSlideSender(session);
			networkSender = new NetworkSender(session);
			networkReceiver = new NetworkReceiver(session, this);
			ackedEvent = new HashMap<GenericEvent, List<User>>();
			if(session.isLeader()){
				networkHelloReceiver = new NetworkHelloReceiver(session);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendEvent(GenericEvent event){
		try {
			networkSender.send(event);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		System.out.println("EVENTO ARRIVATO!!");

		if(!(arg instanceof GenericEvent)){
			throw new IllegalArgumentException();
		}
		switch(((GenericEvent) arg).getType()){
		case ACK:
			System.out.println("ACK RICEVUTO per il pezzetto di immagine");
			if(session.isLeader()){
				System.out.println("ddentro ack, sono leader");
				slideSender.setCont();
				//slideSender.notifyAll();
				
			}
			break;
		case NACK:
			if(session.isLeader()){
				slideSender.sendMissingPacket(((Nack) arg).getSequenceNumber());
			}
			break;
		case SLIDEPART:
			//Ho ricevuto un pezzetto di immagine
			
			if(!session.isLeader()){
				System.out.println("entrato nell'esecuzione di slide part");
				SlidePartData slice =((SlidePart) arg).getData();
				
				//se start inizializzo arraylist
				if(slice.start || currentSessionNumber != slice.sessionNumber){
					currentSessionNumber = slice.sessionNumber;
					tempArray = new ArrayList<SlidePartData>(slice.numPack);
					for(int i=0; i<slice.numPack; i++){
						tempArray.add(null);
					}
				}
				
				tempArray.set(slice.sequenceNumber, slice);
				if(slice.sequenceNumber-1 > 0 && tempArray.get(slice.sequenceNumber-1) == null ){
					//Invio NACK
					try {
						networkSender.send(new Nack(slice.sequenceNumber-1));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					boolean endSlide=true;
					for(SlidePartData part : tempArray){
						if(part==null){
							endSlide = false;
							break;
						}
					}
					if(endSlide){
						try {
							//Invio ACK
							networkSender.send(new Ack(currentSessionNumber));
							
							//Costruisco l'immagine e la aggiungo alla session
							byte[] imageData = new byte[((tempArray.size()-1) * tempArray.get(0).maxPacketSize) + tempArray.get(tempArray.size()-1).data.length];
							for(SlidePartData part : tempArray){
								System.arraycopy(part.data, 0, imageData, part.sequenceNumber*part.maxPacketSize , part.data.length);
							}
							ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
							BufferedImage image = ImageIO.read(bis);
							System.out.println(image);
							session.addSlide(image);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			break;
			
		case ACK_EVENT:
			//Devo memorizzare tutti gli ack ricevuti e quando vengono ricevo un ack verifico se l'ho ricevuto da tutti
			AckEvent event = (AckEvent)arg;
			List<User> ackedUser = ackedEvent.get(event.getEvent());
			if(!ackedUser.contains(event.getUser())){
				ackedUser.add(event.getUser());
			}
			//Controlare che la lista di eventi negli ackedEvent
			if(ackedUser.size() == session.getJoined().size()){
				//Tutti gli utenti hanno inviato l'ack dell'evento event
				//Posso eseguire realmente l'evento
				System.out.println("Sto per ESEGUIRE L'EVENTO: " + event.toString());
				this.executeEvent(event.getEvent());
			}
			
			break;
		case JOIN:
			Join ev = (Join) arg;
			if(session.getMyself().equals(ev.getJoiner())){
				ackedEvent.put((GenericEvent) arg, new ArrayList<User>());
				break;
			}
		case ANSWER:
			break;
		case GOTO:
			session.setActualSlide(((GoTo)arg).getSlideToShow());
			view.changeSlide(session.getSlides().get(((GoTo)arg).getSlideToShow()));
			break;
		case NEWLEADER:
			NewLeader e = (NewLeader) arg;
			session.setLeader(e.getNewLeader());
			//Attivare i pulsanti
			if(session.isLeader()){
				view.activateButtons();
			}
			break;
		case TERMINATE:
			System.out.println("EVENTO!!! sta per uscire traffic");
			ackedEvent.put((GenericEvent) arg, new ArrayList<User>());
			this.sendAck((GenericEvent)arg,session.getMyself());
			break;
		case HELLO:
			break;
		default:
			break;

		}

	}
	
	private void executeEvent(GenericEvent event) {
		switch(((GenericEvent) event).getType()){
		case GOTO:
			
			break;
		case JOIN:
			System.out.println("Aggiunto user: " + ((Join)event).getJoiner().toString());
			session.addJoinedUser(((Join) event).getJoiner());
			break;
		case NEWLEADER:
			session.setLeader(((NewLeader) event).getNewLeader());
			break;
		case TERMINATE:
			break;
		}
		
	}

	private void sendAck(GenericEvent arg, User user){
		AckEvent ack = new AckEvent(arg, user);
		sendEvent(ack);
	}


	public void startSession(){
		slideSender.sendSlides();
	}

	public void prev(){
		int actualSlide = session.getActualSlide();
		if(actualSlide > 0 ){
			this.goTo(actualSlide-1);
		}
	}
	
	public void next(){
		int actualSlide = session.getActualSlide();
		if(actualSlide < session.getSlides().size() ){
			this.goTo(actualSlide+1);
		}
	}
	
	private void goTo(int slideToGo){
		GoTo event = new GoTo(slideToGo);
		try {
			networkSender.send(event);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		session.setActualSlide(slideToGo);
		view.changeSlide(session.getSlides().get(slideToGo));
		
	}
	
	/**
	 * Invocato dall'utente che vuole entrare nella session
	 */
	public void requestToJoin(){
		RequestToJoin rqTJ = new RequestToJoin(session.getMyself());
		try {
			networkSender.sendToLeader(rqTJ);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Invocato da
	 * @param user
	 */
	public void newJoiner(User user){
		Join join = new Join(user);
		try {
			networkSender.send(join);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Ora applico alla mia sessione
		session.addJoinedUser(user);
	}
	


}
