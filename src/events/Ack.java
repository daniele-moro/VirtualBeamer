package events;

import model.User;

public class Ack extends GenericEvent {
	
	private static final long serialVersionUID = 1L;

	private User user;

	
	public Ack(User user) {
		super(EventType.ACK);
		this.user=user;
	}
	
	public User getUser(){
		return user;
	}

	
}