package com.dv.gtusach.client.activity;

import com.dv.gtusach.client.ClientFactory;
import com.dv.gtusach.client.place.LogonPlace;
import com.dv.gtusach.client.ui.LogonView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class LogonActivity extends AbstractActivity implements
		LogonView.Presenter {
	
	// Used to obtain views, eventBus, placeController
	// Alternatively, could be injected via GIN
	private ClientFactory clientFactory;
	// Name that will be appended to "Hello,"
	private String name;

	public LogonActivity(LogonPlace place, ClientFactory clientFactory) {
		this.name = place.getName();
		this.clientFactory = clientFactory;
	}

	/**
	 * Invoked by the ActivityManager to start a new Activity
	 */
	@Override
	public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
		LogonView view = clientFactory.getLogonView();
		view.setName(name);
		view.setPresenter(this);
		containerWidget.setWidget(view.asWidget());
	}

	/**
	 * Ask user before stopping this activity
	 */
	@Override
	public String mayStop() {
		return "Please hold on. This activity is stopping.";
	}

	/**
	 * Navigate to a new Place in the browser
	 */
	public void goTo(Place place) {
		clientFactory.getPlaceController().goTo(place);
	}
}
