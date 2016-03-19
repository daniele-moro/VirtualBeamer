package view;

import javax.swing.JButton;

import model.User;

public class UserButton extends JButton{
	
	private User buttonUser;
	
	public UserButton(User u){
		this.buttonUser = u;
	}

	public User getButtonUser() {
		return buttonUser;
	}

	public void setButtonUser(User buttonUser) {
		this.buttonUser = buttonUser;
	}

}
