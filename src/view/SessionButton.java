package view;

import javax.swing.JButton;

import model.Session;


public class SessionButton extends JButton{
	private String buttonName; 
	private Session buttonSession;

	public SessionButton(Session s, String n)
	{
		this.buttonSession = s;
		this.buttonName = n;
		setText(n);
	}

	public String getButtonName() {
		return buttonName;
	}

	public void setButtonName(String buttonName) {
		this.buttonName = buttonName;
	}

	public Session getButtonSession() {
		return buttonSession;
	}

	public void setButtonSession(Session buttonSession) {
		this.buttonSession = buttonSession;
	}
	
	
	
}

