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
import java.util.Map;
import java.util.Observable;
import java.util.TimerTask;

import controller.Controller;
import events.Alive;
import events.Crash;
import events.GenericEvent;
import model.Session;
import model.User;

/**
 * Classe che invia periodicamente in Multicast un messaggio per dire che è ancora vivo,
 *  inoltre si occupa anche di ricevere questi messaggi e vedere se qualche nodo crasha.
 * @author m-daniele
 *
 */
public class CrashDetector extends Observable{
	private InetAddress group;
	private MulticastSocket socket;
	public final static int NUM_FAIL_ALIVE = 5;
	private TimerAlive timerHello;
	private TimerIncrement timerIncrements;
	private ReceiverAlive receiverAlive;
	
	private Map<User, Integer> counters;
	
	
	public CrashDetector(Session session, Controller controller) {
		this.addObserver(controller);
		//Inizializzare Timer, TimerTask e Thread
		
	}
	
	public synchronized void increment(){
		for(Map.Entry<User, Integer> entry : counters.entrySet() ){
			entry.setValue(entry.getValue()+1);
			if(entry.getValue()>NUM_FAIL_ALIVE){
				Crash crash = new Crash(entry.getKey());
				setChanged();
				notifyObservers(crash);
			}
		}
	}
	
	public synchronized void reset(User user){
		counters.remove(user); 
		counters.put(user, 0);
	}

	
	
}

class ReceiverAlive implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private CrashDetector cd;
	
	public ReceiverAlive(InetAddress group, MulticastSocket socket, CrashDetector cd){
		this.group=group;
		this.socket=socket;
		this.cd=cd;
	}
	
	@Override
	public void run() {
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

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
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
