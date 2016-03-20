package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.sun.corba.se.spi.ior.iiop.GIOPVersion;

import controller.Controller;
import model.User;

public class Gui {

	private JFrame frameG; 
	private ImageIcon currentSlide;
	private static final String[] joinedUsers = {"Steve", "Bill", "Bezos", "Martin",
			"Leonardo", "Heisenberg"};
	private JList viewUsers;
	private JButton nextSlide;
	private JButton prevSlide;
	private JPanel bottomPanel;
	private int pos=0;
	private Icon changeSlide;
	private File[] presentationSlides;
	private JButton giveControl;

	private JFrame frameMaster;
	private JFrame frameClient;
	private JSplitPane centralPanel = new JSplitPane();
	private JLabel centralSlide = new JLabel();
	private ImageIcon currentInitSlide; 
	private JPanel bottomInitPanel; 
	private JButton startButton;
	private JButton leaveButton;
	private JFrame sessionsFrame;
	private JPanel sessionsPanel;
	private JButton selectSession;
	private JPanel rightPanel;
	private UserButton ub;

	private List<User> userList; 

	private Controller controller;


	public Gui() {

	}

//	public void NewPresentationSelect(File[] selectedFile) {
//
//		presentationSlides = selectedFile;
//		//New JFrame for the master that has just created a new session
//		frameG = new JFrame("New Virtual Beamer");
//		frameG.setLayout(new BorderLayout() );
//
//		//Central Panel
//		centralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//		centralSlide = new JLabel();
//		System.out.println("Path of the first iamge: " + selectedFile[pos]);
//		//currentSlide = new ImageIcon(getClass().getResource(selectedString[pos]));
//		//currentSlide = selectedFile[pos];
//		//currentSlide = new ImageIcon(getClass().getResource("//Users//gatto//Desktop//presentation//00.png"));
//		currentSlide = new ImageIcon(presentationSlides[pos].getPath());
//		centralSlide.setIcon(currentSlide);
//		centralSlide.setVisible(true);
//
//		bottomPanel = new JPanel();
//		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
//		prevSlide = new JButton("PREV");
//		prevSlide.setIcon(new ImageIcon(this.getClass().getResource("prev.png")));
//		prevSlide.setSize(250, 96);
//		nextSlide = new JButton("NEXT");
//		nextSlide.setIcon(new ImageIcon(this.getClass().getResource("next.png")));
//		nextSlide.setSize(250, 96);
//		bottomPanel.add(prevSlide);
//		bottomPanel.add(nextSlide);
//
//		centralPanel.setTopComponent(centralSlide);
//		centralPanel.setBottomComponent(bottomPanel);
//		frameG.add(centralPanel, BorderLayout.CENTER);
//
//		//Right Panel with a JButton for each string inside joinedUsers
//		//joinedUsers now is fixed, but it should be built as an ArrayList, adding new user
//		JPanel rightPanel = new JPanel();
//		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
//		passControlHandler controlHandler = new passControlHandler();
//
//		for (String label : joinedUsers) {
//			giveControl = new JButton(label);
//			giveControl.setSize(200, 100);
//			giveControl.addActionListener(controlHandler);
//			rightPanel.add(giveControl); 
//		}
//		frameG.add(rightPanel, BorderLayout.EAST);
//
//		//Set visable the whole frame
//		frameG.setVisible(true);
//		frameG.pack();
//		frameG.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//
//		//Button Handler
//		//ButtonHandlerS handlerBS = new ButtonHandlerS(selectedPath);
//		ButtonHandlerS handlerBS = new ButtonHandlerS();
//		nextSlide.addActionListener(handlerBS);
//		prevSlide.addActionListener(handlerBS);
//
//	}//End of New Presentation
//	
//	
//
//	private class ButtonHandlerS implements ActionListener
//	{
//		/*public ButtonHandlerS(ArrayList<String> selectedPath)
//		{
//			ArrayList<String> sel = selectedPath;
//		}
//		 */
//
//		public void actionPerformed(ActionEvent event)
//		{
//			if(event.getSource() == nextSlide)
//			{
//				if(pos == 5) 
//				{
//					pos=0;
//				}
//				System.out.println("I'm inside the ButtonHandlerS of nextSlide");
//				pos++;
//				System.out.println("Next position is: " + pos);
//				changeSlide = new ImageIcon(presentationSlides[pos].getPath());
//				centralSlide.setIcon(changeSlide);
//			}
//
//			if(event.getSource() == prevSlide)
//			{
//				if(pos == 1)
//				{
//					pos = 6;
//				}
//				System.out.println("I'm inside the ButtonHandlerS of prevSlide");
//				pos--;
//				System.out.println("Next position is: " + pos);
//				changeSlide = new ImageIcon(presentationSlides[pos].getPath());
//				centralSlide.setIcon(changeSlide);
//
//			}
//		}//End of Action Performed
//	}//end of ButtonHandler


	
	public Controller getController() {
		return controller;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public void masterFrame() {
		frameMaster = new JFrame("Select");
		frameMaster.setLocationRelativeTo(null);
		frameMaster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameMaster.setLayout(new BorderLayout());

		centralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		centralSlide = new JLabel(); 
		currentInitSlide = new ImageIcon("src/main/leaderStart.png");
		centralSlide.setIcon(currentInitSlide);
		centralSlide.setVisible(true);
		bottomInitPanel = new JPanel();
		bottomInitPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		startButton = new JButton("START");
		startButton.setSize(250, 96);
		StartHandler startHandler = new StartHandler();
		startButton.addActionListener(startHandler);
		bottomInitPanel.add(startButton);
		//When press on START, ActionListener and all clients must be lead to first slide

		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		passControlHandler controlHandler = new passControlHandler();
		userList = controller.getSession().getJoined();
		for (User user : userList) {
			ub = new UserButton(user);
			ub.setText(user.getName());
			ub.setSize(200, 100);
			ub.addActionListener(controlHandler);
			rightPanel.add(ub); 
			
		}
		frameMaster.add(rightPanel, BorderLayout.EAST);


		centralPanel.setTopComponent(centralSlide);
		centralPanel.setBottomComponent(bottomInitPanel);
		frameMaster.add(centralPanel, BorderLayout.CENTER);
		frameMaster.pack();
		frameMaster.setVisible(true);
		frameMaster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


	}
	
	public void masterFrame(int actualSlide) {
		frameMaster = new JFrame("Select");
		frameMaster.setLocationRelativeTo(null);
		frameMaster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameMaster.setLayout(new BorderLayout());

		centralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		centralSlide = new JLabel(); 
		currentInitSlide = new ImageIcon(controller.getSession().getSlides().get(actualSlide));
		centralSlide.setIcon(currentInitSlide);
		centralSlide.setVisible(true);
		bottomInitPanel = new JPanel();
		bottomInitPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		startButton = new JButton("START");
		startButton.setSize(250, 96);
		NextSlideHandler nsh = new NextSlideHandler();
		startButton.addActionListener(nsh);
		bottomInitPanel.add(startButton);
		//When press on START, ActionListener and all clients must be lead to first slide

		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		passControlHandler controlHandler = new passControlHandler();
		userList = controller.getSession().getJoined();
		for (User user : userList) {
			ub = new UserButton(user);
			ub.setText(user.getName());
			ub.setSize(200, 100);
			ub.addActionListener(controlHandler);
			rightPanel.add(ub); 
			
		}
		frameMaster.add(rightPanel, BorderLayout.EAST);


		centralPanel.setTopComponent(centralSlide);
		centralPanel.setBottomComponent(bottomInitPanel);
		frameMaster.add(centralPanel, BorderLayout.CENTER);
		frameMaster.pack();
		frameMaster.setVisible(true);
		frameMaster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void clientToLeader() {
		frameClient.setVisible(false);
		masterFrame();
		frameMaster.setVisible(true);
		
	}
	
	public void leaderToClient() {
		frameMaster.setVisible(false);
		clientFrame();
		frameClient.setVisible(true);
	}

	public void clientFrame() {
		frameClient = new JFrame("Select");
		frameClient.setLocationRelativeTo(null);
		frameClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameClient.setLayout(new BorderLayout());

		centralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		centralSlide = new JLabel(); 
		currentInitSlide = new ImageIcon("src/main/clientStart.png");
		centralSlide.setIcon(currentInitSlide);
		centralSlide.setVisible(true);

		bottomInitPanel = new JPanel();
		bottomInitPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		leaveButton = new JButton("LEAVE");
		leaveButton.setSize(250, 96);
		LeaveHandler lh = new LeaveHandler();
		leaveButton.addActionListener(lh);
		bottomInitPanel.add(leaveButton);


		centralPanel.setTopComponent(centralSlide);
		centralPanel.setBottomComponent(bottomInitPanel);
		frameClient.add(centralPanel, BorderLayout.CENTER);
		frameClient.pack();
		frameClient.setVisible(true);
		frameClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}


	public void ChangeSlide(Icon icon) {
		//TODO: change the current to the next/previous one
		//The View call this method passing the correct new icon to visualize
		centralSlide.setIcon(icon);

	}
	
	public void presentationButtons(){
		System.out.println("chiamata");
		bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		prevSlide = new JButton("PREV");
		prevSlide.setIcon(new ImageIcon(this.getClass().getResource("prev.png")));
		prevSlide.setSize(100, 96);
		nextSlide = new JButton("NEXT");
		nextSlide.setIcon(new ImageIcon(this.getClass().getResource("next.png")));
		nextSlide.setSize(100, 96);
		bottomPanel.add(prevSlide);
		bottomPanel.add(nextSlide);
		
		NextSlideHandler nsh = new NextSlideHandler();
		nextSlide.addActionListener(nsh);
		
		PrevSlideHandler psh = new PrevSlideHandler();
		prevSlide.addActionListener(psh);
		
		centralPanel.remove(bottomInitPanel);
		centralPanel.setBottomComponent(bottomPanel);
		centralPanel.validate();
		frameMaster.pack();
	}

	public void refreshUsers(List<User> users){
		userList = users;
		passControlHandler controlHandler = new passControlHandler();
		frameMaster.remove(rightPanel);
		
		
		
		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		for (User user : userList) {
			ub = new UserButton(user);
			ub.setText(user.getName());
			ub.setSize(200, 100);
			ub.addActionListener(controlHandler);
			rightPanel.add(ub); 
			
		}
		frameMaster.add(rightPanel, BorderLayout.EAST);
		
		//frameMaster.invalidate();
		frameMaster.pack();
		frameMaster.validate();
		//frameMaster.repaint();
	}


	private class ButtonHandlerS implements ActionListener
	{

		public void actionPerformed(ActionEvent event)
		{
			if(event.getSource() == nextSlide)
			{
				if(pos == 5) 
				{
					pos=0;
				}
				System.out.println("I'm inside the ButtonHandlerS of nextSlide");
				pos++;
				System.out.println("Next position is: " + pos);
				changeSlide = new ImageIcon(presentationSlides[pos].getPath());
				centralSlide.setIcon(changeSlide);
			}

			if(event.getSource() == prevSlide)
			{
				if(pos == 1)
				{
					pos = 6;
				}
				System.out.println("I'm inside the ButtonHandlerS of prevSlide");
				pos--;
				System.out.println("Next position is: " + pos);
				changeSlide = new ImageIcon(presentationSlides[pos].getPath());
				centralSlide.setIcon(changeSlide);

			}
		}//End of Action Performed
	}//end of ButtonHandler

	private class passControlHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			UserButton button = (UserButton) event.getSource();
			User newLeader = button.getButtonUser();
			System.out.println("I've passed control to user: " + newLeader.getName());
			
			//pass control to new user
			//invoke controller method
			controller.newLeader(newLeader);
		
		}
	}//End of class passControlHandler
	
	
	private class StartHandler implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			controller.startSession();
		}
	}


	private class NextSlideHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//TODO: master press START and presentation can start (all users will visualize first slide)
			controller.next();
		}
	}//End of class startHandler
	
	private class PrevSlideHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//TODO: master press START and presentation can start (all users will visualize first slide)
			controller.prev();
		}
	}//End of class startHandler
	
	
	

	private class LeaveHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//TODO: client press on LEAVE and the user is disconnected

		}
	}//End of class leaveHandler



}
