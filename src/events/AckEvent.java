package events;

import model.User;

public class AckEvent extends GenericEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private GenericEvent event;
	private User user;
	
	public AckEvent(GenericEvent event, User user){
		super(EventType.ACK_EVENT);
		this.event=event;
	}
	
	public GenericEvent getEvent(){
		return this.event;
	}
	
	public User getUser(){
		return this.user;
	}
	
	

}
