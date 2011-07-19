package org.sakaiproject.lessonbuildertool;

public class SimpleStudentPageImpl implements SimpleStudentPage {
	private long id; // Basic ID
	private long pageId; // ID of row in lesson_builder_pages
	private String title; // Title of page
	private String owner; // Owner of page
	private boolean groupOwned; // Whether or not the owner is a group
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public long getPageId() {
		return pageId;
	}
	public void setPageId(long pageId) {
		this.pageId = pageId;;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;;
	}
	
	public boolean getGroupOwned() {
		return groupOwned;
	}
	public void setGroupOwned(boolean go) {
		this.groupOwned = go;
	}
}
