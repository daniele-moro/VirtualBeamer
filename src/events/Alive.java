package events;

import model.User;

public class Alive extends GenericEvent {
	
	private User aliveUser;

	public Alive(User aliveUser) {
		super(EventType.ALIVE);
		this.aliveUser = aliveUser;
		
	}

	public User getAliveUser() {
		return aliveUser;
	}

	public void setAliveUser(User aliveUser) {
		this.aliveUser = aliveUser;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
