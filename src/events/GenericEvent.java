package events;

import java.io.Serializable;

public abstract class GenericEvent implements Serializable {

	EventType type;
	
	public GenericEvent(EventType type){
		this.type=type;
	}
}
