package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import controller.Controller;
import events.HelloReply;
import model.Session;
import model.User;
import view.Gui;
import view.SessionButton;
import view.View;

public class Main {

	public static void main(String[] args) {

		Controller controller;
		Session session;
		User user=null;;

		String userName;
		String sessionName;

		//creo tutte le istanze che mi servono per far funzionare il gioco
		JFrame frame = new JFrame();
		JFrame sessionsFrame;
		JPanel sessionsPanel;
		SessionButton selectSession;

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
				user = new User(userName, InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(user.getName() + " "+ user.getIp());

			frame = new JFrame("Select");
			frame.setLocationRelativeTo(null);

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JFileChooser fc = new JFileChooser(new File(""));
			fc.setDialogTitle("Choose slides for presentation");
			fc.setMultiSelectionEnabled(true);
			frame.setVisible(true);
			fc.showOpenDialog(frame);
			File[] selectedFile = fc.getSelectedFiles();
			frame.setVisible(false);

			List<String> listIp = new ArrayList<String>();
			for(Session elem : sessionList){
				listIp.add(elem.getSessionIP());
			}
			if(listIp.isEmpty()){
				listIp.add("228.0.0.0");
			}
			session = new Session(user,user,user,"",sessionName,generateIp(listIp)); //Manca da generare l'IP con il metodo sotto (a partire dalla lista di IP della altre sessioni)
			
			System.out.println("IP della nuova session: " + session.getSessionIP());
			
			//Trasformazione dei file in buffered Image (bisogna essere sicuri che siano immagini)
			for(int i=0; i<selectedFile.length; i++){
				try {
					BufferedImage im = ImageIO.read(selectedFile[i]);

					//Codice per vedere la dimensione dell'immagine originale
//					ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
//					ImageIO.write(im, "jpg", baos1);
//					byte[] bytes1 = baos1.toByteArray();
//					System.out.println("Before conversion: " + bytes1.length);
					
					Image convertedImage = im.getScaledInstance(700, 495, BufferedImage.TYPE_INT_ARGB);
					im = new BufferedImage(700, 495, BufferedImage.TYPE_INT_ARGB);
					im.getGraphics().drawImage(convertedImage, 0, 0, null);

					//Codice pre vedere la dimensione dell'immagine convertita
//					ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
//					ImageIO.write(im, "jpg", baos2);
//					byte[] bytes2 = baos2.toByteArray();
//					System.out.println("After conversion: " + bytes2.length);
					
					session.addSlide(im);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

			//istanzio il controller con tutti i network handler connessi
			Gui masterGui = new Gui();
			controller = new Controller(session, masterGui);
			masterGui.setController(controller);
			masterGui.masterFrame();
			
			try {
				System.in.read();
				controller.startSession();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			break;
			
		case 1: //JOIN--> CLIENT!!!
			System.out.println("Dimension: " + sessionList.size());
			sessionsFrame = new JFrame("Select Session");
			sessionsFrame.setLayout(new BorderLayout());
			sessionsFrame.setLocationRelativeTo(null);
			sessionsPanel = new JPanel();
			sessionsPanel.setLayout(new BoxLayout(sessionsPanel, BoxLayout.Y_AXIS));
			sessionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			selectSessionHandler selectHandler = new selectSessionHandler(sessionList, userName, sessionsFrame);
			
			for(Session elem: sessionList){
				System.out.println("Name of the session: " + elem.getSessionName());
				selectSession = new SessionButton(elem, elem.getSessionName());
				selectSession.setSize(200, 100);
				selectSession.addActionListener(selectHandler);
				sessionsPanel.add(selectSession);
			}
			
			sessionsFrame.add(sessionsPanel, BorderLayout.CENTER);
			sessionsFrame.setVisible(true);
			//sessionsFrame.pack();
			sessionsFrame.setSize(600, 600);
			sessionsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
				eventReceived.getSession().setSessionStarted(false);
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
		socket.close();
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

class selectSessionHandler implements ActionListener
{
	private String userName; 
	private User user;
	private JFrame frame;
	
	public selectSessionHandler(List<Session> sessionList, String userName, JFrame frame){
		this.userName = userName;
		this.frame = frame; 
		
	}
	
	public void actionPerformed(ActionEvent event)
	{
		SessionButton button = (SessionButton) event.getSource();
		System.out.println("I've selected session: " + button.getButtonName());
		//pass to SESSION which is the actual session selected
		Session session = button.getButtonSession();
		
		try {
			user = new User(userName, InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("SESSIONE: " + session.getLeader().getName());
		System.out.println("I'm user:" + user.getName());
		View view = new View(session, user);
		frame.dispose();

		
	}
}
//End of class selectSessionHandler