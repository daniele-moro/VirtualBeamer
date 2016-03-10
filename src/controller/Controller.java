package controller;

import model.Session;

public class Controller {
		
	private Session session; 
	
	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Controller(Session session) {
		this.session = session; 
		
	}
	
	
}
