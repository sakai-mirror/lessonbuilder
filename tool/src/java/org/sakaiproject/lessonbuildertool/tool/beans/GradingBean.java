package org.sakaiproject.lessonbuildertool.tool.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Collection;

import org.sakaiproject.lessonbuildertool.SimplePageComment;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.SimplePageQuestionResponse;
import org.sakaiproject.lessonbuildertool.SimpleStudentPage;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.service.GradebookIfc;
import org.sakaiproject.authz.cover.AuthzGroupService;

public class GradingBean {
	public String id;
	public String points;
	public String jsId;
	public String type;
	
	private SimplePageToolDao simplePageToolDao;
	private GradebookIfc gradebookIfc;
	private SimplePageBean simplePageBean;
	
	public void setSimplePageToolDao(SimplePageToolDao simplePageToolDao) {
		this.simplePageToolDao = simplePageToolDao;
	}
	
	public void setGradebookIfc(GradebookIfc gradebookIfc) {
		this.gradebookIfc = gradebookIfc;
	}
	
	public void setSimplePageBean(SimplePageBean simplePageBean) {
		this.simplePageBean = simplePageBean;
	}
	
	public String[] getResults() {
		if(simplePageBean.getEditPrivs() != 0) {
			return new String[]{"failure", jsId, "-1"};
		}
		
		// Make sure they gave us a valid amount of points.
		try {
			Double.valueOf(points);
		}catch(Exception ex) {
			return new String[]{"failure", jsId, "-1"};
		}
		
		boolean r = false;
		
		if("comment".equals(type)) {
			r = gradeComment();
		}else if("student".equals(type)) {
			r = gradeStudentPage();
		}else if("question".equals(type)) {
			r = gradeQuestion();
		}
		
		if(r) {
			return new String[] {"success", jsId, String.valueOf(Double.valueOf(points))};
		}else {
			return new String[]{"failure", jsId, "-1"};
		}
	}
	
	private boolean gradeComment() {
		boolean r = false;
		
		SimplePageComment comment = simplePageToolDao.findCommentByUUID(id);
		SimplePageItem commentItem = simplePageToolDao.findItem(comment.getItemId());
		SimpleStudentPage studentPage = null;  // comments on student page only
		SimplePageItem topItem = null; // comments on student page only

		if(commentItem.getPageId() <= 0) {
		    studentPage = simplePageToolDao.findStudentPage(Long.valueOf(commentItem.getSakaiId()));
		    topItem = simplePageToolDao.findItem(studentPage.getItemId());
		}

		String gradebookId = null;

		if (studentPage != null) {
		    gradebookId = topItem.getAltGradebook();
		} else {
		    gradebookId = commentItem.getGradebookId();
		}

		if(Double.valueOf(points).equals(comment.getPoints())) {
			return true;
		}
		
		try {
			r = gradebookIfc.updateExternalAssessmentScore(simplePageBean.getCurrentSiteId(), gradebookId, comment.getAuthor(), Double.toString(Double.valueOf(points)));
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		if(r) {
			List<SimplePageComment> comments;
			if(commentItem.getPageId() > 0) {
				comments = simplePageToolDao.findCommentsOnItemByAuthor(comment.getItemId(), comment.getAuthor());
			}else {
				List<SimpleStudentPage> studentPages = simplePageToolDao.findStudentPages(studentPage.getItemId());
				List<Long> commentsItemIds = new ArrayList<Long>();
				for(SimpleStudentPage p : studentPages) {
					commentsItemIds.add(p.getCommentsSection());
				}
				
				comments = simplePageToolDao.findCommentsOnItemsByAuthor(commentsItemIds, comment.getAuthor());
			}
			
			// Make sure all of the comments by this person have the grade.
			for(SimplePageComment c : comments) {
				c.setPoints(Double.valueOf(points));
				simplePageBean.update(c, false);
			}
		}
		
		return r;
	}
	
	private boolean gradeStudentPage() {
		boolean r = false;
		SimpleStudentPage page = simplePageToolDao.findStudentPage(Long.valueOf(id));
		SimplePageItem pageItem = simplePageToolDao.findItem(page.getItemId());
		// the idea was to not update if there's no change in points
		// but there can be reasons to want to force grades back to the gradebook,
		// particually for group pages where the group may have changed
		//if(Double.valueOf(points).equals(page.getPoints())) {
		//  return new String[] {"success", jsId, String.valueOf(page.getPoints())};
	        //}
		
		try {
		    String owner = page.getOwner();
		    String group = page.getGroup();
		    if (group == null)
			r = gradebookIfc.updateExternalAssessmentScore(simplePageBean.getCurrentSiteId(), pageItem.getGradebookId(), page.getOwner(), Double.toString(Double.valueOf(points)));
		    else {
			HashSet<String>groups = new HashSet<String>();
			if (group != null)
			    group = "/site/" + simplePageBean.getCurrentSiteId() + "/group/" + group;
			groups.add(group);
                            Collection<String>users = AuthzGroupService.getAuthzUsersInGroups(groups);
			// if we have more than one user, in theory some might fail and some succeed. For the
			// moment just update the grade 
			r = true;
                            for (String u: users)
                                gradebookIfc.updateExternalAssessmentScore(simplePageBean.getCurrentSiteId(), pageItem.getGradebookId(),
								       u, Double.toString(Double.valueOf(points)));
			
		    }
		}catch(Exception ex) {
		    System.out.println("Exception updating grade " + ex);
		}
		
		if(r) {
			page.setPoints(Double.valueOf(points));
			simplePageBean.update(page, false);
		}
		
		return r;
	}
	
	private boolean gradeQuestion() {
		boolean r = false;
		SimplePageQuestionResponse response = simplePageToolDao.findQuestionResponse(Long.valueOf(id));
		SimplePageItem questionItem = simplePageBean.findItem(response.getQuestionId());
		
		r = "true".equals(questionItem.getAttribute("questionGraded")) || questionItem.getGradebookId() != null;
		if (questionItem.getGradebookId() != null)
		    try {
			r = gradebookIfc.updateExternalAssessmentScore(simplePageBean.getCurrentSiteId(), questionItem.getGradebookId(), response.getUserId(), Double.toString(Double.valueOf(points)));
		    }catch(Exception ex) {
			System.out.println("Exception updating grade " + ex);
		    }
		
		if(r) {
			response.setPoints(Double.valueOf(points));
			
			// Only set the answer as correct if they got the maximum number of points.
			// Unfortunately, points don't map well to the boolean correct/incorrect model,
			// but I'd rather not clutter the faculty interface with more options.
			if (questionItem.getGradebookPoints() == null || points == null)
			    return false;
			if(Double.valueOf(points).equals(Double.valueOf(questionItem.getGradebookPoints()))) {
				response.setCorrect(true);
			}else {
				response.setCorrect(false);
			}
			
			response.setOverridden(true);
			simplePageBean.update(response);
		}
		
		return r;
	}
}
