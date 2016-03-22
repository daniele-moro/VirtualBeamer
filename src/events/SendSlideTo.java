package events;

import model.User;

public class SendSlideTo extends GenericEvent {

	private static final long serialVersionUID = 1L;
	
	
	private User receiver;
	
	public SendSlideTo(User receiver){
		super(EventType.SEND_SLIDE_TO);
		this.receiver=receiver;
	}
	
	public User getReceiver(){
		return receiver;
	}

}
