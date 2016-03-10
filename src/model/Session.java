package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class Session extends Observable{
	

	private User leader;
	private String sessionName;
	private List<User> joined;
	private String actualSlide;
	private String path;
	
	
	public Session(User leader, String path, String sessionName) {
		this.leader = leader;
		this.setSessionName(sessionName);
		this.joined = new ArrayList<User>();
		joined.add(leader);
		this.actualSlide = "-1";
		this.path = path;
	}
	
	public User getLeader() {
		return leader;
	}
	public void setLeader(User leader) {
		this.leader = leader;
	}
	public List<User> getJoined() {
		return joined;
	}
	public void setJoined(List<User> joined) {
		this.joined = joined;
	}
	
	public void addJoinedUser(User user){
		this.joined.add(user);
	}
	
	public void removeUser(User user){
		for(User u: joined){
			if(u.getIp() == user.getIp() && u.getName() == user.getName()){
				joined.remove(u);
				break;
			}
		}
	}
	
	public String getActualSlide() {
		return actualSlide;
	}
	public void setActualSlide(String actualSlide) {
		this.actualSlide = actualSlide;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

}