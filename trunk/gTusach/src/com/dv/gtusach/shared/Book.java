package com.dv.gtusach.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Book implements IsSerializable {
	public static enum BookStatus {
		NONE, WORKING, COMPLETED, ERROR, ABORTED
	};

	private String id;
	private String title;
	private String author;
	private Date createdTime = new Date();
	private String createdBy = "dv";
	private BookStatus status = BookStatus.NONE;
	private int buildTimeSec;
	private String startPageUrl = "";
	private int currentPageNo = 0;
	private String currentPageUrl = "";
	private int maxNumPages = 0;
	private Date lastUpdatedTime = new Date();
	private String errorMsg = "";
	private boolean epubCreated = false;

	public boolean isEpubCreated() {
		return epubCreated;
	}

	public void setEpubCreated(boolean epubCreated) {
		this.epubCreated = epubCreated;
	}

	public String getPages() {
		String pageStr = getCurrentPageNo() + "/";
		if (getMaxNumPages() > 0) {
			pageStr += getMaxNumPages();
		} else {
			pageStr += "end";
		}
		return pageStr;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getIdInt() {
		return (id != null ? Integer.parseInt(id) : 0);
	}

	public void setIdInt(int id) {
		this.id = String.valueOf(id);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public BookStatus getStatus() {
		return status;
	}

	public void setStatus(BookStatus status) {
		this.status = status;
	}

	public String getStatusStr() {
		return (status != null ? status.name() : BookStatus.NONE.name());
	}

	public void setStatusStr(String s) {
		if (s != null && s.length() > 0) {
			status = BookStatus.valueOf(s);
		} else {
			status = BookStatus.NONE;
		}
	}

	public int getBuildTimeSec() {
		return buildTimeSec;
	}

	public void setBuildTimeSec(int buildTimeSec) {
		this.buildTimeSec = buildTimeSec;
	}

	public String getStartPageUrl() {
		return startPageUrl;
	}

	public void setStartPageUrl(String startPageUrl) {
		this.startPageUrl = startPageUrl;
	}

	public int getCurrentPageNo() {
		return currentPageNo;
	}

	public void setCurrentPageNo(int currentPageNo) {
		this.currentPageNo = currentPageNo;
	}

	public String getCurrentPageUrl() {
		return currentPageUrl;
	}

	public void setCurrentPageUrl(String nextPageUrl) {
		this.currentPageUrl = nextPageUrl;
	}

	public int getMaxNumPages() {
		return maxNumPages;
	}

	public void setMaxNumPages(int maxNumPages) {
		this.maxNumPages = maxNumPages;
	}

	public Date getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setLastUpdatedTime(Date lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String toString() {
		return title + "(" + id + ")";
	}
}
