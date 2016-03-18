package events;

public class Nack extends GenericEvent {
	
	private static final long serialVersionUID = 1L;

	private short sequenceNumber;
	private short sessionNumber;
	
	public Nack(short sessionNumber, short squenceNumber) {
		super(EventType.NACK);
		this.sequenceNumber=squenceNumber;
		this.sessionNumber=sessionNumber;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}
	
	public short getSessionNumber(){
		return sessionNumber;
	}

}
