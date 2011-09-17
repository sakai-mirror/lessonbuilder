package org.sakaiproject.lessonbuildertool.tool.view;

import uk.org.ponder.rsf.viewstate.SimpleViewParameters;

public class CommentsViewParameters extends SimpleViewParameters {
	public Long itemId = -1L;
	public boolean showAllComments = false;
	public boolean showNewComments = false;
	public long postedComment = -1;
	public String deleteComment = null;
	public String siteId = null;
	public long pageId = -1;
	
	public CommentsViewParameters() { super(); }
	
	public CommentsViewParameters(String VIEW_ID) {
		super(VIEW_ID);
	}
}
