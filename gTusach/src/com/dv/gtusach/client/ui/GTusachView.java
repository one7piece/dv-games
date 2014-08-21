package com.dv.gtusach.client.ui;

import java.util.List;

import com.dv.gtusach.client.event.PropertyChangeEvent;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.User;
import com.dv.gtusach.shared.User.PermissionEnum;
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
	
	void setPresenter(Presenter listener);
	void setErrorMessage(String error);
	void setInfoMessage(String info);
	void onPropertyChanged(PropertyChangeEvent event);
	//void setBooks(Book[] books, boolean reload);	
	//void setParserScripts(ParserScript[] scripts);
	
	public interface Presenter {
		User getUser();
		void goTo(Place place);
		void create(Book newBook) throws BadDataException;
		void download(String bookId);
		void resume(String bookId);
		void abort(String bookId);
		void delete(String bookId);
		boolean hasPermission(PermissionEnum permission);
		void login(String userName, String password);
		void logout();		
		void saveScript(ParserScript script);
		void deleteScript(String scriptId);
		List<Book> getBooks();
		List<ParserScript> getParserScripts();
	}
}