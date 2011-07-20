package org.sakaiproject.lessonbuildertool;

public class SimpleStudentPageImpl implements SimpleStudentPage {
	private long id; // Basic ID
	private long itemId; // ItemId of the section this page belongs to
	private long pageId; // ID of row in lesson_builder_pages
	private String title; // Title of page
	private String owner; // Owner of page
	private boolean groupOwned; // Whether or not the owner is a group
	
	public SimpleStudentPageImpl() { }
	
	public SimpleStudentPageImpl(long itemId, long pageId, String title, String owner, boolean groupOwned) {
		this.itemId = itemId;
		this.pageId = pageId;
		this.title = title;
		this.owner = owner;
		this.groupOwned = groupOwned;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
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
