package events;

import java.io.Serializable;

public abstract class GenericEvent implements Serializable {

	private EventType type;
	
	public GenericEvent(EventType type){
		this.type = type;
	}

	public EventType getType() {
		return type;
	}
	
}
