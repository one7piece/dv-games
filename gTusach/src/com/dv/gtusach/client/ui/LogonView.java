package com.dv.gtusach.client.ui;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * View interface. Extends IsWidget so a view impl can easily provide its
 * container widget.
 * 
 */
public interface LogonView extends IsWidget {
	void setName(String helloName);

	void setPresenter(Presenter listener);

	public interface Presenter {
		void goTo(Place place);
	}
}