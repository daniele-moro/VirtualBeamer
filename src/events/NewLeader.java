package events;

import model.User;

public class NewLeader extends GenericEvent {
		
	private static final long serialVersionUID = 1L;
	
	private User newLeader;

	public NewLeader(User user) {
		super(EventType.NEWLEADER); 
		this.newLeader = user;
	}
	
	public User getNewLeader() {
		return newLeader;
	}

	public void setNewLeader(User newLeader) {
		this.newLeader = newLeader;
	} 

}
