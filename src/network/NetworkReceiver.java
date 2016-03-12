package network;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Observable;

import events.GenericEvent;
import model.Session;

/**
 * Classe che si occupa di ricevere i messaggi dal gruppo di multicast, crea un thread che rimane in ascolto sul gruppo indicato dalla sessione.
 * Il thread può essere stoppato usando il metodo stopReceiving
 * @author m-daniele
 *
 */
public class NetworkReceiver {
	
	private Session session;
	Thread thReceiver;
	Receiver receiver;
	
	public NetworkReceiver(Session session) throws IOException{
		this.session=session;
		//Creazione del thread di ricezione dei messaggi dal gruppo di multicast
		receiver = new Receiver(session.getSessionIP(), Session.port, true);
		thReceiver = new Thread(receiver);
		thReceiver.start();
	}
	
	public void stopReceiving(){
		receiver.setRun(false);
	}

}

class Receiver extends Observable implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	boolean run;
	
	public Receiver(String ip, int port, boolean run) throws IOException{
		group=InetAddress.getByName(ip);
		socket = new MulticastSocket(Session.port);
		socket.joinGroup(group);
		this.run=run;
	}
	
	public void setRun(boolean run){
		this.run=run;
	}
	
	
	public void run(){
		//Valutare se serve usare il timeout sul socket.receive (settando il setSoTimeout di socket)
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		ByteArrayInputStream byteStream;
		ObjectInputStream is;
		try {
			socket.setSoTimeout(500);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(run){
			try {
				socket.receive(recv);
				System.out.println("Port:" + recv.getPort() + "Address: " + recv.getAddress() + "SocketAddress: " + recv.getSocketAddress());
				GenericEvent eventReceived;
				byteStream = new ByteArrayInputStream(buf);
				is = new ObjectInputStream(new BufferedInputStream(byteStream));
				
				eventReceived=(GenericEvent) is.readObject();
				
				//TODO ora devo generare l'evento per l'observer che deve essere svegliato
				/*... notify....*/
				
			}catch(SocketTimeoutException e){
				System.out.println("------ Timer della receive scaduto-----");
			}catch (IOException e) {
				System.out.println("ERROR: -------- errore nella ricezione del pacchetto o nell bytestream");
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("ERROR: --------OGGETTO RICEVUTO NON è EVENTO--------");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}
}
