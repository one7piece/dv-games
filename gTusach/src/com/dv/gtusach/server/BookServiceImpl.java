package com.dv.gtusach.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dv.gtusach.client.BookService;
import com.dv.gtusach.server.gae.BookMakerGAE;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class BookServiceImpl extends RemoteServiceServlet implements BookService {
	private static final long serialVersionUID = 1L;
	private BookMakerGAE bookMaker = null;

	@Override
	public long getLastUpdateTime() {
		initBookMaker();
		Date t = bookMaker.getPersistence().getLastUpdateTime();
		if (t != null) {
			return t.getTime();
		}
		return 0;
	}
	
	@Override
	public Book[] getBooks(String[] bookIds) {
		initBookMaker();
		List<Book> list = new ArrayList<Book>();
		if (bookIds == null || bookIds.length == 0) {
			list = bookMaker.getPersistence().loadBooks(null);		
		} else {
			for (String bookId: bookIds) {
				Book book = bookMaker.getPersistence().findBook(bookId);
				if (book != null) {
					list.add(book);
				}
			}
		}
		return list.toArray(new Book[list.size()]);
	}

	@Override
	public void createBook(Book newBook) throws BadDataException {
		initBookMaker();
		bookMaker.create(newBook.getStartPageUrl(), newBook.getTitle(), newBook.getMaxNumPages(), newBook.getAuthor());
	}

	@Override
	public Book getBook(String bookId) {
		initBookMaker();
		Book book = bookMaker.getPersistence().findBook(bookId); 
		return book;
	}

	public byte[] downloadBook(String bookId) {
		initBookMaker();
		byte[] data = bookMaker.getPersistence().loadBookData(bookId);
		return data;
	}
	
	public void deleteBook(String bookId) throws BadDataException {
		initBookMaker();
		bookMaker.delete(bookId);
	}
	
	public void resumeBook(String bookId) throws BadDataException {
		initBookMaker();
		bookMaker.resume(bookId);
	}
	
	public void abortBook(String bookId) throws BadDataException {
		initBookMaker();
		bookMaker.abort(bookId);
	}
	
	private void initBookMaker() {
		synchronized (this) {
			if (bookMaker == null) {
				bookMaker = new BookMakerGAE();
				bookMaker.setContext(super.getServletContext());
			}
		}
	}
}
