package nl.tue.ddss.bcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.vecmath.*;

import nl.tue.ddss.bcf.XMLHelper;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.xml.sax.SAXException;

public class ColladaParser {
	
	
        HashMap<String, List<Point3d>> pointMap=new HashMap<String,List<Point3d>>();
        HashMap<String,List<Point3d>> triangleMap=new HashMap<String,List<Point3d>>();
	    HashMap<String, Element> idMap = new HashMap<String, Element>();
	    HashMap<String, Element> expressIdMap = new HashMap<String, Element>();
	    HashMap<Element,int[]> pointerMap=new HashMap<Element,int[]>();
	    Document doc;
	    XPath xpath;
	    

	    public HashMap<String, List<Point3d>> getPointMap() {
			return pointMap;
		}

		public void setPointMap(HashMap<String, List<Point3d>> pointMap) {
			this.pointMap = pointMap;
		}

		public HashMap<String, List<Point3d>> getTriangleMap() {
			return triangleMap;
		}

		public void setTriangleMap(HashMap<String, List<Point3d>> triangleMap) {
			this.triangleMap = triangleMap;
		}

		public ColladaParser() {
	        xpath = XPathFactory.newInstance().newXPath();
	    }
	    
	    private List<Point3d> geometry(Matrix4d matrix, Element geometry,Element node) {
	        Element mesh = XMLHelper.element(geometry, "mesh");
	        if(mesh == null) return null;
	        List<Point3d> points=new ArrayList<Point3d>();
	        Element vertices = XMLHelper.element(mesh, "vertices");
	        if(vertices == null) throw new RuntimeException("found a mesh without vertices");
	        
	        Element input = XMLHelper.element(vertices, "input[@semantic='POSITION']");
	        if(input == null) throw new RuntimeException("found a vertices without an input with POSITION semantic");
	        
	        Element source = getSource(input.getAttribute("source"));
	        if(source == null) throw new RuntimeException("could not find source of input");
	        
	        Element technique_common = XMLHelper.element(source, "technique_common");
	        if(technique_common == null) throw new RuntimeException("could not find common technique for vertices in mesh");
	       
	        Element triangles=XMLHelper.element(mesh, "triangles");
	        if(triangles==null) throw new RuntimeException("found no triangles for this geometry");
	        Element in = XMLHelper.element(triangles, "input[@semantic='VERTEX']");
	        if(in == null) throw new RuntimeException("found a vertices without an input with POSITION semantic");
    	    int oft=Integer.parseInt(in.getAttribute("offset"));
    	    Element primitive=XMLHelper.element(triangles, "p");
    	    int inputQuantity=XMLHelper.elements(triangles, "input").size();
    	    int[] ints=int_array(primitive);
    	    int[] pointers=new int[ints.length/inputQuantity];
    	    for (int i=0;i<pointers.length;i++){
    	    	pointers[i]=ints[oft+i*inputQuantity];
    	    }
    	    
    	    pointerMap.put(node, pointers);
    	    
	        Element accessor = XMLHelper.element(technique_common, "accessor");
	        if(accessor == null) throw new RuntimeException("could not find accessor for vertices in mesh"); 
	        
	        

	        int stride;
	        if(!accessor.getAttribute("stride").isEmpty()) stride = Integer.parseInt(accessor.getAttribute("stride"));
	        else stride = 1;
	        
	        int offset;
	        if(!accessor.getAttribute("offset").isEmpty()) offset = Integer.parseInt(accessor.getAttribute("offset"));
	        else offset = 0;
	        
	        int count = Integer.parseInt(accessor.getAttribute("count"));
	        
	        List<Element> params = childElements(accessor);
	        int accessLength = params.size();
	        int indexX = -1;
	        int indexY = -1;
	        int indexZ = -1;
	        
	        for(int i = 0; i < accessLength; i++) {
	            Element param = params.get(i);
	            if(!param.getTagName().equals("param")) throw new RuntimeException("Found something else then a parameter in an accessor");
	            if(!param.getAttribute("type").equals("float")) throw new RuntimeException("I can only understand vertices of the type float");
	                 if(param.getAttribute("name").equals("X")) indexX = i;
	            else if(param.getAttribute("name").equals("Y")) indexY = i;
	            else if(param.getAttribute("name").equals("Z")) indexZ = i;
	        }
	        if(indexX == -1 || indexY == -1 || indexZ == -1) throw new RuntimeException("I only understand 3d mesh data");
	        
	        double[] doubles = double_array(getSource(accessor.getAttribute("source")));
	        
	        int j = offset;
	        for(int i = 0; i < count ;i++) {
	            Point3d p = new Point3d(doubles[j + indexX], doubles[j + indexY], doubles[j + indexZ]);
	            matrix.transform(p);
	            points.add(p);
	            j += stride;
	        }
	        return points;
	    }
	    

	    
	    private List<Point3d> node(Matrix4d matrix, Element node) {
	    	List<Point3d> points=new ArrayList<Point3d>();
	        for(Element element : childElements(node)) {
	            if(element.getTagName().equals("matrix")) {
	                Matrix4d m = matrix(element);
	                m.mul(matrix, m);
	                matrix = m;
	            }
	            else if(element.getTagName().equals("translate")) {
	                Vector3d t = vector(element);
	                Matrix4d m = new Matrix4d();
	                m.setIdentity();
	                m.setTranslation(t);
	                m.mul(matrix, m);
	                matrix = m;     
	            }
	            else if(element.getTagName().equals("rotate")) {
	                AxisAngle4d r = rotate(element);
	                r.setAngle(Math.toRadians(r.getAngle()));
	                Matrix4d m = new Matrix4d();
	                m.setIdentity();
	                m.setRotation(r);
	                m.mul(matrix, m);
	                matrix = m;     
	            }
	            else if(element.getTagName().equals("scale")) {
	                Vector3d s = vector(element);
	                Matrix4d m = new Matrix4d();
	                m.setM00(s.x);
	                m.setM11(s.y);
	                m.setM22(s.z);
	                m.setM33(1.0);
	                m.mul(matrix, m);
	                matrix = m;     
	            }
	            else if(element.getTagName().equals("scew")) {
	                throw new RuntimeException("encountered a scew element that is not supported");
	            }
	            else if(element.getTagName().equals("node")) {
	                node(matrix, element);
	            }
	            else if(element.getTagName().equals("instance_node")) {
	                node(matrix, getSource(element.getAttribute("url")));
	            }
	            else if(element.getTagName().equals("instance_geometry")) {
	                points=geometry(matrix, getSource(element.getAttribute("url")),node);
	            }            
	        }
	        return points;
	    }
	    
	    
	    private Vector3d vector(Element element) {
	        String[] parts = element.getTextContent().trim().split("\\s+");
	        if(parts.length != 3) throw new RuntimeException("found a vector without 3 components");        
	        return new Vector3d(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	    }
	    
	    private AxisAngle4d rotate(Element element) {
	        String[] parts = element.getTextContent().trim().split("\\s+");
	        if(parts.length != 4) throw new RuntimeException("found a rotation without 4 components");        
	        return new AxisAngle4d(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
	    }

	    
	    private Matrix4d matrix(Element element) {
	        String[] parts = element.getTextContent().trim().split("\\s+");
	        if(parts.length != 16) throw new RuntimeException("found a matrix without 16 components");                
	        Matrix4d m = new Matrix4d();
	        for(int r = 0 ; r < 4; r++) for(int c = 0; c < 4; c++) {
	            m.setElement(r, c, Double.parseDouble(parts[4 * r + c]));
	        }
	        
	        return m;
	    }
	    
	    private double[] double_array(Element element) {
	        String[] parts = element.getTextContent().trim().split("\\s+");
	        double[] double_array = new double[parts.length];
	        for(int i = 0; i < parts.length; i++) {
	            double_array[i] = Double.parseDouble(parts[i]);
	        }
	        return double_array;
	    }
	    
	    private int[] int_array(Element element) {
	        String[] parts = element.getTextContent().trim().split("\\s+");
	        int[] int_array = new int[parts.length];
	        for(int i = 0; i < parts.length; i++) {
	            int_array[i] = Integer.parseInt(parts[i]);
	        }
	        return int_array;
	    }
	    
	    private List<Element> childElements(Element element) {
	        List<Element> elements = new LinkedList<Element>();
	        if (element!=null){
	        Node candidate = element.getFirstChild();
	        while(candidate!=null) {
	            if(candidate instanceof Element) elements.add((Element) candidate);
	            candidate = candidate.getNextSibling();
	        }
	    }
	        return elements;
	    }
	    
	    private Element parentElement(Element element) {
	        Element parent=null;        
	        Node candidate = element.getParentNode();      
	            if(candidate instanceof Element) parent=(Element)candidate;
	        return parent;
	    }
	    
	    
	    private Element getSource(String source) {
	        if(source.startsWith("#")) {
	            Element result = idMap.get(source.substring(1));
	            if(result == null) throw new RuntimeException("Unable to find source: "+source);
	            return result;
	        }
	        else {
	            throw new RuntimeException("unable to parse source xpointer: "+source);
	        }
	    }
	    
	    private void buildGuidMap(){
	    	try {
	    		NodeList instanceGeometries = (NodeList) xpath.evaluate("//instance_geometry", doc, XPathConstants.NODESET);
	            for(int i = 0, n = instanceGeometries.getLength(); i < n; i++) {
	                Element instanceGeometry = (Element) instanceGeometries.item(i);
	                String url=instanceGeometry.getAttribute("url");
	                expressIdMap.put(url.substring(1,url.indexOf("_")), parentElement(instanceGeometry));
	            }
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    private void buildIdMap() {
	        try {
	            NodeList idNodes = (NodeList) xpath.evaluate("//*[@id]", doc, XPathConstants.NODESET);
	            for(int i = 0, n = idNodes.getLength(); i < n; i++) {
	                Element idNode = (Element) idNodes.item(i);
	                idMap.put(idNode.getAttribute("id"), idNode);
	            }
	        } catch (XPathExpressionException ex) { 
	            // This never happens
	            ex.printStackTrace();
	        }
	    }
	    

	    private void buildMaps(Document doc) {
	        this.doc = doc;
	        buildIdMap();
	        buildGuidMap();

	        for (String expressId:expressIdMap.keySet()){
		        List<Point3d> points=new ArrayList<Point3d>();
		        List<Point3d> triangleVertices=new ArrayList<Point3d>();
		        Element node=expressIdMap.get(expressId);
	            Matrix4d matrix = new Matrix4d();
	            matrix.setIdentity();

	        
	        // Handle the unit
	        Double meter = XMLHelper.number(doc.getDocumentElement(), "/COLLADA/asset/unit/@meter");
	        if(meter != null) {
	            Matrix4d m = new Matrix4d();
	            m.setM00(meter);
	            m.setM11(meter);
	            m.setM22(meter);
	            m.setM33(meter);
	            matrix.mul(m);
	        }
	        points=node(matrix, node);
	        int[] pointers=pointerMap.get(node);
	        for (int i=0;i<pointers.length;i++){
	        	triangleVertices.add(points.get(pointers[i]));
	        }
	        
	        pointMap.put(expressId, points);
	        triangleMap.put(expressId, triangleVertices);
	        }
	    }
	            
	    
        public void buildMaps(File file) throws ParserConfigurationException, SAXException, IOException {        
	        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	        buildMaps(builder.parse(file));
	    }
	    


	}

