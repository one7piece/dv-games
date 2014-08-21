package com.dv.gtusach.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class DisclosureViewImp extends Composite implements HasText {

	private static DisclosureViewImpUiBinder uiBinder = GWT
			.create(DisclosureViewImpUiBinder.class);

	interface DisclosureViewImpUiBinder extends
			UiBinder<Widget, DisclosureViewImp> {
	}

	public DisclosureViewImp() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	public DisclosureViewImp(String firstName) {
		initWidget(uiBinder.createAndBindUi(this));
		//button.setText(firstName);
	}

	public void setText(String text) {
		//button.setText(text);
	}

	public String getText() {
		//return button.getText();
		return null;
	}
}
