package com.dv.gtusach.client;

import com.dv.gtusach.shared.Book;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface BookServiceAsync {

	void createBook(Book newBook, AsyncCallback<Void> callback);

	void getBooks(String[] bookIds, AsyncCallback<Book[]> callback);

	void getBook(String bookId, AsyncCallback<Book> callback);

	void deleteBook(String bookId, AsyncCallback<Void> callback);
	
	void resumeBook(String bookId, AsyncCallback<Void> callback);
	
	void abortBook(String bookId, AsyncCallback<Void> callback);

	void downloadBook(String bookId, AsyncCallback<byte[]> callback);

	void getLastUpdateTime(AsyncCallback<Long> callback);

}
