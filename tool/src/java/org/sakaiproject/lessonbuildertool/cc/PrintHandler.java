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
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.CharArrayWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.sakaiproject.util.Validator;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.content.cover.ContentTypeImageService;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.lessonbuildertool.cc.QtiImport;
import org.sakaiproject.lessonbuildertool.service.LessonEntity;
import org.sakaiproject.lessonbuildertool.service.QuizEntity;
import org.sakaiproject.lessonbuildertool.service.ForumInterface;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.sakaiproject.tool.assessment.services.qti.QTIService;
import org.sakaiproject.tool.assessment.qti.constants.QTIVersion;

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
  private static final String URL="url";
  private static final String TITLE="title";
  private static final String TEXT="text";
  private static final String TEXTTYPE="texttype";
  private static final String TEXTHTML="text/html";
  private static final String DESCRIPTION="description";
  private static final String GENERAL="general";
  private static final String STRING="string";
  private static final String ATTACHMENT="attachment";
  private static final String ATTACHMENTS="attachments";
    
  private static final Namespace CC_NS = Namespace.getNamespace("ims", "http://www.imsglobal.org/xsd/imscc/imscp_v1p1");
  private static final Namespace MD_NS= Namespace.getNamespace("lom", "http://ltsc.ieee.org/xsd/imscc/LOM");
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
  private static final int MAX_ATTEMPTS = 100;

  private List<SimplePage> pages = new ArrayList<SimplePage>();
    // list parallel to pages containing sequence of last item on the page
  private List<Integer> sequences= new ArrayList<Integer>();
  CartridgeLoader utils = null;
  SimplePageBean simplePageBean = null;
  SimplePageToolDao simplePageToolDao = null;

  private String title = null;
  private String description = null;
  private String baseName = null;
  private String siteId = null;
  private LessonEntity quiztool = null;
  private LessonEntity topictool = null;
  private Set<String> filesAdded = new HashSet<String>();

  public PrintHandler(SimplePageBean bean, CartridgeLoader utils, SimplePageToolDao dao, LessonEntity q, LessonEntity l) {
      super();
      this.utils = utils;
      this.simplePageBean = bean;
      this.simplePageToolDao = dao;
      this.siteId = bean.getCurrentSiteId();
      this.quiztool = q;
      this.topictool = l;
      System.out.println("setting up quiztool " + q + " topictool " + l);
  }

  public void setAssessmentDetails(String the_ident, String the_title) {
      if (all)
	  System.err.println("assessment ident: "+the_ident +" title: "+the_title);
  }

  public void endCCFolder() {
      if (all)
	  System.err.println("cc folder ends");
      int top = pages.size()-1;
      sequences.remove(top);
      pages.remove(top);
  }

  public void endCCItem() {
      if (all)
	  System.err.println("cc item ends");
  }

  public void startCCFolder(Element folder) {
      String title = this.title;
      if (folder != null)
	  title = folder.getChildText(TITLE, CC_NS);
      System.err.println("create page " + title);

      // add top level pages to left margin
      SimplePage page = null;
      if (pages.size() == 0) {
	  page = simplePageBean.addPage(title, false);  // add new top level page
	  SimplePageItem item = simplePageToolDao.makeItem(page.getPageId(), 1, SimplePageItem.TEXT, "", "");
	  item.setHtml(Validator.escapeHtml(description));
	  simplePageBean.saveItem(item);
	  sequences.add(1);
      } else {
	  page = simplePageToolDao.makePage("0", siteId, title, 0L, 0L);
	  simplePageBean.saveItem(page);
	  SimplePage parent = pages.get(pages.size()-1);
	  int seq = simplePageBean.getItemsOnPage(parent.getPageId()).size() + 1;

	  SimplePageItem item = simplePageToolDao.makeItem(parent.getPageId(), seq, SimplePageItem.PAGE, Long.toString(page.getPageId()), title);
	  simplePageBean.saveItem(item);
	  sequences.add(0);
      }
      pages.add(page);
  }

  public void startCCItem(String the_id, String the_title) {
      if (all) {
	  System.err.println("cc item "+the_id+" begins");
	  System.err.println("title: "+the_title);
      }
  }

  private String makeBaseFolder(String name) {

      if (siteId == null) {
	  simplePageBean.setErrKey("simplepage.nosite");
	  return null;
      }

      if (name == null) 
	  name = "Common Cartridge";
      if (name.trim().length() == 0) 
	  name = "Common Cartridge";

      // we must reject certain characters that we cannot even escape and get into Tomcat via a URL                               

      StringBuffer newname = new StringBuffer(ContentHostingService.getSiteCollection(siteId));

      int length = name.length();
      for (int i = 0; i < length; i++) {
	  if (Validator.INVALID_CHARS_IN_RESOURCE_ID.indexOf(name.charAt(i)) != -1)
	      newname.append("_");
	  else
	      newname.append(name.charAt(i));
      }

      length = newname.length();
      if (length > (ContentHostingService.MAXIMUM_RESOURCE_ID_LENGTH - 5))
	  length = ContentHostingService.MAXIMUM_RESOURCE_ID_LENGTH - 5; // for trailing / and possible count
      newname.setLength(length);

      name = newname.toString();

      ContentCollectionEdit collection = null;
      int tries = 1;
      int olength = name.length();
      for (; tries <= MAX_ATTEMPTS; tries++) {
	  try {
	      collection = ContentHostingService.addCollection(name + "/");  // append / here because we may hack on the name

	      String display = name;
	      int main = name.lastIndexOf("/");
	      if (main >= 0)
		  display = display.substring(main+1);
	      collection.getPropertiesEdit().addProperty(ResourceProperties.PROP_DISPLAY_NAME, display);

	      ContentHostingService.commitCollection(collection);
	      break;   // got it
	  } catch (IdUsedException e) {
	      name = name.substring(0, olength) + "-" + tries;
	  } catch (Exception e) {
	      System.out.println("CC loader: Unable to create resource " + name + " " + e);
	      simplePageBean.setErrKey("simplepage.create.resource.failed" + name + " " +e);
	      return null;
	  }
      }
      if (collection == null) {
	  simplePageBean.setErrKey("simplepage.resource100: " + name);
	  System.out.println("CC loader: failed after 100 attempts to create resource " + name);
	  return null;
      }
      return collection.getId();
  }

  private String getFileName(Element resource) {
      Element file = resource.getChild(FILE, CC_NS);
      if (file != null)
	  return file.getAttributeValue(HREF);
      else
	  return null;
  }

  public void setCCItemXml(Element the_xml, Element resource, AbstractParser parser, CartridgeLoader loader) {
      if (pages.size() == 0)
	  startCCFolder(null);

      System.err.println("\nadd item to page " + pages.get(pages.size()-1).getTitle() +
			 " xml: "+the_xml + 
			 " title " + (the_xml==null?"Question Pool" : the_xml.getChildText(CC_ITEM_TITLE, CC_NS)) +
			 " type " + resource.getAttributeValue(TYPE) +
			 " href " + resource.getAttributeValue(HREF));
      String type = resource.getAttributeValue(TYPE);
      int top = pages.size()-1;
      SimplePage page = pages.get(top);
      Integer seq = sequences.get(top);
      String title = null;
      if (the_xml == null)
	  title = "Question Pool";
      else
	  title = the_xml.getChildText(CC_ITEM_TITLE, CC_NS);

      try {
	  if (type.equals(CC_WEBCONTENT)) {
	      // note: when this code is called the actual sakai resource hasn't been created yet
	      String sakaiId = baseName + resource.getAttributeValue(HREF);
	      String extension = Validator.getFileExtension(sakaiId);
	      String mime = ContentTypeImageService.getContentType(extension);

	      SimplePageItem item = simplePageToolDao.makeItem(page.getPageId(), seq, SimplePageItem.RESOURCE, sakaiId, title);
	      item.setHtml(mime);
	      item.setSameWindow(true);
	      simplePageBean.saveItem(item);
	      sequences.set(top, seq+1);
	  }
	  else if (type.equals(CC_WEBLINK0) || type.equals(CC_WEBLINK1)) {
	      String filename = getFileName(resource);
	      Element linkXml =  parser.getXML(loader, filename);
	      Element urlElement = linkXml.getChild(URL);
	      String url = urlElement.getAttributeValue(HREF);

	      // the name must end in XML, so we can just turn it into URL
	      filename = filename.substring(0, filename.length()-3) + "url";
	      String sakaiId = baseName + filename;

	      if (! filesAdded.contains(filename)) {
		  // we store the URL as a text/url resource
		  ContentResourceEdit edit = ContentHostingService.addResource(sakaiId);
		  edit.setContentType("text/url");
		  edit.setResourceType("org.sakaiproject.content.types.urlResource");
		  edit.setContent(url.getBytes());
		  edit.getPropertiesEdit().addProperty(ResourceProperties.PROP_DISPLAY_NAME, 
						       Validator.escapeResourceName(filename));
		  ContentHostingService.commitResource(edit, NotificationService.NOTI_NONE);
		  filesAdded.add(filename);
	      }

	      // now create the Sakai item
	      SimplePageItem item = simplePageToolDao.makeItem(page.getPageId(), seq, SimplePageItem.RESOURCE, sakaiId, title);
	      item.setHtml(simplePageBean.getTypeOfUrl(url));  // checks the web site to see what it actually is
	      item.setSameWindow(true);
	      simplePageBean.saveItem(item);
	      sequences.set(top, seq+1);
	      
	  } else if (type.equals(CC_TOPIC0) || type.equals(CC_TOPIC1)) {
	      String filename = getFileName(resource);
	      Element topicXml =  parser.getXML(loader, filename);
	      String topicTitle = topicXml.getChildText(TITLE);
	      String text = topicXml.getChildText(TEXT);
	      boolean texthtml = false;
	      if (text != null) {
		  Element textNode = topicXml.getChild(TEXT);
		  String textformat = textNode.getAttributeValue(TEXTTYPE);
		  if (TEXTHTML.equalsIgnoreCase(textformat))
		      texthtml = true;
	      }

	      String base = baseName + filename;
	      int slash = base.lastIndexOf("/");
	      if (slash >= 0)
		  base = base.substring(0, slash+1); // include trailing slash

	      Element attachmentlist = topicXml.getChild(ATTACHMENTS);
	      List<Element>attachments = new ArrayList<Element>();
	      if (attachmentlist != null)
		  attachments = attachmentlist.getChildren();
	      List<String>attachmentHrefs = new ArrayList<String>();
	      for (Element a: attachments) 
		  attachmentHrefs.add(a.getAttributeValue(HREF));

	      System.out.println("topic " + topicTitle + " " + texthtml);
	      System.out.println("text " + text);
	      System.out.println("attachments " + attachmentHrefs);

	      ForumInterface f = (ForumInterface)topictool;

	      // title is for the cartridge. That will be used as the forum
	      f.importObject(title, topicTitle, text, texthtml, base, attachmentHrefs);

	      SimplePageItem item = simplePageToolDao.makeItem(page.getPageId(), seq, SimplePageItem.FORUM, SimplePageItem.DUMMY, title);
	      simplePageBean.saveItem(item);
	      sequences.set(top, seq+1);

	      System.err.println("topic " + getFileName(resource) +
			     " " + parser.getXML(loader, getFileName(resource)));
	  } else if (quiztool != null && (
		     type.equals(CC_ASSESSMENT0) || type.equals(CC_ASSESSMENT1) ||
		     type.equals(CC_QUESTION_BANK0) || type.equals(CC_QUESTION_BANK1))) {

	      boolean isBank = type.equals(CC_QUESTION_BANK0) || type.equals(CC_QUESTION_BANK1);

	      System.out.println("processing test " + isBank);

	      InputStream instream = utils.getFile(getFileName(resource));
	      ByteArrayOutputStream baos = new ByteArrayOutputStream();
	      PrintWriter outwriter = new PrintWriter(baos);
	      
	      QtiImport imp = new QtiImport();
	      try {
		  imp.mainproc(instream, outwriter, isBank);
	      } catch (Exception e) {
		  e.printStackTrace();
		  System.out.println("qti failed" + e);
	      }

	      
	      try {

		  System.out.println("printhand point 1");
		  InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

		  DocumentBuilderFactory builderFactory =
		      DocumentBuilderFactory.newInstance();
		  System.out.println("printhand point 2");
		  builderFactory.setNamespaceAware(true);
		  DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
		  Document document = documentBuilder.parse(inputStream);
		  System.out.println("printhand point 3");

		  QuizEntity q = (QuizEntity)quiztool;
		  System.out.println(quiztool.getToolId());

		  q.importObject(document, isBank, siteId);

		  System.out.println("loaded into quiztool");

	      } catch (Exception e) {
		  System.out.println(e);
		  e.printStackTrace();
		  simplePageBean.setErrKey("simplepage.resource100: " + e);
	      }

	      // question banks don't appear on the page
	      if (!isBank) {
		  SimplePageItem item = simplePageToolDao.makeItem(page.getPageId(), seq, SimplePageItem.ASSESSMENT, SimplePageItem.DUMMY, title);
		  simplePageBean.saveItem(item);
		  sequences.set(top, seq+1);
	      }

	      System.err.println("assessment " + getFileName(resource) + 
				 " " + parser.getXML(loader, getFileName(resource)));
	  } else if (type.equals(CC_QUESTION_BANK0) || type.equals(CC_QUESTION_BANK1))
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
      if (filesAdded.contains(the_file_id))
	  return;

      System.err.println("adding file: "+the_file_id);
      InputStream infile = null;
      try {
	  infile = utils.getFile(the_file_id);
	  System.err.println("Got file " + the_file_id);
	  String name = the_file_id;
	  int slash = the_file_id.lastIndexOf("/");
	  if (slash >=0 )
	      name = name.substring(slash+1);
	  String extension = Validator.getFileExtension(name);
	  String type = ContentTypeImageService.getContentType(extension);

	  ContentResourceEdit edit = ContentHostingService.addResource(baseName + the_file_id);

	  edit.setContentType(type);
	  edit.setContent(infile);
	  edit.getPropertiesEdit().addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

	  ContentHostingService.commitResource(edit, NotificationService.NOTI_NONE);
	  filesAdded.add(the_file_id);

      } catch (Exception e) {
	  simplePageBean.setErrKey("simplepage.create.resource.failed " + e + ": " + the_file_id);
	  System.out.println("CC loader: unable to get file " + the_file_id);
      }
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
      // NOTE: need to handle languages
      Element general = the_md.getChild(GENERAL, MD_NS);
      if (general != null) {
	  System.err.println("got general");
	  Element tnode = general.getChild(TITLE, MD_NS);
	  System.err.println("got title " + tnode);
	  if (tnode != null) {
	      title = tnode.getChildTextTrim(STRING, MD_NS);
	      System.err.println("got title " + title);
	  }
	  Element tdescription=general.getChild(DESCRIPTION, MD_NS);
	  System.err.println("got description " + tdescription);
	  if (tdescription != null) {
	      description = tdescription.getChildTextTrim(STRING, MD_NS);
	      System.err.println("got description " + description);
	  }

      }
      if (title == null || title.equals(""))
	  title = "Cartridge";
      if ("".equals(description))
	  description = null;
      System.err.println("cartridge metadata title:" + title + " description: " + description);
      baseName = makeBaseFolder(title);
      System.err.println("basename " + baseName);

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

