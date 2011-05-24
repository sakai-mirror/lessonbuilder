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
 * $URL: http://ims-dev.googlecode.com/svn/trunk/cc/IMS_CCParser_v1p0/src/main/java/org/imsglobal/cc/PrintHandler.java $
 * $Id: PrintHandler.java 227 2011-01-08 18:26:55Z drchuck $
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

import org.jdom.Element;
import org.jdom.Namespace;
import java.util.List;
import java.util.ArrayList;

/* PJN NOTE:
 * This class is an example of what an implementer might want to do as regards overloading DefaultHandler.
 * In this case, messages are written to the screen. If a method in default handler is not overridden, then it does
 * nothing.
 */

public class PrintHandler extends DefaultHandler implements AssessmentHandler, DiscussionHandler, AuthorizationHandler,
                                       MetadataHandler, LearningApplicationResourceHandler, QuestionBankHandler,
                                       WebContentHandler, WebLinkHandler{


  
  private static final String HREF="href";
  private static final String TYPE="type";
  private static final String FILE="file";
  private static final String XML=".xml";
  private static final String TITLE="title";
  private static final Namespace CC_NS = Namespace.getNamespace("ims", "http://www.imsglobal.org/xsd/imscc/imscp_v1p1");
  private static final String CC_ITEM_TITLE="title";
  private static final String CC_WEBCONTENT="webcontent";
  private static final String CC_WEBLINK0="imswl_xmlv1p0";
  private static final String CC_WEBLINK1="imswl_xmlv1p1";
  private static final String CC_TOPIC0="imsdt_xmlv1p0";
  private static final String CC_TOPIC1="imsdt_xmlv1p1";
  private static final String CC_ASSESSMENT0="imsqti_xmlv1p2/imscc_xmlv1p0/assessment";
  private static final String CC_ASSESSMENT1="imsqti_xmlv1p2/imscc_xmlv1p1/assessment";
  private static final String CC_QUESTION_BANK0="imsqti_xmlv1p2/imscc_xmlv1p0/question-bank";
  private static final String CC_QUESTION_BANK1="imsqti_xmlv1p2/imscc_xmlv1p1/question-bank";
  private static final String CC_BLTI0="imsbasiclti_xmlv1p0";
  private static final String CC_BLTI1="imsbasiclti_xmlv1p1";
  private static final boolean all = false;
  private List<String> pages = new ArrayList<String>();

  public void setAssessmentDetails(String the_ident, String the_title) {
      if (all)
	  System.err.println("assessment ident: "+the_ident +" title: "+the_title);
  }

  public void endCCFolder() {
      if (all)
	  System.err.println("cc folder ends");
      pages.remove(pages.size()-1);
  }

  public void endCCItem() {
      if (all)
	  System.err.println("cc item ends");
  }

  public void startCCFolder(Element folder) {
      String title = folder.getChildText(TITLE, CC_NS);
      System.err.println("create page " + title);
      pages.add(title);
  }

  public void startCCItem(String the_id, String the_title) {
      if (all) {
	  System.err.println("cc item "+the_id+" begins");
	  System.err.println("title: "+the_title);
      }
  }

  private String getFileName(Element resource) {
      Element file = resource.getChild(FILE, CC_NS);
      if (file != null)
	  return file.getAttributeValue(HREF);
      else
	  return null;
  }

  public void setCCItemXml(Element the_xml, Element resource, AbstractParser parser, CartridgeLoader loader) {
      System.err.println("\nadd item to page " + pages.get(pages.size()-1) +
			 " xml: "+the_xml + 
			 " title " + the_xml.getChildText(CC_ITEM_TITLE, CC_NS) +
			 " type " + resource.getAttributeValue(TYPE) +
			 " href " + resource.getAttributeValue(HREF));
      String type = resource.getAttributeValue(TYPE);

      try {
      if (type.equals(CC_WEBCONTENT))
	  System.err.println("web content " + resource.getAttributeValue(HREF));
      else if (type.equals(CC_WEBLINK0) || type.equals(CC_WEBLINK1))
	  System.err.println("weblink " + getFileName(resource));
      else if (type.equals(CC_TOPIC0) || type.equals(CC_TOPIC1)) {
	  System.err.println("topic " + getFileName(resource) +
			     " " + parser.getXML(loader, getFileName(resource)));
      }
      else if (type.equals(CC_ASSESSMENT0) || type.equals(CC_ASSESSMENT1))
	  System.err.println("assessment " + getFileName(resource) + 
			     " " + parser.getXML(loader, getFileName(resource)));
      else if (type.equals(CC_QUESTION_BANK0) || type.equals(CC_QUESTION_BANK1))
	  System.err.println("question bank " + getFileName(resource) +
			     " " + parser.getXML(loader, getFileName(resource)));
      else if (type.equals(CC_BLTI0) || type.equals(CC_BLTI1))
	  System.err.println("blti " + getFileName(resource) + 
			     " " + parser.getXML(loader, getFileName(resource)));
      else
	  System.err.println("implemented type: " + resource.getAttributeValue(TYPE));
      } catch (Exception e) {
	  System.err.println(">>>Exception " + e);
      }

  }

  public void addAttachment(String attachment_path) {
      if (all)
	  System.err.println("adding an attachment: "+attachment_path);
  }

  public void endDiscussion() {
      if (all)
	  System.err.println("end discussion");
  }

  public void startManifest() {
      if (all)
	  System.err.println("start manifest");
  }

  public void setManifestXml(Element the_xml) {
      if (all)
	  System.err.println("manifest xml: "+the_xml);
  }

  public void endManifest() {
      if (all)
	  System.err.println("end manifest");
  }

  public void startDiscussion(String topic_name, String text_type, String text, boolean isProtected) {
      if (all){
	  System.err.println("start a discussion: "+topic_name);
	  System.err.println("text type: "+text_type);
	  System.err.println("text: "+text); 
	  System.err.println("protected: "+isProtected);
      }
  }

  public void endWebLink() {
      if (all)
	  System.err.println("end weblink");
  }

  public void startWebLink(String the_title, String the_url, String the_target, String the_window_features, boolean isProtected) {
      if (all) {
	  System.err.println("start weblink: "+the_title);
	  System.err.println("link to: "+the_url);
	  System.err.println("target window: "+the_target);
	  System.err.println("window features: "+the_window_features);
	  System.err.println("protected: "+isProtected);
      }
  }
 
  public void setWebLinkXml(Element the_link) {
      if (all)
	  System.err.println("weblink xml: "+the_link);
  }

  public void addFile(String the_file_id) {
      System.err.println("adding file: "+the_file_id);
  }

  public void endWebContent() {
      if (all)
	  System.err.println("ending webcontent");
  }

  public void startWebContent(String entry_point, boolean isProtected) {
      if (all) {
	  System.err.println("start web content");
	  System.err.println("protected: "+isProtected);
	  if (entry_point!=null) {
	      System.err.println("entry point is: "+entry_point);
	  }
      }
  }

  public void endLearningApplicationResource() {
      if (all)
	  System.err.println("end learning application resource");
  }

  public void startLearningApplicationResource(String entry_point, boolean isProtected) {
      if (all) {
	  System.err.println("start learning application resource");
	  System.err.println("protected: "+isProtected);
	  if (entry_point!=null) {
	      System.err.println("entry point is: "+entry_point);
	  }
      }
  }

  public void endAssessment() {
      if (all)
	  System.err.println("end assessment");    
  }

  public void setAssessmentXml(Element xml) {
      if (all)
	  System.err.println("assessment xml: "+xml);
  }

  public void startAssessment(String the_file_name, boolean isProtected) {
      if (all) {
	  System.err.println("start assessment contained in: "+the_file_name);
	  System.err.println("protected: "+isProtected);
      }
  }

  public void endQuestionBank() {
      if (all)
	  System.err.println("end question bank");
  }

  public void setQuestionBankXml(Element the_xml) {
      if (all)
	  System.err.println("question bank xml: "+the_xml);
  }

  public void startQuestionBank(String the_file_name, boolean isProtected) {
      if (all) {
	  System.err.println("start question bank in: "+the_file_name);
	  System.err.println("protected: "+isProtected);
      }
  }

  public void setAuthorizationServiceXml(Element the_node) {
      if (all)
	  System.err.println(the_node);
  }

  public void setAuthorizationService(String cartridgeId, String webservice_url) {
      if (all)
	  System.err.println("adding auth service for "+cartridgeId+" @ "+webservice_url);
  }

  public void endAuthorization() {
      if (all)
	  System.err.println("end of authorizations");
  }

  public void startAuthorization(boolean isCartridgeScope, boolean isResourceScope, boolean isImportScope) {
      if (all) {
	  System.err.println("start of authorizations");
	  System.err.println("protect all: "+isCartridgeScope);
	  System.err.println("protect resources: "+isResourceScope);
	  System.err.println("protect import: "+isImportScope);
      }
  }

  public void endManifestMetadata() {
      if (all)
	  System.err.println("end of manifest metadata");
  }

  public void startManifestMetadata(String schema, String schema_version) {
      if (all) {
	  System.err.println("start manifest metadata");
	  System.err.println("schema: "+schema);
	  System.err.println("schema_version: "+schema_version);
      }
  }
 
  public void setPresentationXml(Element the_xml) {
      if (all)
	  System.err.println("QTI presentation xml: "+the_xml);
  }

  public void setQTICommentXml(Element the_xml) {
      if (all)
	  System.err.println("QTI comment xml: "+the_xml);
  }

  public void setSection(String ident, String title) {
      if (all) {
	  System.err.println("set section ident: "+ident);
	  System.err.println("set section title: "+title);
      }
  }

  public void setSectionXml(Element the_xml) {
      if (all)
	  System.err.println("set Section Xml: "+the_xml);
  }

  public void endQTIMetadata() {
      if (all)
	  System.err.println("end of QTI metadata");
  }

  public void setManifestMetadataXml(Element the_md) {
      if (all)
	  System.err.println("manifest md xml: "+the_md);    
  }

  public void setResourceMetadataXml(Element the_md) {
      if (all)
	  System.err.println("resource md xml: "+the_md); 
  }

  public void addQTIMetadataField(String label, String entry) {
      if (all) {
	  System.err.println("QTI md label: "+label);
	  System.err.println("QTI md entry: "+entry);
      }
}

  public void setQTIComment(String the_comment) {
      if (all)
	  System.err.println("QTI comment: "+the_comment);
  }

  public void endDependency() {
      if (all)
	  System.err.println("end dependency");
  }

  public void startDependency(String source, String target) {
      if (all)
	  System.err.println("start dependency- resource : "+source+" is dependent upon: "+target);
  }

  public void startResource(String id, boolean isProtected) {
      if (all)
	  System.err.println("start resource: "+id+ " protected: "+isProtected);
  }

  public void setResourceXml(Element the_xml) {
      if (all)
	  System.err.println("resource xml: "+the_xml);
  }

  public void endResource() {
      if (all)
	  System.err.println("end resource"); 
  }

  public void addAssessmentItem(QTIItem the_item) {
      if (all)
	  System.err.println("add QTI assessment item: "+the_item.toString());
    
  }

  public void addQTIMetadataXml(Element the_md) {
      if (all)
	  System.err.println("add QTI metadata xml: "+the_md);
    
  }

  public void startQTIMetadata() {
      if (all)
	  System.err.println("start QTI metadata");
  }

  public void setDiscussionXml(Element the_element) {
      if (all)
	  System.err.println("set discussion xml: "+the_element); 
  }

  public void addQuestionBankItem(QTIItem the_item) {
      if (all)
	  System.err.println("add QTI QB item: "+the_item.toString()); 
  }

  public void setQuestionBankDetails(String the_ident) {
      if (all)
	  System.err.println("set qti qb details: "+the_ident);  
  }
}

