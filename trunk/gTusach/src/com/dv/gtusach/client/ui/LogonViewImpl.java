package com.dv.gtusach.client.ui;

import com.dv.gtusach.client.place.MainPlace;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LogonViewImpl extends Composite implements LogonView {
	private static LogonViewImplUiBinder uiBinder = GWT
			.create(LogonViewImplUiBinder.class);

	interface LogonViewImplUiBinder extends UiBinder<Widget, LogonViewImpl> {
	}

	@UiField
	SpanElement nameSpan;
	
	@UiField
	Anchor mainViewLink;
	
	private Presenter listener;
	private String name;

	public LogonViewImpl() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	public void setName(String name) {
		this.name = name;
		nameSpan.setInnerText(name);
	}

	@UiHandler("mainViewLink")
	void onClickGoodbye(ClickEvent e) {
		listener.goTo(new MainPlace(name));
	}

	@Override
	public void setPresenter(Presenter listener) {
		this.listener = listener;
	}
}
