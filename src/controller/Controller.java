package controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.imageio.ImageIO;

import events.Ack;
import events.GenericEvent;
import events.Nack;
import events.SlidePart;
import events.SlidePartData;
import model.Session;
import network.NetworkSender;
import network.NetworkSlideSender;

public class Controller implements Observer{

	private Session session; 
	private NetworkSlideSender slideSender;
	private NetworkSender networkSender;
	
	
	private List<SlidePartData> tempArray;
	private int currentSessionNumber=-1;

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Controller(Session session) {
		this.session = session; 

	}

	@Override
	public void update(Observable o, Object arg) {
		
		if(!(arg instanceof GenericEvent)){
			throw new IllegalArgumentException();
		}
		switch(((GenericEvent) arg).getType()){
		case ACK:
			if(session.isLeader()){
				slideSender.setCont();
				slideSender.notifyAll();
			}
			break;
		case NACK:
			if(session.isLeader()){
				slideSender.sendMissingPacket(((Nack) arg).getSequenceNumber());
			}
			break;
		case SLIDEPART:
			//Ho ricevuto un pezzetto di immagine
			
			if(!session.isLeader()){
				
				SlidePartData slice =((SlidePart) arg).getData();
				
				//se start inizializzo arraylist
				if(slice.start || currentSessionNumber != slice.sessionNumber){
					currentSessionNumber = slice.sessionNumber;
					tempArray = new ArrayList<SlidePartData>(slice.numPack);
					for(int i=0; i<slice.numPack; i++){
						tempArray.add(null);
					}
				}
				
				tempArray.add(slice.sequenceNumber, slice);
				
				if(slice.sequenceNumber-1 > 0 && tempArray.get(slice.sequenceNumber-1) == null ){
					//Invio NACK
					try {
						networkSender.send(new Nack(slice.sequenceNumber-1));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					boolean endSlide=true;
					for(SlidePartData part : tempArray){
						if(part==null){
							endSlide = false;
							break;
						}
					}
					if(endSlide){
						try {
							//Invio ACK
							networkSender.send(new Ack(currentSessionNumber));
							
							//Costruisco l'immagine e la aggiungo alla session
							byte[] imageData = null;
							for(SlidePartData part : tempArray){
								System.arraycopy(part.data, 0, imageData, part.sequenceNumber*part.maxPacketSize , part.data.length);
							}
							ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
							BufferedImage image = ImageIO.read(bis);
							session.addSlide(image);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
				
			}
			break;
		case ANSWER:
			break;
		case GOTO:
			break;
		case HELLO:
			break;
		case JOIN:
			break;
		case NEWLEADER:
			break;
		case TERMINATE:
			break;
		default:
			break;

		}

	}





}
