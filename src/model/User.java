package model;

public class User {


	public User(String name, String ip, String id) {
		this.name = name;
		this.ip = ip;
		this.id = id;
	}
	
	private String name;
	private String ip;
	private String id;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
}
