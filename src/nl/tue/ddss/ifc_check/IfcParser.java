package nl.tue.ddss.ifc_check;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import nl.tue.ddss.emf.replace.Ifc2x3tc1StepDeserializerReplace;

import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.IfcModel;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.plugins.deserializers.DeserializeException;

public class IfcParser {
	private IfcModel ifcModel;
	
	public IfcParser(File ifcFile) throws DeserializeException, IOException, URISyntaxException{
		Ifc2x3tc1StepDeserializerReplace p21Parser = new Ifc2x3tc1StepDeserializerReplace(Schema.IFC2X3TC1);		
		PackageMetaData pmd=new PackageMetaData(null,Ifc2x3tc1Package.eINSTANCE, Schema.IFC2X3TC1);
		p21Parser.init(pmd);
		this.ifcModel = (IfcModel) p21Parser.read(ifcFile);
	}

	public IfcModel getIfcModel() {
		return ifcModel;
	}


	
	

}
