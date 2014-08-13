package com.dv.gtusach.client.mvp;

import com.dv.gtusach.client.ClientFactory;
import com.dv.gtusach.client.activity.LogonActivity;
import com.dv.gtusach.client.activity.MainActivity;
import com.dv.gtusach.client.place.LogonPlace;
import com.dv.gtusach.client.place.MainPlace;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class AppActivityMapper implements ActivityMapper {

	private ClientFactory clientFactory;

	/**
	 * AppActivityMapper associates each Place with its corresponding
	 * {@link Activity}
	 * 
	 * @param clientFactory
	 *            Factory to be passed to activities
	 */
	public AppActivityMapper(ClientFactory clientFactory) {
		super();
		this.clientFactory = clientFactory;
	}

	/**
	 * Map each Place to its corresponding Activity. This would be a great use
	 * for GIN.
	 */
	@Override
	public Activity getActivity(Place place) {
		// This is begging for GIN
		if (place instanceof MainPlace)
			return new MainActivity((MainPlace) place, clientFactory);
		else if (place instanceof LogonPlace)
			return new LogonActivity((LogonPlace) place, clientFactory);

		return null;
	}

}
