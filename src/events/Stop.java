package events;

import model.User;

public class Stop extends GenericEvent {
	
	private User user;
	
	public Stop(User user) {
		super(EventType.STOP);
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
