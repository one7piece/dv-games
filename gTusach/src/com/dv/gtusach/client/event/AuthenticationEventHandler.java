package com.dv.gtusach.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface AuthenticationEventHandler extends EventHandler {
	void onAuthenticationChanged(AuthenticationEvent event);
}
