package nl.tue.ddss.ifc_check;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBException;



public class MVDCheckerTest{

	public MVDCheckerTest(String ifcFile,String mvdXMLFile,String reportOutput) throws Exception{
		MvdXMLParser mvdXMLParser=new MvdXMLParser(mvdXMLFile);
		try {
			List<MVDConstraint> constraints=mvdXMLParser.getMVDConstraints();			
			IfcChecker ifcChecker=new IfcChecker(new File(ifcFile),constraints);
			ifcChecker.checkIfcModel(reportOutput);

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) throws Exception{
		/* Three arguments for the main function
		 * The first one is the IFC file path
		 * The second one is the mvdXML file path
		 * The third one is the output folder of BCF files
		 */
	new MVDCheckerTest("example/Duplex_A_20110505_modified.ifc","example/Demo.mvdxml","report/");
	}
}