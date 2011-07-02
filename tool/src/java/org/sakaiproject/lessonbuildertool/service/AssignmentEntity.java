/**********************************************************************************
 * $URL: $
 * $Id: $
 ***********************************************************************************
 *
 * Author: Charles Hedrick, hedrick@rutgers.edu
 *
 * Copyright (c) 2010 Rutgers, the State University of New Jersey
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
 *
 **********************************************************************************/

package org.sakaiproject.lessonbuildertool.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.lessonbuildertool.service.LessonSubmission;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.UrlItem;

import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentEdit;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.cover.AssignmentService;

import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;

import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.MemoryService;

import uk.org.ponder.messageutil.MessageLocator;

/**
 * Interface to Assignment
 *
 * @author Charles Hedrick <hedrick@rutgers.edu>
 * 
 */

// NOTE: almost no other class should import this. We want to be able
// to support both forums and jforum. So typically there will be a 
// forumEntity, but it's injected, and it can be either forum and jforum.
// Hence it has to be declared LessonEntity. That leads to a lot of
// declarations like LessonEntity forumEntity.  In this case forumEntity
// means either a ForumEntity or a JForumEntity. We can't just call the
// variables lessonEntity because the same module will probably have an
// injected class to handle tests and quizes as well. That will eventually
// be converted to be a LessonEntity.

public class AssignmentEntity implements LessonEntity {

    private static Log log = LogFactory.getLog(AssignmentEntity.class);

    private static Cache assignmentCache = null;
    protected static final int DEFAULT_EXPIRATION = 10 * 60;

    private SimplePageBean simplePageBean;

    public void setSimplePageBean(SimplePageBean simplePageBean) {
	this.simplePageBean = simplePageBean;
    }

    private LessonEntity nextEntity = null;
    public void setNextEntity(LessonEntity e) {
	nextEntity = e;
    }
    
    static MemoryService memoryService = null;
    public void setMemoryService(MemoryService m) {
	memoryService = m;
    }

    static MessageLocator messageLocator = null;
    public void setMessageLocator(MessageLocator m) {
	messageLocator = m;
    }

    public void init () {
	assignmentCache = memoryService
	    .newCache("org.sakaiproject.lessonbuildertool.service.AssignmentEntity.cache");

	log.info("init()");

    }

    public void destroy()
    {
	assignmentCache.destroy();
	assignmentCache = null;

	log.info("destroy()");
    }


    // to create bean. the bean is used only to call the pseudo-static
    // methods such as getEntitiesInSite. So type, id, etc are left uninitialized

    protected AssignmentEntity() {
    }

    protected AssignmentEntity(int type, String id, int level) {
	this.type = type;
	this.id = id;
	this.level = level;
    }

    // the underlying object, something Sakaiish
    protected String id;
    protected int type;
    protected int level;
    // not required fields. If we need to look up
    // the actual objects, lets us cache them
    protected Assignment assignment;

    public Assignment getAssignment(String ref) {
	Assignment ret = (Assignment)assignmentCache.get(ref);
	if (ret != null)
	    return ret;

	try {
	    ret = AssignmentService.getAssignment(ref);
	} catch (Exception e) {
	    ret = null;
	}
	
	if (ret != null)
	    assignmentCache.put(ref, ret);
	return ret;
    }


    // type of the underlying object
    public int getType() {
	return type;
    }

    public int getLevel() {
	return level;
    }

    public int getTypeOfGrade() {
	if (assignment == null)
	    assignment = getAssignment(id);
	if (assignment == null)
	    return 1;

	AssignmentContent content = assignment.getContent();
	if (content == null) {
	    return 1;
	} else 
	    return assignment.getContent().getTypeOfGrade();
    }

  // hack for forums. not used for assessments, so always ok
    public boolean isUsable() {
	return true;
    }

    public String getReference() {
	return "/" + ASSIGNMENT + "/" + id;
    }

    // find topics in site, but organized by forum
    public List<LessonEntity> getEntitiesInSite() {

	Iterator i = AssignmentService.getAssignmentsForContext(ToolManager.getCurrentPlacement().getContext());

	List<LessonEntity> ret = new ArrayList<LessonEntity>();
	// security. assume this is only used in places where it's OK, so skip security checks
	while (i.hasNext()) {
	    Assignment a = (Assignment) i.next();
	    AssignmentEntity entity = new AssignmentEntity(TYPE_ASSIGNMENT, a.getId(), 1);
	    entity.assignment = a;
	    ret.add(entity);
	}

	if (nextEntity != null) 
	    ret.addAll(nextEntity.getEntitiesInSite());

	return ret;
    }

    public LessonEntity getEntity(String ref) {
	int i = ref.indexOf("/",1);
	if (i < 0) {
	    // old format, just the number
	    return new AssignmentEntity(TYPE_ASSIGNMENT, ref, 1);
	}
	String typeString = ref.substring(1, i);
	String idString = ref.substring(i+1);
	String id = "";
	try {
	    id = idString;
	} catch (Exception ignore) {
	    return null;
	}

	if (typeString.equals(ASSIGNMENT)) {
	    return new AssignmentEntity(TYPE_ASSIGNMENT, id, 1);
	} else if (nextEntity != null) {
	    return nextEntity.getEntity(ref);
	} else
	    return null;
    }
	

    // properties of entities
    public String getTitle() {
	if (assignment == null)
	    assignment = getAssignment(id);
	if (assignment == null)
	    return null;
	return assignment.getTitle();
    }

    public String getUrl() {
        return "/direct/assignment/" + id;
    }

    public Date getDueDate() {
	if (assignment == null)
	    assignment = getAssignment(id);
	if (assignment == null)
	    return null;
	return new Date(assignment.getDueTime().getTime());
    }

    // the following methods all take references. So they're in effect static.
    // They ignore the entity from which they're called.
    // The reason for not making them a normal method is that many of the
    // implementations seem to let you set access control and find submissions
    // from a reference, without needing the actual object. So doing it this
    // way could save some database activity

    // access control
    public boolean addEntityControl(String siteId, String groupId) throws IOException {
	Site site = null;
	String ref = "/assignment/a/" + siteId + "/" + id;

	try {
	    site = SiteService.getSite(siteId);
	} catch (Exception e) {
	    log.warn("Unable to find site " + siteId, e);
	    return false;
	}

	AssignmentEdit edit = null;
	
	try {
	    edit = AssignmentService.editAssignment(ref);
	} catch (IdUnusedException e) {
	    log.warn("ID unused ", e);
	    return false;
	} catch (PermissionException e) {
	    log.warn(e);
	    return false;
	} catch (InUseException e) {
	    log.warn(e);
	    return false;
	}

	boolean doCancel = true;

	try {
	    // need this to make sure we always unlock
	    
	    if (edit.getAccess() == Assignment.AssignmentAccess.GROUPED) {
		Collection<String> groups = edit.getGroups();
		groupId = "/site/" + siteId + "/group/" + groupId;

		if (groups.contains(groupId)) {
		    return true;
		}
		
		Group group = site.getGroup(groupId);
		if (group == null) {
		    return false;
		}
		
		// odd; getgruops returns a list of string
		// but setgroupacces wants a collection of actual groups
		// so we have to copy the list
		Collection<Group> newGroups = new ArrayList<Group>();
		for (String gid : groups) {
		    newGroups.add(site.getGroup(gid));
		}
		
		// now add in this one
		newGroups.add(group);

		try {
		    edit.setGroupAccess(newGroups);
		} catch (PermissionException e) {
		    log.warn(e);
		    return false;
		}

		AssignmentService.commitEdit(edit);
		doCancel = false;
		return true;

	    } else {
		// currently not grouped
		Collection groups = new ArrayList<String>();
		Group group = site.getGroup(groupId);
		
		if (group == null) {
		    log.warn("Could not find Group");
		    return false;
		}
		
		groups.add(group);
		
		try {
		    // this change mode to grouped
		    edit.setGroupAccess(groups);
		} catch (PermissionException e) {
		    log.warn(e);
		    return false;
		}
		
		AssignmentService.commitEdit(edit);
		doCancel = false;
		return true;
	    }
	} catch (Exception e) {
	    log.warn(e);
	    return false;
	} finally {
	    if (doCancel) {
		AssignmentService.commitEdit(edit);
	    }
	}
    }

    public boolean removeEntityControl(String siteId, String groupId) throws IOException {
	Site site = null;
	String ref = "/assignment/a/" + siteId + "/" + id;
	try {
	    site = SiteService.getSite(siteId);
	} catch (Exception e) {
	    log.warn("Unable to find site " + siteId, e);
	    return false;
	}
	
	AssignmentEdit edit = null;
	
	try {
	    edit = AssignmentService.editAssignment(ref);
	} catch (IdUnusedException e) {
	    log.warn(e);
	    return false;
	} catch (PermissionException e) {
	    log.warn(e);
	    return false;
	} catch (InUseException e) {
	    log.warn(e);
	    return false;
	}
	
	boolean doCancel = true;
	
	try {
	    // need this to make sure we always unlock
	    
	    if (edit.getAccess() == Assignment.AssignmentAccess.GROUPED) {
		Collection<String> groups = edit.getGroups();
		groupId = "/site/" + siteId + "/group/" + groupId;
		
		if (!groups.contains(groupId)) {
		    // nothing to do
		    return true;
		}
		
		// odd; getgruops returns a list of string
		// but setgroupacces wants a collection of actual groups
		// so we have to copy the list
		Collection<Group> newGroups = new ArrayList<Group>();
		for (String gid : groups) {
		    // remove our group
		    if (!gid.equals(groupId)) {
			newGroups.add(site.getGroup(gid));
		    }
		}
		
		if (newGroups.size() > 0) {
		    // there's groups left, just remove ours
		    try {
			edit.setGroupAccess(newGroups);
		    } catch (PermissionException e) {
			log.warn(e);
			return false;
		    }
		} else {
		    // no groups left, put site access back
		    edit.setAccess(Assignment.AssignmentAccess.SITE);
		    edit.clearGroupAccess();
		}
		
		AssignmentService.commitEdit(edit);
		doCancel = false;
		return true;
		
	    } else {
		// currently not grouped
		// nothing to do
		
		return true;
	    }
	    
	} catch (Exception e) {
	    log.warn(e);
	    return false;
	} finally {
	    if (doCancel) {
		AssignmentService.commitEdit(edit);
	    }
	}
	
    }

    // submission
    // do we need the data from submission?
    public boolean needSubmission(){
	return true;
    }

    public LessonSubmission getSubmission(String userId) {
	if (assignment == null)
	    assignment = getAssignment(id);
	if (assignment == null) {
	    log.warn("can't find assignment " + id);
	    return null;
	}

	User user = null;
	AssignmentSubmission submission = null;
	try {
	    user = UserDirectoryService.getUser(userId);
	    submission = AssignmentService.getSubmission(assignment.getReference(), user);
	} catch (Exception e) {
	    return null;
	}

	LessonSubmission ret= new LessonSubmission(null);

	if (submission == null)
	    return null;

	if (submission.getGradeReleased())  {
	    String grade = submission.getGrade();
	    ret.setGradeString(grade);
	}

	return ret;

    }

// we can do this for real, but the API will cause us to get all the submissions in full, not just a count.
// I think it's cheaper to get the best assessment, since we don't actually care whether it's 1 or >= 1.
    public int getSubmissionCount(String user) {
	if (getSubmission(user) == null)
	    return 0;
	else
	    return 1;
    }

    // URL to create a new item. Normally called from the generic entity, not a specific one                                                 
    // can't be null                                                                                                                         
    public List<UrlItem> createNewUrls(SimplePageBean bean) {
	ArrayList<UrlItem> list = new ArrayList<UrlItem>();
	String tool = bean.getCurrentTool("sakai.assignment.grades");
	if (tool != null) {
	    tool = "/portal/tool/" + tool + "?view=lisofass1&panel=Main&sakai_action=doView";
	    list.add(new UrlItem(tool, messageLocator.getMessage("simplepage.create_assignment")));
	}
	if (nextEntity != null)
	    list.addAll(nextEntity.createNewUrls(bean));
	return list;
    }


    // URL to edit an existing entity.                                                                                                       
    // Can be null if we can't get one or it isn't needed                                                                                    
    public String editItemUrl(SimplePageBean bean) {
	String tool = bean.getCurrentTool("sakai.assignment.grades");
	if (tool == null)
	    return null;
    
	return "/portal/tool/" + tool + "?assignmentId=/assignment/a/" + bean.getCurrentSiteId() +
	    "/" + id + "&panel=Main&sakai_action=doEdit_assignment";
    }


    // for most entities editItem is enough, however tests allow separate editing of                                                         
    // contents and settings. This will be null except in that situation                                                                     
    public String editItemSettingsUrl(SimplePageBean bean) {
	return null;
    }


}
