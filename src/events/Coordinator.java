package events;

import model.User;

public class Coordinator extends GenericEvent {
	
	private User newLeader;
	
	public Coordinator(User user) {
		super(EventType.COORDINATOR);
		this.newLeader = user;
	}

	public User getNewLeader() {
		return newLeader;
	}

	public void setNewLeader(User newLeader) {
		this.newLeader = newLeader;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
