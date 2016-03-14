package events;

public class SlidePart extends GenericEvent {
	
	private static final long serialVersionUID = 1L;
	
	private SlidePartData data = new SlidePartData();


	public SlidePart(SlidePartData data){
		super(EventType.SLIDEPART);
		this.data=data;
	}


	public SlidePartData getData() {
		return data;
	}
	

}