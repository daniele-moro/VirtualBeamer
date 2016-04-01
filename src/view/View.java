package view;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import model.Session;
import model.User;
import controller.Controller;

public class View {

	private Session session;
	private Controller controller;
	private Gui gui;
	private Icon changeSlide;
	
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
		gui = new Gui();
		gui.clientFrame();
	}

	public void changeSlide(BufferedImage slideToShow) {
		//display new slide, can be the next one or the previous one
		changeSlide = new ImageIcon(slideToShow);
		//pass the Icon to the GUI to make the change
		gui.ChangeSlide(changeSlide);

	}
	
	public void displayUsers(List<User> users) {
		//when a user joins the session, the master view is updated
		gui.refreshUsers(users);
	}

	public void becomeMaster(){
		//when invoked by the controller, change the GUI such that user becomes a master
		gui.clientToLeader();
		//button NEXT and PREV appear and also users list with possibility to select new master
		gui.presentationButtons();
	}
	
	public void becomeClient(){
		gui.leaderToClient();
	}
	public void presentationButtons(){
		gui.presentationButtons();
	}

}


