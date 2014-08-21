package com.dv.gtusach.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ParserScript implements IsSerializable {
	private String id;
	private String domainName;
	private String script;
	private Date timestamp;
	
	public String getDomainName() {
		return domainName;
	}
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	public String getScript() {
		return script;
	}
	public void setScript(String script) {
		this.script = script;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}		
	
	public String toString() {
		return domainName + "(" + timestamp + ")";
	}
}
