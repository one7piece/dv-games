package com.dv.gtusach.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SystemInfo implements IsSerializable {
	private String id;
	private Date bookLastUpdateTime;
	private Date userLastUpdateTime;
	private Date scriptLastUpdateTime;
	private Boolean editingScript;
		
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getBookLastUpdateTime() {
		return bookLastUpdateTime;
	}
	public void setBookLastUpdateTime(Date bookLastUpdateTime) {
		this.bookLastUpdateTime = bookLastUpdateTime;
	}
	public Date getUserLastUpdateTime() {
		return userLastUpdateTime;
	}
	public void setUserLastUpdateTime(Date userLastUpdateTime) {
		this.userLastUpdateTime = userLastUpdateTime;
	}
	public Date getScriptLastUpdateTime() {
		return scriptLastUpdateTime;
	}
	public void setScriptLastUpdateTime(Date scriptLastUpdateTime) {
		this.scriptLastUpdateTime = scriptLastUpdateTime;
	}
	public Boolean isEditingScript() {
		return editingScript;
	}
	public void setEditingScript(Boolean editingScript) {
		this.editingScript = editingScript;
	}	
}
