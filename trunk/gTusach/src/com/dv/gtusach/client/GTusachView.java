package com.dv.gtusach.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class GTusachView extends Composite implements ClickHandler {
	@UiField FlexTable bookListTable;
	@UiField FlexTable bookCreateTable;
	@UiField Button createButton;
	@UiField Label errorLabel;
	@UiField Label bookListHeaderLabel;
		
	TextBox textURL = new TextBox();
	TextBox textTitle = new TextBox();
	TextBox textNumPages = new TextBox();
	TextBox textAuthor = new TextBox();
	BookComparator comparator = new BookComparator();
	long libraryUpdateTime = 0;
	Map<Integer, Book> bookTableMap = new HashMap<Integer, Book>();
	
	private static GTusachViewUiBinder uiBinder = GWT
			.create(GTusachViewUiBinder.class);

	interface GTusachViewUiBinder extends UiBinder<Widget, GTusachView> {
	}
	
	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private BookServiceAsync bookService; // = GWT.create(BookService.class);
	
	public GTusachView() {
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
		bookListTable.getCellFormatter().addStyleName(0, 2,
				"bookListNumericColumn");
		
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
		
		loadBooks(new String[0]);
		
		Timer refreshTimer = new Timer() {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshTimer.scheduleRepeating(20000);		
	}

	public GTusachView(String firstName) {
		initWidget(uiBinder.createAndBindUi(this));
	}

	private BookServiceAsync getBookService() {
		synchronized (this) {
			if (bookService == null) {
				bookService = GWT.create(BookService.class);
	    }		
		}
		return bookService;
	}
	
	private void refresh() {
		final List<String> workingBookIds = new ArrayList<String>();
		for (int i=1; i<bookListTable.getRowCount(); i++) {
			Book book = bookTableMap.get(i);
			if (book != null && book.getStatus() == BookStatus.WORKING) {
				workingBookIds.add(book.getId());
			}
		}
		
		// setup callback
		AsyncCallback<Long> callback = new AsyncCallback<Long>() {
			public void onFailure(Throwable caught) {
			}
			public void onSuccess(Long t) {
				if (libraryUpdateTime == 0 || t.longValue() != libraryUpdateTime) {
					libraryUpdateTime = t;
					loadBooks(new String[0]);
				} else if (workingBookIds.size() > 0) {
					loadBooks(workingBookIds.toArray(new String[0]));
				}
			}
		};
		getBookService().getLastUpdateTime(callback);		
	}
	
	private void loadBooks(final String[] bookIds) {		
		// setup callback
		AsyncCallback<Book[]> callback = new AsyncCallback<Book[]>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				bookListHeaderLabel.setText("Error loading books: " + details);
			}

			public void onSuccess(Book[] result) {
				updateBookList(result, (bookIds == null || bookIds.length == 0));
			}
		};
		if (bookIds == null || bookIds.length == 0) {
			bookListHeaderLabel.setText("Loading book list...");
		} else {
			bookListHeaderLabel.setText("Updating working books...");
		}
		getBookService().getBooks(bookIds, callback);		
	}
	
	private void updateBook(int row, Book book) {
		HorizontalPanel panel = new HorizontalPanel();
		panel.setSpacing(5);		
		bookListTable.setWidget(row, 0, panel);	
		
		boolean isWorking = (book.getStatus() == BookStatus.WORKING);			
		
		panel.add(createImageWidget(row, book.getId(), ActionEnum.Download, !isWorking));
		panel.add(createImageWidget(row, book.getId(), ActionEnum.Resume, !isWorking));
		
		if (isWorking) {
			panel.add(createImageWidget(row, book.getId(), ActionEnum.Abort, true));			
		} else {
			panel.add(createImageWidget(row, book.getId(), ActionEnum.Delete, true));
		}
		
		bookListTable.setText(row, 1, book.getTitle());			
		bookListTable.setText(row, 2, book.getStatusStr());			
		bookListTable.setText(row, 3, book.getPages());			
		bookListTable.setText(row, 4, book.getLastUpdatedTime().toString());			
		bookListTable.setText(row, 5, book.getErrorMsg());		
	}
	
	private Widget createImageWidget(int row, String bookId, ActionEnum action, boolean enabled) {
		Anchor anchor = new Anchor();
		anchor.getElement().getStyle().setCursor(Cursor.POINTER);
		Image image = new ActionImage(row, bookId, action, enabled);
		anchor.getElement().appendChild(image.getElement());
		String ref = action.name() + "@" + bookId;
		anchor.setName(ref);
		anchor.addClickHandler(this);
		return anchor;
	}
	
	private void updateBookList(Book[] books, boolean reload) {
		String header = books.length + " books. ";
		if (libraryUpdateTime > 0) {
			header += new Date(libraryUpdateTime);
		}
		bookListHeaderLabel.setText(header);
		
		if (reload) {
			bookTableMap.clear();
			for (int i=1; i<bookListTable.getRowCount(); i++) {
				bookListTable.removeRow(i);
			}
			List<Book> bookList = new ArrayList<Book>(Arrays.asList(books));		
			Collections.sort(bookList, comparator);
			for (int i=0; i<bookList.size(); i++) {
				int row = i+1;
				updateBook(row, bookList.get(i));
				bookTableMap.put(row, bookList.get(i));
			}
		} else {
			for (Book book: books) {
				// find the row number of this book
				int row = -1;
				Iterator<Entry<Integer, Book>> iter = bookTableMap.entrySet().iterator();
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
	public void onClick(ClickEvent event) {
		if (event.getSource() instanceof Anchor) {			
			Anchor anchor = (Anchor)event.getSource();
			String ref = anchor.getName();
			int index = ref.indexOf("@");
			ActionEnum action = ActionEnum.valueOf(ref.substring(0, index));
			String bookId = ref.substring(index+1);
			
			AsyncCallback<Void> callback = new AsyncCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					String errorMsg = caught.getMessage();
					errorLabel.setText(errorMsg);
				}

				@Override
				public void onSuccess(Void result) {
					refresh();
				}
			};	
						
			switch (action) {
			case Download:
				downloadBook(bookId);
				break;
			case Resume:
				getBookService().resumeBook(bookId, callback);		
				break;						
			case Delete:
				getBookService().deleteBook(bookId, callback);		
				break;
			case Abort:
				getBookService().abortBook(bookId, callback);		
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
			createBook(newBook);
			
			textURL.setText("");
			textTitle.setText("");
			textAuthor.setText("");
			textNumPages.setText("0");
		}
	}	
	
	private void downloadBook(final String bookId) {
		AsyncCallback<byte[]> callback = new AsyncCallback<byte[]>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				errorLabel.setText(errorMsg);
			}

			@Override
			public void onSuccess(byte[] data) {
			}
		};	
		
		Window.open("/downloadBook?bookId=" + bookId, "", "");
	}
	
	private void createBook(Book newBook) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				errorLabel.setText(errorMsg);
			}

			@Override
			public void onSuccess(Void result) {
				errorLabel.setText("");
			}
		};

		getBookService().createBook(newBook, callback);		
	}
	
	static enum ActionEnum {
		Download,
		Delete,
		Resume,
		Abort
	};
	
	class ActionImage extends Image {
		String bookId;
		ActionEnum action;
		int row;
		
		public ActionImage(int row, String bookId, ActionEnum action, boolean enabled) {
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
			if (o1.getStatus() == BookStatus.WORKING && o2.getStatus() != BookStatus.WORKING) {
				return -1;
			}
			if (o2.getStatus() == BookStatus.WORKING && o1.getStatus() != BookStatus.WORKING) {
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
