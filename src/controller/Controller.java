package controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import events.Crash;
import events.GenericEvent;
import events.GoTo;
import events.Join;
import events.NewLeader;
import events.RequestToJoin;
import events.SendSlideTo;
import events.StartSession;
import model.Session;
import model.User;
import network.CrashDetector;
import network.MulticastDownload;
import network.NetworkHandler;
import network.NetworkHelloReceiver;
import network.NetworkLeaderHandler;
import view.Gui;
import view.View;

public class Controller implements Observer{

	private Session session;

	private CrashDetector crashDetector; 

	//Gestisce la rete per i client
	private NetworkHandler networkHandler;

	//Gestisce le richieste di connessione verso il Leader
	private NetworkLeaderHandler nlh;


	private NetworkHelloReceiver networkHelloReceiver;
	private View view;

	//Socket e I/O sul socket bidirezionale verso/da il leader
	private Map<GenericEvent, List<User>> ackedEvent;

	private int nextSender;


	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Controller(Session session, View view){
		this.nextSender=0;
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
		this.nextSender=1;
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


	@Override
	public void update(Observable o, Object arg) {
		System.out.println("EVENTO ARRIVATO!!");

		if(!(arg instanceof GenericEvent)){
			throw new IllegalArgumentException();
		}
		switch(((GenericEvent) arg).getType()){

		case SEND_SLIDE_TO:{
			SendSlideTo evSST = (SendSlideTo) arg;
			List<User> receivers = new ArrayList<User>();
			receivers.add(evSST.getReceiver());
			MulticastDownload sendSlides = new MulticastDownload(session, session.getSlides().size(), true, receivers);
			sendSlides.generatePackets(session.getSlides());
			sendSlides.sendSlides();

		}break;

		case START_SESSION:
			//inizia la sessione, devo ricevere le slide (solo sul client)
			StartSession evStart = (StartSession) arg;
			MulticastDownload slReceiver = new MulticastDownload(session,evStart.getNumSlide(), false, null);
			//ricevo le slide
			while(true){
				BufferedImage imgRec = slReceiver.receive();
				if(imgRec!=null){
					session.addSlide(imgRec);
					System.out.println(session.getSlides().size());
					System.out.println("--------------------INserisco una slide------------" + session.getSlides().size());
				} else break;
			}
			session.setSessionStarted(true);
			view.changeSlide(session.getSlides().get(session.getActualSlide()));
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

				if(session.isSessionStarted()){
					System.out.println("Sessione già iniziata");
					//La sessione è già partita, quindi c'è una join a metà di una session
					//Devo inviare la start_session all'utente che ha richiesto la join e dire ad un utente di mandare le slide
					StartSession evSt = new StartSession(session.getSlides().size());
					SendSlideTo evSST = new SendSlideTo(rqtj.getJoiner());
					nextSender = findNextSender(nextSender);
					if(session.getJoined().get(nextSender).equals(session.getLeader())) {
						List<User> joiner = new ArrayList<User>();
						joiner.add(rqtj.getJoiner());
						MulticastDownload sendSlides = new MulticastDownload(session, session.getSlides().size(), true, joiner);
						sendSlides.generatePackets(session.getSlides());
						nlh.sendToUser(rqtj.getJoiner(), evSt);
						sendSlides.sendSlides();
					} else {
						nlh.sendToUser(session.getJoined().get(nextSender), evSST);
						try {
							Thread.sleep(200);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						nlh.sendToUser(rqtj.getJoiner(), evSt);
					}

					nextSender++;
					//nextSender=(nextSender+1)%session.getJoined().size();
				}

			}
			System.out.println("SOno nel request To JOIN, il nuovo utente è "+rqtj.getJoiner().getName());
			//Aggiorno la view con gli utenti
			view.displayUsers(session.getJoined());
			break;
		case JOIN:
			//Sono un client e devo aggiungere un nuovo utente agli utenti della sessione
			System.out.println("SONO NELLA JOIN, TUTTO VA BENE");
			Join ev = (Join) arg;
			session.addJoinedUser(ev.getJoiner());
			crashDetector.addUser(ev.getJoiner());
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


				//ora devo inizializzare tutti i socket che verranno aperti dagli altri client
				System.out.println("-------APERTURA SOCKET VERSO TUTTI GLI ALTRI CLIENT-------");
				nlh = new NetworkLeaderHandler(this);
				try {
					networkHelloReceiver= new NetworkHelloReceiver(session);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				view.changeSlide(session.getSlides().get(session.getActualSlide()));
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
			System.out.println("crash di " + crash.getCrashedUser().getName());
			//se il controller non ha già gestito questo evento di crash in precedenza, allora lo gestisce ora
			if(session.getJoined().contains(crash.getCrashedUser())){
				//rimuovo l'utente dalla lista degli utenti connessi alla sessione
				removeUser(crash.getCrashedUser());
				System.out.println("+++++++++++++Ho rilevato il CRASH");
				System.out.println("+++++++++++++SessionCreator: "+ session.getSessionCreator().getName());
				if(session.isSessionCreator()) {
					System.out.println("+++++++ sono il sessionCreator");
					//crash del leader e io sono session creator
					if(crash.getCrashedUser().equals(session.getLeader())) {
						System.out.println("+++++++++++++++++++++++++++++è crashato il leader e io sono il sessionCreator");
						//chiudo l'handler precedente (da utente normale) e apro l'handler da leader
						try {
							networkHelloReceiver = new NetworkHelloReceiver(session);
							//setto il nuovo leader
							session.setLeader(session.getMyself());
							networkHandler.close();
							networkHandler = null; 
							view.becomeMaster();
							nlh = new NetworkLeaderHandler(this);
							view.changeSlide(session.getSlides().get(session.getActualSlide()));
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
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
						if(session.isLeader()){
							view.displayUsers(session.getJoined());
						}
					}
				}
			}
			break;
		default:
			break;

		}

	}

	public void startSession(){
		if(session.getJoined().size()>1){
			//invio l'evento di start, che segnala quante slide ci sono da visualizzare
			//Invio le slide
			List<User> receivers = new ArrayList<User>(session.getJoined());
			receivers.remove(session.getLeader());
			MulticastDownload sendSlides = new MulticastDownload(session, session.getSlides().size(), true, receivers);
			sendSlides.generatePackets(session.getSlides());
			StartSession evStart = new StartSession(session.getSlides().size());
			nlh.sendToUsers(evStart);
			//		for(BufferedImage elem : session.getSlides()){
			//			sendSlides.sendSlide(elem);
			//		}
			sendSlides.sendSlides();
			view.presentationButtons();
			session.setSessionStarted(true);
			this.goTo(0);
		}
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
			networkHelloReceiver.stopReceiving();
			networkHelloReceiver=null;
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
			//Sono il leader
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

	private int findNextSender(int nextSenderIn) {

		if(nextSender < session.getJoined().size() -1) {
			return nextSenderIn;
		} 
		return 0;
		//		for(int i = nextSender + 1; i < session.getJoined().size()-1; i++) {
		//			if(!session.getJoined().get(i).equals(session.getLeader())) {
		//				return i; 
		//			}
		//		}
		//		for(int i = 0; i < nextSender && i < session.getJoined().size() -1; i++) {
		//			if(!session.getJoined().get(i).equals(session.getLeader())) {
		//				return i; 
		//			}
		//		}
		//		for(int i = 0; i < session.getJoined().size() - 1; i++) {
		//			if(session.getJoined().get(i).equals(session.getLeader())) {
		//				return i; 
		//			}
		//		}
		//		return 0; 
	}


}
