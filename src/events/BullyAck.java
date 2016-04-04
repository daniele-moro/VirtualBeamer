package events;

import model.User;

public class BullyAck extends Ack {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private GenericEvent eventToAck;
	
	public BullyAck(User user, GenericEvent eventToAck){
		super(user);
		this.eventToAck=eventToAck;
		
	}
	
	public GenericEvent getEventToAck(){
		return this.eventToAck;
	}

}
