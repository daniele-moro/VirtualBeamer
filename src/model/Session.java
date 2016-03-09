package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class Session extends Observable{
	

	private User leader;
	private List<User> joined;
	private String actualSlide;
	private String path;
	
	
	public Session(User leader, String path) {
		super();
		this.leader = leader;
		this.joined = new ArrayList<User>();
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

}
