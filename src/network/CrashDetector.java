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
 * Classe che invia periodicamente in Multicast un messaggio per dire che è ancora vivo,
 *  inoltre si occupa anche di ricevere questi messaggi e vedere se qualche nodo crasha.
 * @author m-daniele
 *
 */
public class CrashDetector extends Observable{
	private final static int SEND_INTERVAL = 50000;
	private final static int INCREMENT_INTERVAL = 50000;
	private InetAddress group;
	private MulticastSocket socket;
	private final static int NUM_FAIL_ALIVE = 5;
	private TimerAlive timerHello;
	private TimerIncrement timerIncrements;
	private ReceiverAlive receiverAlive;
	private Session session;
	private boolean alreadySentElect = false;
	private Timer checkForElectionConfirm; 

	private Map<User, Integer> counters;


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

	public synchronized void increment(){
		List<User> crashedUsers = new ArrayList<User>();
		for(Map.Entry<User, Integer> entry : counters.entrySet() ){
			entry.setValue(entry.getValue()+1);
			if(entry.getValue()>NUM_FAIL_ALIVE){
				crashedUsers.add(entry.getKey());
				Crash crash = new Crash(entry.getKey());
				setChanged();
				notifyObservers(crash);
			}	
		}
		for(User u : crashedUsers) {
			removeUser(u);
		}
	}

	public void notifyAfterCoordinator(NewLeader event) {
		setChanged(); 
		notifyObservers(event);
	}

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
				socket.receive(recv);
				GenericEvent eventReceived = null;
				byteStream = new ByteArrayInputStream(buf);
				is = new ObjectInputStream(new BufferedInputStream(byteStream));

				eventReceived=(GenericEvent) is.readObject();

				if(eventReceived instanceof Alive){
					Alive alv = (Alive) eventReceived;
					cd.reset(alv.getAliveUser());
				}

				if(eventReceived instanceof Elect) {
					//Nella elect c'è l'utente che vuole diventare leader
					Elect elect = (Elect) eventReceived;
					System.out.println(elect.getUser().getId() + ": questo è l'id di chi ha lanciato elect");
					System.out.println(session.getMyself().getId() +": questo è l'id di chi ha ricevuto l'elect, cioè lo user attuale");
					if(!elect.getUser().equals(session.getMyself()) && elect.getUser().getId() < session.getMyself().getId()) {
						System.out.println("devo mandare io lo stop per bloccare l'altro");
						//invio stop per fermare l'elezione a quell'utente
						Stop stop = new Stop(elect.getUser());
						sendEvent(stop);
						if(!cd.isAlreadySentElect()) {
							//creo e invio la nuova elezione
							cd.setAlreadySentElect(true);
							cd.startElect();
						}
					}
				}

				if(eventReceived instanceof Stop) {
					//Nello stop c'è l'user da stoppare
					Stop stop = (Stop) eventReceived;
					if(stop.getUser().equals(session.getMyself())) {
						cd.stopElect();
					}
				}

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

	public synchronized void sendEvent(GenericEvent event) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
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

class TimerIncrement extends TimerTask{

	private CrashDetector cd;

	public TimerIncrement(CrashDetector cd){
		super();
		this.cd=cd;
	}

	@Override
	public void run() {
		cd.increment();	
	}

}


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
			Alive alv = new Alive(session.getMyself());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
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
