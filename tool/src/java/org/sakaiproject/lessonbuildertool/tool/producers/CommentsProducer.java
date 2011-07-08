package org.sakaiproject.lessonbuildertool.tool.producers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.lessonbuildertool.SimplePageComment;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
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
			eParams.showAllComments=true;
			UIInternalLink.make(container, "to-load", eParams);
			
			UIOutput.make(container, "load-more-link", messageLocator.getMessage("simplepage.see_all_comments").replace("{}", Integer.toString(comments.size())));
			
			// Show most recent 5 comments
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
		fckInput.decorate(new UIFreeAttributeDecorator("height", "200"));
		fckInput.decorate(new UIFreeAttributeDecorator("width", "600"));
		richTextEvolver.evolveTextInput(fckInput);
		
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
		
		UIOutput.make(commentContainer, "comment", comment.getComment());
	}
	
	public String getTimeDifference(long timeMillis) {
		long difference = Math.round((System.currentTimeMillis() - timeMillis) / 1000); // In seconds
		
		// These constants are calculated to take rounding into effect, and try to give a fairly
		// accurate representation of the time difference using words.
		
		if(difference < 45) {
			return "Seconds Ago";
		}else if(difference < 90) {
			return "1 Minute Ago";
		}else if(difference < 3570) { // 2 mins --> 59 mins
			int minutes = Math.max(2, Math.round(difference / 60));
			return minutes + " Minutes Ago";
		}else if(difference < 7170) {
			return "1 Hour Ago";
		}else if(difference < 84600) { // 2 hours --> 23 hours
			int hours = Math.max(2, Math.round(difference / 3600));
			return hours + " Hours Ago";
		}else if(difference < 129600) {
			return "1 Day Ago";
		}else if(difference < 2548800) { // 2 days --> 29 days
			int days = Math.max(2, Math.round(difference / 86400));
			return days + " Days Ago";
		}else if(difference < 3888000) {
			return "1 Month Ago";
		}else if(difference < 29808000) { // 2 months --> 11 months
			int months = Math.max(2, Math.round(difference / 2592000));
			return months + " Months Ago";
		}else if(difference < 47304000) {
			return "1 Year Ago";
		}else { // 2+ years
			int years = Math.max(2, Math.round(difference / 31536000));
			return years + " Years Ago";
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
