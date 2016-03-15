package view;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;

public class OpenFile {

	private JFrame frameOp; 
	private JButton selectB;
	private ArrayList<String> PresentationFiles;
	private Gui gui;

	public OpenFile( Gui gui ){
		JList<String> FileList = new JList();
		frameOp = new JFrame("Selector of Images");
		frameOp.setLayout(new FlowLayout());
		selectB = new JButton("Select Slides");
		selectB.setSize(500, 500);
		SelectorHandler handlerS = new SelectorHandler();
		selectB.addActionListener(handlerS);

		frameOp.add(selectB);
		frameOp.setVisible(true);
		//frameOp.pack();
		frameOp.setLocationRelativeTo(null);;
		frameOp.setSize(300, 300);
		frameOp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.gui=gui;
	}


	private class SelectorHandler implements ActionListener 
	{
		public void actionPerformed(ActionEvent event)
		{
			if(event.getSource() == selectB)
			{
				JFileChooser fc = new JFileChooser(new File(""));
				//fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setDialogTitle("Choose slides for presentation");
				fc.setMultiSelectionEnabled(true);
				fc.showOpenDialog(frameOp);
				File[] selectedFile = fc.getSelectedFiles();

				gui.NewPresentationSelect(selectedFile);

			}

		}

	}

}
