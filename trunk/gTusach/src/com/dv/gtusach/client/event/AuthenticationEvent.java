package com.dv.gtusach.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class AuthenticationEvent extends GwtEvent<AuthenticationEventHandler> {
	public static enum AuthenticationTypeEnum {
		LOG_IN,
		LOG_OUT
	};
	
	public static Type<AuthenticationEventHandler> TYPE = new Type<AuthenticationEventHandler>();
	private String userName;
	private boolean success;
	private AuthenticationTypeEnum type;
	
	public AuthenticationEvent(String userName, AuthenticationTypeEnum type, boolean success) {
		this.userName = userName;
		this.success = success;
		this.type = type;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<AuthenticationEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(AuthenticationEventHandler handler) {
		handler.onAuthenticationChanged(this);
	}

	public String getUserName() {
		return userName;
	}

	public boolean isSuccess() {
		return success;
	}

	public AuthenticationTypeEnum getType() {
		return type;
	}		
}
