package com.dv.gtusach.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.dv.gtusach.client.event.PropertyChangeEvent;
import com.dv.gtusach.client.event.PropertyChangeEvent.EventTypeEnum;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.User;
import com.dv.gtusach.shared.User.PermissionEnum;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class GTusachViewImpl extends Composite implements GTusachView, ClickHandler {
	static Logger log = Logger.getLogger("GTusachViewImpl");
	public static enum ScriptModeEnum {
		VIEWING,
		EDITING,
		SAVING
	}
	
	@UiField DisclosurePanel profilePanel;
	@UiField FlexTable profileTable;

	@UiField DisclosurePanel createPanel;
	@UiField FlexTable createTable;

	@UiField ScrollPanel bookListPanel;
	@UiField FlexTable bookListTable;

	@UiField DisclosurePanel scriptPanel;
	@UiField ScrollPanel scriptScrollPanel;
	@UiField TextArea scriptTextArea;
	@UiField ListBox chkScriptList;	
	@UiField TextBox textDomainName;
	@UiField Button newButton;
	@UiField Button saveEditButton;
	@UiField Button deleteCancelButton;
	
	@UiField Label messageLabel;
	@UiField CheckBox showBookDetails;
	
	Button createBookButton = new Button("Create");
	Button logInOutButton = new Button("");
	TextBox textURL = new TextBox();
	TextBox textTitle = new TextBox();
	TextBox textNumPages = new TextBox();
	TextBox textAuthor = new TextBox();
	TextBox textUserName = new TextBox();
	PasswordTextBox textPassword = new PasswordTextBox();	
	
	private Map<Integer, Book> bookTableMap = new HashMap<Integer, Book>();
	private ScriptModeEnum scriptMode = ScriptModeEnum.VIEWING;
	private ParserScript currentScript;
	private List<ParserScript> scripts = new ArrayList<ParserScript>();

	private Presenter listener;
	//private User user;
	private BookComparator comparator = new BookComparator();	
	private Timer resizeTimer = new Timer() {
		@Override
		public void run() {
			updateBookListPanelSize();
		}
	};

	private static GTusachViewImplUiBinder uiBinder = GWT
			.create(GTusachViewImplUiBinder.class);

	interface GTusachViewImplUiBinder extends UiBinder<Widget, GTusachViewImpl> {
	}

	public GTusachViewImpl() {
		initWidget(uiBinder.createAndBindUi(this));
		
		initProfilePanel();
		//profilePanel.setOpen(true);
		
		initCreatePanel();

		initScriptPanel();
		
		showBookDetails.setValue(false);
		showBookDetails.addClickHandler(this);
		
		initBookListPanel();
		
		/*
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			if (logInOutButton.getText().toLowerCase().equals("sign in")) {
				logInOutButton.click();
			}
		}
		*/
		
		chkScriptList.addClickHandler(this);
		newButton.addClickHandler(this);
		saveEditButton.addClickHandler(this);
		deleteCancelButton.addClickHandler(this);
		createBookButton.addClickHandler(this);		
		logInOutButton.addClickHandler(this);
		profilePanel.addOpenHandler(new OpenHandler<DisclosurePanel> (){
			@Override
			public void onOpen(OpenEvent<DisclosurePanel> event) {
				createPanel.setOpen(false);
				updateBookListPanelSize();
			}});
		profilePanel.addCloseHandler(new CloseHandler<DisclosurePanel> (){
			@Override
			public void onClose(CloseEvent<DisclosurePanel> event) {
				updateBookListPanelSize();
			}});
		createPanel.addOpenHandler(new OpenHandler<DisclosurePanel> (){
			@Override
			public void onOpen(OpenEvent<DisclosurePanel> event) {
				profilePanel.setOpen(false);
				updateBookListPanelSize();
			}});
		createPanel.addCloseHandler(new CloseHandler<DisclosurePanel> (){
			@Override
			public void onClose(CloseEvent<DisclosurePanel> event) {
				updateBookListPanelSize();
			}});
		
		Window.addResizeHandler(new ResizeHandler() {
			@Override
			public void onResize(ResizeEvent event) {
				resizeTimer.cancel();
				resizeTimer.schedule(250);
			}			
		});
	}
	
	private User getUser() {
		if (listener != null) {
			return listener.getUser();
		} else {
			return new User();
		}
	}
	private void initProfilePanel() {				
		profileTable.removeAllRows();
		User user = getUser();
		if (user != null && user.getSessionId() > 0) {
			// user has logged on, display profile
			//profilePanel.getHeader().setTitle("Profile");
			profileTable.setText(0, 0, "Name:");
			profileTable.setText(0, 1, user.getName());
			profileTable.setText(1, 0, "Role:");
			profileTable.setText(1, 1, user.getRole());
			profileTable.setWidget(2, 0, logInOutButton);
			profileTable.getFlexCellFormatter().setColSpan(2, 0, 2);
			logInOutButton.setText("Sign out");
		} else {
			// not loggged on, display login dialog
			//profilePanel.getHeader().setTitle("Sign In");
			profileTable.setText(0, 0, "Name:");
			profileTable.setWidget(0, 1, textUserName);
			textUserName.setWidth("150px");			
			profileTable.setText(1, 0, "Password:");
			profileTable.setWidget(1, 1, textPassword);
			textPassword.setWidth("150px");
			profileTable.setWidget(2, 0, logInOutButton);
			profileTable.getFlexCellFormatter().setColSpan(2, 0, 2);
			logInOutButton.setText("Sign in");
		}		
	}
	
	private void initCreatePanel() {
		createTable.setText(0, 0, "URL");
		createTable.setWidget(0, 1, textURL);
		textURL.setWidth("90%");
		createTable.setText(1, 0, "Title");
		createTable.setWidget(1, 1, textTitle);
		textTitle.setWidth("90%");
		createTable.setText(2, 0, "Author");
		createTable.setWidget(2, 1, textAuthor);
		textAuthor.setWidth("90%");
		createTable.setText(3, 0, "Num Pages");
		createTable.setWidget(3, 1, textNumPages);
		textNumPages.setWidth("90px");
		textNumPages.setText("0");
		createTable.setWidget(4, 0, createBookButton);
		createTable.getFlexCellFormatter().setColSpan(4, 0, 2);
		User user = getUser();
		if (user == null || user.getSessionId() <= 0) {
			createPanel.setVisible(false);
		} else {
			createPanel.setVisible(false);			
		}		
	}

	private void initScriptPanel() {
		User user = getUser();
		boolean isAdmin = (user != null && user.getSessionId() > 0 && user.getRole().contains("admin"));
		scriptPanel.setVisible(isAdmin);
		if (isAdmin) {
			chkScriptList.setEnabled(scriptMode == ScriptModeEnum.VIEWING);
			newButton.setEnabled(scriptMode == ScriptModeEnum.VIEWING);
			saveEditButton.setEnabled(scriptMode != ScriptModeEnum.SAVING);
			deleteCancelButton.setEnabled(scriptMode != ScriptModeEnum.SAVING);				
			textDomainName.setEnabled(scriptMode == ScriptModeEnum.EDITING);
			scriptTextArea.setEnabled(scriptMode == ScriptModeEnum.EDITING);
			scriptTextArea.getElement().setAttribute("wrap", "off");
			
			if (scriptMode != ScriptModeEnum.SAVING) {				
				if (scriptMode == ScriptModeEnum.VIEWING) {			
					scriptTextArea.setText("");
					textDomainName.setText("");				
					chkScriptList.setSelectedIndex(-1);
					saveEditButton.setText("Edit");
					deleteCancelButton.setText("Delete");
				} else if (scriptMode == ScriptModeEnum.EDITING) {
					saveEditButton.setText("Save");
					deleteCancelButton.setText("Cancel");				
				}
			}
		}
	}
	
	private void initBookListPanel() {
		bookListTable.setHeight("30px");
		bookListTable.setText(0, 0, "");
		bookListTable.getColumnFormatter().setWidth(0, "75px");
		bookListTable.setText(0, 1, "Title");
		bookListTable.getColumnFormatter().setWidth(1, "100px");
		bookListTable.setText(0, 2, "Status");
		bookListTable.getColumnFormatter().setWidth(2, "50px");
		bookListTable.setText(0, 3, "#Pages");		
		bookListTable.getColumnFormatter().setWidth(3, "50px");
		
		bookListTable.setText(0, 4, "Date");
		bookListTable.setText(0, 5, "Error Message");
		bookListTable.setText(0, 6, "Current Page URL");			

		//for (int i=4; i<=6; i++) {
			//bookListTable.getCellFormatter().setVisible(0, i, showBookDetails.getValue());
		//}
		
		// Add styles to elements in the stock list table.
		bookListTable.setCellPadding(5);

		// add style (see StockWatcher.css) to elements in stock list table
		bookListTable.getRowFormatter().addStyleName(0, "bookListHeader");
		bookListTable.addStyleName("bookListTable");
		bookListTable.getCellFormatter().addStyleName(0, 2, "bookListNumericColumn");				
		
		updateBookListPanelSize();		
	}
	
	private void updateBookListPanelSize() {
		int heightPx = Math.max(200, Window.getClientHeight() - bookListPanel.getAbsoluteTop() - 50);
		bookListPanel.setHeight(heightPx + "px");		
	}
	
	private void updateBook(int row, Book book) {
		HorizontalPanel panel = new HorizontalPanel();
		panel.setSpacing(10);
		bookListTable.setWidget(row, 0, panel);

		boolean isWorking = (book.getStatus() == BookStatus.WORKING);

		panel.add(createImageWidget(row, book, ActionEnum.Download, 
				listener.hasPermission(PermissionEnum.Download) && book.isEpubCreated() && !isWorking));
		panel.add(createImageWidget(row, book, ActionEnum.Resume, 
				listener.hasPermission(PermissionEnum.Update) && !isWorking));

		if (isWorking) {
			panel.add(createImageWidget(row, book, ActionEnum.Abort, listener.hasPermission(PermissionEnum.Update)));
		} else {
			panel.add(createImageWidget(row, book, ActionEnum.Delete, listener.hasPermission(PermissionEnum.Delete)));
		}

		bookListTable.setText(row, 1, book.getTitle());
		bookListTable.setText(row, 2, book.getStatusStr());
		bookListTable.setText(row, 3, book.getPages());
		bookListTable.setText(row, 4, book.getLastUpdatedTime().toString());
		bookListTable.setText(row, 5, book.getErrorMsg());
		bookListTable.setText(row, 6, book.getCurrentPageUrl());		
	}

	private Widget createImageWidget(int row, Book book, ActionEnum action,
			boolean enabled) {
		Anchor anchor = new Anchor();
		try {
			anchor.getElement().getStyle().setCursor(Cursor.POINTER);
			Image image = new ActionImage(row, book.getId(), action, enabled);
			anchor.getElement().appendChild(image.getElement());
			String ref = action.name() + "@" + book.getId();
			anchor.setName(ref);
			if (action == ActionEnum.Download) {
				AnchorElement.as(anchor.getElement()).setType("application/epub+zip");
				String url = "/downloadBook/" + book.getTitle() + ".epub" + "?bookId=" + book.getId();
				anchor.setHref(url);
			} else {
				//anchor.setHref("/downloadBook?bookId=" + bookId);
				anchor.addClickHandler(this);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return anchor;
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource().equals(logInOutButton)) {
			if (logInOutButton.getText().equals("Sign in")) {
				setInfoMessage("Signing in...");
				listener.login(textUserName.getText(), textPassword.getText());
			} else {
				listener.logout();
			}
		} else if (event.getSource() instanceof Anchor) {
			Anchor anchor = (Anchor) event.getSource();
			String ref = anchor.getName();
			int index = ref.indexOf("@");
			ActionEnum action = ActionEnum.valueOf(ref.substring(0, index));
			String bookId = ref.substring(index + 1);

			switch (action) {
			case Download:
				listener.download(bookId);
				break;
			case Resume:
				listener.resume(bookId);
				break;
			case Delete:
				if (Window.confirm("Are you sure you want to delete this book?")) {
					listener.delete(bookId);
				}
				break;
			case Abort:
				listener.abort(bookId);
				break;
			}

		} else if (event.getSource().equals(createBookButton)) {
			createNewBook();
		} else if (event.getSource().equals(newButton)) {
			// enter edit mode
			currentScript = new ParserScript();
			textDomainName.setText("");
			scriptTextArea.setText("");
			setScriptMode(ScriptModeEnum.EDITING);
		} else if (event.getSource().equals(saveEditButton)) {
			if (saveEditButton.getText().equalsIgnoreCase("Save")) {
				// save script
				setScriptMode(ScriptModeEnum.SAVING);
				ParserScript updatingScript = new ParserScript();
				updatingScript.setId(currentScript.getId());
				updatingScript.setDomainName(textDomainName.getText());
				updatingScript.setScript(scriptTextArea.getText());				
				listener.saveScript(updatingScript);
			} else {
				// edit script
				if (currentScript != null) {
					setScriptMode(ScriptModeEnum.EDITING);									
				} else {
					setErrorMessage("No script selected!");
				}
			}			
		} else if (event.getSource().equals(deleteCancelButton)) {
			if (deleteCancelButton.getText().equalsIgnoreCase("Delete")) {
				if (currentScript != null) {
					setScriptMode(ScriptModeEnum.SAVING);									
					listener.deleteScript(currentScript.getId());
				} else {
					setErrorMessage("No script selected!");
				}
			} else {
				currentScript = null;
				setScriptMode(ScriptModeEnum.VIEWING);									
			}
			// delete current script			
		} else if (event.getSource().equals(chkScriptList)) {
			log.info("selected script index: " + chkScriptList.getSelectedIndex());
			if (chkScriptList.getSelectedIndex() != -1) {
				currentScript = scripts.get(chkScriptList.getSelectedIndex());
				log.info("selected script: " + currentScript);
				textDomainName.setText(currentScript.getDomainName());
				scriptTextArea.setText(currentScript.getScript());
			} else {
				currentScript = null;
			}
		} else if (event.getSource().equals(showBookDetails)) {
			List<Book> list = new ArrayList<Book>(bookTableMap.values());
			setBooks(list, false);								
		}
		
	}

	@Override
	public void setPresenter(Presenter listener) {
		this.listener = listener;		
		this.initProfilePanel();
	}

	@Override
	public void setErrorMessage(String error) {
		messageLabel.addStyleName("errorLabel");
		messageLabel.setText(error);
	}

	@Override
	public void setInfoMessage(String info) {
		messageLabel.removeStyleName("errorLabel");
		messageLabel.setText(info);
	}

	private void setBooks(List<Book> bookList, boolean reload) {
		if (reload) {
			bookTableMap.clear();
			for (int i = 1; i < bookListTable.getRowCount(); i++) {
				bookListTable.removeRow(i);
			}
			Collections.sort(bookList, comparator);
			for (int i = 0; i < bookList.size(); i++) {
				int row = i + 1;
				updateBook(row, bookList.get(i));
				bookTableMap.put(row, bookList.get(i));
			}
		} else {
			for (Book book : bookList) {
				// find the row number of this book
				int row = -1;
				Iterator<Entry<Integer, Book>> iter = bookTableMap.entrySet()
						.iterator();
				while (iter.hasNext()) {
					Entry<Integer, Book> entry = iter.next();
					if (entry.getValue().getId().equals(book.getId())) {
						row = entry.getKey();
						break;
					}
				}
				if (row != -1) {
					updateBook(row, book);
					bookTableMap.put(row, book);
				}
			}
		}
		
		for (int i=0; i<bookListTable.getRowCount(); i++) {
			bookListTable.getCellFormatter().setVisible(i, 4, showBookDetails.getValue());
			bookListTable.getCellFormatter().setVisible(i, 5, showBookDetails.getValue());
			bookListTable.getCellFormatter().setVisible(i, 6, showBookDetails.getValue());
		}				
	}
	
	@Override
	public void onPropertyChanged(PropertyChangeEvent event) {
		if (event.getEventType() == EventTypeEnum.Authentication) {			
			if (event.getErrorMessage() != null) {
				setErrorMessage(event.getErrorMessage());				
			} else {
				// logon/logout successfull, reinitialise the profile/create panel
				
				User user = getUser();
				setInfoMessage("");
				
				initProfilePanel();			
				profilePanel.setOpen(false);
				
				// initialise create panel
				createBookButton.setEnabled(listener.hasPermission(PermissionEnum.Create));
				if (user == null || user.getSessionId() <= 0) {
					createPanel.setVisible(false);
				} else {
					createPanel.setVisible(true);			
				}			
				// initialise script panel
				initScriptPanel();
				
				// force refresh to update icon's states
				List<Book> list = new ArrayList<Book>(bookTableMap.values());
				setBooks(list, false);					
			}
		} else if (event.getEventType() == EventTypeEnum.Script) {
			if (event.getErrorMessage() != null) {
				setErrorMessage(event.getErrorMessage());
				if (event.getName().equals("update")) {
					setScriptMode(ScriptModeEnum.EDITING);
				} else {
					setScriptMode(ScriptModeEnum.VIEWING);
				}
			} else {
				// refresh scripts
				scripts.clear();
				scripts.addAll(listener.getParserScripts());
				chkScriptList.clear();
				for (ParserScript script: scripts) {
					chkScriptList.addItem(script.getDomainName());
				}				
				if (event.getName().equals("update")) {
					currentScript = null;
					setScriptMode(ScriptModeEnum.VIEWING);
				} else if (event.getName().equals("deleted")) {
					currentScript = null;
					setScriptMode(ScriptModeEnum.VIEWING);
				}				
			}
			
		} else if (event.getEventType() == EventTypeEnum.Book) {
			if (event.getName().equals("load")) {
				// refresh books
				setBooks((List<Book>)event.getValue(), true);
			} else if (event.getName().equals("update")) {
				// update books
				setBooks((List<Book>)event.getValue(), false);
			}
		}						
	}
	
	private void setScriptMode(ScriptModeEnum mode) {
		this.scriptMode = mode;
		initScriptPanel();		
	}
	
	private void createNewBook() {
		setInfoMessage("");
		String url = textURL.getText();
		String title = textTitle.getText();
		String numPageStr = textNumPages.getText();
		String author = textAuthor.getText();
		if (url == null || url.trim().length() == 0) {
			setErrorMessage("The URL cannot be empty!");
			return;
		}
		if (title == null || title.trim().length() == 0) {
			setErrorMessage("The Title cannot be empty!");
			return;
		}
		int numPages = 0;
		if (numPageStr.trim().length() > 0) {
			try {
				numPages = Integer.parseInt(numPageStr.trim());
			} catch (NumberFormatException ex) {
				setErrorMessage("Num Pages must be an integer!");
				return;
			}
		}
		Book newBook = new Book();
		newBook.setAuthor(author);
		newBook.setTitle(title);
		newBook.setMaxNumPages(numPages);
		newBook.setStartPageUrl(url);
		
		try {
			listener.create(newBook);
			textURL.setText("");
			textTitle.setText("");
			textAuthor.setText("");
			textNumPages.setText("0");
		} catch (BadDataException ex) {
			setErrorMessage(ex.getMessage());
		}
	}
	
	class ActionImage extends Image {
		String bookId;
		ActionEnum action;
		int row;

		public ActionImage(int row, String bookId, ActionEnum action,
				boolean enabled) {
			this.row = row;
			this.bookId = bookId;
			this.action = action;
			String tooltip = "";
			String url = "images/";
			if (action == ActionEnum.Download) {
				url += "download";
				tooltip = "Download Book";
			} else if (action == ActionEnum.Delete) {
				url += "delete";
				tooltip = "Delete Book";
			} else if (action == ActionEnum.Resume) {
				url += "resume";
				tooltip = "Resume Book";
			} else {
				tooltip = "Abort Book";
				url += "abort";
			}
			if (!enabled) {
				url += "-disabled";
			}
			url += ".png";
			super.setUrl(url);
			super.setTitle(tooltip);
		}
	}
	
	class BookComparator implements Comparator<Book> {
		@Override
		public int compare(Book o1, Book o2) {
			if (o1.getStatus() == BookStatus.WORKING
					&& o2.getStatus() != BookStatus.WORKING) {
				return -1;
			}
			if (o2.getStatus() == BookStatus.WORKING
					&& o1.getStatus() != BookStatus.WORKING) {
				return 1;
			}
			if (o1.getLastUpdatedTime() != null && o2.getLastUpdatedTime() != null) {
				return o2.getLastUpdatedTime().compareTo(o1.getLastUpdatedTime());
			}
			if (o1.getLastUpdatedTime() != null) {
				return -1;
			}
			if (o2.getLastUpdatedTime() != null) {
				return 1;
			}
			return 0;
		}
	}
	
}
