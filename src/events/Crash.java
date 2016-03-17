package events;

import model.User;

public class Crash extends GenericEvent {
	
	private User crashedUser; 

	public Crash(User crashedUser) {
		super(EventType.CRASH);
		this.crashedUser = crashedUser;
	}

	public User getCrashedUser() {
		return crashedUser;
	}

	public void setCrashedUser(User crashedUser) {
		this.crashedUser = crashedUser;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	
}
