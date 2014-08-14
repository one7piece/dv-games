package com.dv.gtusach.client.ui;

import com.dv.gtusach.client.event.AuthenticationEvent;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * View interface. Extends IsWidget so a view impl can easily provide its
 * container widget.
 * 
 */
public interface GTusachView extends IsWidget {
	static enum ActionEnum {
		Download, Delete, Resume, Abort
	};
	
	void setErrorMessage(String error);
	void setHeaderMessage(String header);
	void setBooks(Book[] books, boolean reload);	
	void setPresenter(Presenter listener);
	void onAuthenticationChanged(AuthenticationEvent event);
	
	public interface Presenter {
		void goTo(Place place);
		void create(Book newBook) throws BadDataException;
		void download(String bookId);
		void resume(String bookId);
		void abort(String bookId);
		void delete(String bookId);
		boolean canDownload(Book book);
		boolean canAbort(Book book);
		boolean canResume(Book book);
		boolean canDelete(Book book);
		boolean canCreate();
	}
}