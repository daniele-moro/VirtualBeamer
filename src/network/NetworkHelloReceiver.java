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
 * @author m-daniele
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

class HelloReceiver extends Observable implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private boolean run;
	private Session session;
	private int port;

	public HelloReceiver(String ip, int port, boolean run, Session session) throws IOException{
		this.session = session;
		group=InetAddress.getByName(ip);
		socket = new MulticastSocket(port);
		socket.joinGroup(group);
		this.run=run;
		this.port=port;
	}

	public void setRun(boolean run){
		this.run=run;
	}

	public void run(){
		//Valutare se serve usare il timeout sul socket.receive (settando il setSoTimeout di socket)
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
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
				String dataReceived = new String(recv.getData());
				System.out.println("DATA: " + dataReceived);
				
				if(dataReceived.contains("hello")){
					System.out.println("Spedisco risposta ad HELLO");
					HelloReply eventReply = new HelloReply(session);
					
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(eventReply);
					baos.toByteArray();
					//Creazione del pacchetto
					DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.portHello);
					socket.send(packetedEvent);
					
				} else {
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

	}
}
