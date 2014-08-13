package com.dv.gtusach.client;

import com.dv.gtusach.client.ui.GTusachView;
import com.dv.gtusach.client.ui.GTusachViewImpl;
import com.dv.gtusach.client.ui.LogonView;
import com.dv.gtusach.client.ui.LogonViewImpl;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;

public class ClientFactoryImpl implements ClientFactory {
	private static final EventBus eventBus = new SimpleEventBus();
	private static final PlaceController placeController = new PlaceController(
			eventBus);
	private static final LogonView logonView = new LogonViewImpl();
	private static final GTusachView mainView = new GTusachViewImpl();

	@Override
	public EventBus getEventBus() {
		return eventBus;
	}

	@Override
	public LogonView getLogonView() {
		return logonView;
	}

	@Override
	public PlaceController getPlaceController() {
		return placeController;
	}

	@Override
	public GTusachView getMainView() {
		return mainView;
	}

}
