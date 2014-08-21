package com.dv.gtusach.client.activity;

import com.dv.gtusach.client.ClientFactory;
import com.dv.gtusach.client.event.AuthenticationEvent;
import com.dv.gtusach.client.event.AuthenticationEvent.AuthenticationTypeEnum;
import com.dv.gtusach.client.place.LogonPlace;
import com.dv.gtusach.client.place.MainPlace;
import com.dv.gtusach.client.ui.LogonView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class LogonActivity extends AbstractActivity implements
		LogonView.Presenter {
		
	// Used to obtain views, eventBus, placeController
	// Alternatively, could be injected via GIN
	private ClientFactory clientFactory;

	public LogonActivity(LogonPlace place, ClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}
	
	/**
	 * Invoked by the ActivityManager to start a new Activity
	 */
	@Override
	public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
		LogonView view = clientFactory.getLogonView();
		view.setPresenter(this);
		containerWidget.setWidget(view.asWidget());
	}
	
	/**
	 * Navigate to a new Place in the browser
	 */
	public void goTo(Place place) {
		clientFactory.getPlaceController().goTo(place);
	}

	@Override
	public void logon(final String userName, final String password) {
	}
	
	public void logout() {
	}
	
}
