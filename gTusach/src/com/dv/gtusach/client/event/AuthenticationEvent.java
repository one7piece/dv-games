package com.dv.gtusach.client.event;

import com.dv.gtusach.shared.User;
import com.google.gwt.event.shared.GwtEvent;

public class AuthenticationEvent extends GwtEvent<AuthenticationEventHandler> {
	public static enum AuthenticationTypeEnum {
		LOG_IN,
		LOG_OUT
	};
	
	public static Type<AuthenticationEventHandler> TYPE = new Type<AuthenticationEventHandler>();
	private User user;
	private String errorMessage;
	private AuthenticationTypeEnum type;
	
	public AuthenticationEvent(User user, AuthenticationTypeEnum type, String errorMessage) {
		this.user = user;
		this.errorMessage = errorMessage;
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

	public User getUser() {
		return user;
	}

	public boolean isSuccess() {
		return (errorMessage == null);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
	
	public AuthenticationTypeEnum getType() {
		return type;
	}		
}
