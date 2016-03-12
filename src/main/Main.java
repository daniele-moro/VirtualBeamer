package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import controller.Controller;
import model.Session;
import model.User;

public class Main {

	public static void main(String[] args) {
		
		Controller controller;
		Session session;
		User user=null;;
		
		String userName;
		String sessionName;
		InetAddress group = null;
		InetAddress globalLan = null;
		
		try {
			globalLan = InetAddress.getByName("228.5.6.8");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		//CREO tutte le istanze che mi servono per far funzionare il gioco
		JFrame frame = new JFrame();
		
		String[] optionsStart = {"NEW", "JOIN"};
		int choice = JOptionPane.showOptionDialog(frame,
				"",
				"JOIN OR NEW",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				optionsStart,
				optionsStart[0]);
		
		userName = (String)JOptionPane.showInputDialog(
				frame,
				"Insert your name",
				"",
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				"");	
		
		List<Temp> sessionList = null;
		try {
			sessionList = sendHello();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(Temp tmp : sessionList){
			System.out.println("IP: " + tmp.ip + "NOME: "+ tmp.nome);
		}
		
		switch(choice){
		case 0: //NEW--> MASTER!!!
			sessionName = (String)JOptionPane.showInputDialog(
					frame,
					"Insert session name",
					"",
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					"");
			
			//Manca il Pathv dove è presente la presentazione (i file jpg)
			
			try {
				user = new User(userName, InetAddress.getLocalHost().getHostAddress(), "0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(user.getName() + " "+ user.getIp());
			//session = new Session(user, "AAA", sessionName);
			
			
			
			break;
		case 1: //JOIN--> CLIENT!!!
			
			break;
			
		}
		
	}
	/**
	 * Metodo per la spedizione dell'HELLO nella rete multicast riservata a pubblicizzare le sessioni
	 * @return ritorna la lista delle sessioni attualmente disponibili
	 * @throws IOException
	 */
	static public List<Temp> sendHello() throws IOException{
		InetAddress globalLan = InetAddress.getByName(Session.ipHello);
		List<Temp> sessionList = new ArrayList<Temp>();
		String msg = "HELLO";
		MulticastSocket socket = new MulticastSocket(Session.portHello);
		socket.joinGroup(globalLan);
		int cont;
		DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), globalLan, Session.portHello);
		socket.send(hi);
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		socket.setSoTimeout(10000);
		try{
			while(true){
				socket.receive(recv);
				//Leggere i dati ricevuti dai vari server
				String dataReceived = new String(recv.getData());
				String[] splitted = dataReceived.split(",");
				if(splitted[0]=="REPLY"){
					sessionList.add(new Temp(splitted[1], splitted[2]));
				}
			}
		}catch(SocketTimeoutException e2){
			System.out.println("Exception Raised, Timeout Expired");
		}
		return sessionList;
		
	}
	
	static public String generateIp(List<String> alreadyUsedIp) {
	    List<Integer> lastElements = new ArrayList<Integer>(); 
	    for(String el : alreadyUsedIp) {
	      System.out.println(el);
	      String[] four = el.split("\\.");
	      lastElements.add(Integer.parseInt(four[3]));
	    }
	    int number; 
	    boolean alreadyPresent = false;
	    do{
	      number = 1+(int)(Math.random()*253);
	      for(Integer el : lastElements) {
	        if(number == el) {
	          alreadyPresent = true;
	        }
	      }
	    } while(alreadyPresent);
	    for(String el : alreadyUsedIp) {
	      String[] four = el.split("\\.");
	      System.out.println("" + four[0] + "." + four[1] + "." + four[2] + "." + (number + 1));
	      return "" + four[0] + "." + four[1] + "." + four[2] + "." + (number + 1);
	    }
	    return null;
	  }
}

class Temp{
	String ip;
	String nome;
	
	public Temp(String ip, String nome){
		this.nome=nome;
		this.ip=ip;
	}
}
