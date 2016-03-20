package events;

public class StartSession extends GenericEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private int numSlide;
	
	public StartSession(int numSlide){
		super(EventType.START_SESSION);
		this.numSlide=numSlide;
	}
	
	public int getNumSlide(){
		return this.numSlide;
	}

}
