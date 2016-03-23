package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;

import controller.Controller;
import events.GenericEvent;
import model.Session;

/**
 * Classe che si occupa di spedire i messaggi nel gruppo di multicast, usando il metodo send possiamo spedire un evento nella rete, cosi che venga effettuato il comando specificato
 * @author m-daniele
 *
 */
public class NetworkHandler {
//	private InetAddress group;
//	private MulticastSocket socket;
	private PrintWriter output;
	private BufferedReader input;
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private Thread receiverThread;
	private SingleReceiver singleReceiver;
	
	
//	public NetworkSender(Session session) throws IOException{
//		this.session=session;
//		group = InetAddress.getByName(session.getSessionIP());
//		socket = new MulticastSocket(Session.port); //Da decidere se la porta la teniamo fissa per tutti o la decidiamo a runtime e la mettiamo in Session
//		socket.joinGroup(group);
//	}
	
	/**
	 * Costruttore usato per aprire il socket da parte dei client, verso il serverSocket
	 * @param ip
	 * @param controller
	 */
	public NetworkHandler(String ip, Controller controller) {
		System.out.println("Entrato nel Costruttore del NetworkHandler");
		try {
			//aspetto che il server abbia avviato il serverSocket
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			socket = new Socket(ip, controller.getSession().getPortLeader());
			
			oos  = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ois  = new ObjectInputStream(socket.getInputStream());
			System.out.println("Ho creato oos, ois");
			
			singleReceiver = new SingleReceiver(ois);
			singleReceiver.addObserver(controller);
			receiverThread = new Thread(singleReceiver);
			System.out.println("Faccio partire il thread di ricezione");
			receiverThread.start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Costruttore usato dal serverSocket quando ha ricevuto la richiesta di apertura del socket
	 * @param oos
	 * @param ois
	 * @param socket
	 * @param controller
	 */
	public NetworkHandler(ObjectOutputStream oos,ObjectInputStream ois, Socket socket, Controller controller){
		this.oos=oos;
		this.ois=ois;
		this.socket=socket;
		
		singleReceiver = new SingleReceiver(ois);
		singleReceiver.addObserver(controller);
		receiverThread = new Thread(singleReceiver);
		System.out.println("Faccio partire il thread di ricezione");
		receiverThread.start();
	}
	
	
/*	public void send(GenericEvent event) throws IOException{
		//Serializzazione dell'oggetto
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(event);
		baos.toByteArray();
		//Creazione del pacchetto
		DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.port);
		//Spedizione pacchetto
		socket.send(packetedEvent);
	}*/

	public void send(GenericEvent event){
		try {
			oos.flush();
			oos.writeObject(event);
			oos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			System.out.println("-----------------CHIUDO IL SOCKET------------------");
			singleReceiver.terminate();
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
class SingleReceiver extends Observable implements Runnable{
	private ObjectInputStream ois;
	private boolean terminate;
	
	public SingleReceiver(ObjectInputStream ois){
		super();
		this.ois=ois;
		terminate=false;
	}
	
	public void terminate(){
		terminate=true;
	}

	@Override
	public void run() {
		GenericEvent event = null;
		while(!terminate){
			try {
				System.out.println("----Attendo un evento----");
				if((event = (GenericEvent) ois.readObject()) !=null){
					System.out.println("ricevuto un evento");
					setChanged();
					notifyObservers(event);
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println("HO CAPITO TUTTO!!!!!!!!!!--------------------------");
			}
		}
	}
	
}

