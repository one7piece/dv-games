package com.dv.gtusach.client;

import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.User;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;


@RemoteServiceRelativePath("bookService")
public interface BookService extends RemoteService {
	long getLastUpdateTime(); 
	Book[] getBooks(String[] bookIds);
	Book getBook(String bookId);
	byte[] downloadBook(long sessionId, String bookId) throws BadDataException;	
	void createBook(long sessionId, Book newBook) throws BadDataException;
	void deleteBook(long sessionId, String bookId) throws BadDataException;
	void resumeBook(long sessionId, String bookId) throws BadDataException;
	void abortBook(long sessionId, String bookId) throws BadDataException;
	User login(String userName, String password);
	void logout(long sessionId);
	User getLogonUser(long sessionId);
	ParserScript[] getParserScripts(long sessionId) throws BadDataException;
	ParserScript saveParserScript(long sessionId, ParserScript script) throws BadDataException;
	void deleteParserScript(long sessionId, String scriptId) throws BadDataException;
}
