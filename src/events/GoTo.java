package events;


public class GoTo extends GenericEvent {

	private static final long serialVersionUID = 1L;
	
	private int slideToShow;
	
	public GoTo(int slide) {
		super(EventType.GOTO);
		this.slideToShow = slide;
	}

	public int getSlideToShow() {
		return slideToShow;
	}

	public void setSlideToShow(int slideToShow) {
		this.slideToShow = slideToShow;
	} 
	
}
