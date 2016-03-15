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

	@Override
	public String toString() {
		return "GenericEvent [type=" + type + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericEvent other = (GenericEvent) obj;
		if (type != other.type)
			return false;
		return true;
	}
	
}
