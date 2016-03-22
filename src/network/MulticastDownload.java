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
import java.util.Timer;
import java.util.TimerTask;

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
	private Map<Integer, Map<Integer, byte[]>> sentPacket;
	private Session session;
	//List<User> ackedUsers;
	private int numSlide;

	private Receiverr receiverr; 

	private boolean sentAll;

	private BufferedImage img;

	List<User> notAckedUsers;

	/**
	 * Costruttore della classe, bisogna passargli l'istanza della sessione (quindi il model), il numero totale da inviare
	 *  e un booleano che segnala se l'utente che istanzia la classe è chi spedisce o meno.
	 * @param session
	 * @param numslide
	 * @param sender
	 */
	public MulticastDownload(Session session, int numslide, boolean sender, List<User> receivers){
		try {
			group = InetAddress.getByName(session.getSessionIP());
			socket = new MulticastSocket(Session.portSlide);
			socket.joinGroup(group);
			this.notAckedUsers = receivers;
			this.sentAll=false;
			this.sentPacket = new HashMap<Integer, Map<Integer, byte[]>>();
			this.numSlide=numslide;
			this.sessionNumber=0;
			this.session=session;

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



	public int getNumSlide() {
		return numSlide;
	}
	
	public void generatePackets(List<BufferedImage> images){
		List<byte[]> packets;
		if(numSlide!=images.size()){
			System.out.println("Errore nel numero di slide passate");
			return;
		}
		if(numSlide>0){
			try {
				sentPacket = new HashMap<Integer, Map<Integer, byte[]>>();
				//Inserisco le immagini in memoria covnertite in pacchetti pronti da inviare
				System.out.println("inserisco le immagini spacchettate in memoria");
				for(BufferedImage elem: images){
					
						packets = PacketCreator.createPackets(elem, sessionNumber);
					
					
					sentPacket.put(sessionNumber, new HashMap<Integer, byte[]>());
					
					for(int i =0; i<packets.size(); i++){
						System.out.println("SessionNumber: "+sessionNumber + " SequenceNum: "+ i + " "+ packets.get(i));
						sentPacket.get(sessionNumber).put(i,packets.get(i));
					}
					sessionNumber++;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	public boolean sendSlides(){
//		List<byte[]> packets;
//		if(numSlide!=images.size()){
//			System.out.println("Errore nel numero di slide passate");
//			return false;
//		}
//		if(numSlide>0){
//			try {
//				sentPacket = new HashMap<Integer, Map<Integer, byte[]>>();
//				//Inserisco le immagini in memoria covnertite in pacchetti pronti da inviare
//				System.out.println("inserisco le immagini spacchettate in memoria");
//				for(BufferedImage elem: images){
//					packets = PacketCreator.createPackets(elem, sessionNumber);
//					
//					sentPacket.put(sessionNumber, new HashMap<Integer, byte[]>());
//					
//					for(int i =0; i<packets.size(); i++){
//						System.out.println("SessionNumber: "+sessionNumber + " SequenceNum: "+ i + " "+ packets.get(i));
//						sentPacket.get(sessionNumber).put(i,packets.get(i));
//					}
//					sessionNumber++;
//				}
if(sentPacket.size()==numSlide){
				//Invio i pacchetti
				for(int i = 0; i<numSlide; i++){
					for(Map.Entry<Integer, byte[]> entry : sentPacket.get(i).entrySet() ){
						System.out.println("Invio pacchetto session: " + i + " Sequence: " + entry.getKey());
						//Spedisco il pacchetto selezionando il sequence e il sessionNumber corrispondendi
						//al pacchetto appena aggiunto alla hashMap
						this.sendPacket(i, entry.getKey());
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				synchronized(this){
					while(!sentAll){
						try {
							this.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				receiverr.setRun(false);
				numSlide=0;
				return false;
}
return false;
//
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				return true;
//			}
//		}
//		return false;


	}


	/**
	 * Metodo per spedire le immagini in multicast, genera i pacchetti a partire da una BufferedImage (tramite il metodo createPackets di PacketCreator)
	 * e li spedisce in multicast usando il metodo sendPacket, incrementa il sessionNumber che rappresenta l'id dell'immagine
	 * se sono state spedite tutte le immagini alla si mette in attesa che tutti gli ACK dai client arrivino correttamente
	 * Il metodo quindi si blocca solo nel caso in cui stiamo inviando l'ultima immagine
	 * Il metodo torna true se l'immagine è stata spedita e ci sono altre immagini da spedire, torna false quando tutte le immagini sono state spedite
	 * @param bufferedImage
	 * @return True: la slide è stat spedita, False: la slide non è stata spedita perchè sono già state spedite tutte oppure perchè c'è stato un errore
	 */
//	public boolean sendSlide(BufferedImage bufferedImage){
//		List<byte[]> packets;
//		if(numSlide>0){
//			try {
//				packets = PacketCreator.createPackets(bufferedImage, sessionNumber);
//				if(!sentPacket.containsKey(sessionNumber)){
//					sentPacket.put(sessionNumber, new HashMap<Integer, byte[]>());
//				}
//				for(byte[] elem: packets) {
//					System.out.println("Invio pacchetto session: " + (int) elem[1] + " Sequence: " + (int) elem[5]);
//					//Aggiungo il paccchetto alla hashMap
//					sentPacket.get(sessionNumber).put((int) elem[5], elem);
//					//Spedisco il pacchetto selezionando il sequence e il sessionNumber corrispondendi
//					//al pacchetto appena aggiunto alla hashMap
//					this.sendPacket((int)elem[1], (int)elem[5]);
//
//					//					DatagramPacket  dPacket = new DatagramPacket(elem, elem.length, group, Session.portSlide);
//					//					synchronized(sentPacket){
//					//						socket.send(dPacket);
//					//					}
//
//				}
//				sessionNumber++;
//				numSlide--;
//				if(numSlide==0){
//					//Ho finito di inviare le Slide, mi blocco e attendo di ricevere tutti gli ack
//					synchronized(this){
//						while(!sentAll){
//							try {
//								this.wait();
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//					}
//					receiverr.setRun(false);
//					return false;
//				}
//				return true;
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
//		return false;
//	}

	/**
	 * Metodo per spedire un pacchetto contenuto nella HashMap, viene passato il sessionNumber e il sequenceNumber
	 * del pacchetto da spedire
	 * @param sessionNumber
	 * @param sequenceNumber
	 */
	void sendPacket(int sessionNumber, int sequenceNumber){
		try {
			synchronized(this){
			DatagramPacket  dPacket = new DatagramPacket(sentPacket.get(sessionNumber).get(sequenceNumber), 
					sentPacket.get(sessionNumber).get(sequenceNumber).length, group, Session.portSlide);
			
				socket.send(dPacket);

			}
			System.out.println("Spedito pacchetto session: "+sessionNumber + " sequence: "+sequenceNumber);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Metodo bloccante, aspetta la ricezione di una slide, la ricezione viene effettuata in ordine a seconda del sessionNumber
	 * Il metodo quindi si mette in Wait finchè il thread di ricezione delle immagini non effettua la notifyAll per svegliarlo 
	 * (il quale prima setta l'immagine appena letta)
	 * @return
	 */
	public synchronized BufferedImage receive(){
		if(numSlide>0){
			try {
				while(img==null) {
					this.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numSlide--;
			System.out.println("NUMSLIDE RIMANENTI: " +numSlide);
			if(numSlide==0){
				System.out.println("INVIO L'ACK");
				//Ho finito di ricevere le immagini, mando l'ACK
				Ack evAck = new Ack(session.getMyself());
				receiverr.sendEvent(evAck);
				//Stoppo il thread in ricezione
				receiverr.setRun(false);
			}
			BufferedImage img2 = img;
			img=null;
			return img2;
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



/**
 *  Classe che implementa il thread di ricezione dei messaggi nel gruppo di multicast per l'invio delle slide
 *  quando viene avviato il thread correlato, si mette in attesa di messaggi e li interpreata eseguendo le azioni correlate
 * @author m-daniele
 *
 */
class Receiverr extends Observable implements Runnable{
	private InetAddress group;
	private MulticastSocket socket;
	private boolean run;
	private boolean sender;
	private MulticastDownload md;
	private TimerReceiveSlide trs;
	private Timer timer;
	private int numSlide;

	protected Map<Integer, List<SlidePartData>> getReceivedPacket() {
		return receivedPacket;
	}


	private Map<Integer,  List<SlidePartData>> receivedPacket;

	/**
	 * Costruttore della classe, si occupa di istanziare tutti gli oggetti necessari al thread.
	 *  se l'utente non è il sender dovrò istanziare la struttura dati per memorizzare i dati ricevuti
	 * @param group
	 * @param socket
	 * @param sender
	 * @param md
	 */
	public Receiverr(InetAddress group, MulticastSocket socket, boolean sender, MulticastDownload md){
		this.numSlide=md.getNumSlide();
		this.group=group;
		this.socket = socket;
		this.receivedPacket = new HashMap<Integer, List<SlidePartData>>();
		this.sender = sender;
		this.md=md;
		this.run=true;
		if(!sender) {
			for(int i = 0; i < numSlide; i++) {
				receivedPacket.put(i, new ArrayList<SlidePartData>());
			}
//			trs = new TimerReceiveSlide(this);
//			timer = new Timer(true); 
//			timer.scheduleAtFixedRate(trs, 2000, 1000);
		}
	}

	public void setRun(boolean run){
		if(!sender){
			trs.cancel();
		}
		this.run=run;
	}

	/**
	 * Metodo che si occupa di serializzare e inviare in multicast un qualsiasi evento
	 * @param event
	 */
	public void sendEvent(GenericEvent event){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(event);
			baos.toByteArray();
			//Creazione del pacchetto
			DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.portSlide);
			//Spedizione pacchetto
			socket.send(packetedEvent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Metodo che viene avviato all'avvio del thread, si mette in ascolto di tutti i messaggi che passano nel gruppo di multicast
	 * Ricevuto un messaggio tenta di deserializzarlo(perchè potrebbe essere un evento, a cui è interessato solo il sender (gli eventi sono NACK o ACK)), 
	 * in questo caso effettua ciò che è correlato all'evento.
	 * Se invece la deserializzazione fallisce, allora sto ricevendo un pezzo di immagine e quindi (se non sono il sender) la inserisco nella struttura dati dei dati ricevuti
	 * nel caso in cui abbia ricevuto un immagine completa allora posso notificare la classe superiore (in particolare il metodo receive) che ho ricevuto un immagine
	 */
	public void run(){
		//Valutare se serve usare il timeout sul socket.receive (settando il setSoTimeout di socket)
		byte[] buf = new byte[50000000];
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
			System.out.println("RUN: " + run);
			try {
				System.out.println("ATTENDO RICEZIONE PACCHETTO (ACK, NACK o IMAGE) 35463746374-------------------------------------");
				socket.receive(recv);
				System.out.println("Port:" + recv.getPort() + "Address: " + recv.getAddress() + "SocketAddress: " + recv.getSocketAddress());
				GenericEvent eventReceived = null;
				try{
					byteStream = new ByteArrayInputStream(buf);
					is = new ObjectInputStream(new BufferedInputStream(byteStream));

					eventReceived=(GenericEvent) is.readObject();
					System.out.println("Evento Ricevuto: " + eventReceived.toString());

					//qui ricevo l'evento di fine o gli ack e nack

					if(sender && eventReceived instanceof Nack){
						//sono il sender e ho ricevuto un Nack devo rispedire indietro il pacchetto richiesto
						System.out.println("Ho ricevuto un NACK");
						Nack ev = (Nack) eventReceived;
						System.out.println("PAchetto richiesto da NACK: Session:" +ev.getSessionNumber() + " Sequence: " + ev.getSequenceNumber());
						md.sendPacket(ev.getSessionNumber(), ev.getSequenceNumber());
						System.out.println("Ho spedito il pacchetto NACKATO");
					}
					if(sender && eventReceived instanceof Ack){
						//Sono il sender e ho ricevuto un ACK, devo aggiungerlo agli utenti che mi hanno ackato
						//e verificare se tutti gli utenti mi hanno inviato l'ACK
						System.out.println("HO RICEVUTO UN ACK");
						Ack ackEv = (Ack) eventReceived;
						//devo veder se ho ricevuto  l'ack da tutti

						if(md.notAckedUsers.contains(ackEv.getUser())){
							md.notAckedUsers.remove(ackEv.getUser());
						}
						if(md.notAckedUsers.isEmpty()){
							//Ho ricevuto l'ack da tutti, ho finito la spedizione.
							System.out.println("Ho ricevuto l'ACK da tutti!!");
							md.sentAll();
							synchronized(md){
								md.notifyAll();
							}
						}
					}

				}catch(StreamCorruptedException exc){
					System.out.println("Sto ricevendo un pezzo di immagine");
					if(!sender){
//						trs = new TimerReceiveSlide(this);
//						timer.cancel();
//						timer = new Timer();
//						timer.schedule(trs, 3000);
						if(timer==null){
							trs= new TimerReceiveSlide(this);
							timer = new Timer();
							timer.scheduleAtFixedRate(trs, 1000, 2000);
						}
						System.out.println("ricevuto pezzo di immagine");
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
						System.out.println("pacchetto ricevuto: session number = " + slice.sessionNumber + ", sequenceNumber = " + slice.sequenceNumber);
						System.out.println("Stampo contenuto pacchetto immagine: \n" + size);
						slice.data = new byte[size];
						System.arraycopy(data, HEADER_SIZE, slice.data, 0, size);
						System.out.println("SESSION NUMBER: "+ slice.sessionNumber);
						if(receivedPacket.get((int)slice.sessionNumber).size()==0){
							System.out.println("RIempio il vettore con NULL");
							for(int i=0; i<slice.numPack; i++){
								receivedPacket.get((int)slice.sessionNumber).add(null);
							}
						}

						receivedPacket.get((int)slice.sessionNumber).set(slice.sequenceNumber, slice);

						//Controllo se devo mandare NACK
						//TODO -1 dello short?????
//						if(slice.sequenceNumber-1 >= 0){
//							if(receivedPacket.get((int)slice.sessionNumber).get(slice.sequenceNumber-1) == null){
//								System.out.println("INVIO NACK Session: "+ (int) slice.sessionNumber + "Sequence: " + (slice.sequenceNumber-1));
//								//devo inviare il NACK per il pacchetto SeqNum= data[5]-1 e sessionNum=data[1], oppure bisogna usare l'ultimo sequenceN arrivato
//								Nack evNack = new Nack((int) slice.sessionNumber, (int) (slice.sequenceNumber-1));
//								sendEvent(evNack);
//							}
//						}
//						for(int i=k; i<=slice.sessionNumber; i++){
//							int j=0;
//							for(SlidePartData elem: receivedPacket.get(i) ){
//								if((i<slice.sessionNumber || (i==slice.sessionNumber && j<slice.sequenceNumber)) && elem==null){
//									Nack evNack = new Nack(i,j);
//									sendEvent(evNack);
//								}
//								j++;
//							}
//						}
						
						
						
						//Controllare se ho finito una slide
						while(k < numSlide && receivedPacket.get(k).size()>0 && !receivedPacket.get(k).contains(null)){
							System.out.println("FINITO UNA SLIDE?");
							//if(!receivedPacket.get(k).contains(null)){
							System.out.println("Una slide è finita!!!  k: "+ k);
							//Costruisco l'immagine e la aggiungo alla session
							byte[] imageData = new byte[((receivedPacket.get(k).size()-1) * receivedPacket.get(k).get(0).maxPacketSize) + receivedPacket.get(k).get(receivedPacket.get(k).size()-1).data.length];
							for(SlidePartData part : receivedPacket.get(k)){
								System.arraycopy(part.data, 0, imageData, part.sequenceNumber*part.maxPacketSize , part.data.length);
							}
							k++;
							ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
							BufferedImage image = ImageIO.read(bis);
							md.setImg(image);
							synchronized(md){
								md.notifyAll();
							}
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//}
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
				System.out.println("CLASS NOT FOUND EXEC");
			}
		}
		if(run==false){
			//Esco dalla esecuzione del multicastDownload Receiverr-----
			socket.close();
		}
	}
}

class TimerReceiveSlide extends TimerTask{

	private Receiverr r; 

	public TimerReceiveSlide(Receiverr r) {

		this.r=r;

	}


	@Override
	public void run() {
		for(Map.Entry<Integer, List<SlidePartData>> entry : r.getReceivedPacket().entrySet()){
			if(entry.getValue().size()==0) {
				//non mi sono arrivati pacchetti di questa slide
				Nack nack = new Nack(entry.getKey(), 0); 
				r.sendEvent(nack);
			} else if (entry.getValue().contains(null)){
				int i = 0; 
				for(SlidePartData el : entry.getValue()){
					if(el == null) {
						System.out.println("invio NACK Session: "+entry.getKey() + " Sequence: "+i);
						Nack nack = new Nack(entry.getKey(), i); 
						r.sendEvent(nack);
					}
					i++;
				}
			}
		}



	}


}
