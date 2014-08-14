package com.dv.gtusach.server;

import java.util.Calendar;
import java.util.Date;

import com.dv.gtusach.client.BookService;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class BookServiceSimulatorImpl extends RemoteServiceServlet implements BookService {
	private static final long serialVersionUID = 1L;
	private final int NUM_BOOKS = 50;

	public long getLastUpdateTime() {
		return 0;
	}
	
	@Override
	public Book[] getBooks(String[] bookIds) {
		long now = System.currentTimeMillis();
		now -= NUM_BOOKS*1000;
		
		Book[] result = new Book[NUM_BOOKS];
		int statusIndex = 0;
		for (int i=0; i<result.length; i++) {
			result[i] = new Book();
			result[i].setTitle("Book Title: " + (i+1));
			result[i].setCurrentPageNo(i+100);
			result[i].setMaxNumPages(0);
			result[i].setStatus(BookStatus.values()[statusIndex]);
			if (++statusIndex >= BookStatus.values().length) {
				statusIndex = 0;
			}
			result[i].setLastUpdatedTime(new Date(now + i*1000));
		}
		return result;
	}

	@Override
	public void createBook(long sessionId, Book newBook) throws BadDataException {
		// TODO Auto-generated method stub		
	}

	@Override
	public Book getBook(String bookId) {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] downloadBook(long sessionId, String bookId) {
		return null;
	}
	
	public void deleteBook(long sessionId, String bookId) throws BadDataException {
	}
	
	public void resumeBook(long sessionId, String bookId) throws BadDataException {
	}
	
	public void abortBook(long sessionId, String bookId) throws BadDataException {
	}
	
	public long login(String userName, String password) {
		long sessionId = -1;
		return sessionId;
	}

	public void logout(long sessionId) {
		
	}	
}
