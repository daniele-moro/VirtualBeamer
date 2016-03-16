package network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import events.GenericEvent;
import model.Session;

/**
 * Classe che si occupa di spedire i messaggi nel gruppo di multicast, usando il metodo send possiamo spedire un evento nella rete, cosi che venga effettuato il comando specificato
 * @author m-daniele
 *
 */
public class NetworkSender {
	private Session session;
	private InetAddress group;
	private MulticastSocket socket;
	
	
	public NetworkSender(Session session) throws IOException{
		this.session=session;
		group = InetAddress.getByName(session.getSessionIP());
		socket = new MulticastSocket(Session.port); //Da decidere se la porta la teniamo fissa per tutti o la decidiamo a runtime e la mettiamo in Session
		socket.joinGroup(group);
	}
	
	
	public void send(GenericEvent event) throws IOException{
		//Serializzazione dell'oggetto
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(event);
		baos.toByteArray();
		//Creazione del pacchetto
		DatagramPacket packetedEvent = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, group, Session.port);
		//Spedizione pacchetto
		socket.send(packetedEvent);
	}
	
	public void sendToLeader(GenericEvent event) throws IOException{
		//TODO
	}

}
