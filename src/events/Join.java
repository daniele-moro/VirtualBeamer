package events;

import model.User;

public class Join extends GenericEvent{

	private static final long serialVersionUID = 1L;
	
	private User joiner;
	
	public Join(User user) {
		super(EventType.JOIN);
		this.joiner = user;
	}
	
	public User getJoiner() {
		return joiner;
	}
	public void setJoiner(User joiner) {
		this.joiner = joiner;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Join other = (Join) obj;
		if (joiner == null) {
			if (other.joiner != null)
				return false;
		} else if (!joiner.equals(other.joiner))
			return false;
		return true;
	}
	
	
}
