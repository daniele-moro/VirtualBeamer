package events;

import model.Session;

public class HelloReply extends GenericEvent {
	
	private static final long serialVersionUID = 1L;
	
	private Session session;
	
	public HelloReply( Session session ){
		super(EventType.HELLOREPLY);
		this.session=session;
	}
	
	public Session getSession(){
		return this.session;
	}

}
