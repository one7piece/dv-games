package com.dv.gtusach.client;

import com.dv.gtusach.client.ui.GTusachView;
import com.dv.gtusach.client.ui.GTusachViewImpl;
import com.dv.gtusach.client.ui.LogonView;
import com.dv.gtusach.client.ui.LogonViewImpl;
import com.dv.gtusach.shared.User;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;

public class ClientFactoryImpl implements ClientFactory {
	private static final EventBus eventBus = new SimpleEventBus();
	private static final PlaceController placeController = new PlaceController(
			eventBus);
	private static final LogonView logonView = new LogonViewImpl();
	private static final GTusachView mainView = new GTusachViewImpl();
	private static BookServiceAsync bookService;
	public static final User user = new User("", "");

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

	@Override
	public BookServiceAsync getBookService() {
		synchronized (this) {
			if (bookService == null) {
				bookService = GWT.create(BookService.class);
			}
		}
		return bookService;
	}

	public User getUser() {
		return user;
	}
}
