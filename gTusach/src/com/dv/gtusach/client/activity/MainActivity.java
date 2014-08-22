package com.dv.gtusach.client.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.dv.gtusach.client.ClientFactory;
import com.dv.gtusach.client.event.PropertyChangeEvent;
import com.dv.gtusach.client.event.PropertyChangeEvent.EventTypeEnum;
import com.dv.gtusach.client.event.PropertyChangeEventHandler;
import com.dv.gtusach.client.place.MainPlace;
import com.dv.gtusach.client.ui.GTusachView;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.User;
import com.dv.gtusach.shared.User.PermissionEnum;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class MainActivity extends AbstractActivity implements
		GTusachView.Presenter {

	private final long SESSION_EXPIRE_TIME = 1000*60*60*24*14;
	// Used to obtain views, eventBus, placeController
	// Alternatively, could be injected via GIN
	private ClientFactory clientFactory;
	private GTusachView tusachView;
	private List<Book> currentBooks = new ArrayList<Book>();
	private List<ParserScript> currentScripts = new ArrayList<ParserScript>();
	private long libraryUpdateTime = -1;
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
		tusachView.setInfoMessage("Loading...");
		
		containerWidget.setWidget(tusachView.asWidget());
		
		String sid = Cookies.getCookie("sid");
		if (sid != null) {
			try {
				long sessionId = Long.parseLong(sid);
				// retrieve the user info from session id
				AsyncCallback<User> callback = new AsyncCallback<User>() {
					@Override
					public void onFailure(Throwable caught) {
					}

					@Override
					public void onSuccess(User result) {
						if (result != null) {
							// session has expired
							clientFactory.getUser().update(result);
							fireEvent(EventTypeEnum.Authentication, "login", clientFactory.getUser());  
						}
					}
				};
				clientFactory.getBookService().getLogonUser(sessionId, callback);			
			} catch (Exception ex) {
				// ignore
			}			
		}
		
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
		
		clientFactory.getEventBus().addHandler(PropertyChangeEvent.TYPE, new PropertyChangeEventHandler() {
			@Override
			public void onPropertyChanged(PropertyChangeEvent event) {
				// pass on event, except for login error
				tusachView.onPropertyChanged(event);
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

	public User getUser() {
		return clientFactory.getUser();
	}
	
	public List<Book> getBooks() {
		return currentBooks;
	}
	
	public List<ParserScript> getParserScripts() {
		return currentScripts;
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
				validateSession();				
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
				validateSession();				
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
		validateSession();				
		Window.open("/downloadBook?bookId=" + bookId, "", "");
	}
	
	@Override
	public void abort(String bookId) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				String errorMsg = caught.getMessage();
				tusachView.setErrorMessage(errorMsg);
				validateSession();				
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
				validateSession();				
			}

			@Override
			public void onSuccess(Void result) {
				refresh();
			}
		};
		clientFactory.getBookService().deleteBook(clientFactory.getUser().getSessionId(), bookId, callback);
	}
	
	private void validateSession() {
		if (clientFactory.getUser().getSessionId() > 0) {
			AsyncCallback<User> callback = new AsyncCallback<User>() {
				@Override
				public void onFailure(Throwable caught) {
				}

				@Override
				public void onSuccess(User result) {
					if (result == null) {
						// session has expired
						clientFactory.getUser().setSessionId(-1);
						tusachView.setErrorMessage("Session has expired!");
						fireEvent(EventTypeEnum.Authentication, "logout", clientFactory.getUser());  
					}
				}
			};
			clientFactory.getBookService().getLogonUser(clientFactory.getUser().getSessionId(), callback);
		}
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
				
				List<Book> list = (reload ? currentBooks : Arrays.asList(result));
				
				String header = (reload ? "Loaded " : "Updated ") + list.size() + " books. ";
				if (libraryUpdateTime > 0) {
					header += new Date(libraryUpdateTime);
				}
				tusachView.setInfoMessage(header);
				fireEvent(EventTypeEnum.Book, (reload ? "load" : "update"), list);				
			}
		};
		if (bookIds == null || bookIds.length == 0) {
			tusachView.setInfoMessage("Loading book list...");
		} else {
			tusachView.setInfoMessage("Updating working books...");
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
				if (libraryUpdateTime == -1 || t.longValue() != libraryUpdateTime) {
					libraryUpdateTime = t.longValue();
					loadBooks(new String[0]);
				} else if (workingBookIds.size() > 0) {
					loadBooks(workingBookIds.toArray(new String[0]));
				}
			}
		};
		clientFactory.getBookService().getLastUpdateTime(callback);
	}
		
	@Override
	public boolean hasPermission(PermissionEnum permission) {
		if (permission == PermissionEnum.Download) {
			return true;
		} else if (permission == PermissionEnum.Create) {
			return (clientFactory.getUser().getSessionId() > 0);
		} else if (permission == PermissionEnum.Update) {
			// abort or resume
			return (clientFactory.getUser().getSessionId() > 0);
		} else if (permission == PermissionEnum.Delete) {
			return (clientFactory.getUser().getSessionId() > 0);
		} else if (permission == PermissionEnum.Javascript) {
			boolean isAdmin = (clientFactory.getUser().getRole().toLowerCase().indexOf("admin") != -1); 
			return (clientFactory.getUser().getSessionId() > 0 && isAdmin);
		}
		return false;
	}

	@Override
	public void login(final String userName, final String password) {
		AsyncCallback<User> callback = new AsyncCallback<User>() {
			public void onFailure(Throwable caught) {
				fireEvent(EventTypeEnum.Authentication, "login", clientFactory.getUser(), "Error connecting to server!");
			}
			public void onSuccess(User user) {
				if (user != null) {
					clientFactory.getUser().update(user);
					// store in cookie
			    Cookies.setCookie("sid", ""+user.getSessionId());					
					boolean isAdmin = (user != null && user.getSessionId() > 0 
							&& user.getRole().contains("admin"));
					if (isAdmin) {
						loadParserScripts();
					}					
					fireEvent(EventTypeEnum.Authentication, "login", clientFactory.getUser());
				} else {
					fireEvent(EventTypeEnum.Authentication, "login", clientFactory.getUser(), "Invalid user name or password!");					
				}
			}
		};		
		clientFactory.getBookService().login(userName, password, callback);
	}
	
	@Override
	public void logout() {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				fireEvent(EventTypeEnum.Authentication, "logout", clientFactory.getUser(), "Error connecting to server!");				
			}
			public void onSuccess(Void v) {
				clientFactory.getUser().setName("");
				clientFactory.getUser().setSessionId(-1);
				fireEvent(EventTypeEnum.Authentication, "logout", clientFactory.getUser());				
			}
		};		
		clientFactory.getBookService().logout(clientFactory.getUser().getSessionId(), callback);
	}
	
	private void loadParserScripts() {
		AsyncCallback<ParserScript[]> callback = new AsyncCallback<ParserScript[]>() {
			public void onFailure(Throwable e) {
				tusachView.setErrorMessage("Error loading parser scripts. " + e.getMessage());
			}
			
			public void onSuccess(ParserScript[] scripts) {
				tusachView.setInfoMessage("Loaded " + scripts.length + " parser scripts");
				currentScripts.clear();
				currentScripts.addAll(Arrays.asList(scripts));
				fireEvent(EventTypeEnum.Script, "load", currentScripts);				
			}
		};		
		clientFactory.getBookService().getParserScripts(clientFactory.getUser().getSessionId(), callback);
	}

	@Override
	public void saveScript(final ParserScript script) {	
		final String oldId = script.getId();
		AsyncCallback<ParserScript> callback = new AsyncCallback<ParserScript>() {
			public void onFailure(Throwable caught) {
				fireEvent(EventTypeEnum.Script, "update", script, caught.getMessage());								
			}
			
			public void onSuccess(ParserScript savedScript) {
				tusachView.setInfoMessage("Parser script saved.");
				int index = -1;
				for (int i=0; i<currentScripts.size(); i++) {
					if (oldId != null && currentScripts.get(i).getId().equals(oldId)) {
						index = i;
						break;
					}
				}
				if (index != -1) {
					currentScripts.set(index, savedScript);
				} else {
					currentScripts.add(savedScript);
				}	
				fireEvent(EventTypeEnum.Script, "update", savedScript);
			}
		};		
		clientFactory.getBookService().saveParserScript(clientFactory.getUser().getSessionId(), script, callback);
	}

	@Override
	public void deleteScript(final String scriptId) {	
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				fireEvent(EventTypeEnum.Script, "delete", scriptId, caught.getMessage());								
			}
			
			public void onSuccess(Void v) {
				tusachView.setInfoMessage("Parser script deleted.");
				int index = -1;
				for (int i=0; i<currentScripts.size(); i++) {
					if (currentScripts.get(i).getId().equals(scriptId)) {
						index = i;
						break;
					}
				}
				if (index != -1) {
					ParserScript deleteScript = currentScripts.remove(index);
					fireEvent(EventTypeEnum.Script, "delete", deleteScript);
				} 
			}
		};		
		clientFactory.getBookService().deleteParserScript(clientFactory.getUser().getSessionId(), scriptId, callback);
	}
	
	private void fireEvent(EventTypeEnum type, String name, Object value) {
		clientFactory.getEventBus().fireEvent(new PropertyChangeEvent(type, name, value, null));
	}
	
	private void fireEvent(EventTypeEnum type, String name, Object value, String error) {
		clientFactory.getEventBus().fireEvent(new PropertyChangeEvent(type, name, value, error));
	}
}
