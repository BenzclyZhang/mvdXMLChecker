package nl.tue.ddss.bcf;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Point3d;
import javax.xml.parsers.ParserConfigurationException;

import nl.tue.ddss.ifc_check.IfcParser;

import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.xml.sax.SAXException;

public class GeometryLoader {
    private String ifcFile;
    private HashMap<String,List<Point3d>> productGeometry;
    private HashMap<String,List<Point3d>> pointMap;
     
     public GeometryLoader(String ifcFile) throws IOException, RenderEngineException, ParserConfigurationException, SAXException{
    	 this.ifcFile=ifcFile;
    	 ColladaConverter.convert(ifcFile);
    	 ColladaParser cp=new ColladaParser();
    	 cp.buildMaps(new File("D:\\Demo\\Model_View_Checker\\temp\\temp.dae"));
    	 pointMap=cp.getTriangleMap();
    	 productGeometry=new HashMap<String,List<Point3d>>();
     }
        
 	
 	public BoundingBox getBoundingBox(IfcProduct ifcProduct) throws IOException{
        BoundingBox boundingBox=new BoundingBox();
        List<Point3d> points=getGeometryforProduct(ifcProduct);
        for(Point3d point:points){
        	boundingBox.add(point);
        }
		return boundingBox;
	}
 	
 	private List<Point3d> getGeometryforProduct(IfcProduct ifcProduct) throws IOException{
 		List<Point3d> points=pointMap.get(Integer.toString(ifcProduct.getExpressId()));        
		return points;
 	}
 	
 	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap<String,List<Point3d>> getProductGeometry() throws ClassNotFoundException, DeserializeException, IOException, URISyntaxException{
 		IfcParser ifcParser=new IfcParser(new File(ifcFile));
 		Class cls=Class.forName("org.bimserver.models.ifc2x3tc1.IfcProduct");
 		List<Object> allProducts = ifcParser.getIfcModel().getAllWithSubTypes(cls);
 		for (Object object:allProducts){
 			List<Point3d> points=pointMap.get(Integer.toString(((IfcProduct)object).getExpressId()));
 			if (points!=null&&points.size()>0){
 				productGeometry.put(((IfcProduct)object).getGlobalId(), points);
 			}
 		}
		return productGeometry;
 	}
 	
 	
/* 	private void processExtends(BoundingBox boundingBox, float[] transformationMatrix, float[] vertices, int index) {
		float x = vertices[index];
		float y = vertices[index + 1];
		float z = vertices[index + 2];
		float[] result = new float[4];
		Matrix.multiplyMV(result, 0, transformationMatrix, 0, new float[]{x, y, z, 1}, 0);
		x = result[0];
		y = result[1];
		z = result[2];
		Point3d point=new Point3d(x/1000,y/1000,z/1000);
		boundingBox.add(point);
	}*/
 	
 /*	private Point3d processPoints(float[] transformationMatrix, float[] vertices, int index){
 		float x = vertices[index];
		float y = vertices[index + 1];
		float z = vertices[index + 2];
		float[] result = new float[4];
		Matrix.multiplyMV(result, 0, transformationMatrix, 0, new float[]{x, y, z, 1}, 0);
		x = result[0];
		y = result[1];
		z = result[2];
		Point3d point=new Point3d(x/1000,y/1000,z/1000);
		return point;
 	}*/
}
