package events;

public class Nack extends GenericEvent {
	
	private static final long serialVersionUID = 1L;

	private int sequenceNumber;
	private int sessionNumber;
	
	public Nack(int sessionNumber, int squenceNumber) {
		super(EventType.NACK);
		this.sequenceNumber=squenceNumber;
		this.sessionNumber=sessionNumber;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public int getSessionNumber(){
		return sessionNumber;
	}

}
