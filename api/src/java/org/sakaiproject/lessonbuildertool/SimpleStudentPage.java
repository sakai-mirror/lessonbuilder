package org.sakaiproject.lessonbuildertool;

public interface SimpleStudentPage {
	public long getId();
	public void setId(long id);
	
	public long getItemId();
	public void setItemId(long itemId);
	
	public long getPageId();
	public void setPageId(long pageId);
	
	public String getTitle();
	public void setTitle(String title);
	
	public String getOwner();
	public void setOwner(String owner);
	
	public boolean getGroupOwned();
	public void setGroupOwned(boolean go);
}
