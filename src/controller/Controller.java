package controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import events.AckEvent;
import events.Crash;
import events.GenericEvent;
import events.GoTo;
import events.Join;
import events.Nack;
import events.NewLeader;
import events.RequestToJoin;
import events.SlidePartData;
import events.StartSession;
import model.Session;
import model.User;
import network.CrashDetector;
import network.MulticastDownload;
import network.NetworkHandler;
import network.NetworkHelloReceiver;
import network.NetworkLeaderHandler;
import network.NetworkSlideSender;
import view.Gui;
import view.View;

public class Controller implements Observer{

	private Session session; 
	private NetworkSlideSender slideSender;

	private CrashDetector crashDetector; 

	//Gestisce la rete per i client
	private NetworkHandler networkHandler;

	//Gestisce le richieste di connessione verso il Leader
	private NetworkLeaderHandler nlh;


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

	public Controller(Session session, View view){
		this.session=session;
		this.view= view;
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


	public Controller(Session session, Gui gui){
		this.session=session;
		this.view= new View(session, this, gui);
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

		case START_SESSION:
			//inizia la sessione, devo ricevere le slide
			StartSession evStart = (StartSession) arg;
			MulticastDownload slReceiver = new MulticastDownload(session,evStart.getNumSlide(), false);
			//ricevo le slide
			while(true){
				BufferedImage imgRec = slReceiver.receive();
				if(imgRec!=null){
					session.addSlide(imgRec);
					System.out.println(session.getSlides().size());
					System.out.println("--------------------INserisco una slide------------" + session.getSlides().size());
				} else break;
			}
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
				System.out.println(crashDetector);
				System.out.println(rqtj);
				System.out.println(rqtj.getJoiner());
				crashDetector.addUser(rqtj.getJoiner());
			}
			//Aggiorno la view con gli utenti
			view.displayUsers(session.getJoined());
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
			//Chiudo il socket verso il vecchio leader
			networkHandler.close();
			if(session.isLeader()){
				//Sono il nuovo leander

				//Cambio la view
				view.becomeMaster();
				view.changeSlide(session.getSlides().get(session.getActualSlide()));

				//ora devo inizializzare tutti i socket che verranno aperti dagli altri client
				System.out.println("-------APERTURA SOCKET VERSO TUTTI GLI ALTRI CLIENT-------");
				nlh = new NetworkLeaderHandler(this);

			} else {
				//Non sono io il nuovo leader, ma sono un semplice client che
				//deve aprire il socket verso il nuovo leader, per farlo uso la request to Join
				networkHandler = new NetworkHandler(session.getLeader().getIp(), this);
				RequestToJoin reqTJ = new RequestToJoin(session.getMyself());
				networkHandler.send(reqTJ);
			}
			crashDetector.setAlreadySentElect(false);

			break;
		case CRASH: 
			Crash crash = (Crash) arg;
			removeUser(crash.getCrashedUser());
			System.out.println("+++++++++++++Ho rilevato il CRASH");
			System.out.println("+++++++++++++SessionCreator: "+ session.getSessionCreator().getName());
			if(session.isSessionCreator()) {
				System.out.println("+++++++ sono il sessionCreator");
				//crash del leader e io sono session creator
				if(crash.getCrashedUser().equals(session.getLeader())) {
					System.out.println("+++++++++++++++++++++++++++++è crashato il leader e io sono il sessionCreator");
					//chiudo l'handler precedente (da utente normale) e apro l'handler da leader
					session.setLeader(session.getMyself());
					networkHandler.close();
					networkHandler = null; 
					view.becomeMaster();
					view.changeSlide(session.getSlides().get(session.getActualSlide()));
					nlh = new NetworkLeaderHandler(this);
					
				} else {
					//crash non del leader e io sono session creator
					//removeUser(crash.getCrashedUser());
				}
			} else {
				//non sono il session creator
				if(crash.getCrashedUser().equals(session.getLeader())) {
					//siamo nel caso in cui è crashato il leader
					if(session.getLeader().equals(session.getSessionCreator())) {
						//se session creator e session leader coincidono e quell'utente è crashato, leader election
						System.out.println("CRASH DEL LEADER, SESSION CREATOR COINCIDE CON LEADER, LANCIO LEADER ELECTION");
						leaderElection();
					} else {
						//session creator e session leader sono due utenti diversi
						if(session.getJoined().contains(session.getSessionCreator())) {
							//il session creator è attivo e diventerà leader, quindi apro il socket verso di lui
							System.out.println("+++++++++++++++++++++++++++++SONO un vecchio client, continuo ad essere client");
							session.setLeader(session.getSessionCreator());
							networkHandler.close();
							networkHandler = null;
							networkHandler = new NetworkHandler(session.getSessionCreator().getIp(), this);
							RequestToJoin rqtj1 = new RequestToJoin(session.getMyself());
							networkHandler.send(rqtj1);
						} else {
							//il session creator non è attivo, quindi devo lanciare la leader election
							System.out.println("CRASH DEL LEADER, SESSION CREATOR NON ATTIVO, LANCIO LEADER ELECTION");
							leaderElection();
						}

					}
				} else {
					//non è crashato il leader: rimuovo solo l'utente
					//removeUser(crash.getCrashedUser());
					if(session.isLeader()){
						view.displayUsers(session.getJoined());
					}
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
		//invio l'evento di start, che segnala quante slide ci sono da visualizzare
		StartSession evStart = new StartSession(session.getSlides().size());
		nlh.sendToUsers(evStart);
		//Invio le slide
		MulticastDownload sendSlides = new MulticastDownload(session, session.getSlides().size(), true);
		for(BufferedImage elem : session.getSlides()){
			sendSlides.sendSlide(elem);
		}
		view.presentationButtons();
		this.goTo(0);
	}

	public void prev(){
		int actualSlide = session.getActualSlide();
		if(actualSlide > 0 ){
			this.goTo(actualSlide-1);
		}
	}

	public void next(){
		int actualSlide = session.getActualSlide();
		if(actualSlide < session.getSlides().size() - 1 ){
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
		if(!user.equals(session.getLeader())){
			view.becomeClient();
			view.changeSlide(session.getSlides().get(session.getActualSlide()));
			view.displayUsers(session.getJoined());
			//Creo l'evento e lo invio a tutti i client (specificando qual'è il nuovo leader)
			NewLeader nl = new NewLeader(user); 
			nlh.sendToUsers(nl);
			session.setLeader(user);
			nlh.closeOldSockets();
			nlh = null;

			//Ora non sono più leader, devo aprire un socket verso il New Leader
			networkHandler = new NetworkHandler(session.getLeader().getIp(), this);
			RequestToJoin rqtj = new RequestToJoin(session.getMyself());
			networkHandler.send(rqtj);
		}

	}

	public void removeUser(User crashed) {
		System.out.println("sono entrato per rimuovere l'utente");
		session.getJoined().remove(crashed);
		if(session.isLeader()) {
			view.displayUsers(session.getJoined());
			nlh.removeSocket(crashed);
		}
	}

	public void leaderElection() {
		if(!crashDetector.isAlreadySentElect()) {
			crashDetector.startElect();
		}
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
