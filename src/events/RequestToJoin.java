package events;

import model.User;

public class RequestToJoin extends GenericEvent {

	private User joiner; 
	
	public RequestToJoin(User joiner) {
		super(EventType.REQUEST_TO_JOIN);
		this.joiner = joiner;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public User getJoiner() {
		return joiner;
	}

	public void setJoiner(User joiner) {
		this.joiner = joiner;
	}

}
