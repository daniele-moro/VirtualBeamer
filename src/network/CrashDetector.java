package network;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

import events.Coordinator;
import controller.Controller;
import events.Alive;
import events.Crash;
import events.Elect;
import events.GenericEvent;
import events.NewLeader;
import events.Stop;
import model.Session;
import model.User;

/**
 * Classe che invia periodicamente in Multicast un messaggio per dire che è ancora vivo (ALIVE),
 *  inoltre si occupa anche di ricevere questi messaggi e vedere se qualche nodo crasha.
 *  Si occupa anche di inviare i messaggi di leader election, di riceverli e di notificare il controllore quando
 *  la leader election ha trovato l'utente nuovo leader
 *
 */

public class CrashDetector extends Observable{
	private final static int SEND_INTERVAL = 250;		//Intevallo di spedizione degli Alive
	private final static int INCREMENT_INTERVAL = 250; //Intervallo di incremento del contatori counters
	private final static int NUM_FAIL_ALIVE = 5;		//Numero di incrementi max prima di considerare un nodo crashato
	
	//Var per multicast (gruppo e socket)
	private InetAddress group;
	private MulticastSocket socket;
	
	//Timer per inviare gli Alive
	private TimerAlive timerHello;
	
	//Timer per effettuare gli incrementi
	private TimerIncrement timerIncrements;
	//Thread di ricezione degli Alive
	private ReceiverAlive receiverAlive;
	
	private Session session;
	private boolean alreadySentElect = false;
	//Timer per effettuare il check dell'elezione
	private Timer checkForElectionConfirm; 

	//Mappa di contatori per vedere quali nodi crashano
	private Map<User, Integer> counters;


	/**
	 * Costruttore che si occupa di aprire le connessioni al gruppo di multicast e di avviare:
	 * - Thread per ricezione degli alive (receiverAlive)
	 * - Timer periodico per l'invio degli alive (timerHello)
	 * - Timer periodico per incrementare i contatori (timerIncrements)
	 * @param session
	 * @param controller
	 */
	public CrashDetector(Session session, Controller controller) {
		this.addObserver(controller);
		this.counters = new HashMap<User, Integer>();
		this.session=session;
		try {

			group = InetAddress.getByName(session.getSessionIP());
			socket = new MulticastSocket(Session.port);
			socket.joinGroup(group);

			timerHello = new TimerAlive(group, socket, session);
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(timerHello, 0, SEND_INTERVAL);

			timerIncrements = new TimerIncrement(this);
			Timer timerInc = new Timer(true);
			timerInc.scheduleAtFixedRate(timerIncrements, 0, INCREMENT_INTERVAL);


			receiverAlive = new ReceiverAlive(group, socket, this, session);
			Thread thread = new Thread(receiverAlive);
			thread.start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Inizializzare Timer, TimerTask e Thread
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

	public boolean isAlreadySentElect() {
		return alreadySentElect;
	}

	public void setAlreadySentElect(boolean alreadySentElect) {
		this.alreadySentElect = alreadySentElect;
	}


	/**
	 * Metodo per effettuare l'incremento dei contatori
	 * Quando un contatore arriva a NUM_FAIL_ALIVE, viene notificato il controllore con un evento di CRASH
	 */
	public synchronized void increment(){
		List<User> crashedUsers = new ArrayList<User>();
		for(Map.Entry<User, Integer> entry : counters.entrySet() ){
			entry.setValue(entry.getValue()+1);
			if(entry.getValue()>NUM_FAIL_ALIVE){
				//Se ho avuto più di NUM_FAIL_ALIVE incrementi senza ricevere alive
				//notifico il controller con l'evento di CRASH (specificando l'utente che è crashato)
				crashedUsers.add(entry.getKey());
				Crash crash = new Crash(entry.getKey());
				setChanged();
				notifyObservers(crash);
			}	
		}
		//Tutti gli utenti che sono crashati, vengono rimossi dalla mappa con i contatori
		for(User u : crashedUsers) {
			removeUser(u);
		}
	}

	public void notifyAfterCoordinator(NewLeader event) {
		setChanged(); 
		notifyObservers(event);
	}

	/**
	 * Metodo per resettare il contatore dell'utente corrispondente, viene invocato dal receiverAlive
	 * quando riceve l'alive dal utente corrispondente
	 * @param user
	 */
	public synchronized void reset(User user){
		//counters.remove(user); 
		counters.put(user, 0);
	}

	public synchronized void removeUser(User user){
		counters.remove(user);
	}

	public synchronized void addUser(User user){
		System.out.println(counters);
		counters.put(user, 0);
	}

	/**
	 * Questo metodo viene eseguito quando l'utente inizia la leader election. Viene generato un evento 
	 * "newElectEvent" che contiene una indicazione sull'utente che ha iniziato l'elezione (cioè colui
	 * che sta eseguendo il metodo) e viene inviato a tutti. Poi l'utente fa partire un timer: alla scadenza
	 * del timer, se non ha ricevuto messaggi di stop da altri, diventa il nuovo leader: in tal caso crea un 
	 * evento "coordinator" e lo invia a tutti gli altri utenti. 
	 */
	public void startElect() {

		Elect newElectEvent = new Elect(session.getMyself());
		receiverAlive.sendEvent(newElectEvent);
		alreadySentElect = true;
		checkForElectionConfirm = new Timer(true);
		checkForElectionConfirm.schedule(new TimerTask()  {

			@Override
			public void run() {
				System.out.println("sono il nuovo leader!");
				Coordinator c = new Coordinator(session.getMyself());
				receiverAlive.sendEvent(c); 

			}
		}, 1000);


	}

	public void stopElect(){
		checkForElectionConfirm.cancel();
	}

}

/**
 * Classe implementa il thread che si occupa di ricevere gli alive nel gruppo di multicast
 * Si occupa anche di ricevere i messaggi che vengono spediti per la leader Election (ELECT, COORDINATE, STOP)
 * Nota: ricordarsi che si ricevono anche gli alive spediti dal nodo stesso
 *
 */
class ReceiverAlive implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private CrashDetector cd;
	private Session session;

	public ReceiverAlive(InetAddress group, MulticastSocket socket, CrashDetector cd, Session session){
		this.group=group;
		this.socket=socket;
		this.cd=cd;
		this.session = session;
	}

	@Override
	public void run() {
		while(true){
			byte[] buf = new byte[1000];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			ByteArrayInputStream byteStream;
			ObjectInputStream is;
			try {
				//Mi metto in attesa di un messaggio nel gruppo di multicast
				socket.receive(recv);
				
				GenericEvent eventReceived = null;
				byteStream = new ByteArrayInputStream(buf);
				is = new ObjectInputStream(new BufferedInputStream(byteStream));

				//Deserializzo l'evento
				eventReceived=(GenericEvent) is.readObject();

				//Ora controllo di che evento si tratta
				//EVENTO: ALIVE
				if(eventReceived instanceof Alive){
					//Se l'evento ricevuto è ALIVE, allora devo resettare il counter corrispondente all'utente
					Alive alv = (Alive) eventReceived;
					cd.reset(alv.getAliveUser());
				}

				//EVENTO: ELECT
				if(eventReceived instanceof Elect) {
					//Nella elect c'è l'utente che vuole diventare leader
					Elect elect = (Elect) eventReceived;
					System.out.println(elect.getUser().getId() + ": questo è l'id di chi ha lanciato elect");
					System.out.println(session.getMyself().getId() +": questo è l'id di chi ha ricevuto l'elect, cioè lo user attuale");
					//se un utente riceve una elect da un altro utente e ha un id maggiore del suo, deve stoppare la sua election
					//e eventualmente far partire una nuova election (se non l'ha già fatto)
					if(!elect.getUser().equals(session.getMyself()) && elect.getUser().getId() < session.getMyself().getId()) {
						System.out.println("devo mandare io lo stop per bloccare l'altro");
						//invio stop per fermare l'elezione a quell'utente
						Stop stop = new Stop(elect.getUser());
						sendEvent(stop);
						if(!cd.isAlreadySentElect()) {
							//creo e invio la nuova elezione se non l'ho già iniziata
							cd.setAlreadySentElect(true);
							cd.startElect();
						}
					}
				}

				//EVENTO: STOP
				//Viene ricevuto da un utente che ha iniziato una elezione, ma questa deve essere stoppata, 
				//perchè c'è un altro utente che è attivo e ha un id maggiore del suo
				if(eventReceived instanceof Stop) {
					//Nello stop c'è l'user da stoppare
					Stop stop = (Stop) eventReceived;
					if(stop.getUser().equals(session.getMyself())) {
						cd.stopElect();
					}
				}

				//EVENTO: COORDINATOR
				//Viene ricevuto da un utente quando è terminata la leader election: colui che invia
				//questo evento è il nuovo leader. 
				if(eventReceived instanceof Coordinator) {
					NewLeader nl = new NewLeader(((Coordinator) eventReceived).getNewLeader());
					cd.notifyAfterCoordinator(nl);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Metodo generico per inviare un evento nel gruppo di multicast, serializzo l'evento e lo invio nel gruppo
	 * @param event
	 */
	public synchronized void sendEvent(GenericEvent event) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			//Serializzo l'evento che devo spedire
			oos.writeObject(event);
			baos.toByteArray();
			//Creazione del pacchetto
			DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.port);
			//Spedizione pacchetto
			socket.send(packetedEvent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

/**
 * Classe che implementa il timer per l'incremento dei contatori per verificare quando un utente è crashato
 *
 */
class TimerIncrement extends TimerTask{

	private CrashDetector cd;

	public TimerIncrement(CrashDetector cd){
		super();
		this.cd=cd;
	}

	@Override
	public void run() {
		//chiamo il metodo del crashDetector che si occupa di effettuare l'incremento
		cd.increment();	
	}

}

/**
 * Classe che implementa il timer per l'invio degli ALIVE nel gruppo di multicast
 *
 */
class TimerAlive extends TimerTask{
	private InetAddress group;
	private MulticastSocket socket;
	private Session session;

	public TimerAlive(InetAddress group, MulticastSocket socket, Session session){
		this.group=group;
		this.socket=socket;
		this.session = session;
	}

	@Override
	public void run() {
		try {
			//Genero l'evento ALIVE, settando come utente me stesso
			Alive alv = new Alive(session.getMyself());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			//Serializzo l'evento
			oos.writeObject(alv);
			baos.toByteArray();
			//Creazione del pacchetto
			DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.port);
			//Spedizione pacchetto
			socket.send(packetedEvent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
