package network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Observable;

import events.HelloReply;
import model.Session;

/**
 * Classe per ricevere e gestire l'HELLO ricevuto nella rete multicast dedicata alla pubblicizzazione delle sessioni diponibili
 * Questa classe viene istanziata solo dal leader il quale sarà responsabile di rispondere ai messaggi degli utenti
 *  che vogliono scoprire quali sessioni sono disponibili
 *
 */
public class NetworkHelloReceiver {
	private Thread thReceiver;
	private HelloReceiver receiver;

	public NetworkHelloReceiver(Session session) throws IOException{
		//Creazione del thread di ricezione dei messaggi dal gruppo di multicast
		receiver = new HelloReceiver(Session.ipHello, Session.portHello, true, session);
		thReceiver = new Thread(receiver);
		thReceiver.start();
	}

	public void stopReceiving(){
		receiver.setRun(false);
	}

}

/**
 * Classe che implementa il thread di ricezione degli HELLO (messaggi per scoprire quali sessioni sono disponibili
 *
 */
class HelloReceiver extends Observable implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private boolean run;
	private Session session;

	/**
	 * Costruttore che si occupa di aprire il socket del gruppo di multicast
	 * @param ip
	 * @param port
	 * @param run
	 * @param session
	 * @throws IOException
	 */
	public HelloReceiver(String ip, int port, boolean run, Session session) throws IOException{
		this.session = session;
		group=InetAddress.getByName(ip);
		socket = new MulticastSocket(port);
		socket.joinGroup(group);
		this.run=run;
	}

	/**
	 * Questo metodo permette di stoppare il thread di ricezione e risposta agli HELLO
	 * @param run
	 */
	public void setRun(boolean run){
		this.run=run;
	}

	public void run(){
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		try {
			//Setto il timeout, ogni 500ms, anche se non ricevo un messaggio nel gruppo, la socket.receive() si sblocca
			socket.setSoTimeout(500);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(run){
			try {
				//Attendo la ricezione di un messsaggio, il timeout, la fa sbloccare anche se non ha ricevuto veramente un messaggio
				socket.receive(recv);
				System.out.println("Port:" + recv.getPort() + "Address: " + recv.getAddress() + "SocketAddress: " + recv.getSocketAddress());
				//Leggo il messaggio (non sono usati eventi serializzati per la scoperta delle sessioni)
				String dataReceived = new String(recv.getData());
				System.out.println("DATA: " + dataReceived);
				
				//----HELLO-------
				if(dataReceived.contains("hello")){
					//Se il messaggio è un hello, allora devo gestirlo, e rispondere con la sessione di cui sono il leader
					System.out.println("Spedisco risposta ad HELLO");
					//Creo l'evento di HelloReply
					HelloReply eventReply = new HelloReply(session);
					//Istanzio gli stream
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					//Serializzo l'evento
					oos.writeObject(eventReply);
					baos.toByteArray();
					//Creazione del pacchetto
					DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.portHello);
					//Spedisco, sempre in multicast, la risposta alla richiesta di HELLO
					socket.send(packetedEvent);
				} else {
					//In questo caso sto ricevento la mia stessa risposta (HelloReply), oppure quella degli altri leader di altre sessioni
					//Non devo fare niente, stampo solo per debug
					System.out.println("FROM: " + recv.getSocketAddress());
				}

			}catch(SocketTimeoutException e){
				//System.out.println("------ Timer della receive scaduto-----");
			}catch (IOException e) {
				System.out.println("ERROR: -------- errore nella ricezione del pacchetto o nell bytestream");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if(run==false){
			socket.close();
		}

	}
}
