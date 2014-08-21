package com.dv.gtusach.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.GwtTransient;
import com.google.gwt.user.client.rpc.IsSerializable;

public class User implements IsSerializable {
	public static enum PermissionEnum {
		None,
		Download,
		Create,
		Delete,
		Update,
		Javascript
	}
	
	private String id;
	private String name;
	private String role;
	private Date lastLoginTime;
	private long sessionId = -1;
	@GwtTransient private String password;
	
	public User() {		
	}
	
	public User(String name, String role) {
		this.name = name;
		this.role = role;
	}
	
	public void update(User user) {
		this.id = user.id;
		this.name = user.name;
		this.password = user.password;
		this.lastLoginTime = user.lastLoginTime;
		this.role = user.role;
		this.sessionId = user.sessionId;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public Date getLastLoginTime() {
		return lastLoginTime;
	}
	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}	
	public long getSessionId() {
		return sessionId;
	}
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}
	public String toString() {
		return name + "/" + role;
	}
}
