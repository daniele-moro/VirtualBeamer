package events;

public class Ack extends GenericEvent {
	
	private static final long serialVersionUID = 1L;

	private int sessionNumber;
	
	public Ack(int sessionNumber) {
		super(EventType.ACK);
		this.setSessionNumber(sessionNumber);
	}

	public int getSessionNumber() {
		return sessionNumber;
	}

	public void setSessionNumber(int sessionNumber) {
		this.sessionNumber = sessionNumber;
	}

}