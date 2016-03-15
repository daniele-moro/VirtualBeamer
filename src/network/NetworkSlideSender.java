package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

import model.Session;

public class NetworkSlideSender {
	private Session session;
	private InetAddress group;
	private MulticastSocket socket;
	private int sessionNumber;
	private List<byte[]> packetsList;
	private boolean cont;

	
	public NetworkSlideSender(Session session) throws IOException{
		this.session=session;
		//Apro la sessione per spedire le slide a tutti i nodi della rete
		group = InetAddress.getByName(session.getSessionIP());
		socket = new MulticastSocket(Session.port);
		socket.joinGroup(group);
		sessionNumber=-1;
		cont=false;
		group = InetAddress.getByName(session.getSessionIP());
		socket = new MulticastSocket(Session.port);
		socket.joinGroup(group);
	}
	
	private void sendSlide(){
		sessionNumber++;
		try {
			packetsList = PacketCreator.createPackets(session.getSlides().get(sessionNumber), sessionNumber);
			
			
			for(byte[] elem : packetsList){
				System.out.println("Invio pezzo di pacchetto\n" +  elem);
				DatagramPacket packet = new DatagramPacket(elem, elem.length, group, Session.port);
				socket.send(packet);
			}
			System.out.println("Fine spedizione prima slide");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void sendSlides(){
		for(int i=0; i<session.getSlides().size(); i++){
			cont=false;
			sendSlide();
			while(!cont){
				try {
					System.out.println("Mi addormento");
					//this.wait();
					Thread.sleep(100);
					System.out.println("Mi sveglio");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void setCont(){
		cont=true;
	}
	
	public synchronized void sendMissingPacket(int sequenceNumber){
		DatagramPacket packet = 
				new DatagramPacket(packetsList.get(sequenceNumber), packetsList.get(sequenceNumber).length, group, Session.port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
