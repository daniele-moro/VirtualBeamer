package main;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
		
		//CREO tutte le istanze che mi servono per far funzionare il gioco
		JFrame frame = new JFrame();
		
		String[] optionsStart = {"NEW", "JOIN"};
		int choice = JOptionPane.showOptionDialog(frame,
				"no pain no gain, dani moro secco!",
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
			
			//Manca il Path dove è presente la presentazione (i file jpg)
			
			try {
				user = new User(userName, InetAddress.getLocalHost().getHostAddress(), "0");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(user.getName() + " "+ user.getIp());
			session= new Session(user, "AAA", sessionName);
			
			
			break;
		case 1: //JOIN--> CLIENT!!!
			
			break;
			
		}
		
	}

}
