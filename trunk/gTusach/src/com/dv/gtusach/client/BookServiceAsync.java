package com.dv.gtusach.client;

import com.dv.gtusach.shared.Book;
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

	void login(String userName, String password, AsyncCallback<Long> callback);
	void logout(long sessionId, AsyncCallback<Void> callback);
}
