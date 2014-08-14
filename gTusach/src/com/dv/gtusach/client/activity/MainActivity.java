package com.dv.gtusach.client.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.dv.gtusach.client.BookService;
import com.dv.gtusach.client.BookServiceAsync;
import com.dv.gtusach.client.ClientFactory;
import com.dv.gtusach.client.event.AuthenticationEvent;
import com.dv.gtusach.client.event.AuthenticationEvent.AuthenticationTypeEnum;
import com.dv.gtusach.client.event.AuthenticationEventHandler;
import com.dv.gtusach.client.place.MainPlace;
import com.dv.gtusach.client.ui.GTusachView;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class MainActivity extends AbstractActivity implements
		GTusachView.Presenter {

	// Used to obtain views, eventBus, placeController
	// Alternatively, could be injected via GIN
	private ClientFactory clientFactory;
	private GTusachView tusachView;
	private List<Book> currentBooks = new ArrayList<Book>();
	private long libraryUpdateTime = 0;
	private Timer refreshTimer;

	public MainActivity(MainPlace place, ClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	/**
	 * Invoked by the ActivityManager to start a new Activity
	 */
	@Override
	public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
		tusachView = clientFactory.getMainView();		
		tusachView.setPresenter(this);
		tusachView.setErrorMessage("No error");
		tusachView.setHeaderMessage("Loading...");
		tusachView.setBooks(new Book[0], true);
		
		containerWidget.setWidget(tusachView.asWidget());
		
		loadBooks(new String[0]);

		if (refreshTimer != null && refreshTimer.isRunning()) {
			refreshTimer.cancel();
		}
		refreshTimer = new Timer() {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshTimer.scheduleRepeating(20000);
		
		clientFactory.getEventBus().addHandler(AuthenticationEvent.TYPE, new AuthenticationEventHandler() {			
			@Override
			public void onAuthenticationChanged(AuthenticationEvent event) {
				if (!(event.getType() == AuthenticationTypeEnum.LOG_OUT && !event.isSuccess())) {
					tusachView.onAuthenticationChanged(event);
				}
			}
		});
	}

	/**
	 * Ask user before stopping this activity
	 */
	@Override
	public void onStop() {
		refreshTimer.cancel();
	}

	/**
	 * Navigate to a new Place in the browser
	 */
	public void goTo(Place place) {
		clientFactory.getPlaceController().goTo(place);
	}
	
	@Override
	public void create(Book newBook) throws BadDataException {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				tusachView.setErrorMessage(errorMsg);
			}

			@Override
			public void onSuccess(Void result) {
				tusachView.setErrorMessage("");
			}
		};

		clientFactory.getBookService().createBook(clientFactory.getUser().getSessionId(), newBook, callback);
	}
	
	@Override
	public void resume(final String bookId) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				tusachView.setErrorMessage(errorMsg);
			}

			@Override
			public void onSuccess(Void result) {
				refresh();
			}
		};
		clientFactory.getBookService().resumeBook(clientFactory.getUser().getSessionId(), bookId, callback);
	}
	
	@Override
	public void download(final String bookId) {		
		Window.open("/downloadBook?bookId=" + bookId, "", "");
	}
	
	@Override
	public void abort(String bookId) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				tusachView.setErrorMessage(errorMsg);
			}

			@Override
			public void onSuccess(Void result) {
				refresh();
			}
		};
		clientFactory.getBookService().abortBook(clientFactory.getUser().getSessionId(), bookId, callback);
	}

	@Override
	public void delete(String bookId) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				tusachView.setErrorMessage(errorMsg);
			}

			@Override
			public void onSuccess(Void result) {
				refresh();
			}
		};
		clientFactory.getBookService().deleteBook(clientFactory.getUser().getSessionId(), bookId, callback);
	}
	
	private void loadBooks(final String[] bookIds) {
		// setup callback
		AsyncCallback<Book[]> callback = new AsyncCallback<Book[]>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				tusachView.setErrorMessage("Error loading books: " + details);
			}

			public void onSuccess(Book[] result) {
				boolean reload = (bookIds == null || bookIds.length == 0);
				if (reload) {
					currentBooks.clear();
					currentBooks.addAll(Arrays.asList(result));
				} else {
					for (Book updatingBook: result) {
						int index = -1;
						for (int i=0; i<currentBooks.size(); i++) {
							if (currentBooks.get(i).getId().equals(updatingBook.getId())) {
								index = i;
								break;
							}
						}
						if (index != -1) {
							currentBooks.set(index, updatingBook);
						} else {
							// shouldn't happens!!!
						}
					}
				}
				
				String header = currentBooks.size() + " books. ";
				if (libraryUpdateTime > 0) {
					header += new Date(libraryUpdateTime);
				}
				tusachView.setHeaderMessage(header);
				tusachView.setBooks(result, reload);				
			}
		};
		if (bookIds == null || bookIds.length == 0) {
			tusachView.setHeaderMessage("Loading book list...");
		} else {
			tusachView.setHeaderMessage("Updating working books...");
		}
		clientFactory.getBookService().getBooks(bookIds, callback);
	}
	
	private void refresh() {
		final List<String> workingBookIds = new ArrayList<String>();
		for (Book book : currentBooks) {
			if (book.getStatus() == BookStatus.WORKING) {
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
		clientFactory.getBookService().getLastUpdateTime(callback);
	}
		
	@Override
	public boolean canDownload(Book book) {
		boolean isWorking = (book.getStatus() == BookStatus.WORKING);
		return !isWorking;
	}
	
	@Override
	public boolean canAbort(Book book) {
		boolean isWorking = (book.getStatus() == BookStatus.WORKING);
		return (clientFactory.getUser().getSessionId() > 0 && isWorking);
	}
	
	@Override
	public boolean canResume(Book book) {
		boolean isWorking = (book.getStatus() == BookStatus.WORKING);
		return (clientFactory.getUser().getSessionId() > 0 && !isWorking);
	}
	
	@Override
	public boolean canDelete(Book book) {
		boolean isWorking = (book.getStatus() == BookStatus.WORKING);
		return (clientFactory.getUser().getSessionId() > 0 && !isWorking);
	}
	
	@Override
	public boolean canCreate() {
		return (clientFactory.getUser().getSessionId() > 0);
	}
	
}