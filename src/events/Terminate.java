package events;

public class Terminate extends GenericEvent{
	
	private static final long serialVersionUID = 1L;

	public Terminate() {
		super(EventType.TERMINATE); 
	}
	
	
}
