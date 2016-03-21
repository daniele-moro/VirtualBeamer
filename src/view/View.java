package view;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import controller.Controller;
import model.Session;
import model.User;

public class View {

	private Session session;
	private Controller controller;
	private Gui gui;
	private Icon changeSlide;
	private boolean sessionStarted = false;
	
	public View(Session session, Controller controller){
		this.session=session;
		this.controller = controller;
		
	}
	
	public View(Session session, Controller controller, Gui gui){
		this.session=session;
		this.controller = controller;
		this.gui = gui;
	}


	public View(Session s, User u){
		this.session = s;
		this.session.setMyself(u);
		this.controller = new Controller(this.session, this);
		session.setSlides(new ArrayList<BufferedImage>());
		controller.requestToJoin();
		initGui();
		gui.setController(this.controller);
		
	}


	public void initGui(){
		//TODO: when we first initialize the GUI, a start/wait slide appears
		gui = new Gui();
		gui.clientFrame();
	}

	public void changeSlide(BufferedImage slideToShow) {
		//display new slide, can be the next one or the previous one

		changeSlide = new ImageIcon(slideToShow);
		System.out.println(gui);
		gui.ChangeSlide(changeSlide);
		/*if(!sessionStarted) {
			sessionStarted = true; 
			gui.presentationButtons();
		}*/
		

		//TODO: add column with user lists


	}
	
	public void displayUsers(List<User> users) {
		//when a user joins the session, the master view is updated
		gui.refreshUsers(users);
	}

	public void becomeMaster(){
		gui.clientToLeader();
		//TODO: when invoked by the controller, change the GUI such that user becomes a master
		//button NEXT and PREV appear and also users list with possibility to select new master
	}
	
	public void becomeClient(){
		gui.leaderToClient();
	}
	public void presentationButtons(){
		gui.presentationButtons();
	}

	
	
	

//	//Ogni socket è "collegato" ad un utente della sessione
//	Map<User, Socket> socketsList;
//	GenericEvent event;
//
//	public View (){
//		socketsList = new HashMap<User, Socket>();
//
//
//		class EventReceiver implements Runnable{
//			Socket s; 
//			public EventReceiver(Socket s) {
//				this.s = s; 
//			}
//
//			public void run() {
//				try {
//					waitMessage(s);
//				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//			}
//		}
//
//		for(Socket socket : socketsList.values()) {
//			(new Thread(new EventReceiver(socket))).start();
//		}
//	}
//
//	private void waitMessage(Socket s) throws IOException, ClassNotFoundException {
//		GenericEvent event = null; 
//		ObjectInputStream in; 
//		while(true) {
//			in = (new ObjectInputStream(s.getInputStream())); 
//			event = (GenericEvent)in.readObject(); 
//			if(event!=null) {
//				setChanged(); 
//				notifyObservers(event); 
//			}
//		}
//	}
//
//	@Override
//	public void next() {
//		// TODO Auto-generated method stub
//		//Creo evento next
//
//	}
//
//	@Override
//	public void prev() {
//		// TODO Auto-generated method stub
//		//Crea evento prev
//
//	}
//
//	@Override
//	public void newLeader() {
//		// TODO Auto-generated method stub
//		//Creo evento new Leader
//
//	}
//
//	@Override
//	public void closeSession() {
//		// TODO Auto-generated method stub
//		//Creo evento close Session
//
//	}

}


