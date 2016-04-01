package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;

import controller.Controller;
import events.GenericEvent;

/**
 * Classe che si occupa di spedire i messaggi da parte dei client al server(leader) e da parte del server(leader)
 * a tutti gli utenti della sessione
 *
 */
public class NetworkHandler {

	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	//Thread che si occupa di ricevere i messaggi nel socket
	private Thread receiverThread;
	private SingleReceiver singleReceiver;


	/**
	 * Costruttore usato per aprire il socket da parte dei client, verso il serverSocket del server (leader)
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
			//Apro il socket verso il leader
			socket = new Socket(ip, controller.getSession().getPortLeader());
			//istanzio gli stream di output e di input
			oos  = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ois  = new ObjectInputStream(socket.getInputStream());
			System.out.println("Ho creato oos, ois");
			
			//Il single receiver si occupa di ricevere i messaggi nel socket
			singleReceiver = new SingleReceiver(ois);
			//Aggiungo l'observer, cioè il controller che verrà notificato quando arriverà un utente
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
	 * Costruttore usato dal serverSocket quando ha ricevuto la richiesta di apertura del socket per aprire il socket
	 * per ricevere e spedire i messaggi
	 * @param oos
	 * @param ois
	 * @param socket
	 * @param controller
	 */
	public NetworkHandler(ObjectOutputStream oos,ObjectInputStream ois, Socket socket, Controller controller){
		this.oos=oos;
		this.ois=ois;
		this.socket=socket;
		
		//istanzio il thread che si occupa di ricever i messaggi
		singleReceiver = new SingleReceiver(ois);
		singleReceiver.addObserver(controller);
		receiverThread = new Thread(singleReceiver);
		System.out.println("Faccio partire il thread di ricezione");
		receiverThread.start();
	}
	
	/**
	 * Metodo che si occupa di spedire un evento nel socket corrispondente all'istanza di questa classe
	 * @param event
	 */
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

	/**
	 * Metodo per chiudere il socket e fermare il thread di ricezione dei messaggi
	 */
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

/**
 * Classe che implementa il thread di ricezione dei messaggi
 * si mette in ascolto e quando arriva un messaggio, notifica l'evento al controllore
 *
 */
class SingleReceiver extends Observable implements Runnable{
	private ObjectInputStream ois;
	private boolean terminate;

	public SingleReceiver(ObjectInputStream ois){
		super();
		this.ois=ois;
		terminate=false;
	}

	/**
	 * Questo metodo permette di stoppare il thread in modo da bloccare la ricezione dei messaggi
	 */
	public void terminate(){
		terminate=true;
	}

	@Override
	public void run() {
		GenericEvent event = null;
		//Continuo ad ascoltare finchè non viene invocato il metodo terminate()
		while(!terminate){
			try {
				System.out.println("----Attendo un evento----");
				//Attendo l'evento e lo deserializzo direttamente, gli stream vengono settati nel costruttore
				if((event = (GenericEvent) ois.readObject()) !=null){
					System.out.println("ricevuto un evento");
					//Quando ricevo un evento notifico il controllore con l'evento correlato
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

