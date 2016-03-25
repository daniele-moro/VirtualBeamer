package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import controller.Controller;
import events.GenericEvent;
import events.RequestToJoin;
import model.User;

/**
 * Classe che gestisce le richieste di connessione al Leader, si occupa di gestire il SocketServer e di aprire i socket
 * verso i client che ne fanno richiesta
 *
 */
public class NetworkLeaderHandler{
	//Attributo che mantiene l'associaizone tra utente e socket corrispondente (in particolare con il NetworkHandler)
	private Map<User, NetworkHandler> networkMap;
	//Thread che implementa il ServerSocket che aspetta le richieste di connessioni
	private ConnectionServer connectionServer;
	private Thread thread;
	
	/**
	 * Costruttore che si occupa di istanziare il thread di ricezione delle richieste di connessione
	 * e di generare la networkMap
	 * @param controller
	 */
	public NetworkLeaderHandler(Controller controller){
		try {
			//Istazio il thread di ricezione delle richieste di connessione
			connectionServer = new ConnectionServer(this, controller);
			thread = new Thread(connectionServer);
			connectionServer.addObserver(controller);
			thread.start();
			
			//Istanzio la mappa (per adesso vuota)
			networkMap = new HashMap<User, NetworkHandler>();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Metodo per aggiungere un nuovo utente alla mappa dei connessi
	 * @param user
	 * @param networkHandler
	 */
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
	
	/**
	 * Metodo per inviare un evento ad un utente specifico (non a tutti i partecipanti della session)
	 * Utile per inviare l'evento per dire ad un client di spedire le slide ad un altro nuovo utente
	 * @param user
	 * @param event
	 */
	public void sendToUser(User user, GenericEvent event){
		networkMap.get(user).send(event);
	}
	

	/**
	 * Metodo per chiudere tutti i socket verso i vari client
	 */
	public void closeOldSockets() {
		for(Map.Entry<User, NetworkHandler> entry : networkMap.entrySet()){
			System.out.println("SOno il vecchio leader, chiudo il socket di: "+entry.getKey().getName());
			entry.getValue().close();
		}
		connectionServer.terminate();
	}
	
	/**
	 * Metodo per rimuovere un solo socket, utile nel caso in cui un utente crasha
	 * @param user
	 */
	public void removeSocket(User user){
		networkMap.get(user).close();
		networkMap.remove(user);
	}
}


/**
 * Classe che implementa il thread che accetta le richieste di connessione
 * Nota: un utente quando apre il socket, la prima cosa che fa è inviare un evento (RequestToJoin), così si
 * fa identificare dal server, anche se non è veramente un nuovo utente
 * (infatti la richeista di apertura di socket può avvenire anche quando c'è il crash del leader (e quindi c'è una leader Electio)
 * o quando viene passata la leaderShip ad un altro utente)
 *
 */
class ConnectionServer extends Observable implements Runnable{

	private ServerSocket serverSocket;
	private NetworkLeaderHandler nlh;
	private Controller controller;
	
	/**
	 * Costruttore che istanzia il serverSocket che si occupa di attendere e accettare le richieste di connessione
	 * @param nlh
	 * @param controller
	 * @throws IOException
	 */
	public ConnectionServer(NetworkLeaderHandler nlh, Controller controller) throws IOException{
		super();
		this.nlh=nlh;
		serverSocket = new ServerSocket(controller.getSession().getPortLeader());
		this.controller=controller;
	}
	
	/**
	 * Metodo per bloccare e terminare il serverSocket, utile quando devo passare la leadership a qualche altri utente
	 */
	public void terminate(){
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		System.out.println("sto per notificare observer1");
		//Qui attendo le richieste di connessione, naturalmente il thread continua l'esecuzione 
		// finchè non viene chiuso il serverSocket
		while(!serverSocket.isClosed()){
			try {
				System.out.println("sto per notificare observer2");
				Thread.sleep(10);
				//Attendo una nuova connessione
				Socket newJoiner = serverSocket.accept();
				
				System.out.println("---ACCETTATA CONNESSIONE----");
				//Istanzio gli stream del nuovo socket
				ObjectOutputStream oos = new ObjectOutputStream(newJoiner.getOutputStream());
				oos.flush();
				ObjectInputStream ois = new ObjectInputStream(newJoiner.getInputStream());
				RequestToJoin event=null;
				//Attendo il primo evento nel socket (che sarà una RequestToJoin)
				while(true){
					System.out.println("sto per notificare observer3");
					//attendo la ricezione dell'evento che so mi verrà inviato
					if((event = (RequestToJoin) ois.readObject())!=null){
						//Quando ricevo l'evento, istanzio il NetworkHandler corrispondente all'utente che mi ha inviato l'evento
						//Cosi facendo ora ho la possibilità di contattare l'utente e potergli inviare eventi e messaggi
						nlh.addElementToMap(event.getJoiner(), 
								new NetworkHandler(oos, ois, newJoiner, controller));
						//Notifico il controller per dire che un nuovo utente ha fatto richesta di join
						//Questo metodo viene usato anche quando c'è il crash del leader o il passaggio di leadership
						//per aprire i nuovi socket che mettono in comunicazione il nuovo leader con tutti gli utenti
						//della sessione
						//Sarà il controller a verificare se l'utente che ha inviato la RequestToJoin è già utente della sessione
						// o è un nuovo utente
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
				System.out.println("IOEXCEPTION in serverSocket");
				//e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
}
