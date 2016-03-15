package main;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.sun.medialib.mlib.Image;

import controller.Controller;
import events.GenericEvent;
import events.HelloReply;
import events.Join;
import model.Session;
import model.User;
import network.NetworkHelloReceiver;
import network.NetworkReceiver;
import view.Gui;
import view.OpenFile;

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

		List<Session> sessionList = null;
		try {
			sessionList = sendHello();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int cont=0;
		System.out.println(sessionList.size());
		for(Session elem : sessionList){
			cont++;
			System.out.println(cont+") "+ "IP: " + elem.getSessionIP() + "NOME: "+ elem.getSessionName());
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

			try {
				user = new User(userName, InetAddress.getLocalHost().getHostAddress(), "0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(user.getName() + " "+ user.getIp());

			//OpenFile newOpen = new OpenFile(gui);
			frame = new JFrame("Select");
			frame.setLocationRelativeTo(null);

			//frame.setSize(300, 300);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JFileChooser fc = new JFileChooser(new File(""));
			fc.setDialogTitle("Choose slides for presentation");
			fc.setMultiSelectionEnabled(true);
			frame.setVisible(true);
			fc.showOpenDialog(frame);
			File[] selectedFile = fc.getSelectedFiles();
			frame.setVisible(false);

			//TODO Creazione della sessione
			List<String> listIp = new ArrayList<String>();
			for(Session elem : sessionList){
				listIp.add(elem.getSessionIP());
			}
			if(listIp.isEmpty()){
				listIp.add("228.0.0.0");
			}
			session = new Session(user,user,"",sessionName,generateIp(listIp)); //Manca da generare l'IP con il metodo sotto (a partire dalla lista di IP della altre sessioni)
			session.setLeader(true);
			
			System.out.println("IP della nuova session: " + session.getSessionIP());
			
			//Trasformazione dei file in buffered Image (bisogna essere sicuri che siano immagini)
			for(int i=0; i<selectedFile.length; i++){
				try {
					BufferedImage im = ImageIO.read(selectedFile[i]);
					System.out.println("Stampa immagine: " + im);
					session.addSlide(ImageIO.read(selectedFile[i]));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//istanzio il controller con tutti i network handler connessi
			controller = new Controller(session);
			
			try {
				System.in.read();
				controller.startSession();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			break;
		case 1: //JOIN--> CLIENT!!!
			//controller = new Controller();
			System.out.println("Seleziona una sessione a cui fare la JOIN: ");
			int elem=0;
			try {
				elem= System.in.read() -48;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Elemento Selezionato:" + elem);
			session = sessionList.get(elem-1);
			try {
				user = new User(userName, InetAddress.getLocalHost().getHostAddress(), "0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("SESSIONE: " + session.getLeader().getName());
			controller = new Controller(session);
			session.setMyself(user);
			Join eventJoin = new Join(user);
			controller.sendEvent(eventJoin);

			break;

		}

	}
	/**
	 * Metodo per la spedizione dell'HELLO nella rete multicast riservata a pubblicizzare le sessioni
	 * @return ritorna la lista delle sessioni attualmente disponibili
	 * @throws IOException
	 */
	static public List<Session> sendHello() throws IOException{
		InetAddress globalLan = InetAddress.getByName(Session.ipHello);
		List<Session> sessionList = new ArrayList<Session>();
		String msg = "hello";
		MulticastSocket socket = new MulticastSocket(Session.portHello);
		socket.joinGroup(globalLan);
		int cont;
		DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), globalLan, Session.portHello);
		socket.send(hi);
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		socket.setSoTimeout(1000);
		try{
			ByteArrayInputStream byteStream;
			ObjectInputStream is;
			while(true){
				socket.receive(recv);
				HelloReply eventReceived;
				byteStream = new ByteArrayInputStream(buf);
				try{
				is = new ObjectInputStream(new BufferedInputStream(byteStream));
				eventReceived=(HelloReply) is.readObject();
				sessionList.add(eventReceived.getSession());
				}catch(StreamCorruptedException e){
					System.out.println("HELLO ricevuto da me stesso");
				}
				
			}
		}catch(SocketTimeoutException e2){
			System.out.println("Exception Raised, Timeout Expired");
		} catch (ClassNotFoundException e) {
			// Ho ricevuto il mio stesso hello
			e.printStackTrace();
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
			System.out.println("" + four[0] + "." + four[1] + "." + four[2] + "." + (number));
			return "" + four[0] + "." + four[1] + "." + four[2] + "." + (number);
		}
		return null;
	}
}