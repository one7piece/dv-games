package com.dv.gtusach.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface PropertyChangeEventHandler extends EventHandler {
	void onPropertyChanged(PropertyChangeEvent event);
}
