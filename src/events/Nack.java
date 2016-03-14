package events;

public class Nack extends GenericEvent {
	
	private static final long serialVersionUID = 1L;

	private int sequenceNumber;
	
	public Nack(int sequenceNumber) {
		super(EventType.NACK);
		this.setSequenceNumber(sequenceNumber);
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

}
