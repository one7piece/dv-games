package com.dv.gtusach.client;

import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.User;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface BookServiceAsync {
	void getLastUpdateTime(AsyncCallback<Long> callback);

	void getBooks(String[] bookIds, AsyncCallback<Book[]> callback);
	void getBook(String bookId, AsyncCallback<Book> callback);

	void createBook(long sessionId, Book newBook, AsyncCallback<Void> callback);
	void deleteBook(long sessionId, String bookId, AsyncCallback<Void> callback);	
	void resumeBook(long sessionId, String bookId, AsyncCallback<Void> callback);	
	void abortBook(long sessionId, String bookId, AsyncCallback<Void> callback);
	void downloadBook(long sessionId, String bookId,
			AsyncCallback<byte[]> callback);

	void login(String userName, String password, AsyncCallback<User> callback);
	void logout(long sessionId, AsyncCallback<Void> callback);

	void getParserScripts(long sessionId, AsyncCallback<ParserScript[]> callback);

	void saveParserScript(long sessionId, ParserScript script,
			AsyncCallback<ParserScript> callback);

	void deleteParserScript(long sessionId, String scriptId,
			AsyncCallback<Void> callback);

	void getLogonUser(long sessionId, AsyncCallback<User> callback);
}
