package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import controller.Controller;
import events.GenericEvent;
import events.RequestToJoin;
import model.Session;
import model.User;

/**
 * Classe che gestisce le richieste di connessione al Leader
 *
 */
public class NetworkLeaderHandler{
	private Map<User, NetworkHandler> networkMap;
	private ConnectionServer connectionServer;
	private Thread thread;
	
	public NetworkLeaderHandler(Controller controller) throws IOException{
		connectionServer = new ConnectionServer(this, controller);
		thread = new Thread(connectionServer);
		connectionServer.addObserver(controller);
		thread.start();
		networkMap = new HashMap<User, NetworkHandler>();
		
	}
	
	public void addElementToMap(User user, NetworkHandler networkHandler){
		if(!networkMap.containsKey(user))
			networkMap.put(user, networkHandler);
		else{
			System.out.println("ERRORE: utente già nella sessione");
		}
	}
	
	/**
	 * Metodo per inviare l'evento a tutti gli utenti della sessione (inviato dal master)
	 * @param event
	 */
	public void sendToUsers(GenericEvent event){
		for(Map.Entry<User, NetworkHandler> entry : networkMap.entrySet()){
			entry.getValue().send(event);
		}
	}
}


class ConnectionServer extends Observable implements Runnable{

	private ServerSocket serverSocket;
	private NetworkLeaderHandler nlh;
	private Controller controller;
	
	public ConnectionServer(NetworkLeaderHandler nlh, Controller controller) throws IOException{
		super();
		this.nlh=nlh;
		serverSocket = new ServerSocket(Session.portLeader);
		this.controller=controller;
	}
	
	@Override
	public void run() {
		System.out.println("sto per notificare observer1");
		while(!serverSocket.isClosed()){
			try {
				System.out.println("sto per notificare observer2");
				Thread.sleep(10);
				//Attendo una nuova connessione
				Socket newJoiner = serverSocket.accept();
				
				ObjectInputStream ois = new ObjectInputStream(newJoiner.getInputStream());
				RequestToJoin event=null;
				//Attendo il primo evento nel socket (che sarà una join)
				while(true){
					System.out.println("sto per notificare observer3");
					if((event = (RequestToJoin) ois.readObject())!=null){
						nlh.addElementToMap(event.getJoiner(), 
								new NetworkHandler(event.getJoiner().getIp(), controller));
						setChanged();
						System.out.println("sto per notificare observer4");
						notifyObservers(event);
						break;
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
}
