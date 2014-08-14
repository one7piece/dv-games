package com.dv.gtusach.server;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dv.gtusach.client.BookService;
import com.dv.gtusach.server.gae.BookMakerGAE;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class BookServiceImpl extends RemoteServiceServlet implements
		BookService {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(BookServiceImpl.class
			.getCanonicalName());
	private BookMakerGAE bookMaker = null;
	private HashMap<Long, Long> sessionMap = new HashMap<Long, Long>();

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
			for (String bookId : bookIds) {
				Book book = bookMaker.getPersistence().findBook(bookId);
				if (book != null) {
					list.add(book);
				}
			}
		}
		return list.toArray(new Book[list.size()]);
	}

	@Override
	public void createBook(long sessionId, Book newBook) throws BadDataException {
		initBookMaker();
		bookMaker.create(newBook.getStartPageUrl(), newBook.getTitle(),
				newBook.getMaxNumPages(), newBook.getAuthor());
	}

	@Override
	public Book getBook(String bookId) {
		initBookMaker();
		Book book = bookMaker.getPersistence().findBook(bookId);
		return book;
	}

	@Override
	public byte[] downloadBook(long sessionId, String bookId)
			throws BadDataException {
		checkPermission(sessionId, "read");
		initBookMaker();
		byte[] data = bookMaker.getPersistence().loadBookData(bookId);
		return data;
	}

	@Override
	public void deleteBook(long sessionId, String bookId) throws BadDataException {
		checkPermission(sessionId, "delete");
		initBookMaker();
		bookMaker.delete(bookId);
	}

	@Override
	public void resumeBook(long sessionId, String bookId) throws BadDataException {
		checkPermission(sessionId, "write");
		initBookMaker();
		bookMaker.resume(bookId);
	}

	@Override
	public void abortBook(long sessionId, String bookId) throws BadDataException {
		checkPermission(sessionId, "write");
		initBookMaker();
		bookMaker.abort(bookId);
	}

	@Override
	public long login(String userName, String password) {
		initBookMaker();
		long sessionId = -1;
		User user = bookMaker.getPersistence().getUser(userName);
		if (user != null) {
			// encrypt password and compare
			String hash = hash(password);
			if (hash.equals(user.getPassword())) {
				sessionId = System.currentTimeMillis();
				synchronized (sessionMap) {
					sessionMap.put(sessionId, System.currentTimeMillis());
				}
			} else {
				log.log(Level.INFO, userName + " has attempted to log in with wrong password!"
						+ ", password:" + password
						+ ", hashPassword: " + hash
						+ ", expected hashPassword: " + user.getPassword());
			}
		}

		return sessionId;
	}

	@Override
	public void logout(long sessionId) {
		synchronized (sessionMap) {
			sessionMap.remove(sessionId);
		}
	}

	private void checkPermission(long sessionId, String permission)
			throws BadDataException {
		if (!permission.equals("read")) {
			synchronized (sessionMap) {
				if (sessionMap.get(sessionId) == null) {
					throw new BadDataException("Not login!");
				}
			}
		}
	}

	private void initBookMaker() {
		synchronized (this) {
			if (bookMaker == null) {
				bookMaker = new BookMakerGAE();
				bookMaker.setContext(super.getServletContext());
				createDefaultUsers();
			}
		}
	}

	private void createDefaultUsers() {
		User admin = bookMaker.getPersistence().getUser("admin");
		if (admin == null) {
			admin = new User();
			admin.setName("admin");
			admin.setRole("administrator");
			admin.setPassword(hash("spidey"));
			bookMaker.getPersistence().saveUser(admin);
		}

		User dad = bookMaker.getPersistence().getUser("vinhvan");
		if (dad == null) {
			dad = new User();
			dad.setName("vinhvan");
			dad.setRole("creator");
			dad.setPassword(hash("colong"));
			bookMaker.getPersistence().saveUser(dad);
		}
	}

	private String hash(String str) {
		byte[] hash;
		try {
			hash = MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8"));
			return new String(hash, "UTF-8");
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Failed to hash password. " + ex.getClass() + ": "
					+ ex.getMessage());
		}
		return str;
	}

}
