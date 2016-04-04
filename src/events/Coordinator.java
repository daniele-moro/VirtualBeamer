package events;

import model.User;

public class Coordinator extends GenericEvent {
	
	private User newLeader;
	
	public Coordinator(User user) {
		super(EventType.COORDINATOR);
		this.newLeader = user;
	}

	public User getNewLeader() {
		return newLeader;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((newLeader == null) ? 0 : newLeader.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coordinator other = (Coordinator) obj;
		if (newLeader == null) {
			if (other.newLeader != null)
				return false;
		} else if (!newLeader.equals(other.newLeader))
			return false;
		return true;
	}

	public void setNewLeader(User newLeader) {
		this.newLeader = newLeader;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
