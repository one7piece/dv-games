package com.dv.gtusach.client.ui;

import com.dv.gtusach.client.place.MainPlace;
import com.dv.gtusach.shared.BadDataException;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;

public class LogonViewImpl extends Composite implements LogonView {
	private static LogonViewImplUiBinder uiBinder = GWT
			.create(LogonViewImplUiBinder.class);

	interface LogonViewImplUiBinder extends UiBinder<Widget, LogonViewImpl> {
	}

	@UiField TextBox userName;
	@UiField PasswordTextBox password;	
	@UiField Button loginButton;
	@UiField Button cancelButton;
	@UiField Label headerLabel;
	private Presenter listener;

	public LogonViewImpl() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	public void setPresenter(Presenter listener) {
		this.listener = listener;
	}

	@Override
	public void setUserName(String userName) {
		this.userName.setText(userName);
	}

	@Override
	public void setPassword(String password) {
		this.password.setText(password);
	}
		
	@UiHandler("loginButton")
	void loginButtonClicked(ClickEvent event) {
		headerLabel.setStyleName("gwt-Label");
		headerLabel.setText("Signing in...");
		loginButton.setEnabled(false);
		cancelButton.setEnabled(false);
		listener.logon(userName.getText(), password.getText());
	}
	
	@UiHandler("cancelButton")
	void cancelButtonClicked(ClickEvent event) {
		listener.goTo(new MainPlace("main"));
	}

	@Override
	public void setErrorMessage(String error) {
		headerLabel.setStyleName("gwt-Label-Error");
		headerLabel.setText(error);
		loginButton.setEnabled(true);
		cancelButton.setEnabled(true);
	}
}
