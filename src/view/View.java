package view;

import java.util.Observable;

import events.EventType;
import events.GenericEvent;

public class View extends Observable implements IFView{
	GenericEvent event;

	@Override
	public void next() {
		// TODO Auto-generated method stub
		//Creo evento next
		
	}

	@Override
	public void prev() {
		// TODO Auto-generated method stub
		//Crea evento prev
		
	}

	@Override
	public void newLeader() {
		// TODO Auto-generated method stub
		//Creo evento new Leader
		
	}

	@Override
	public void closeSession() {
		// TODO Auto-generated method stub
		//Creo evento close Session
		
	}

}
