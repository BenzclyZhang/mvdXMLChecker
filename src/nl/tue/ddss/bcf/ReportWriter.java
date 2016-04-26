package nl.tue.ddss.bcf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.bcf.Bcf;
import org.bimserver.bcf.BcfException;
import org.bimserver.bcf.Issue;
import org.bimserver.bcf.markup.Comment;
import org.bimserver.bcf.markup.CommentStatus;
import org.bimserver.bcf.markup.Header;
import org.bimserver.bcf.markup.Markup;
import org.bimserver.bcf.markup.Topic;
import org.bimserver.bcf.visinfo.Component;
import org.bimserver.bcf.visinfo.Direction;
import org.bimserver.bcf.visinfo.PerspectiveCamera;
import org.bimserver.bcf.visinfo.Point;
import org.bimserver.bcf.visinfo.VisualizationInfo;
import org.bimserver.emf.ModelMetaData;
import org.bimserver.ifc.IfcModel;
import org.bimserver.interfaces.objects.SIfcHeader;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.plugins.renderengine.IndexFormat;
import org.bimserver.plugins.renderengine.Precision;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.bimserver.plugins.renderengine.RenderEngineSettings;
import org.bimserver.plugins.serializers.SerializerException;
import org.ifcopenshell.IfcOpenShellEngine;
import org.xml.sax.SAXException;

public class ReportWriter {
	private String ifcProjectGuid;
	private String sIfcHeaderFilename;
	private Date sIfcHeaderTimeStamp;
	private Bcf bcf;
	Set<Long> roids;
	RenderEngineModel renderEngineModel;
	
	TempGeometry tempGeometry = new TempGeometry();
	

	public ReportWriter(IfcModel ifcModel, File ifcFile)
			throws DeserializeException, SerializerException, IOException, RenderEngineException {
		bcf = new Bcf();
		List<IfcProject> projects = ifcModel.getAll(IfcProject.class);
		if (projects.size() != 1)
			throw new RuntimeException(
					"The IFC model should have only one IfcProject");
		else {
				ifcProjectGuid = projects.get(0).getGlobalId();
			ModelMetaData mmd = ifcModel.getModelMetaData();
			SIfcHeader sIfcHeader = mmd.getIfcHeader();
			sIfcHeaderFilename = sIfcHeader.getFilename();
			sIfcHeaderTimeStamp = sIfcHeader.getTimeStamp();
			renderEngineModel=getRenderEngineModel(ifcFile);
		}		
	}
	
	public RenderEngineModel getRenderEngineModel(File ifcFile) throws RenderEngineException, IOException{
	/*	PluginManager pluginManager=new PluginManager();
		RenderEngine renderEngine=new JvmIfcEngine(new File("IFC2X3_TC1.exp"), new File(pluginManager.getTempDir(), "ifcenginedll"),pluginManager.getTempDir(), System.getProperty("java.class.path")+File.pathSeparator+"Internal"+File.pathSeparator);
        
	    ((JvmIfcEngine)renderEngine).startJvm();
	    
	    InputStream in=new FileInputStream(ifcFile);*/
		IfcOpenShellEngine ifcOpenShellEngine = new IfcOpenShellEngine("exe/64/win/IfcGeomServer.exe");
		RenderEngineModel model = ifcOpenShellEngine.openModel(ifcFile);
		RenderEngineSettings settings = new RenderEngineSettings();
		settings.setPrecision(Precision.SINGLE);
		settings.setIndexFormat(IndexFormat.AUTO_DETECT);
		settings.setGenerateNormals(false);
		settings.setGenerateTriangles(true);
		settings.setGenerateWireFrame(false);
		model.setSettings(settings);
		model.generateGeneralGeometry();
		return model;
	}
	
    private Markup addMarkup(String ifcSpatialStructureElement, IfcRoot ifcRoot, String comment,String topicGuid){
		Markup markup = new Markup();
		Header header = new Header();
		Header.File headerFile = new Header.File();
		headerFile.setIfcProject(ifcProjectGuid);
		headerFile.setIfcSpatialStructureElement(ifcSpatialStructureElement);
		headerFile.setFilename(sIfcHeaderFilename);
		GregorianCalendar gregorianCalender = new GregorianCalendar();
		gregorianCalender.setTime(sIfcHeaderTimeStamp);
		try {
			headerFile.setDate(DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(gregorianCalender));
		} catch (DatatypeConfigurationException e1) {
			e1.printStackTrace();
		}
		header.getFile().add(headerFile);
		markup.setHeader(header);

		Topic topic = new Topic();
		topic.setGuid(topicGuid);
		topic.setReferenceLink("None Available"); // e.g. URL to external mvdXML
													// file
		topic.setTitle("Issue regarding: " + ifcRoot.getGlobalId());
		markup.setTopic(topic);

		Comment comments = new Comment();
		try {
			comments.setDate(DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(new GregorianCalendar()));
		} catch (DatatypeConfigurationException e1) {
			e1.printStackTrace();
		}
		String commentGuid = UUID.randomUUID().toString();
		comments.setGuid(commentGuid);
		comments.setVerbalStatus("Open");
		comments.setStatus(CommentStatus.ERROR);
		String commentAuthor = System.getProperty("user.name");
		comments.setAuthor(commentAuthor);
		comments.setComment(comment);
		Comment.Topic commentTopic = new Comment.Topic();
		commentTopic.setGuid(topicGuid);
		comments.setTopic(commentTopic);
		markup.getComment().add(comments);		
		return markup;
    }
   
    private VisualizationInfo addVisInfo(IfcProduct ifcProduct) throws SAXException, ParserConfigurationException{
		tempGeometry.setUpCamera(renderEngineModel,ifcProduct);
		VisualizationInfo visualizationInfo = new VisualizationInfo();

		Component component1 = new Component();
		component1.setIfcGuid(ifcProduct.getGlobalId());
		component1.setOriginatingSystem("BCFReportWriter");
		component1.setAuthoringToolId("BCFReportWriter");

		VisualizationInfo.Components components = new VisualizationInfo.Components();
		visualizationInfo.setComponents(components);
		components.getComponent().add(component1);

		Direction cameraDirection = new Direction();
		cameraDirection.setX(tempGeometry.cameraDirectionX);
		cameraDirection.setY(tempGeometry.cameraDirectionY);
		cameraDirection.setZ(tempGeometry.cameraDirectionZ);

		Direction cameraUpVector = new Direction();
		cameraUpVector.setX(tempGeometry.cameraUpVectorX);
		cameraUpVector.setY(tempGeometry.cameraUpVectorY);
		cameraUpVector.setZ(tempGeometry.cameraUpVectorZ);

		Point cameraViewPoint = new Point();
		cameraViewPoint.setX(tempGeometry.cameraViewPointX);
		cameraViewPoint.setY(tempGeometry.cameraViewPointY);
		cameraViewPoint.setZ(tempGeometry.cameraViewPointZ);

		PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
		perspectiveCamera.setFieldOfView(45.0);
		perspectiveCamera.setCameraUpVector(cameraUpVector);
		perspectiveCamera.setCameraViewPoint(cameraViewPoint);
		perspectiveCamera.setCameraDirection(cameraDirection);

		visualizationInfo.setPerspectiveCamera(perspectiveCamera);
		visualizationInfo.setLines(new VisualizationInfo.Lines());
		visualizationInfo
				.setClippingPlanes(new VisualizationInfo.ClippingPlanes());
		
		return visualizationInfo;
    }
	
    
    
    public void addIssue(String ifcSpatialStructureElement, IfcRoot ifcRoot,
			String comment) throws SAXException,
			ParserConfigurationException {
    	UUID uuid=UUID.randomUUID();
		Markup markup=addMarkup(ifcSpatialStructureElement, ifcRoot, comment,uuid.toString());
		VisualizationInfo visInfo=null;
		if (ifcRoot instanceof IfcProduct){
		visInfo=addVisInfo((IfcProduct)ifcRoot);	
		}
		Issue issue = new Issue(uuid,markup,visInfo);
		bcf.addIssue(issue);
	}
    
 /*   private VisualizationInfo addVisInfo(String ifcGuid) throws SAXException, ParserConfigurationException{
		VisualizationInfo visualizationInfo = new VisualizationInfo();

		Component component1 = new Component();
		component1.setIfcGuid(ifcGuid);
		component1.setOriginatingSystem("BCFReportWriter");
		component1.setAuthoringToolId("BCFReportWriter");

		VisualizationInfo.Components components = new VisualizationInfo.Components();
		visualizationInfo.setComponents(components);
		components.getComponent().add(component1);

		Direction cameraDirection = new Direction();
		cameraDirection.setX(tempGeometry.gCameraDirectionX);
		cameraDirection.setY(tempGeometry.gCameraDirectionY);
		cameraDirection.setZ(tempGeometry.gCameraDirectionZ);

		Direction cameraUpVector = new Direction();
		cameraUpVector.setX(tempGeometry.gCameraUpVectorX);
		cameraUpVector.setY(tempGeometry.gCameraUpVectorY);
		cameraUpVector.setZ(tempGeometry.gCameraUpVectorZ);

		Point cameraViewPoint = new Point();
		cameraViewPoint.setX(tempGeometry.gCameraViewPointX);
		cameraViewPoint.setY(tempGeometry.gCameraViewPointY);
		cameraViewPoint.setZ(tempGeometry.gCameraViewPointZ);

		PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
		perspectiveCamera.setFieldOfView(45.0);
		perspectiveCamera.setCameraUpVector(cameraUpVector);
		perspectiveCamera.setCameraViewPoint(cameraViewPoint);
		perspectiveCamera.setCameraDirection(cameraDirection);

		visualizationInfo.setPerspectiveCamera(perspectiveCamera);
		visualizationInfo.setLines(new VisualizationInfo.Lines());
		visualizationInfo
				.setClippingPlanes(new VisualizationInfo.ClippingPlanes());
		
		return visualizationInfo;
    } */ 


	public void writeReport(String output) {

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(
					output);
			bcf.write(outputStream);
		} catch (BcfException | IOException e) {
			e.printStackTrace();
		}
		new File("TempGeometry.txt").delete();
	}
}