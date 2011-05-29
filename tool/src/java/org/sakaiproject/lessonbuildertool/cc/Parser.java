package org.sakaiproject.lessonbuildertool.cc;

/***********
 * This code is based on a reference implementation done for the IMS Consortium.
 * The copyright notice for that implementation is included below. 
 * All modifications are covered by the following copyright notice.
 *
 * Copyright (c) 2011 Rutgers, the State University of New Jersey
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");                                                                
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**********************************************************************************
 * $URL: http://ims-dev.googlecode.com/svn/trunk/cc/IMS_CCParser_v1p0/src/main/java/org/imsglobal/cc/Parser.java $
 * $Id: Parser.java 227 2011-01-08 18:26:55Z drchuck $
 **********************************************************************************
 *
 * Copyright (c) 2010 IMS GLobal Learning Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. 
 *
 **********************************************************************************/

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

/**
 * 
 * This is a simple cartridge parser library for IMS common cartridges. This parser is non validating, and has been
 * written against version 1.0 of the CC specification.
 * 
 * Users of the parser need to do the following:
 * 1) Create a Cartridge Loader, and supply it with a ZIP file.
 * 2) Create a Parser: the createCartridgeParser(CartridgeLoader) method in this class.
 * 3) Override DefaultHandler to process the events that arise during the parse process.
 * 4) Call Parser.parse(DefaultHandler) with your default Handler.
 * 
 * The parser will read the manifest file, as well as any declared xml resources (question banks, assessments, discussions,
 * and weblinks). DefaultHandler will always return xml in the form of JDOM elements, as well as (in some cases), java
 * objects (strings mostly). The parser will also return the details for authorization services, metadata and indicate if 
 * a resource is protected or not.
 * 
 * @author Phil Nicholls
 * @version 1.0
 *
 */

public class Parser extends AbstractParser {
  
  CartridgeLoader utils; 
  
  private static final Map<String, ContentParser> parsers;
  
  private static final String IMS_MANIFEST="imsmanifest.xml";
  
  private static final String LAR="associatedcontent/imscc_xmlv1p0/learning-application-resource";
  private static final String DISCUSSION="imsdt_xmlv1p0";
  private static final String ASSESSMENT="imsqti_xmlv1p2/imscc_xmlv1p0/assessment";
  private static final String WEBLINK="imswl_xmlv1p0";
  private static final String WEBCONTENT="webcontent";
  private static final String QUESTION_BANK="imsqti_xmlv1p2/imscc_xmlv1p0/question-bank";
  
  private static final String AUTH_QUERY="/ims:manifest/auth:authorizations";
  private static final String ITEM_QUERY="/ims:manifest/ims:organizations/ims:organization/ims:item";
  
  private static final QuestionBankParser qbp;
  
  private static final String AUTH_IMPORT="import";
  private static final String AUTH_ACCESS="access";
  private static final String AUTH_ACCESS_CARTRIDGE="cartridge";
  private static final String AUTH_ACCESS_RESOURCE="resource";
  
  private static final Namespace CC_NS = Namespace.getNamespace("ims", "http://www.imsglobal.org/xsd/imscc/imscp_v1p1");
  private static final Namespace AUTH_NS = Namespace.getNamespace("auth", "http://www.imsglobal.org/xsd/imsccauth_v1p0");
  private static final Namespace MD_NS= Namespace.getNamespace("lom", "http://ltsc.ieee.org/xsd/imscc/LOM");
  
  private static final String AUTH_AUTHORIZATION="authorization";
  private static final String AUTH_CCID="cartridgeId";
  private static final String AUTH_WEBSERVICE="webservice";
  
  private static final String MD="metadata";
  private static final String MD_SCHEMA="schema";
  private static final String MD_SCHEMA_VERSION="schemaversion";
  private static final String MD_ROOT="lom";
  
  private static final String CC_ITEM="item";
  private static final String CC_ITEM_ID="identifier";
  private static final String CC_ITEM_IDREF="identifierref";
  private static final String CC_ITEM_TITLE="title";
  private static final String CC_RESOURCE="resource";
  private static final String CC_RESOURCES="resources";
  private static final String CC_RES_TYPE="type";
  
  static {
    qbp=new QuestionBankParser();
    parsers=new HashMap<String, ContentParser>();
    parsers.put(LAR, new LearningApplicationResourceParser());
    parsers.put(DISCUSSION, new DiscussionParser());
    parsers.put(ASSESSMENT, new AssessmentParser());
    parsers.put(WEBLINK, new WebLinkParser());
    parsers.put(WEBCONTENT, new WebContentParser());
  }
  
  private
  Parser(CartridgeLoader the_cu) {
    super();
    utils=the_cu;
  }
  
  public void
  parse(DefaultHandler the_handler) throws FileNotFoundException, ParseException {
    try {
      Element manifest=this.getXML(utils, IMS_MANIFEST);
      processManifest(manifest, the_handler);
    } catch (Exception e) {
	System.out.println("before stack trace");
	e.printStackTrace();
	System.out.println("parse error " + e);
	//      throw new ParseException(e.getMessage(),e);
    }
  }
  
  private void
  processManifest(Element the_manifest, DefaultHandler the_handler) throws ParseException {
    the_handler.startManifest();
    the_handler.setManifestXml(the_manifest);
    processAuthorization(the_manifest, the_handler); 
    processManifestMetadata(the_manifest, the_handler);
    try {
      XPath path=XPath.newInstance(ITEM_QUERY);
      path.addNamespace(CC_NS);
      Element item = (Element)path.selectSingleNode(the_manifest);
      if (item!=null) {     
	  System.out.println("have item " + item);
        for (Iterator iter=item.getChildren(CC_ITEM, CC_NS).iterator();iter.hasNext();) {
	    Element thisitem = (Element)iter.next();
	    System.out.println("processitem " + thisitem + "::" + the_manifest.getChild(CC_RESOURCES, CC_NS));
          processItem((Element)thisitem, the_manifest.getChild(CC_RESOURCES, CC_NS), the_handler);
        }
      } 
      //now we need to check for the question bank...
      if (the_manifest.getChild(CC_RESOURCES, CC_NS) != null &&
	  the_manifest.getChild(CC_RESOURCES, CC_NS).getChildren(CC_RESOURCE, CC_NS) != null)
      for (Iterator iter=the_manifest.getChild(CC_RESOURCES, CC_NS).getChildren(CC_RESOURCE, CC_NS).iterator(); iter.hasNext(); ) {
        Element resource=(Element)iter.next();
        if (resource.getAttributeValue(CC_RES_TYPE).equals(QUESTION_BANK)) {
	    // I know it's not really an item, but it uses the same code as an assessment
	    the_handler.setCCItemXml(null, resource, this, utils);
          processResource(resource, the_handler);
          qbp.parseContent(the_handler, utils, resource, isProtected(resource));
        }
      }
      the_handler.endManifest();
    } catch (JDOMException e) {
      System.err.println(e.getMessage());
      throw new ParseException(e.getMessage(),e);
    }
  }
  
  private void 
  processManifestMetadata(Element manifest,
                          DefaultHandler the_handler) {
    Element metadata=manifest.getChild(MD, CC_NS);
    if (metadata!=null) {
      the_handler.startManifestMetadata(metadata.getChildText(MD_SCHEMA, CC_NS), 
                                        metadata.getChildText(MD_SCHEMA_VERSION, CC_NS));
      Element lom=metadata.getChild(MD_ROOT, MD_NS);
      if (lom!=null) {
        the_handler.setManifestMetadataXml(lom);
        the_handler.endManifestMetadata();
      }
    }
  }

  private void 
  processAuthorization(Element the_manifest,
                       DefaultHandler the_handler) throws ParseException {
    try {
      XPath path=XPath.newInstance(AUTH_QUERY);
      path.addNamespace(CC_NS);
      path.addNamespace(AUTH_NS);
      Element result=(Element)path.selectSingleNode(the_manifest);
      if (result!=null) {
        String import_scope=result.getAttributeValue(AUTH_IMPORT);
        String access_scope=result.getAttributeValue(AUTH_ACCESS);
        if (access_scope.equals(AUTH_ACCESS_CARTRIDGE)) {
          the_handler.startAuthorization(true, false, Boolean.parseBoolean(import_scope));
        } else {
          if (access_scope.equals(AUTH_ACCESS_RESOURCE)) {
            the_handler.startAuthorization(false, true, Boolean.parseBoolean(import_scope));
          }
        }
        Element authorizationElement = result.getChild(AUTH_AUTHORIZATION, AUTH_NS);
        the_handler.setAuthorizationServiceXml(authorizationElement);
        the_handler.setAuthorizationService(authorizationElement.getChildText(AUTH_CCID, AUTH_NS), 
                                            authorizationElement.getChildText(AUTH_WEBSERVICE,  AUTH_NS));
        the_handler.endAuthorization();
      } 
    } catch (Exception e) {
      throw new ParseException(e.getMessage(),e);
    }
  }
  
  private void
  processItem(Element the_item, 
              Element the_resources,
              DefaultHandler the_handler) throws ParseException {
    if (the_item.getAttributeValue(CC_ITEM_IDREF)!=null) {
      Element resource=findResource(the_item.getAttributeValue(CC_ITEM_IDREF), the_resources);
      System.out.println("item proce 1");
      the_handler.startCCItem(the_item.getAttributeValue(CC_ITEM_ID),
                              the_item.getChildText(CC_ITEM_TITLE, CC_NS));
      System.out.println("item proce 2");
      the_handler.setCCItemXml(the_item, resource, this, utils);
      System.out.println("item proce 3");
      ContentParser parser=parsers.get(resource.getAttributeValue(CC_RES_TYPE));
      if (parser==null) {
        throw new ParseException("content type not recongised");
      }
      processResource(resource,
                      the_handler);
      parser.parseContent(the_handler, utils, resource, isProtected(resource));
      the_handler.endCCItem();
    } else {
      the_handler.startCCFolder(the_item);
      for (Iterator iter=the_item.getChildren(CC_ITEM, CC_NS).iterator();iter.hasNext();) {
        processItem((Element)iter.next(), the_resources, the_handler);
      }
      the_handler.endCCFolder();
      }
  } 
  
  public static Parser
  createCartridgeParser(CartridgeLoader the_cartridge) throws FileNotFoundException, IOException {
    return new Parser(the_cartridge);
  }  
}
