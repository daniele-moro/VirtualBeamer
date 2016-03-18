package controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;

import events.*;
import model.Session;
import model.User;
import network.CrashDetector;
import network.NetworkHandler;
import network.NetworkHelloReceiver;
import network.NetworkLeaderHandler;
import network.NetworkReceiver;
import network.NetworkSlideSender;
import view.View;

public class Controller implements Observer{

	private Session session; 
	private NetworkSlideSender slideSender;
	
	private CrashDetector crashDetector; 

	//Gestisce la rete per i client
	private NetworkHandler networkHandler;

	//Gestisce le richieste di connessione verso il Leader
	private NetworkLeaderHandler nlh;



	private NetworkReceiver networkReceiver;
	private NetworkHelloReceiver networkHelloReceiver;
	private View view;

	//Socket e I/O sul socket bidirezionale verso/da il leader


	private Map<GenericEvent, List<User>> ackedEvent;


	private List<SlidePartData> tempArray;
	private int currentSessionNumber=-1;

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Controller(Session session){
		this.session=session;
		this.view= new View(session, this);
		this.crashDetector = new CrashDetector(session, this);
		if(session.isLeader()){
			//Se è leader devo istanziare il serverSocket
			try {
				networkHelloReceiver = new NetworkHelloReceiver(session);
				nlh = new NetworkLeaderHandler(this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	//	public Controller(Session session) {
	//		this.session = session;
	//		view = new View(session,this); 
	//		try {
	//			slideSender = new NetworkSlideSender(session);
	//			networkSender = new NetworkHandler(session);
	//			networkReceiver = new NetworkReceiver(session, this);
	//			ackedEvent = new HashMap<GenericEvent, List<User>>();
	//			if(session.isLeader()){
	//				networkHelloReceiver = new NetworkHelloReceiver(session);
	//			}
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}

	//	public Controller(){
	//		try {
	//			slideSender = new NetworkSlideSender(session);
	//			networkSender = new NetworkHandler(session);
	//			networkReceiver = new NetworkReceiver(session, this);
	//			ackedEvent = new HashMap<GenericEvent, List<User>>();
	//			if(session.isLeader()){
	//				networkHelloReceiver = new NetworkHelloReceiver(session);
	//			}
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}


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
			//			//Ho ricevuto un pezzetto di immagine
			//
			//			if(!session.isLeader()){
			//				System.out.println("entrato nell'esecuzione di slide part");
			//				SlidePartData slice =((SlidePart) arg).getData();
			//
			//				//se start inizializzo arraylist
			//				if(slice.start || currentSessionNumber != slice.sessionNumber){
			//					currentSessionNumber = slice.sessionNumber;
			//					tempArray = new ArrayList<SlidePartData>(slice.numPack);
			//					for(int i=0; i<slice.numPack; i++){
			//						tempArray.add(null);
			//					}
			//				}
			//
			//				tempArray.set(slice.sequenceNumber, slice);
			//				if(slice.sequenceNumber-1 > 0 && tempArray.get(slice.sequenceNumber-1) == null ){
			//					//Invio NACK
			//					try {
			//						networkSender.send(new Nack(slice.sequenceNumber-1));
			//					} catch (IOException e) {
			//						// TODO Auto-generated catch block
			//						e.printStackTrace();
			//					}
			//				} else {
			//					boolean endSlide=true;
			//					for(SlidePartData part : tempArray){
			//						if(part==null){
			//							endSlide = false;
			//							break;
			//						}
			//					}
			//					if(endSlide){
			//						try {
			//							//Invio ACK
			//							networkSender.send(new Ack(currentSessionNumber));
			//
			//							//Costruisco l'immagine e la aggiungo alla session
			//							byte[] imageData = new byte[((tempArray.size()-1) * tempArray.get(0).maxPacketSize) + tempArray.get(tempArray.size()-1).data.length];
			//							for(SlidePartData part : tempArray){
			//								System.arraycopy(part.data, 0, imageData, part.sequenceNumber*part.maxPacketSize , part.data.length);
			//							}
			//							ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
			//							BufferedImage image = ImageIO.read(bis);
			//							System.out.println(image);
			//							session.addSlide(image);
			//						} catch (IOException e) {
			//							// TODO Auto-generated catch block
			//							e.printStackTrace();
			//						}
			//					}
			//				}
			//			}
			break;


		case REQUEST_TO_JOIN:
			//Sono nel controller del leader
			RequestToJoin rqtj = (RequestToJoin) arg;
			//Avvisiamo tutti gli utenti che c'è stata una join
			Join joinEv = new Join(rqtj.getJoiner());
			//Se l'utente che deve accedere è un nuovo utente lo aggiunto, altrimenti siamo nel cambio leader
			if(!session.getJoined().contains(rqtj.getJoiner())){
				nlh.sendToUsers(joinEv);
				//Aggiungiamo l'utente nel model locale
				session.addJoinedUser(rqtj.getJoiner());
				crashDetector.addUser(rqtj.getJoiner());
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
				//this.executeEvent(event.getEvent());
			}

			break;
		case JOIN:
			//Sono un client e devo aggiungere un nuovo utente agli utenti della sessione
			System.out.println("SONO NELLA JOIN, TUTTO VA BENE");
			Join ev = (Join) arg;
			session.addJoinedUser(ev.getJoiner());
			crashDetector.addUser(ev.getJoiner());
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
				//ora devo inizializzare tutti i socket che verranno aperti dagli altri client

				nlh = new NetworkLeaderHandler(this);

			} else {
				//Non sono io il nuovo leader, ma sono un semplice client che
				//deve aprire il socket verso il nuovo leader

				networkHandler = new NetworkHandler(session.getLeader().getIp(), this);
				RequestToJoin reqTJ = new RequestToJoin(session.getMyself());
				networkHandler.send(reqTJ);
			}

			break;
		case CRASH: 
			Crash crash = (Crash) arg;

			if(session.isSessionCreator()) {
				//crash del leader e io sono session creator
				if(crash.getCrashedUser().equals(session.getLeader())) {
					//chiudo l'handler precedente (da utente normale) e apro l'handler da leader
					session.setLeader(session.getMyself());
					networkHandler.close();
					networkHandler = null; 
					nlh = new NetworkLeaderHandler(this);
					view.activateButtons();

				} else {
					//crash non del leader e io sono session creator
					removeUser(crash.getCrashedUser());
				}
			} else {
				//non sono il session creator

				if(crash.getCrashedUser().equals(session.getLeader())) {
					//siamo nel caso in cui è crashato il leader
					if(session.getLeader().equals(session.getSessionCreator())) {
						//se session creator e session leader coincidono e quell'utente è crashato, leader election
						leaderElection();
					} else {
						//session creator e session leader sono due utenti diversi
						if(session.getJoined().contains(session.getSessionCreator())) {
							//il session creator è attivo e diventerà leader, quindi apro il socket verso di lui
							session.setLeader(session.getSessionCreator());
							networkHandler.close();
							networkHandler = new NetworkHandler(session.getSessionCreator().getIp(), this);
						} else {
							//il session creator non è attivo, quindi devo lanciare la leader election
							leaderElection();
						}

					}
				} else {
					//non è crashato il leader: rimuovo solo l'utente
					removeUser(crash.getCrashedUser());
				}
			}

			break;
		case TERMINATE:
			System.out.println("EVENTO!!! è uscita traffic");
			ackedEvent.put((GenericEvent) arg, new ArrayList<User>());
			//this.sendAck((GenericEvent)arg,session.getMyself());
			break;
		case HELLO:
			break;
		default:
			break;

		}

	}

//	private void executeEvent(GenericEvent event) {
//		switch(((GenericEvent) event).getType()){
//		case GOTO:
//
//			break;
//		case JOIN:
//			System.out.println("Aggiunto user: " + ((Join)event).getJoiner().toString());
//			session.addJoinedUser(((Join) event).getJoiner());
//			break;
//		case NEWLEADER:
//			session.setLeader(((NewLeader) event).getNewLeader());
//			break;
//		case TERMINATE:
//			break;
//		}
//
//	}

	//	private void sendAck(GenericEvent arg, User user){
	//		AckEvent ack = new AckEvent(arg, user);
	//		sendEvent(ack);
	//	}


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
		nlh.sendToUsers(event);
		session.setActualSlide(slideToGo);
		view.changeSlide(session.getSlides().get(slideToGo));

	}



	/**
	 * Invocato dal leader, per avvisare tutti che c'è stata una Join di un utente
	 * @param user
	 */
	public void newJoiner(User user){
		Join join = new Join(user);
		nlh.sendToUsers(join);
		//Ora applico alla mia sessione
		session.addJoinedUser(user);
		crashDetector.addUser(user);
		}


	public void newLeader(User user) {
		NewLeader nl = new NewLeader(user); 
		nlh.sendToUsers(nl);
		session.setLeader(user);
		nlh.closeOldSockets();
		nlh = null;
		//Ora non sono più leader, devo aprire un socket verso il New Leader

		networkHandler = new NetworkHandler(user.getIp(), this);

	}

	public void removeUser(User crashed) {
		session.getJoined().remove(crashed);
		if(session.isLeader()) {
			nlh.removeSocket(crashed);
		}
	}

	public void leaderElection() {
		crashDetector.startElect();
	}






	/**
	 * Invocato dall'utente che vuole entrare nella session, istanzia il networkHandler e invia l'evento di Join
	 */
	public void requestToJoin(){

		//istanzio il networkHandler e invio la richiesta di Join al Leader
		networkHandler = new NetworkHandler(session.getLeader().getIp(), this);
		System.out.println(session.getLeader().getIp());
		RequestToJoin rqTJ = new RequestToJoin(session.getMyself());
		networkHandler.send(rqTJ);


	}


}
