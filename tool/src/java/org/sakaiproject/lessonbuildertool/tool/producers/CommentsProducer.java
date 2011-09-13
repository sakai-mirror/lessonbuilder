package org.sakaiproject.lessonbuildertool.tool.producers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Calendar;

import org.sakaiproject.lessonbuildertool.SimplePageComment;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageLogEntry;
import org.sakaiproject.lessonbuildertool.SimpleStudentPage;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.view.CommentsViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.GeneralViewParameters;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.util.ResourceLoader;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.localeutil.LocaleGetter;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UIVerbatim;
import uk.org.ponder.rsf.components.decorators.UIFreeAttributeDecorator;
import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

public class CommentsProducer implements ViewComponentProducer, ViewParamsReporter, NavigationCaseReporter {
	public static final String VIEW_ID = "Comments";

	private SimplePageBean simplePageBean;
	private MessageLocator messageLocator;
	private LocaleGetter localeGetter;
	private SimplePageToolDao simplePageToolDao;
	private HashMap<String, String> anonymousLookup = new HashMap<String, String>();
	private String currentUserId;
	private String owner = null;
        Locale M_locale = null;
        DateFormat df = null;
        DateFormat dfTime = null;
        DateFormat dfDate = null;
	
	public String getViewID() {
		return VIEW_ID;
	}
	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {
		CommentsViewParameters params = (CommentsViewParameters) viewparams;
			
		// set up locale
		String langLoc[] = localeGetter.get().toString().split("_");
		if (langLoc.length >= 2) {
			if ("en".equals(langLoc[0]) && "ZA".equals(langLoc[1])) {
				M_locale = new Locale("en", "GB");
			} else {
				M_locale = new Locale(langLoc[0], langLoc[1]);
			}
		} else {
			M_locale = new Locale(langLoc[0]);
		}

		df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, new ResourceLoader().getLocale());
		df.setTimeZone(TimeService.getLocalTimeZone());
		System.out.println(TimeService.getLocalTimeZone().getDisplayName());
		dfTime = DateFormat.getTimeInstance(DateFormat.SHORT, M_locale);
		dfTime.setTimeZone(TimeService.getLocalTimeZone());
		dfDate = DateFormat.getDateInstance(DateFormat.MEDIUM, M_locale);
		dfDate.setTimeZone(TimeService.getLocalTimeZone());

		// errors redirect back to ShowPage. But if thisi s embeded in the page, ShowPage
		// will call us again. This is very hard for the user to recover from. So trap
		// all possible errors. It may result in an incomplete page or something invalid,
		// but better that than an infinite recursion.

		try {
		
		    SimplePage currentPage = simplePageToolDao.getPage(params.pageId);
		    simplePageBean.setCurrentSiteId(params.siteId);
		    simplePageBean.setCurrentPage(currentPage);
		    simplePageBean.setCurrentPageId(params.pageId);

		    UIOutput.make(tofill, "mainlist").decorate(new UIFreeAttributeDecorator("aria-label", messageLocator.getMessage("simplepage.comments-section")));

		SimplePageItem commentsItem = simplePageToolDao.findItem(params.itemId);

		if(commentsItem != null && commentsItem.getSakaiId() != null && !commentsItem.getSakaiId().equals("")) {
			SimpleStudentPage studentPage = simplePageToolDao.findStudentPage(Long.valueOf(commentsItem.getSakaiId()));
			if(studentPage != null) {
				owner = studentPage.getOwner();
			}
		}
		
		if(params.deleteComment != null) {
			simplePageBean.deleteComment(params.deleteComment);
		}
		
		List<SimplePageComment> comments = (List<SimplePageComment>) simplePageToolDao.findComments(params.itemId);
		
		// Make sure everything is chronological
		Collections.sort(comments, new Comparator<SimplePageComment>() {
			public int compare(SimplePageComment c1, SimplePageComment c2) {
				return c1.getTimePosted().compareTo(c2.getTimePosted());
			}
		});
		
		currentUserId = UserDirectoryService.getCurrentUser().getId();
		
		boolean anonymous = simplePageBean.findItem(params.itemId).isAnonymous();
		if(anonymous) {
			int i = 1;
			for(SimplePageComment comment : comments) {
				if(!anonymousLookup.containsKey(comment.getAuthor())) {
					anonymousLookup.put(comment.getAuthor(), messageLocator.getMessage("simplepage.anonymous") + " " + i);
					i++;
				}
			}
		}
		
		boolean highlighted = false;
		
		// Remove any "phantom" comments. So that the anonymous order stays the same,
		// comments are deleted by removing all content.
		for(int i = comments.size()-1; i >= 0; i--) {
			if(comments.get(i).getComment() == null || comments.get(i).getComment().equals("")) {
				comments.remove(i);
			}
		}
		
		// We don't want page owners to edit comments on their page
		// at the moment.  Perhaps add option?
		boolean canEditPage = simplePageBean.getEditPrivs() == 0;
		
		boolean editable = false;
		
		if(comments.size() <= 5 || params.showAllComments) {
			for(int i = 0; i < comments.size(); i++) {
				boolean canEdit = simplePageBean.canModifyComment(comments.get(i), canEditPage);
				
				printComment(comments.get(i), tofill, (params.postedComment == comments.get(i).getId()), anonymous, canEdit, params);
				if(!highlighted) {
					highlighted = (params.postedComment == comments.get(i).getId());
				}
				
				if(!editable) editable = canEdit;
			}
		}else {
			UIBranchContainer container = UIBranchContainer.make(tofill, "commentDiv:");
			CommentsViewParameters eParams = new CommentsViewParameters(VIEW_ID);
			eParams.itemId = params.itemId;
			eParams.showAllComments=true;
			eParams.pageId = params.pageId;
			eParams.siteId = params.siteId;
			
			UIInternalLink.make(container, "to-load", eParams);
			
			UIOutput.make(container, "load-more-link", messageLocator.getMessage("simplepage.see_all_comments").replace("{}", Integer.toString(comments.size())));
			
			// Show 5 most recent comments
			for(int i = comments.size()-5; i < comments.size(); i++) {
				boolean canEdit = simplePageBean.canModifyComment(comments.get(i), canEditPage);
				printComment(comments.get(i), tofill, (params.postedComment == comments.get(i).getId()), anonymous, canEdit, params);
				if(!highlighted) {
					highlighted = (params.postedComment == comments.get(i).getId());
				}
				
				if(!editable) editable = canEdit;
			}
		}
		
		if(highlighted) {
			// We have something to highlight
			UIOutput.make(tofill, "highlightScript");
		}
		
		if(anonymous && canEditPage && comments.size() > 0 && simplePageBean.getLogEntry(params.itemId) == null) {
			// Tells the admin that they can see the names, but everyone else can't
			UIOutput.make(tofill, "anonymousAlert");
			SimplePageLogEntry log = simplePageToolDao.makeLogEntry(currentUserId, params.itemId, null);
			simplePageBean.saveItem(log);
		}else if(editable && simplePageBean.getEditPrivs() != 0) {
			// Warns user that they only have 30 mins to edit.
			UIOutput.make(tofill, "editAlert");
		}

		} catch (Exception e) {
		    e.printStackTrace();
		    System.out.println("comments error " + e);
		};

	}
	
	public void printComment(SimplePageComment comment, UIContainer tofill, boolean highlight, boolean anonymous, boolean showModifiers, CommentsViewParameters params) {
		UIBranchContainer commentContainer = UIBranchContainer.make(tofill, "commentDiv:");
		if(highlight) commentContainer.decorate(new UIStyleDecorator("highlight-comment"));
		
		String author;
		
		if(!anonymous) {
			try {
				User user = UserDirectoryService.getUser(comment.getAuthor());
				author = user.getDisplayName();
			}catch(Exception ex) {
				author = messageLocator.getMessage("simplepage.comment-unknown-user");
				ex.printStackTrace();
			}
		}else {
			author = anonymousLookup.get(comment.getAuthor());
			
			if(comment.getAuthor().equals(owner)) {
				author = messageLocator.getMessage("simplepage.comment-author-owner");
			}
			
			if(author == null) author = "Anonymous User"; // Shouldn't ever occur
			
			if(simplePageBean.getEditPrivs() == 0) {
				try {
					User user = UserDirectoryService.getUser(comment.getAuthor());
					author += " (" + user.getDisplayName() + ")";
				}catch(Exception ex) {
					author += " (" + messageLocator.getMessage("simplepage.comment-unknown-user") + ")";
				}
			}else if(comment.getAuthor().equals(currentUserId)) {
				author += " (" + messageLocator.getMessage("simplepage.comment-you") + ")";
			}
		}
		
		UIOutput authorOutput = UIOutput.make(commentContainer, "userId", author);
		
		if(comment.getAuthor().equals(currentUserId)) {
			authorOutput.decorate(new UIStyleDecorator("specialCommenter"));
			authorOutput.decorate(new UIStyleDecorator("personalComment"));
		}else if(comment.getAuthor().equals(owner)) {
			authorOutput.decorate(new UIStyleDecorator("specialCommenter"));
			authorOutput.decorate(new UIStyleDecorator("ownerComment"));
		}
		
		String timeDifference = getTimeDifference(comment.getTimePosted().getTime());
		
		UIOutput.make(commentContainer, "timePosted", timeDifference);
		
		if(showModifiers) {
			UIOutput.make(commentContainer, "deleteSpan");
			
			CommentsViewParameters eParams = (CommentsViewParameters) params.copy();
			eParams.deleteComment = comment.getUUID();
			eParams.pageId = params.pageId;
			eParams.siteId = params.siteId;
			
			UIInternalLink.make(commentContainer, "deleteCommentURL", eParams);
			
			UIOutput.make(commentContainer, "deleteComment").
			    decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.comment-delete").replace("{}", author)));

			UIOutput.make(commentContainer, "editComment").
			    decorate(new UIFreeAttributeDecorator("onclick", "edit($(this), " + comment.getId() + ");")).
			    decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.comment-edit").replace("{}", author)));

		}
		
		String dateString = df.format(comment.getTimePosted());

		UIOutput.make(commentContainer, "replyTo").
		    decorate(new UIFreeAttributeDecorator("onclick", "replyToComment($(this),'" + 
							     messageLocator.getMessage("simplepage.in-reply-to").replace("{1}", author).replace("{2}", dateString) + "')")).
		    decorate(new UIFreeAttributeDecorator("title", messageLocator.getMessage("simplepage.comment-reply").replace("{}",author)));

		if(!comment.getHtml()) {
			UIOutput.make(commentContainer, "comment", comment.getComment());
		}else {
			UIVerbatim.make(commentContainer, "comment", comment.getComment());
		}
	}
	
	public String getTimeDifference(long timeMillis) {
		long difference = Math.round((System.currentTimeMillis() - timeMillis) / 1000); // In seconds
		
		// These constants are calculated to take rounding into effect, and try to give a fairly
		// accurate representation of the time difference using words.
		
		String descrip = "";
		
		if(difference < 45) {
			descrip = messageLocator.getMessage("simplepage.seconds");
		}else if(difference < 90) {
			descrip = messageLocator.getMessage("simplepage.one_min");
		}else if(difference < 3570) { // 2 mins --> 59 mins
			int minutes = Math.max(2, Math.round(difference / 60));
			descrip = messageLocator.getMessage("simplepage.x_min").replace("{}", String.valueOf(minutes));
		}else if(difference < 7170) {
			descrip = messageLocator.getMessage("simplepage.one_hour");
		}else if(difference < 84600) { // 2 hours --> 23 hours
			int hours = Math.max(2, Math.round(difference / 3600));
			descrip = messageLocator.getMessage("simplepage.x_hour").replace("{}", String.valueOf(hours));
		}else if(difference < 129600) {
			descrip = messageLocator.getMessage("simplepage.one_day");
		}else if(difference < 2548800) { // 2 days --> 29 days
			int days = Math.max(2, Math.round(difference / 86400));
			descrip = messageLocator.getMessage("simplepage.x_day").replace("{}", String.valueOf(days));
		}else if(difference < 3888000) {
			descrip = messageLocator.getMessage("simplepage.one_month");
		}else if(difference < 29808000) { // 2 months --> 11 months
			int months = Math.max(2, Math.round(difference / 2592000));
			descrip = messageLocator.getMessage("simplepage.x_month").replace("{}", String.valueOf(months));
		}else if(difference < 47304000) {
			descrip = messageLocator.getMessage("simplepage.one_year");
		}else { // 2+ years
			int years = Math.max(2, Math.round(difference / 31536000));
			descrip = messageLocator.getMessage("simplepage.x_year").replace("{}", String.valueOf(years));
		}
		
		Date d = new Date(timeMillis);
		Date now = new Date();
		
		Calendar cpost = Calendar.getInstance(TimeService.getLocalTimeZone(), M_locale);
		Calendar cnow = Calendar.getInstance(TimeService.getLocalTimeZone(), M_locale);
		cpost.setTime(d);
		cnow.setTime(now);

		if(cpost.get(Calendar.MONTH) == cnow.get(Calendar.MONTH) &&
		   cpost.get(Calendar.DATE) == cnow.get(Calendar.DATE) &&
		   cpost.get(Calendar.YEAR) == cnow.get(Calendar.YEAR)) {
			return dfTime.format(d) + " (" + descrip + ")";
		}else if(d.getYear() == now.getYear()) {
			return dfDate.format(d) + " (" + descrip + ")";
		}else {
			return dfDate.format(d) + " (" + descrip + ")";
		}
	}
	
	public void setSimplePageBean(SimplePageBean bean) {
		simplePageBean = bean;
	}
	
	public void setMessageLocator(MessageLocator locator) {
		messageLocator = locator;
	}
	
	public void setLocaleGetter(LocaleGetter getter) {
		localeGetter = getter;
	}

	public void setSimplePageToolDao(SimplePageToolDao simplePageToolDao) {
		this.simplePageToolDao = simplePageToolDao;
	}
	
	public ViewParameters getViewParameters() {
		return new CommentsViewParameters();
	}
	
	public List reportNavigationCases() {
		List<NavigationCase> togo = new ArrayList<NavigationCase>();
		togo.add(new NavigationCase(null, new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("success", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		
		GeneralViewParameters commentsParameters = new GeneralViewParameters(ShowPageProducer.VIEW_ID);
		commentsParameters.postedComment = true;
		togo.add(new NavigationCase("added-comment", commentsParameters));
		
		return togo;
	}
	
}
