package org.sakaiproject.lessonbuildertool.ccexport;;

/***********
 *
 * Copyright (c) 2013 Rutgers, the State University of New Jersey
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");                                                                
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.component.cover.ServerConfigurationService;

import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.ccexport.SamigoExport;
import org.sakaiproject.lessonbuildertool.ccexport.AssignmentExport;
import org.sakaiproject.lessonbuildertool.ccexport.ForumsExport;
import org.sakaiproject.lessonbuildertool.ccexport.ZipPrintStream;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.view.ExportCCViewParameters;

import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.viewstate.ViewParameters;

public class CCExport {

    private static Log log = LogFactory.getLog(CCExport.class);

    private File root;
    private String rootPath;
    long nextid = 1;
  
    static ContentHostingService contentHostingService;
    public void setContentHostingService(ContentHostingService chs) {
	contentHostingService = chs;
    }
    static SamigoExport samigoExport;
    public void setSamigoExport(SamigoExport se) {
	samigoExport = se;
    }
    static AssignmentExport assignmentExport;
    public void setAssignmentExport(AssignmentExport se) {
	assignmentExport = se;
    }
    static ForumsExport forumsExport;
    public void setForumsExport(ForumsExport se) {
	forumsExport = se;
    }
    static BltiExport bltiExport;
    public void setBltiExport(BltiExport se) {
      bltiExport = se;
    }

    static MessageLocator messageLocator;
    public void setMessageLocator(MessageLocator x) {
	messageLocator = x;
    }
    static SimplePageToolDao simplePageToolDao;
    public void setSimplePageToolDao(Object dao) {
	simplePageToolDao = (SimplePageToolDao) dao;
    }

    HttpServletResponse response;
    File errFile = null;
    PrintStream errStream = null;
    String siteId = null;
    static String server = ServerConfigurationService.getServerName();
    int version = V12;
    boolean doBank = false;

    class Resource {
	String sakaiId;
	String resourceId;
	String location;
	String use;
	List<String> dependencies;
    }

    // map of all file resource to be included in cartridge
    Map<String, Resource> fileMap = new HashMap<String, Resource>();
    // map of all Samigo tests
    Map<String, Resource> samigoMap = new HashMap<String, Resource>();
    Resource samigoBank = null;
    // map of all Assignments
    Map<String, Resource> assignmentMap = new HashMap<String, Resource>();
    // map of all Forums
    Map<String, Resource> forumsMap = new HashMap<String, Resource>();
    // map of all Blti instances
    Map<String, Resource> bltiMap = new HashMap();
 
    // to prevent pages from being output more than once
    Set<Long> pagesDone = new HashSet();


    // the error messages are a problem. They won't show until the next page display
    // however errrors at this level are unusual, and we interrupt the download, so the
    // user should never see an incomplete one. Most common errors have to do with
    // problems converting for CC format. Those go into a log file that's included in
    // the ZIP, so the user will see those errors (if he knows the look)

    public static void setErrMessage(String s) {
	ToolSession toolSession = SessionManager.getCurrentToolSession();
	if (toolSession == null) {
	    log.error("Lesson Builder error not in tool: " + s);
	    return;
	}
	List<String> errors = (List<String>)toolSession.getAttribute("lessonbuilder.errors");
	if (errors == null)
	    errors = new ArrayList<String>();
	errors.add(s);
	toolSession.setAttribute("lessonbuilder.errors", errors);
    }

    public static void setErrKey(String key, String text ) {
	if (text == null)
	    text = "";
	setErrMessage(messageLocator.getMessage(key).replace("{}", text));
    }

    // current we don't support 1.0
    public static final int V11 = 1;
    public static final int V12 = 2;

    /*
     * maintain global lists of resources, adding as they are referenced on a page or
     * adding all resources of a kind, depending. Each type of resource has a map
     * indexed by sakai ID, with a generated ID for the cartridge and the name of the
     * file or XML file.
     *
     * the overall flow will be to load all resources and tests into the temp directory
     * and the maps, the walk the lesson hierarchy building imsmanifest.xml. Any resources
     * not used will get some kind of dummy entries in imsmanifest.xml, so that the whole
     * contents of the site is brought over.
     */

    public void doExport(String sid, HttpServletResponse httpServletResponse, ExportCCViewParameters params) {
	response = httpServletResponse;
	siteId = sid;
	if ("1.1".equals(params.getVersion()))
	    version = V11;
	if ("1".equals(params.getBank()))
	    doBank = true;

	if (! startExport())
	    return;
	if (! addAllFiles(siteId))
	    return;
	if (! addAllSamigo(siteId))
	    return;
	if (! addAllAssignments(siteId))
	    return;
	if (! addAllForums(siteId))
	    return;
	if (!addAllBlti(siteId))
	    return;
	download();

    }

    /*
     * create temp dir and start writing 
     */
    public boolean startExport() {
	try {
	    root = File.createTempFile("ccexport", "root");
	    if (root.exists())
		root.delete();
	    root.mkdir();
	    errFile = new File(root, "export-errors");
	    errStream = new PrintStream(errFile);
	    
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, startExport " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}
	return true;
    }

    String getResourceId () {
       	return "res" + (nextid++);
    }

    public void setIntendeduse (String sakaiId, String intendeduse) {
	Resource ref = fileMap.get(sakaiId);
	if (ref == null)
	    return;
	ref.use = intendeduse;
    }

    String getLocation(String sakaiId) {
	Resource ref = fileMap.get(sakaiId);
	if (ref == null)
	    return null;
	return ref.location;
    }

    public void addFile(String sakaiId, String location) {
	addFile(sakaiId, location, null);
    }

    public void addFile(String sakaiId, String location, String use) {
	Resource res = new Resource();
	res.sakaiId = sakaiId;
	res.resourceId = getResourceId();
	res.location = location;
	res.dependencies = new ArrayList<String>();
	res.use = use;

	fileMap.put(sakaiId, res);

    }

    public boolean addAllFiles(String siteId) {
	try {
	    String base = contentHostingService.getSiteCollection(siteId);
	    ContentCollection baseCol = contentHostingService.getCollection(base);
	    return addAllFiles(baseCol, base.length());
	} catch (org.sakaiproject.exception.IdUnusedException e) {
	    setErrKey("simplepage.exportcc-noresource", e.getMessage());
	    return false;
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, addAllFiles " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}

    }

    public boolean addAllFiles(ContentCollection baseCol, int baselen) {
	try {

	    List<ContentEntity> members = baseCol.getMemberResources();
	    for (ContentEntity e: members) {

		// don't export things we generate. Can lead to collisions
		String filename = e.getId().substring(baselen);
		if (filename.equals("cc-objects/export-errors") || filename.equals("cc-objects"))
		    continue;

		if (e instanceof ContentResource) {
		    addFile(e.getId(), e.getId().substring(baselen));
		} else 
		    addAllFiles((ContentCollection)e, baselen);
	    }
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, addAllFiles 2 " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}
	return true;
    }

    public boolean outputAllFiles (ZipPrintStream out) {
	try {
	    for (Map.Entry<String, Resource> entry: fileMap.entrySet()) {

		// normally this is a file ID for contenthosting.
		// But jforum gives us an actual filesystem filename. We stick /// on
		// the front to make that clear. inSakai is contenthosting.
		boolean inSakai = !entry.getKey().startsWith("///"); 

		ZipEntry zipEntry = new ZipEntry(entry.getValue().location);

		// for contenthosting
		ContentResource resource = null;
		// for raw file
		File infile = null;
		InputStream instream = null;
		if (inSakai) {
		    resource = contentHostingService.getResource(entry.getKey());
		    zipEntry.setSize(resource.getContentLength());
		} else {
		    infile = new File(entry.getKey().substring(3));
		    instream = new FileInputStream(infile);
		}

		out.putNextEntry(zipEntry);

		InputStream contentStream = null;
					    
		// see if this is HTML. If so, we need to scan it.
		String filename = entry.getKey();
		int lastdot = filename.lastIndexOf(".");
		int lastslash = filename.lastIndexOf("/");
		String extension = "";
		if (lastdot >= 0 && lastdot > lastslash)
		    extension = filename.substring(lastdot+1);
		String mimeType = null;
		if (inSakai)
		    mimeType = resource.getContentType();
		boolean isHtml = false;
		if (mimeType != null && (mimeType.startsWith("http") || mimeType.equals("")))
		    mimeType = null;
		if (mimeType != null && (mimeType.equals("text/html") || mimeType.equals("application/xhtml+xml"))
				|| mimeType == null && (extension.equals("html") || extension.equals("htm"))) {
		    isHtml = true;
		}

		try {
		    if (isHtml) {
			// treat html separately. Need to convert urls to relative
			String content = null;
			if (inSakai)
			    content = new String(resource.getContent());
			else {
			    byte[] b = new byte[(int) infile.length()];  
			    instream.read(b);  			    
			    content = new String(b);
			}
			content = relFixup(content, entry.getValue());
			out.print(content);
		    } else {
			if (inSakai)
			    contentStream = resource.streamContent();
			else 
			    contentStream = instream;
			IOUtils.copy(contentStream, out);
		    }

		} catch (Exception e) {
		    log.error("Lessons export error outputting file " + e);
		} finally {
		    if (contentStream != null) {
			contentStream.close();
		    }
		}
	    }
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, outputAllFiles " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}

	return true;

    }

    public boolean addAllSamigo(String siteId) {
	List<String> tests = samigoExport.getEntitiesInSite(siteId);
	if (tests == null)
	    return true;
	// These are going to be loaded into the final file system. I considered
	// putting them in a separate directory to avoid conflicting with real files.
	// However this would force all URLs to be written with ../ at the start,
	// which is probably more dangerous, as it depends upon loaders making the
	// same interpretation of a somewhat ambiguous specification.
	for (String sakaiId: tests) {
	    Resource res = new Resource();
	    res.resourceId = getResourceId();
	    res.location = "cc-objects/" + res.resourceId + ".xml";
	    res.sakaiId = sakaiId;
	    res.dependencies = new ArrayList<String>();
	    res.use = null;
	    samigoMap.put(res.sakaiId, res);
	}
	if (doBank && samigoExport.havePoolItems()) {
	    Resource res = new Resource();
	    res.resourceId = getResourceId();
	    res.location = "cc-objects/" + res.resourceId + ".xml";
	    res.sakaiId = null;
	    res.dependencies = new ArrayList<String>();
	    res.use = null;
	    samigoBank = res;
	}
	return true;
    }

    public boolean outputAllSamigo(ZipPrintStream out) {
	try {
	    for (Map.Entry<String, Resource> entry: samigoMap.entrySet()) {

		ZipEntry zipEntry = new ZipEntry(entry.getValue().location);
		out.putNextEntry(zipEntry);
		boolean ok = samigoExport.outputEntity(entry.getValue().sakaiId, out, errStream, this, entry.getValue(), version);
		if (!ok)
		    return false;

	    }
	    if (samigoBank != null) {
		ZipEntry zipEntry = new ZipEntry(samigoBank.location);
		out.putNextEntry(zipEntry);
		boolean ok = samigoExport.outputBank(out, errStream, this, samigoBank, version);
		if (!ok)
		    return false;
	    }
	} catch (Exception e) {
	    log.error("output sam " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    e.printStackTrace();
	    return false;
	}

	return true;

    }

    public boolean addAllAssignments(String siteId) {
	List<String> assignments = assignmentExport.getEntitiesInSite(siteId, this);
	if (assignments == null)
	    return true;
	for (String sakaiId: assignments) {
	    Resource res = new Resource();
	    res.resourceId = getResourceId();
	    int slash = sakaiId.indexOf("/");
	    res.location = "attachments/" + sakaiId.substring(slash+1) + "/assignmentpage.html";
	    res.sakaiId = sakaiId;
	    res.dependencies = new ArrayList<String>();
	    res.use = null;
	    assignmentMap.put(res.sakaiId, res);
	}

	return true;
    }

    public boolean outputAllAssignments(ZipPrintStream out) {
	try {
	    for (Map.Entry<String, Resource> entry: assignmentMap.entrySet()) {

		ZipEntry zipEntry = new ZipEntry(entry.getValue().location);

		out.putNextEntry(zipEntry);
		boolean ok = assignmentExport.outputEntity(entry.getValue().sakaiId, out, errStream, this, entry.getValue());
		if (!ok)
		    return false;

	    }
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, outputAllAssignments " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}

	return true;
    }

    public boolean addAllForums(String siteId) {
	List<String> forums = forumsExport.getEntitiesInSite(siteId, this);
	if (forums == null)
	    return true;
	for (String sakaiId: forums) {
	    Resource res = new Resource();
	    res.resourceId = getResourceId();
	    res.location = "cc-objects/" + res.resourceId + ".xml";
	    res.sakaiId = sakaiId;
	    res.dependencies = new ArrayList<String>();
	    res.use = null;
	    forumsMap.put(res.sakaiId, res);
	}
	return true;
    }

    public boolean outputAllForums(ZipPrintStream out) {
	try {
	    for (Map.Entry<String, Resource> entry: forumsMap.entrySet()) {
		ZipEntry zipEntry = new ZipEntry(entry.getValue().location);

		out.putNextEntry(zipEntry);
		boolean ok = forumsExport.outputEntity(entry.getValue().sakaiId, out, errStream, this, entry.getValue(), version);
		if (!ok)
		    return false;

	    }
	} catch (Exception e) {
	    log.error("problem in outputallforums, outputAllForums " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}

	return true;

    }

    public boolean addAllBlti(String siteId) {
	List<String> bltis = bltiExport.getEntitiesInSite(siteId, this);
	if (bltis == null)
	    return true;
	for (String sakaiId : bltis) {
	    Resource res = new Resource();
	    res.resourceId = getResourceId();
	    res.location = ("cc-objects/" + res.resourceId + ".xml");
	    res.sakaiId = sakaiId;
	    res.dependencies = new ArrayList();
	    res.use = null;
	    bltiMap.put(res.sakaiId, res);
	}
	return true;
    }

    public boolean outputAllBlti(ZipPrintStream out) {
	try {
	    for (Map.Entry entry : bltiMap.entrySet()) {
		ZipEntry zipEntry = new ZipEntry(((Resource)entry.getValue()).location);
		
		out.putNextEntry(zipEntry);
		boolean ok = bltiExport.outputEntity(((Resource)entry.getValue()).sakaiId, out, this.errStream, this, (Resource)entry.getValue(), version);
		if (!ok)
		    return false;
	    }
	} catch (Exception e) {
	    log.error("problem in outputallforums, outputAllBlti " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}
	return true;
    }

    public boolean outputAllTexts(ZipPrintStream out) {
	try {
	    List<SimplePageItem> items = simplePageToolDao.findTextItemsInSite(this.siteId);
	    
	    for (SimplePageItem item : items) {
		String location = "attachments/item-" + item.getId() + ".html";
		
		ZipEntry zipEntry = new ZipEntry(location);
		out.putNextEntry(zipEntry);

		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">");
		out.println("<body>");
		out.print(item.getHtml());
		out.println("</body>");
		out.println("</html>");

		Resource res = new Resource();
		res.sakaiId = ("/text/" + item.getId());
		res.resourceId = getResourceId();
		res.location = location;
		res.dependencies = new ArrayList();
		res.use = null;
		
		fileMap.put(res.sakaiId, res);
	    }
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, outputAllTexts " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}
	return true;
    }

    public boolean outputLessons(ZipPrintStream out)  {
	out.println("  <organization identifier=\"page\" structure=\"rooted-hierarchy\">");
	out.println("    <item identifier=\"I_1\">");
	List<SimplePageItem> sitePages = simplePageToolDao.findItemsInSite(ToolManager.getCurrentPlacement().getContext());

	for (SimplePageItem i : sitePages)
	    pagesDone.add(Long.valueOf(i.getSakaiId()));
	for (SimplePageItem i : sitePages) {
	    outputLessonPage(out, Long.valueOf(i.getSakaiId()), i.getName(), 6, true);
	}
	out.println("    </item>");
	out.println("  </organization>");
	return true;
    }
    
    public void outputIndent(ZipPrintStream out, int indent) {
	for (int i = 0; i < indent; i++)
	    out.print(" ");
    }

    public SimplePageItem outputLessonPage(ZipPrintStream out, Long pageId, String title, int indent, boolean shownext) {
	SimplePageItem next = null;
	boolean multiplenext = false;
	
	pagesDone.add(pageId);

	outputIndent(out, indent); out.println("<item identifier=\"page_" + pageId + "\">");
	outputIndent(out, indent + 2); out.println("<title>" + StringEscapeUtils.escapeHtml(title) + "</title>");

	List<SimplePageItem> items = simplePageToolDao.findItemsOnPage(pageId.longValue());
	for (SimplePageItem item : items) {
	    if (item.getNextPage()) {
		if (next == null) {
		    next = item;
		}
		else if (!multiplenext) {
		    next = null;
		    multiplenext = true;
		}
	    }
	}

	for (SimplePageItem item : items) {
	    Resource res = null;
	    String sakaiId = null;
	    String itemString = null;

	    switch (item.getType()) {
	    case SimplePageItem.PAGE:
		Long pId = Long.valueOf(item.getSakaiId());
		if (this.pagesDone.contains(pId)) {
		    this.errStream.println(messageLocator.getMessage("simplepage.exportcc-pagealreadydone").replace("{1}", title).replace("{2}", item.getName()));
		} else if ((next != null) && (item.getId() == next.getId())) {
		    if (shownext) {
			SimplePageItem n = outputLessonPage(out, pId, item.getName(), indent + 2, false);
			while ((n != null) && (!this.pagesDone.contains(pId = Long.valueOf(n.getSakaiId())))) {
			    n = outputLessonPage(out, pId, n.getName(), indent + 2, false);
			}
			if ((n != null) && (this.pagesDone.contains(pId))) {
			    errStream.println(messageLocator.getMessage("simplepage.exportcc-pagealreadydone").replace("{1}", title).replace("{2}", item.getName()));
			}
		    }
		} else {
		    outputLessonPage(out, pId, item.getName(), indent + 2, true);
		}
		break;
	    case SimplePageItem.RESOURCE:
	    case SimplePageItem.MULTIMEDIA:
		res = (Resource)this.fileMap.get(item.getSakaiId());
		break;
	    case SimplePageItem.ASSIGNMENT:
		sakaiId = item.getSakaiId();
		if (sakaiId.indexOf("/", 1) < 0)
		    sakaiId = "assignment/" + sakaiId;
		else
		    sakaiId = sakaiId.substring(1);
		res = (Resource)this.assignmentMap.get(sakaiId);
		break;
	    case SimplePageItem.ASSESSMENT:
		sakaiId = item.getSakaiId();
		if (sakaiId.indexOf("/", 1) < 0)
		    sakaiId = "sam_pub/" + sakaiId;
		else
		    sakaiId = sakaiId.substring(1);
		res = (Resource)samigoMap.get(sakaiId);
		break;
	    case SimplePageItem.TEXT:
		res = (Resource)fileMap.get("/text/" + item.getId());
		break;
	    case SimplePageItem.FORUM:
		res = (Resource)forumsMap.get(item.getSakaiId().substring(1));
		break;
	    case SimplePageItem.BLTI:
		res = (Resource)bltiMap.get(item.getSakaiId().substring(1));
		break;
	    case SimplePageItem.COMMENTS:
	    case SimplePageItem.STUDENT_CONTENT:
		switch (item.getType()) {
		case SimplePageItem.COMMENTS:
		    itemString = messageLocator.getMessage("simplepage.comments-section");
		    break;
		case SimplePageItem.STUDENT_CONTENT:
		    itemString = messageLocator.getMessage("simplepage.student-content");
		    break;
		}
		errStream.println(messageLocator.getMessage("simplepage.exportcc-bad-type").replace("{1}", title).replace("{2}", item.getName()).replace("{3}", itemString));
		break;
	    }
	    if (res != null) {
		outputIndent(out, indent + 2); out.println("<item identifier=\"item_" + item.getId() + "\" identifierref=\"" + res.resourceId + "\">");
		String ititle = item.getName();
		
		if ((ititle == null) || (ititle.equals("")))
		    ititle = messageLocator.getMessage("simplepage.importcc-texttitle");
		outputIndent(out, indent + 4); out.println("<title>" + StringEscapeUtils.escapeHtml(ititle) + "</title>");
		outputIndent(out, indent + 2); out.println("</item>"); 
	    }
	}
	outputIndent(out, indent); out.println("</item>");
	if (shownext) {
	    return null;
	}
	return next;
    }

    public boolean outputManifest(ZipPrintStream out) {
	try {
	    ZipEntry zipEntry = new ZipEntry("imsmanifest.xml");
	    out.putNextEntry(zipEntry);
	    switch (version) {
	    case V11:
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<manifest identifier=\"cctd0001\"");
		out.println("  xmlns=\"http://www.imsglobal.org/xsd/imsccv1p1/imscp_v1p1\"");
		out.println("  xmlns:lom=\"http://ltsc.ieee.org/xsd/imsccv1p1/LOM/resource\"");
		out.println("  xmlns:lomimscc=\"http://ltsc.ieee.org/xsd/imsccv1p1/LOM/manifest\"");
		out.println("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		out.println("  xsi:schemaLocation=\"");
		out.println("  http://ltsc.ieee.org/xsd/imsccv1p1/LOM/resource http://www.imsglobal.org/profile/cc/ccv1p1/LOM/ccv1p1_lomresource_v1p0.xsd ");
		out.println("  http://www.imsglobal.org/xsd/imsccv1p1/imscp_v1p1 http://www.imsglobal.org/profile/cc/ccv1p1/ccv1p1_imscp_v1p2_v1p0.xsd ");
		out.println("  http://ltsc.ieee.org/xsd/imsccv1p1/LOM/manifest http://www.imsglobal.org/profile/cc/ccv1p1/LOM/ccv1p1_lommanifest_v1p0.xsd\">");
		out.println("  <metadata>");
		out.println("    <schema>IMS Common Cartridge</schema>");
		out.println("    <schemaversion>1.1.0</schemaversion>");
		out.println("    <lomimscc:lom>");
		out.println("      <lomimscc:general>");
		out.println("        <lomimscc:title>");
		out.println("          <lomimscc:string language=\"en-US\">Sakai Export</lomimscc:string>");
		out.println("        </lomimscc:title>");
		out.println("        <lomimscc:description>");
		out.println("          <lomimscc:string language=\"en-US\">Sakai Export, including only files from site</lomimscc:string>");
		out.println("        </lomimscc:description>");
		out.println("        <lomimscc:keyword>");
		out.println("          <lomimscc:string language=\"en-US\">Export</lomimscc:string>");
		out.println("        </lomimscc:keyword>");
		out.println("      </lomimscc:general>");
		out.println("    </lomimscc:lom>");
		out.println("  </metadata>");
	    break;

	    default:
	    out.print(
		      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<manifest identifier=\"sakai1\"\n  xmlns=\"http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1\"\nxmlns:lom=\"http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource\"\nxmlns:lomimscc=\"http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\nxsi:schemaLocation=\"                                                                                                                        \n  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lomresource_v1p0.xsd                  \n  http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1 http://www.imsglobal.org/profile/cc/ccv1p2/ccv1p2_imscp_v1p2_v1p0.xsd                     \n  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lommanifest_v1p0.xsd\">\n  <metadata>\n    <schema>IMS Common Cartridge</schema>\n    <schemaversion>1.2.0</schemaversion>\n    <lomimscc:lom>\n      <lomimscc:general>\n	<lomimscc:title>\n	  <lomimscc:string language=\"en-US\">Sakai Export</lomimscc:string>\n	</lomimscc:title>\n	<lomimscc:description>\n	  <lomimscc:string language=\"en-US\">Sakai Export, including only files from site</lomimscc:string>\n	</lomimscc:description>\n	<lomimscc:keyword>\n	  <lomimscc:string language=\"en-US\">Export</lomimscc:string>\n	</lomimscc:keyword>\n      </lomimscc:general>\n    </lomimscc:lom>\n  </metadata>\n ");
	    }

	    out.println("  <organizations>");
	    outputLessons(out);
	    out.println("  </organizations>");

	    String qtiid = null;
	    String bankid = null;
	    String topicid = null;
	    String usestr = "";
	    switch (version) {
	    case V11:
		qtiid = "imsqti_xmlv1p2/imscc_xmlv1p1/assessment";
		bankid = "imsqti_xmlv1p2/imscc_xmlv1p1/question-bank";
		topicid = "imsdt_xmlv1p1";
		usestr = "";
		break;
	    default:
		qtiid = "imsqti_xmlv1p2/imscc_xmlv1p2/assessment";
		bankid = "imsqti_xmlv1p2/imscc_xmlv1p2/question-bank";
		topicid = "imsdt_xmlv1p2";
		usestr = " intendeduse=\"assignment\"";
	    }

	    out.println("  <resources>");
	    for (Map.Entry<String, Resource> entry: fileMap.entrySet()) {
		String use = "";
		if (version >= V12) {
		    if (entry.getValue().use != null)
			use = " intendeduse=\"" + entry.getValue().use + "\"";
		}
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\" identifier=\"" + entry.getValue().resourceId + "\" type=\"webcontent\"" + use + ">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\"/>");
		out.println("    </resource>");
	    }

	    for (Map.Entry<String, Resource> entry: samigoMap.entrySet()) {
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\" identifier=\"" + entry.getValue().resourceId + "\" type=\"" + qtiid + "\">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\"/>");
		for (String d: entry.getValue().dependencies)
		    out.println("      <dependency identifierref=\"" + d + "\"/>");
		out.println("    </resource>");
	    }

	    // question bank
	    if (samigoBank != null) {
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(samigoBank.location) + "\" identifier=\"" + samigoBank.resourceId + "\" type=\"" + bankid + "\">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(samigoBank.location) + "\"/>");
		for (String d: samigoBank.dependencies)
		    out.println("      <dependency identifierref=\"" + d + "\"/>");
		out.println("    </resource>");
	    }
	    for (Map.Entry<String, Resource> entry: assignmentMap.entrySet()) {
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\" identifier=\"" + entry.getValue().resourceId + "\" type=\"webcontent\"" + usestr + ">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\"/>");
		for (String d: entry.getValue().dependencies)
		    out.println("      <dependency identifierref=\"" + d + "\"/>");
		out.println("    </resource>");
	    }

	    for (Map.Entry<String, Resource> entry: forumsMap.entrySet()) {
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\" identifier=\"" + entry.getValue().resourceId + "\" type=\"" + topicid + "\">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(entry.getValue().location) + "\"/>");
		for (String d: entry.getValue().dependencies)
		    out.println("      <dependency identifierref=\"" + d + "\"/>");
		out.println("    </resource>");
	    }

	    for (Map.Entry entry : this.bltiMap.entrySet()) {
		out.println("    <resource href=\"" + StringEscapeUtils.escapeXml(((Resource)entry.getValue()).location) + "\" identifier=\"" + ((Resource)entry.getValue()).resourceId + "\" type=\"imsbasiclti_xmlv1p0\">");
		out.println("      <file href=\"" + StringEscapeUtils.escapeXml(((Resource)entry.getValue()).location) + "\"/>");
		for (String d : ((Resource)entry.getValue()).dependencies)
		    out.println("      <dependency identifierref=\"" + d + "\"/>");
		out.println("    </resource>");
	    }

	    // add error log at the very end
	    String errId = getResourceId();

	    out.println(("    <resource href=\"cc-objects/export-errors\" identifier=\"" + errId + 
			   "\" type=\"webcontent\">\n      <file href=\"cc-objects/export-errors\"/>\n    </resource>"));
	    
	    out.println("  </resources>\n</manifest>");

	    errStream.close();
	    zipEntry = new ZipEntry("cc-objects/export-errors");
	    out.putNextEntry(zipEntry);
	    InputStream contentStream = null;
	    try {
		contentStream = new FileInputStream(errFile);
		IOUtils.copy(contentStream, out);
	    } finally {
		if (contentStream != null) {
		    contentStream.close();
		}
	    }
	} catch (Exception e) {
	    log.error("Lessons export error outputting file, outputManifest " + e);
	    setErrKey("simplepage.exportcc-fileerr", e.getMessage());
	    return false;
	}

	return true;


    }

    public boolean download() {

        OutputStream htmlOut = null;
	ZipPrintStream out = null;
        try {
	    htmlOut = response.getOutputStream();
	    out = new ZipPrintStream(htmlOut);

	    response.setHeader("Content-disposition", "inline; filename=sakai-export.imscc");
	    response.setContentType("application/zip");
	    
	    outputAllFiles (out);
	    outputAllSamigo (out);
	    outputAllAssignments (out);
	    outputAllForums (out);
	    outputAllBlti(out);
	    outputAllTexts(out);
	    outputManifest (out);
	    
	    if (out != null)
		out.close();

        } catch (Exception ioe) {
	    if (out != null) {
		try {
		    out.close();
		} catch (Exception ignore) {
		}
	    }
	    log.error("Lessons export error outputting file, download " + ioe);
	    setErrKey("simplepage.exportcc-fileerr", ioe.getMessage());
	    return false;
	}

	return true;

    }

    public void addDependency(Resource resource, String sakaiId) {
	Resource ref = fileMap.get(sakaiId);
	if (ref != null)
	    resource.dependencies.add(ref.resourceId);
    }

    public String fixup (String s, Resource resource) {
	// http://lessonbuilder.sakaiproject.org/53605/
	StringBuilder ret = new StringBuilder();
	String sakaiIdBase = "/group/" + siteId;
	Pattern target = Pattern.compile("/access/content/group/" + siteId + "|http://lessonbuilder.sakaiproject.org/", Pattern. CASE_INSENSITIVE);
	Matcher matcher = target.matcher(s);
	// technically / isn't allowed in an unquoted attribute, but sometimes people
	// use sloppy HTML	
	Pattern wordend = Pattern.compile("[^-a-zA-Z0-9._:/]");
	int index = 0;
	while (true) {
	    if (!matcher.find()) {
		ret.append(s.substring(index));
		break;
	    }		
	    String sakaiId = null;
	    int start = matcher.start();
	    if (s.regionMatches(false, start, "/access", 0, 7)) { // matched /access/content...
		int sakaistart = start + "/access/content".length(); //start of sakaiid, can't find end until we figure out quoting
		int last = start + "/access/content/group/".length() + siteId.length();
		if (s.regionMatches(true, (start - server.length()), server, 0, server.length())) {    // servername before it
		    start -= server.length();
		    if (s.regionMatches(true, start - 7, "http://", 0, 7)) {   // http:// or https:// before that
			start -= 7;
		    } else if (s.regionMatches(true, start - 8, "https://", 0, 8)) {
			start -= 8;
		    }
		}
		// need to find sakaiend. To do that we need to find the close quote
		int sakaiend = 0;
		char quote = s.charAt(start-1);
		if (quote == '\'' || quote == '"')  // quoted, this is easy
		    sakaiend = s.indexOf(quote, sakaistart);
		else { // not quoted. find first char not legal in unquoted attribute
		    Matcher wordendMatch = wordend.matcher(s);
		    if (wordendMatch.find(sakaistart)) {
			sakaiend = wordendMatch.start();
		    }
		    else
			sakaiend = s.length();
		}
		sakaiId = s.substring(sakaistart, sakaiend);
		ret.append(s.substring(index, start));
		ret.append("$IMS-CC-FILEBASE$..");
		index = last;  // start here next time
	    } else { // matched http://lessonbuilder.sakaiproject.org/
		int last = matcher.end(); // should be start of an integer
		int endnum = s.length();  // end of the integer
		for (int i = last; i < s.length(); i++) {
		    if ("0123456789".indexOf(s.charAt(i)) < 0) {
			endnum = i;
			break;
		    }
		}
		String numString = s.substring(last, endnum);
		if (numString.length() >= 1) {
		    Long itemId = new Long(numString);
		    SimplePageItem item = simplePageToolDao.findItem(itemId);
		    sakaiId = item.getSakaiId();
		    int itemType = item.getType();
		    if ((itemType == SimplePageItem.RESOURCE || itemType == SimplePageItem.MULTIMEDIA) && 
			sakaiId.startsWith(sakaiIdBase)) {
			ret.append(s.substring(index, start));
			ret.append("$IMS-CC-FILEBASE$.." + sakaiId.substring(sakaiIdBase.length()));
			index = endnum;
		    }
		}
	    }
	    if (sakaiId != null) {
		Resource r = fileMap.get(sakaiId);
		if (r != null) {
		    resource.dependencies.add(r.resourceId);
		}
	    }
	}
	return StringEscapeUtils.escapeXml(ret.toString());
    }		

    // turns the links into relative links
    public String relFixup (String s, Resource resource) {
	// http://lessonbuilder.sakaiproject.org/53605/
	StringBuilder ret = new StringBuilder();
	String sakaiIdBase = "/group/" + siteId;
	Pattern target = Pattern.compile("/access/content/group/" + siteId + "|http://lessonbuilder.sakaiproject.org/", Pattern. CASE_INSENSITIVE);
	Matcher matcher = target.matcher(s);
	// technically / isn't allowed in an unquoted attribute, but sometimes people
	// use sloppy HTML	
	Pattern wordend = Pattern.compile("[^-a-zA-Z0-9._:/]");
	int index = 0;
	while (true) {
	    if (!matcher.find()) {
		ret.append(s.substring(index));
		break;
	    }		
	    String sakaiId = null;
	    int start = matcher.start();
	    if (s.regionMatches(false, start, "/access", 0, 7)) { // matched /access/content...
		int sakaistart = start + "/access/content".length(); //start of sakaiid, can't find end until we figure out quoting
		int last = start + "/access/content/group/".length() + siteId.length();
		if (s.regionMatches(true, (start - server.length()), server, 0, server.length())) {    // servername before it
		    start -= server.length();
		    if (s.regionMatches(true, start - 7, "http://", 0, 7)) {   // http:// or https:// before that
			start -= 7;
		    } else if (s.regionMatches(true, start - 8, "https://", 0, 8)) {
			start -= 8;
		    }
		}
		// need to find sakaiend. To do that we need to find the close quote
		int sakaiend = 0;
		char quote = s.charAt(start-1);
		if (quote == '\'' || quote == '"')  // quoted, this is easy
		    sakaiend = s.indexOf(quote, sakaistart);
		else { // not quoted. find first char not legal in unquoted attribute
		    Matcher wordendMatch = wordend.matcher(s);
		    if (wordendMatch.find(sakaistart)) {
			sakaiend = wordendMatch.start();
		    }
		    else
			sakaiend = s.length();
		}
		last = sakaiend;
		sakaiId = s.substring(sakaistart, sakaiend);
		ret.append(s.substring(index, start));
		// do the mapping. resource.location is a relative URL of the page we're looking at
		// sakaiid is the URL of the object, starting /group/
		String base = getParent(resource.location);
		String thisref = sakaiId.substring(sakaiIdBase.length()+1);
		String relative = relativize(thisref, base);
		ret.append(relative.toString());
		index = last;  // start here next time
	    } else { // matched http://lessonbuilder.sakaiproject.org/
		int last = matcher.end(); // should be start of an integer
		int endnum = s.length();  // end of the integer
		for (int i = last; i < s.length(); i++) {
		    if ("0123456789".indexOf(s.charAt(i)) < 0) {
			endnum = i;
			break;
		    }
		}
		String numString = s.substring(last, endnum);
		if (numString.length() >= 1) {
		    Long itemId = new Long(numString);
		    SimplePageItem item = simplePageToolDao.findItem(itemId);
		    sakaiId = item.getSakaiId();
		    int itemType = item.getType();
		    if ((itemType == SimplePageItem.RESOURCE || itemType == SimplePageItem.MULTIMEDIA) && 
			sakaiId.startsWith(sakaiIdBase)) {
			ret.append(s.substring(index, start));
			String base = getParent(resource.location);
			String thisref = sakaiId.substring(sakaiIdBase.length()+1);
			String relative = relativize(thisref, base);
			ret.append(relative);
			index = endnum;
		    }
		}
	    }
	    if (sakaiId != null) {
		Resource r = fileMap.get(sakaiId);
		if (r != null) {
		    resource.dependencies.add(r.resourceId);
		}
	    }
	}
	return ret.toString();
    }		

    // return base directory of file, including trailing /
    // "" if it is in home directory
    public String getParent(String s) {
	int i = s.lastIndexOf("/");
	if (i < 0)
	    return "";
	else
	    return s.substring(0, i+1);
    }

    // return relative path to target from base
    // base is assumed to be "" or ends in /
    public String relativize(String target, String base) {
	if (base.equals(""))
	    return target;
	if (target.startsWith(base))
	    return target.substring(base.length());
	else {
	    // get parent directory of base directory.
	    // base directory ends in /
	    int i = base.lastIndexOf("/", base.length()-2);
	    if (i < 0)
		base = "";
	    else
		base = base.substring(0, i+1); // include /
	    return "../" + relativize(target, base);
	}
    }

}
