package nl.tue.ddss.ifc_check;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import nl.tue.ddss.bcf.ReportWriter;
import nl.tue.ddss.ifc_check.IfcHashMapBuilder.ObjectToValue;
import nl.tue.ddss.rule_parse.*;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.bimserver.models.ifc2x3tc1.IfcElement;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.models.ifc2x3tc1.IfcSpatialStructureElement;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.emf.IdEObject;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.*;
import org.buildingsmart_tech.mvdxml.mvdxml1_1.Definitions.Definition;
import org.xml.sax.SAXException;

public class IfcChecker {
	IfcParser ifcParser;
	File ifcFile;
	List<MVDConstraint> constraints;
	ExchangeRequirement er;

	public IfcChecker(File ifcFile, ExchangeRequirement er) {
		this.ifcFile = ifcFile;
		this.er = er;
	}

	public IfcChecker(File ifcFile, List<MVDConstraint> constraints)
			throws DeserializeException, IOException, URISyntaxException {
		this.ifcFile = ifcFile;
		this.constraints = constraints;
		this.ifcParser=new IfcParser(ifcFile);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void checkIfcModel(String outputFolder) throws JAXBException,
			DeserializeException, ParserConfigurationException, SAXException,
			SerializerException, IOException, RenderEngineException {
		ReportWriter reportWriter = new ReportWriter(ifcParser.getIfcModel(), ifcFile);
		for (MVDConstraint constraint : constraints) {
			List<AttributeRule> attributeRules = constraint.getAttributeRules();
			List<TemplateRule> templateRules = constraint.getTemplateRules();
			try {
				Class cls =  Class.forName("org.bimserver.models.ifc2x3tc1."+constraint.getConceptRoot().getApplicableRootEntity()
						);
				List<Object> allRoots = ifcParser.getIfcModel().getAllWithSubTypes(cls);
				for (Object ifcObject : allRoots) {
					IfcHashMapBuilder ifcHashMapBuilder = new IfcHashMapBuilder(
							ifcObject, attributeRules);
					List<HashMap<AbstractRule, ObjectToValue>> hashMaps = ifcHashMapBuilder
							.getHashMaps();
					String comment = new String();
					for (HashMap<AbstractRule, ObjectToValue> hashMap : hashMaps) {
						comment = templateLevelRuleCheck(hashMap);
					}
					if (templateRules.size() > 0) {
						for (TemplateRule templateRule : templateRules) {
							for (int i = 0; i < hashMaps.size(); i++) {
								Boolean result = conceptLevelRuleCheck(
										templateRule.getParameters(),
										hashMaps.get(i));
								if (result != null && result == true) {
									break;
								}
								if (result == false && i == hashMaps.size() - 1) {
									comment = comment
											+ "\n This Object has to fulfil the requirements of "
											+ templateRule.getParameters();
								}
							}
						}
					}
					if (comment.length() > 0) {
						Definitions definitions = constraint.getConcept()
								.getDefinitions();
						if (definitions != null) {
							List<Definition> definitionList = definitions
									.getDefinition();
							for (Definition definition : definitionList) {
								comment = comment + "\n"
										+ definition.getBody().getValue();
							}
						}
						String type = ifcObject.getClass().getSimpleName();
						type = type.substring(0, type.length() - 4);
						if (ifcObject instanceof IfcProduct) {
							String spatialStructureElement = new String();
							if (ifcObject instanceof IfcElement){
							spatialStructureElement = getIfcSpatialStructure((IfcElement) ifcObject);
							}
							List<String> componantGuids = new LinkedList<String>();
							componantGuids = getComponantGuids(componantGuids,
									(IfcProduct) ifcObject);

							try {
								reportWriter.addIssue(spatialStructureElement,
										((IfcProduct) ifcObject),
										((IfcProduct) ifcObject).getGlobalId()
												+ "\n" + comment);
							} catch (SAXException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ParserConfigurationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						else {
							try {
								reportWriter.addIssue(null,
										(IfcRoot) ifcObject,
										((IfcRoot) ifcObject).getGlobalId()
												+ "\n" + comment);
							} catch (SAXException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ParserConfigurationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

						System.out.println(type + " "
								+ ((IfcRoot) ifcObject).getGlobalId() + ": "
								+ comment);
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		new File(outputFolder).mkdir();
		reportWriter.writeReport(outputFolder + "checking_result" + ".bcfzip");
	}

	private String getIfcSpatialStructure(IfcElement ifcObject) {
		String guid = new String();
		List<IfcRelContainedInSpatialStructure> ircsisss = ifcObject
				.getContainedInStructure();
		if (ircsisss != null && ircsisss.size() == 1) {
			IfcSpatialStructureElement isse = ircsisss.get(0)
					.getRelatingStructure();
			guid = isse.getGlobalId();
		}
		return guid;
	}

	private List<String> getComponantGuids(List<String> guids,
			IfcObjectDefinition ifcObject) {
		if (ifcObject.getIsDecomposedBy().size() >= 1) {
			List<IfcRelDecomposes> irds = ifcObject.getIsDecomposedBy();
			for (IfcRelDecomposes ird : irds) {
				List<IfcObjectDefinition> ifcObjects = ird.getRelatedObjects();
				for (IfcObjectDefinition io : ifcObjects) {
					getComponantGuids(guids, io);
				}
			}
		} else
			guids.add(ifcObject.getGlobalId());
		return guids;
	}

	@SuppressWarnings("rawtypes")
	public String templateLevelRuleCheck(
			HashMap<AbstractRule, ObjectToValue> hashMap) {
		String report = new String();
		Set<AbstractRule> rules = hashMap.keySet();
		for (AbstractRule rule : rules) {
			ObjectToValue objectToValue = hashMap.get(rule);
			Object ifcObject = objectToValue.getIfcObject();
			Object value = objectToValue.getValue();
			List<Object> valueList = new ArrayList<Object>();
			if (value == null) {
				valueList = null;
			} else if (value instanceof Collection && value != null) {
				for (Object object : ((Collection) value)) {
					valueList.add(object);
				}
			} else {
				valueList.add(value);
			}
			if (rule instanceof AttributeRule) {
				String cardinality = ((AttributeRule) rule).getCardinality();
				boolean carCheck = cardinalityCheck(cardinality, valueList);

				if (carCheck == false) {
					if (ifcObject == null) {
						report = report + "\n"
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality;
					} else if (ifcObject instanceof IfcRoot) {
						report = report + "\n"
								+ ((IfcRoot) ifcObject).getGlobalId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality;
					} else if (ifcObject instanceof IdEObject) {
						report = report + "\n"
								+ ((IdEObject) ifcObject).getExpressId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality;
					}
				}
			} else {
				String cardinality = ((EntityRule) rule).getCardinality();
				boolean carCheck = cardinalityCheck(cardinality, valueList);
				if (carCheck == false) {
					if (ifcObject == null) {
						report = report + "\n"
								+ ((EntityRule) rule).getEntityName()
								+ " should have " + cardinality;
					} else if (ifcObject instanceof IfcRoot) {
						report = report + "\n"
								+ ((IfcRoot) ifcObject).getGlobalId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality + " "
								+ ((EntityRule) rule).getEntityName();
					} else if (ifcObject instanceof IdEObject) {
						report = report + "\n"
								+ ((IdEObject) ifcObject).getExpressId() + " "
								+ ((AttributeRule) rule).getAttributeName()
								+ " should have " + cardinality + " "
								+ ((EntityRule) rule).getEntityName();
					}
				}
			}
		}
		return report;
	}

	public Boolean conceptLevelRuleCheck(String rule,
			HashMap<AbstractRule, ObjectToValue> hashMap) {
		Boolean result = false;
		CharStream charStream = new ANTLRStringStream(rule);
		MvdXMLv1_1Lexer lexer = new MvdXMLv1_1Lexer(charStream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		MvdXMLv1_1Parser parser = new MvdXMLv1_1Parser(tokenStream, hashMap);
		try {
			result = parser.expression();
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public Boolean cardinalityCheck(String cardinality, List<Object> valueList) {
		Boolean result = true;
		if (cardinality != null) {
			if (cardinality.equals("Zero")) {
				if (valueList.size() > 0) {
					result = false;
				}

			} else if (cardinality.equals("ZeroToOne")) {
				if (valueList.size() > 1) {
					result = false;
				}
			} else if (cardinality.equals("One")) {
				if (valueList==null || valueList.size() != 1) {
					result = false;
				}
			} else if (cardinality.equals("OneToMany")) {
				if (valueList == null || valueList.size()==0) {
					result = false;
				}
			} else if (cardinality.equals("_asSchema")) {

			} else if (cardinality.matches(".+:.+")) {

			} else {
				System.out.println("Cardinality Syntax error of mvdXML");
			}
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public List<Boolean> entityTypeCheck(List<EntityRule> entityRules,
			List<Object> valueList) {
		List<Boolean> result = new ArrayList<Boolean>();
		List<Class> entityTypes = new ArrayList<Class>();
		for (EntityRule entityRule : entityRules) {
			try {
				Class cls = Class.forName("org.bimserver.models.ifc2x3tc1."
						+ entityRule.getEntityName());
				entityTypes.add(cls);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (int i = 0; i < valueList.size(); i++) {
			Object value = valueList.get(i);
			if (value == null) {
				result.add(true);
			}
			if (value instanceof Collection) {
				List<Boolean> cTypeCheckResult = new ArrayList<Boolean>();
				for (Object obj : (Collection) value) {
					for (int j = 0; j < entityTypes.size(); j++) {
						if (entityTypes.get(j).isInstance(value)) {
							cTypeCheckResult.add(true);
							break;
						} else if (j == entityTypes.size() - 1) {
							cTypeCheckResult.add(false);
						}
					}
				}
				for (int j = 0; j < cTypeCheckResult.size(); j++) {
					if (cTypeCheckResult.get(j) == false) {
						result.add(false);
						break;
					} else if (cTypeCheckResult.get(j) == true
							&& j == cTypeCheckResult.size() - 1) {
						result.add(true);
					}
				}
			} else {
				for (int j = 0; j < entityTypes.size(); j++) {
					if (entityTypes.get(j).isInstance(value)) {
						result.add(true);
						break;
					} else if (j == entityTypes.size() - 1) {
						result.add(false);
					}
				}
			}
		}
		return result;
	}

}
