package com.dv.gtusach.client;

import com.dv.gtusach.client.ui.GTusachView;
import com.dv.gtusach.client.ui.LogonView;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;

public interface ClientFactory {
	EventBus getEventBus();

	PlaceController getPlaceController();

	LogonView getLogonView();

	GTusachView getMainView();
}
