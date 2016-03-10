package events;

import model.User;

public class Join extends GenericEvent{

	private static final long serialVersionUID = 1L;
	
	private User joiner;
	
	public Join(User user) {
		super(EventType.JOIN);
		this.joiner = user;
	}
	
	public User getJoiner() {
		return joiner;
	}
	public void setJoiner(User joiner) {
		this.joiner = joiner;
	}
}
