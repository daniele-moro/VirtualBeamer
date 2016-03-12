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
		
		MulticastSocket s = null; 
		List<String> sessionList = new ArrayList<String>();
		int cont =0;
		try {
			String msg = "Hello";
			s = new MulticastSocket(6971);
			s.joinGroup(globalLan);
			DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), globalLan, 6971);
			s.send(hi);
			byte[] buf = new byte[1000];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			s.setSoTimeout(10000);
			try{
				while(true){
					s.receive(recv);
					//Leggere i dati ricevuti dai vari server
					sessionList.add( new String(recv.getData()) + recv.getAddress());
					cont++;
				}
			}catch(SocketTimeoutException e2){
				System.out.println("Exception Raised, Timeout Expired");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		for(String st : sessionList){
			System.out.println(st);
		}
		System.out.println("Numero ricezioni: " + cont);
		
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
