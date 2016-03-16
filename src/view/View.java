package view;

import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import controller.Controller;
import model.Session;

public class View {

	private Session session;
	private Controller controller;
	
	public View(Session session, Controller controller){
		this.session=session;
		this.controller = controller;
		
	}

	public void changeSlide(BufferedImage slideToShow) {
		//TODO: display new slide
	}
	
	public void activateButtons(){
		//TODO: attivare i bottoni per prev/next e selezione new Leader
	}
	
	
	public void initGui(){
		
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
