package events;

import model.User;

public class Elect extends GenericEvent {
	
	private User user;
	
	public Elect(User user) {
		super(EventType.ELECT);
		this.user = user;
	}


	public User getUser() {
		return user;
	}



	public void setUser(User user) {
		this.user = user;
	}



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
