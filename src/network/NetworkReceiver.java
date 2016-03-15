package network;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Observable;

import controller.Controller;
import events.GenericEvent;
import events.SlidePart;
import events.SlidePartData;
import model.Session;

/**
 * Classe che si occupa di ricevere i messaggi dal gruppo di multicast, crea un thread che rimane in ascolto sul gruppo indicato dalla sessione.
 * Il thread può essere stoppato usando il metodo stopReceiving
 * @author m-daniele
 *
 */
public class NetworkReceiver{

	private Thread thReceiver;
	private Receiver receiver;

	public NetworkReceiver(Session session, Controller controller) throws IOException{
		//Creazione del thread di ricezione dei messaggi dal gruppo di multicast
		receiver = new Receiver(session.getSessionIP(), Session.port, true, controller);
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
	private boolean run;

	public Receiver(String ip, int port, boolean run, Controller controller) throws IOException{
		group=InetAddress.getByName(ip);
		socket = new MulticastSocket(port);
		socket.joinGroup(group);
		this.run=run;
		this.addObserver(controller);
	}

	public void setRun(boolean run){
		this.run=run;
	}


	public void run(){
		//Valutare se serve usare il timeout sul socket.receive (settando il setSoTimeout di socket)
		byte[] buf = new byte[100000];
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
				GenericEvent eventReceived = null;
				try{
					byteStream = new ByteArrayInputStream(buf);
					is = new ObjectInputStream(new BufferedInputStream(byteStream));

					eventReceived=(GenericEvent) is.readObject();
					System.out.println("Evento Ricevuto: " + eventReceived.toString());

					setChanged();
					this.notifyObservers(eventReceived);

				}catch(StreamCorruptedException exc){
					//Qui forse stiamo ricevendo l'immagine
					System.out.println("Qui forse stiamo ricevendo l'immagine");
					int SESSION_START = 128;
					int SESSION_END = 64;
					int HEADER_SIZE = 8;

					//Creo l'evento SlidePart
					SlidePart eventSlidePart;
					SlidePartData slice = new SlidePartData();
					byte[] data = recv.getData();

					slice.sessionNumber = (short)(data[1] & 0xff);
					slice.numPack = (short)(data[2] & 0xff);
					slice.maxPacketSize = (int)((data[3] & 0xff) << 8 | (data[4] & 0xff)); // mask the sign bit
					slice.sequenceNumber = (short)(data[5] & 0xff);
					int size = (int)((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask the sign bit
					slice.start=false;
					slice.end= false;
					if((data[0] & SESSION_START) == SESSION_START) {
						slice.start=true;
					}
					if((data[0] & SESSION_END) == SESSION_END){
						slice.end=true;
					}
					System.out.println("Stampo contenuto pacchetto immagine: \n" + size);
					slice.data = new byte[size];
					System.arraycopy(data, HEADER_SIZE, slice.data, 0, size);
					eventSlidePart = new SlidePart(slice);

					//TODO Notifico il controller con l'evento eventSlidePart
					
					setChanged();
					notifyObservers(eventSlidePart);
				}

			}catch(SocketTimeoutException e){
				//System.out.println("------ Timer della receive scaduto-----");
			}catch (IOException e) {
				System.out.println("ERROR: -------- errore nella ricezione del pacchetto o nell bytestream");
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				//TODO QUI non stiamo ricevendo l'immagine

			}

		}

	}
}
