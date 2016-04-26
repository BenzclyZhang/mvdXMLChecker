package nl.tue.ddss.bcf;

import java.io.IOException;

public class ColladaConverter {
	
	public static void convert(String ifcFile) throws IOException{
		Runtime rt = Runtime.getRuntime();
		String[] cmd={"D:\\Demo\\Model_View_Checker\\IfcConvert",ifcFile,"D:\\Demo\\Model_View_Checker\\temp\\temp.dae"};
		rt.exec(cmd);
	}

}
