package nl.tue.ddss.bcf;


import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.bimserver.geometry.Matrix;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineGeometry;
import org.bimserver.plugins.renderengine.RenderEngineInstance;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;

public class TempGeometry {
	ArrayList<String> stringList = new ArrayList<String>();
	ArrayList<Float> floatList = new ArrayList<Float>();
	ArrayList<Float> xList = new ArrayList<Float>();
	ArrayList<Float> yList = new ArrayList<Float>();
	ArrayList<Float> zList = new ArrayList<Float>();
	private BoundingBox boundingBox;
	double cameraViewPointX;
	double cameraViewPointY;
	double cameraViewPointZ;
	double cameraUpVectorX;
	double cameraUpVectorY;
	double cameraUpVectorZ;
	double cameraDirectionX;
	double cameraDirectionY;
	double cameraDirectionZ;
	
		public BoundingBox getBoundingBox(BoundingBox boundingBox,RenderEngineModel renderEngineModel, IfcProduct ifcProduct){
			RenderEngineInstance renderEngineInstance;
			try {
				renderEngineInstance = renderEngineModel.getInstanceFromExpressId(ifcProduct.getExpressId());
				RenderEngineGeometry geometry = renderEngineInstance.generateGeometry();
				if (geometry != null && geometry.getNrIndices() > 0) {	
					boundingBox=new BoundingBox();
					float[] tranformationMatrix = new float[16];
					if (renderEngineInstance.getTransformationMatrix() != null) {
						tranformationMatrix = renderEngineInstance.getTransformationMatrix();
						tranformationMatrix = Matrix.changeOrientation(tranformationMatrix);
					} else {
						Matrix.setIdentityM(tranformationMatrix, 0);
					}

					for (int i=0; i<geometry.getIndices().length; i++) {
						processExtends(boundingBox, tranformationMatrix, geometry.getVertices(), geometry.getIndices()[i] * 3);
					}
			}
			} catch (RenderEngineException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return boundingBox;
		}
		
		public BoundingBox getBoundingBox(BoundingBox boundingBox,RenderEngineModel renderEngineModel, List<IfcProduct> ifcProducts){
			if (ifcProducts!=null && ifcProducts.size()>0){
			for (IfcProduct ifcProduct:ifcProducts){
				getBoundingBox(boundingBox,renderEngineModel,ifcProduct);
			}
			}
			return boundingBox;
		}
		
		private void processExtends(BoundingBox boundingBox, float[] transformationMatrix, float[] vertices, int index) {
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
		}
		
		public void setUpCamera(RenderEngineModel renderEngineModel,IfcProduct ifcProduct) throws SAXException,
		ParserConfigurationException {
		boundingBox = getBoundingBox(boundingBox,renderEngineModel, ifcProduct);

		Point3d max = boundingBox.getMax();
		Point3d min = boundingBox.getMin();

		double centerX = (max.getX() + min.getX()) / 2;
		double centerY = (max.getY() + min.getY()) / 2;
		double centerZ = (max.getZ() + min.getZ()) / 2;
		double lengthX = max.getX() - min.getX();
		double lengthY = max.getY() - min.getY();
		double lengthZ = max.getZ() - min.getZ();
		
		double lengthMax=lengthX;
		if (lengthY>lengthMax) lengthMax=lengthY;
		if (lengthZ>lengthMax) lengthMax=lengthZ;

		cameraViewPointX = centerX
				+ ((2 * lengthMax) * Math.cos(Math.PI / 4));
		cameraViewPointY = centerY
				+ ((2 * lengthMax) * Math.cos(Math.PI / 4));
		cameraViewPointZ = centerZ + 0.5 * lengthMax;

		cameraDirectionX = centerX - cameraViewPointX;
		cameraDirectionY = centerY - cameraViewPointY;
		cameraDirectionZ = centerZ - cameraViewPointZ;

		cameraUpVectorX = cameraDirectionX;
		cameraUpVectorY = cameraDirectionY;
		cameraUpVectorZ = ((Math.pow(cameraDirectionX, 2) + Math.pow(
				cameraDirectionY, 2)) / -cameraDirectionZ);

	} 

	}
