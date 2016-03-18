package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import model.Session;

public class LeaderElection {
	InetAddress group; 
	MulticastSocket socket; 
	
	public LeaderElection(Session session) {
		try {
			group = InetAddress.getByName(session.getSessionIP());
			socket = new MulticastSocket(Session.port);
			socket.joinGroup(group);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
