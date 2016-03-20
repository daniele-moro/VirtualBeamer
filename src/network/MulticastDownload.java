package network;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.imageio.ImageIO;

import events.Ack;
import events.GenericEvent;
import events.Nack;
import events.SlidePartData;
import model.Session;
import model.User;

/**
 * Classe per spedire e ricevere le slide in multicast
 * @author m-daniele
 *
 */
public class MulticastDownload {

	private InetAddress group;
	private MulticastSocket socket;

	private int sessionNumber;
	private Map<Integer, Map<Byte, byte[]>> sentPacket;
	Session session;
	List<User> ackedUsers;
	private int numSlide;

	private Receiverr receiverr; 

	private boolean sentAll;

	public MulticastDownload(Session session, int numslide, boolean sender){
		try {
			group = InetAddress.getByName(session.getSessionIP());
			socket = new MulticastSocket(Session.portSlide);
			socket.joinGroup(group);
			
			this.sentAll=false;
			this.sentPacket = new HashMap<Integer, Map<Byte, byte[]>>();
			this.numSlide=numslide;
			this.sessionNumber=0;
			this.session=session;
			this.ackedUsers=new ArrayList<User>();

			//genero il thread per ricevere i pacchetti (ACK, NACK, Parti di slide)
			receiverr = new Receiverr(group,socket,sender, this);
			Thread t = new Thread(receiverr);
			t.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//	public void sendEnd(){
	//		//invio l'evento di fine trasmissione slide in mutlicast
	//		EndSlides end = new EndSlides();
	//		receiverr.sendEvent(end);
	//		while(!sentAll){
	//			try {
	//				this.wait();
	//			} catch (InterruptedException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//		}
	//	}



	/**
	 * Metodo per spedire le immagini in multicast
	 * @param bufferedImage
	 * @return True: la slide è stat spedita, False: la slide non è stata spedita perchè sono già state spedite tutte oppure perchè c'è stato un errore
	 */
	public boolean sendSlide(BufferedImage bufferedImage){
		List<byte[]> packets;
		if(numSlide>0){
			try {
				packets = PacketCreator.createPackets(bufferedImage, sessionNumber);
				if(!sentPacket.containsKey(sessionNumber)){
					sentPacket.put(sessionNumber, new HashMap<Byte, byte[]>());
				}
				for(byte[] elem: packets) {

					DatagramPacket  dPacket = new DatagramPacket(elem, elem.length, group, Session.port);
					synchronized(sentPacket){
						socket.send(dPacket);
					}
					sentPacket.get(sessionNumber).put(elem[5], elem);
				}
				sessionNumber++;
				numSlide--;
				if(numSlide==0){
					//Ho finito di inviare le Slide, mi blocco e attendo di ricevere tutti gli ack
					while(!sentAll){
						try {
							this.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return false;
				}
				return true;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return false;
	}

	void sendPacket(short sessionNumber, short sequenceNumber){
		try {
			DatagramPacket  dPacket = new DatagramPacket(sentPacket.get(sessionNumber).get(sequenceNumber), 
					sentPacket.get(sessionNumber).get(sequenceNumber).length, group, Session.port);
			synchronized(sentPacket){
				socket.send(dPacket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private BufferedImage img;
	/**
	 * Metodo bloccante, aspetta la ricezione di una slide
	 * @return
	 */
	public synchronized BufferedImage receive(){
		if(numSlide>0){
			img=null;
			try {
				while(img==null) {
					this.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numSlide--;
			if(numSlide==0){
				//Ho finito di ricevere le immagini, mando l'ACK
				Ack evAck = new Ack(session.getMyself());
				receiverr.sendEvent(evAck);
				//Stoppo il thread in ricezione
				receiverr.setRun(false);
			}
			return img;
		}else {
			return null;
		}

	}

	synchronized void setImg(BufferedImage img){

		this.img=img;

	}
	synchronized void sentAll(){
		sentAll=true;
	}

}




class Receiverr extends Observable implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private boolean run;
	private boolean sender;
	private MulticastDownload md;

	private Map<Short,  List<SlidePartData>> receivedPacket;

	public Receiverr(InetAddress group, MulticastSocket socket, boolean sender, MulticastDownload md){
		this.group=group;
		this.socket = socket;
		this.receivedPacket = new HashMap<Short, List<SlidePartData>>();
		this.sender = sender;
		this.md=md;
	}

	public void setRun(boolean run){
		this.run=run;
	}

	public void sendEvent(GenericEvent event){
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


	public void run(){
		//Valutare se serve usare il timeout sul socket.receive (settando il setSoTimeout di socket)
		byte[] buf = new byte[100000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		ByteArrayInputStream byteStream;
		ObjectInputStream is;
		int k=0;
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

					//qui ricevo l'evento di fine o gli ack e nack
					if(eventReceived instanceof Nack && sender){
						Nack ev = (Nack) eventReceived;
						//ho ricevuto un Nack devo rispedire indietro il pacchetto richiesto
						md.sendPacket(ev.getSessionNumber(), ev.getSequenceNumber());
					}
					if(eventReceived instanceof Ack && sender){
						Ack ackEv = (Ack) eventReceived;
						//devo veder se ho ricevuto  l'ack da tutti
						if(md.ackedUsers.contains(ackEv.getUser())){
							md.ackedUsers.add(ackEv.getUser());
						}
						if(md.ackedUsers.size() == md.session.getJoined().size()){
							//Ho ricevuto l'ack da tutti, ho finito la spedizione.
							md.sentAll();
							md.notifyAll();
						}
					}

				}catch(StreamCorruptedException exc){
					if(!sender){
						//Le immagini mi interessano solo se non sono il sender
						//Qui forse stiamo ricevendo l'immagine
						System.out.println("Qui forse stiamo ricevendo l'immagine");
						int SESSION_START = 128;
						int SESSION_END = 64;
						int HEADER_SIZE = 8;


						//Creo l'evento SlidePart
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
						if(!receivedPacket.containsKey(slice.sessionNumber)){
							receivedPacket.put(slice.sessionNumber, new ArrayList<SlidePartData>());
							for(int i=0; i<slice.numPack; i++){
								receivedPacket.get(slice.sessionNumber).add(null);
							}
						}

						receivedPacket.get(slice.sessionNumber).set(slice.sequenceNumber, slice);

						//Controllo se devo mandare NACK
						if(slice.sequenceNumber-1 >= 0){
							if(receivedPacket.get(slice.sessionNumber).get(slice.sequenceNumber-1) == null){
								//devo inviare il NACK per il pacchetto SeqNum= data[5]-1 e sessionNum=data[1], oppure bisogna usare l'ultimo sequenceN arrivato
								Nack evNack = new Nack(slice.sessionNumber, (byte) (slice.sequenceNumber-1));
								sendEvent(evNack);
							}
						}
						//Controllare se ho finito una slide
						if( receivedPacket.get(k) != null){
							if(!receivedPacket.get(k).contains(null)){
								//Costruisco l'immagine e la aggiungo alla session
								byte[] imageData = new byte[((receivedPacket.get(k).size()-1) * receivedPacket.get(k).get(0).maxPacketSize) + receivedPacket.get(k).get(receivedPacket.get(k).size()-1).data.length];
								for(SlidePartData part : receivedPacket.get(k)){
									System.arraycopy(part.data, 0, imageData, part.sequenceNumber*part.maxPacketSize , part.data.length);
								}
								k++;
								ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
								BufferedImage image = ImageIO.read(bis);
								md.setImg(image);
								md.notifyAll();
							}
						}
					}
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
