package com.dv.gtusach.client.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.dv.gtusach.client.event.AuthenticationEvent;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class GTusachViewImpl extends Composite implements GTusachView,
		ClickHandler {
	
	@UiField
	FlexTable bookListTable;
	@UiField
	FlexTable bookCreateTable;
	@UiField
	Button createButton;
	@UiField
	Label errorLabel;
	@UiField
	Label bookListHeaderLabel;

	TextBox textURL = new TextBox();
	TextBox textTitle = new TextBox();
	TextBox textNumPages = new TextBox();
	TextBox textAuthor = new TextBox();
	Map<Integer, Book> bookTableMap = new HashMap<Integer, Book>();

	private Presenter listener;
	private BookComparator comparator = new BookComparator();

	private static GTusachViewImplUiBinder uiBinder = GWT
			.create(GTusachViewImplUiBinder.class);

	interface GTusachViewImplUiBinder extends UiBinder<Widget, GTusachViewImpl> {
	}

	public GTusachViewImpl() {
		initWidget(uiBinder.createAndBindUi(this));

		bookListTable.setHeight("30px");
		bookListTable.setText(0, 0, "");
		bookListTable.getColumnFormatter().setWidth(0, "50px");
		bookListTable.setText(0, 1, "Title");
		bookListTable.setText(0, 2, "Status");
		bookListTable.getColumnFormatter().setWidth(2, "50px");
		bookListTable.setText(0, 3, "#Pages");
		bookListTable.getColumnFormatter().setWidth(3, "50px");
		bookListTable.setText(0, 4, "Date");
		bookListTable.setText(0, 5, "Error Message");

		// Add styles to elements in the stock list table.
		bookListTable.setCellPadding(5);

		// add style (see StockWatcher.css) to elements in stock list table
		bookListTable.getRowFormatter().addStyleName(0, "bookListHeader");
		bookListTable.addStyleName("bookList");
		bookListTable.getCellFormatter()
				.addStyleName(0, 2, "bookListNumericColumn");

		bookCreateTable.setText(0, 0, "URL");
		bookCreateTable.setWidget(0, 1, textURL);
		textURL.setWidth("80%");
		bookCreateTable.setText(1, 0, "Title");
		bookCreateTable.setWidget(1, 1, textTitle);
		textTitle.setWidth("80%");
		bookCreateTable.setText(2, 0, "Author");
		bookCreateTable.setWidget(2, 1, textAuthor);
		textAuthor.setWidth("80%");
		bookCreateTable.setText(3, 0, "Num Pages");
		bookCreateTable.setWidget(3, 1, textNumPages);
		textNumPages.setWidth("50px");
		textNumPages.setText("0");
		
		createButton.addClickHandler(this);		
	}

	private void updateBook(int row, Book book) {
		HorizontalPanel panel = new HorizontalPanel();
		panel.setSpacing(5);
		bookListTable.setWidget(row, 0, panel);

		boolean isWorking = (book.getStatus() == BookStatus.WORKING);

		panel.add(createImageWidget(row, book.getId(), ActionEnum.Download, listener.canDownload(book)));
		panel.add(createImageWidget(row, book.getId(), ActionEnum.Resume, listener.canResume(book)));

		if (isWorking) {
			panel.add(createImageWidget(row, book.getId(), ActionEnum.Abort, listener.canAbort(book)));
		} else {
			panel.add(createImageWidget(row, book.getId(), ActionEnum.Delete, listener.canDelete(book)));
		}

		bookListTable.setText(row, 1, book.getTitle());
		bookListTable.setText(row, 2, book.getStatusStr());
		bookListTable.setText(row, 3, book.getPages());
		bookListTable.setText(row, 4, book.getLastUpdatedTime().toString());
		bookListTable.setText(row, 5, book.getErrorMsg());
	}

	private Widget createImageWidget(int row, String bookId, ActionEnum action,
			boolean enabled) {
		Anchor anchor = new Anchor();
		try {
			anchor.getElement().getStyle().setCursor(Cursor.POINTER);
			Image image = new ActionImage(row, bookId, action, enabled);
			anchor.getElement().appendChild(image.getElement());
			String ref = action.name() + "@" + bookId;
			anchor.setName(ref);
			anchor.addClickHandler(this);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return anchor;
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() instanceof Anchor) {
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
				listener.delete(bookId);
				break;
			case Abort:
				listener.abort(bookId);
				break;
			}

		} else if (event.getSource().equals(createButton)) {
			errorLabel.setText("");
			String url = textURL.getText();
			String title = textTitle.getText();
			String numPageStr = textNumPages.getText();
			String author = textAuthor.getText();
			if (url == null || url.trim().length() == 0) {
				errorLabel.setText("The URL cannot be empty!");
				return;
			}
			if (title == null || title.trim().length() == 0) {
				errorLabel.setText("The Title cannot be empty!");
				return;
			}
			int numPages = 0;
			if (numPageStr.trim().length() > 0) {
				try {
					numPages = Integer.parseInt(numPageStr.trim());
				} catch (NumberFormatException ex) {
					errorLabel.setText("Num Pages must be an integer!");
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
				errorLabel.setText(ex.getMessage());
			}

		}
	}

	@Override
	public void setPresenter(Presenter listener) {
		this.listener = listener;
		createButton.setEnabled(listener.canCreate());		
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

	@Override
	public void setErrorMessage(String error) {
		this.errorLabel.setText(error);
	}

	@Override
	public void setHeaderMessage(String header) {
		this.bookListHeaderLabel.setText(header);
	}

	@Override
	public void setBooks(Book[] books, boolean reload) {
		if (reload) {
			bookTableMap.clear();
			for (int i = 1; i < bookListTable.getRowCount(); i++) {
				bookListTable.removeRow(i);
			}
			List<Book> bookList = new ArrayList<Book>(Arrays.asList(books));
			Collections.sort(bookList, comparator);
			for (int i = 0; i < bookList.size(); i++) {
				int row = i + 1;
				updateBook(row, bookList.get(i));
				bookTableMap.put(row, bookList.get(i));
			}
		} else {
			for (Book book : books) {
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
	}

	@Override
	public void onAuthenticationChanged(AuthenticationEvent event) {
		createButton.setEnabled(listener.canCreate());
		// force refresh to update icon's states
		List<Book> list = new ArrayList<Book>(bookTableMap.values());
		setBooks(list.toArray(new Book[0]), false);
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
