package nl.tue.ddss.ifc_check;

import java.io.File;
import java.awt.*;

import javax.swing.*;

public class CreateTable {
	public static void main(String args[]){
		File[] IFCFiles = new File("D:\\Data\\Emiel\\CreateTable\\IFC").listFiles();
		File[] XMLFiles = new File("D:\\Data\\Emiel\\CreateTable\\XML").listFiles(); 
		Object[] col = new Object[ IFCFiles.length + 1];
		Object[][] dat = new Object[XMLFiles.length][IFCFiles.length + 1];
		col[0] = " ";
		for (int j = 0; j < IFCFiles.length; j++){
			col[j+1] = IFCFiles[j].getName();
		
		
			for (int i = 0; i < XMLFiles.length; i++){
				File[] BCFFiles = new File("D:\\Data\\Emiel\\CreateTable\\Results\\" + IFCFiles[j].getName() + "\\" + XMLFiles[i].getName()).listFiles();
				System.out.println(BCFFiles.length);
				
				dat[i][0] = XMLFiles[i].getName();
				dat[i][j+1] = BCFFiles.length;
			}}

		
			JFrame frame = new JFrame("MVD Checker");
			JTable table = new JTable(dat,col);
			frame.setVisible(true);
			frame.setBounds(0, 0, 500, 500);
			frame.add(table.getTableHeader(),BorderLayout.PAGE_START);
			frame.add(table);
}
}
