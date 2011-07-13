package org.sakaiproject.lessonbuildertool.tool.producers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.sakaiproject.lessonbuildertool.SimplePageComment;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.evolvers.SakaiFCKTextEvolver;
import org.sakaiproject.lessonbuildertool.tool.view.CommentsViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.GeneralViewParameters;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UIVerbatim;
import uk.org.ponder.rsf.components.decorators.UIFreeAttributeDecorator;
import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
import uk.org.ponder.rsf.evolvers.TextInputEvolver;
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
	private SimplePageToolDao simplePageToolDao;
	private HashMap<String, String> anonymousLookup = new HashMap<String, String>();
	private String currentUserId;
	
	public TextInputEvolver richTextEvolver;
	
	public String getViewID() {
		return VIEW_ID;
	}
	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {
		CommentsViewParameters params = (CommentsViewParameters) viewparams;
		
		// CKEDITOR complains if you try to replace the editor with one with the same name
		String editorId = "" + params.commentsCount;
		if(params.showAllComments) {
			editorId += "B";
		}
		
		if(params.deleteComment != null) {
			simplePageBean.deleteComment(params.deleteComment);
			
			// Since comment deletion is via AJAX, you have to make sure the ID is unique.
			editorId += params.deleteComment;
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
					anonymousLookup.put(comment.getAuthor(), "Anonymous " + i);
					i++;
				}
			}
		}
		
		boolean highlighted = false;
		
		boolean canEditPage = simplePageBean.canEditPage();
		
		// Remove any "phantom" comments. So that the anonymous order stays the same,
		// comments are deleted by removing all content.
		for(int i = comments.size()-1; i >= 0; i--) {
			if(comments.get(i).getComment().equals("") || comments.get(i).getComment() == null) {
				comments.remove(i);
			}
		}
		
		if(comments.size() <= 5 || params.showAllComments) {
			for(int i = 0; i < comments.size(); i++) {
				printComment(comments.get(i), tofill, (params.postedComment == comments.get(i).getId()), anonymous, canEditPage, params);
				if(!highlighted) {
					highlighted = (params.postedComment == comments.get(i).getId());
				}
			}
		}else {
			UIBranchContainer container = UIBranchContainer.make(tofill, "commentDiv:");
			CommentsViewParameters eParams = new CommentsViewParameters(VIEW_ID);
			eParams.itemId = params.itemId;
			eParams.commentsCount = params.commentsCount;
			eParams.showAllComments=true;
			UIInternalLink.make(container, "to-load", eParams);
			
			UIOutput.make(container, "load-more-link", messageLocator.getMessage("simplepage.see_all_comments").replace("{}", Integer.toString(comments.size())));
			
			// Show 5 most recent comments
			for(int i = comments.size()-5; i < comments.size(); i++) {
				printComment(comments.get(i), tofill, (params.postedComment == comments.get(i).getId()), anonymous, canEditPage, params);
				if(!highlighted) {
					highlighted = (params.postedComment == comments.get(i).getId());
				}
			}
		}
		
		if(highlighted) {
			// We have something to highlight
			UIOutput.make(tofill, "highlightScript");
		}
		
		if(anonymous && canEditPage) {
			UIOutput.make(tofill, "anonymousAlert");
		}
		
		UIForm form = UIForm.make(tofill, "comment-form");

		UIInput.make(form, "comment-item-id", "#{simplePageBean.itemId}", params.itemId.toString());
		UIInput.make(form, "comment-text-area", "#{simplePageBean.comment}");
		
		UIInput fckInput = UIInput.make(form, "comment-text-area-evolved:", "#{simplePageBean.formattedComment}");
		fckInput.decorate(new UIFreeAttributeDecorator("height", "175"));
		fckInput.decorate(new UIFreeAttributeDecorator("width", "800"));
		fckInput.decorate(new UIStyleDecorator("evolved-box"));
		
		((SakaiFCKTextEvolver)richTextEvolver).evolveTextInput(fckInput, editorId);
		
		UICommand.make(form, "add-comment", "#{simplePageBean.addComment}");
	}
	
	public void printComment(SimplePageComment comment, UIContainer tofill, boolean highlight, boolean anonymous, boolean showDelete, CommentsViewParameters params) {
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
			
			if(author == null) author = "Anonymous User"; // Shouldn't ever occur
			
			if(simplePageBean.canEditPage()) {
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
		}
		
		String timeDifference = getTimeDifference(comment.getTimePosted().getTime());
		
		UIOutput.make(commentContainer, "timePosted", timeDifference);
		
		if(showDelete) {
			UIOutput.make(commentContainer, "deleteSpan");
			
			CommentsViewParameters eParams = (CommentsViewParameters) params.copy();
			eParams.deleteComment = comment.getUUID();
			
			UIInternalLink.make(commentContainer, "deleteCommentURL", eParams);
		}
		
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
		if(d.getMonth() == now.getMonth() && d.getDate() == now.getDate() && d.getYear() == now.getYear()) {
			return ((d.getHours()-1) % 12 + 1) + ":" + (d.getMinutes() < 10? "0" : "") + d.getMinutes() + " (" + descrip + ")";
		}else if(d.getYear() == now.getYear()) {
			return translateMonth(d.getMonth()) + " " + d.getDate() + " (" + descrip + ")";
		}else {
			return d.getMonth() + "/" + d.getDate() + "/" + d.getYear() + " (" + descrip + ")";
		}
	}
	
	private String translateMonth(int month) {
		switch(month) {
		case 1:
			return messageLocator.getMessage("simplepage.jan");
		case 2:
			return messageLocator.getMessage("simplepage.feb");
		case 3:
			return messageLocator.getMessage("simplepage.mar");
		case 4:
			return messageLocator.getMessage("simplepage.apr");
		case 5:
			return messageLocator.getMessage("simplepage.may");
		case 6:
			return messageLocator.getMessage("simplepage.jun");
		case 7:
			return messageLocator.getMessage("simplepage.jul");
		case 8:
			return messageLocator.getMessage("simplepage.aug");
		case 9:
			return messageLocator.getMessage("simplepage.sept");
		case 10:
			return messageLocator.getMessage("simplepage.oct");
		case 11:
			return messageLocator.getMessage("simplepage.nov");
		case 12:
			return messageLocator.getMessage("simplepage.dec");
		default:
			return "";
		}
	}
	
	public void setSimplePageBean(SimplePageBean bean) {
		simplePageBean = bean;
	}
	
	public void setMessageLocator(MessageLocator locator) {
		messageLocator = locator;
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
