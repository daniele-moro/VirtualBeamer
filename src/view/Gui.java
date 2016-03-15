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

public class Gui {

	private JFrame frameG; 
	private JLabel centralSlide;
	private ImageIcon currentSlide;
	private static final String[] joinedUsers = {"Steve", "Bill", "Bezos", "Martin",
			"Leonardo", "Heisenberg"};
	private JList viewUsers;
	private JButton nextSlide;
	private JButton prevSlide;
	private JPanel bottomPanel;
	private JSplitPane centralPanel;
	private int pos=0;
	private Icon changeSlide;
	private File[] presentationSlides;
	private JButton giveControl;

	/*private String[] listSlides = {
			"presentation/slide0.png",
			"presentation/slide01.png",
			"presentation/slide02.png",
			"presentation/slide03.png",
			"presentation/slide04.png",
			"presentation/slide05.png"
	};
	 */


	public Gui() {

	}

	public void NewPresentationSelect(File[] selectedFile) {

		presentationSlides = selectedFile;
		//New JFrame for the master that has just created a new session
		frameG = new JFrame("New Virtual Beamer");
		frameG.setLayout(new BorderLayout() );

		//Central Panel
		centralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		centralSlide = new JLabel();
		System.out.println("Path of the first iamge: " + selectedFile[pos]);
		//currentSlide = new ImageIcon(getClass().getResource(selectedString[pos]));
		//currentSlide = selectedFile[pos];
		//currentSlide = new ImageIcon(getClass().getResource("//Users//gatto//Desktop//presentation//00.png"));
		currentSlide = new ImageIcon(presentationSlides[pos].getPath());
		centralSlide.setIcon(currentSlide);
		centralSlide.setVisible(true);

		bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		prevSlide = new JButton("PREV");
		prevSlide.setIcon(new ImageIcon(this.getClass().getResource("prev.png")));
		prevSlide.setSize(250, 96);
		nextSlide = new JButton("NEXT");
		nextSlide.setIcon(new ImageIcon(this.getClass().getResource("next.png")));
		nextSlide.setSize(250, 96);
		bottomPanel.add(prevSlide);
		bottomPanel.add(nextSlide);

		centralPanel.setTopComponent(centralSlide);
		centralPanel.setBottomComponent(bottomPanel);
		frameG.add(centralPanel, BorderLayout.CENTER);

		//Right Panel with a JButton for each string inside joinedUsers
		//joinedUsers now is fixed, but it should be built as an ArrayList, adding new user
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		passControlHandler controlHandler = new passControlHandler();

		for (String label : joinedUsers) {
			giveControl = new JButton(label);
			giveControl.setSize(200, 100);
			giveControl.addActionListener(controlHandler);
			rightPanel.add(giveControl); 
		}
		frameG.add(rightPanel, BorderLayout.EAST);

		//Set visable the whole frame
		frameG.setVisible(true);
		frameG.pack();
		frameG.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		//Button Handler
		//ButtonHandlerS handlerBS = new ButtonHandlerS(selectedPath);
		ButtonHandlerS handlerBS = new ButtonHandlerS();
		nextSlide.addActionListener(handlerBS);
		prevSlide.addActionListener(handlerBS);

	}//End of New Presentation
	
	

	private class ButtonHandlerS implements ActionListener
	{
		/*public ButtonHandlerS(ArrayList<String> selectedPath)
		{
			ArrayList<String> sel = selectedPath;
		}
		 */

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
			JButton button = (JButton) event.getSource();
			String clickedUser = button.getText();
			System.out.println("I've passed control to user: " + clickedUser);

		}
	}//End of class passControlHandler




}
