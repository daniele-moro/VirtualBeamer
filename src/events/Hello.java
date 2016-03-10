package events;

public class Hello extends GenericEvent{
	
	private static final long serialVersionUID = 1L;

	public Hello() {
		super(EventType.HELLO); 
	}
}
