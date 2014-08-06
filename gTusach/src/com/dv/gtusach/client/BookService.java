package com.dv.gtusach.client;

import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;


@RemoteServiceRelativePath("bookService")
public interface BookService extends RemoteService {
	long getLastUpdateTime(); 
	Book[] getBooks(String[] bookIds);
	Book getBook(String bookId);
	void createBook(Book newBook) throws BadDataException;
	byte[] downloadBook(String bookId);
	void deleteBook(String bookId) throws BadDataException;
	void resumeBook(String bookId) throws BadDataException;
	void abortBook(String bookId) throws BadDataException;
}
