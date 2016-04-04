package events;

public enum EventType {
	GOTO,
	NEWLEADER, 
	JOIN,
	NACK,
	ACK,
	HELLOREPLY, 
	REQUEST_TO_JOIN, 
	CRASH, 
	ALIVE, 
	ELECT, 
	STOP, 
	COORDINATOR,
	START_SESSION,
	SEND_SLIDE_TO;
}
