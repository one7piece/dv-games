package com.dv.gtusach.server;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dv.gtusach.client.BookService;
import com.dv.gtusach.server.gae.BookMakerGAE;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.SystemInfo;
import com.dv.gtusach.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class BookServiceImpl extends RemoteServiceServlet implements
		BookService {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(BookServiceImpl.class
			.getCanonicalName());
	private static final long SESSION_TIMEOUT_MS = 15*60*1000;
	private BookMakerGAE bookMaker = null;
	private HashMap<Long, LogonUser> sessionMap = new HashMap<Long, LogonUser>();

	@Override
	public long getLastUpdateTime() {
		initBookMaker();
		SystemInfo info = bookMaker.getPersistence().getSystemInfo();
		if (info != null && info.getBookLastUpdateTime() != null) {
			return info.getBookLastUpdateTime().getTime();
		}
		return 0;
	}

	@Override
	public ParserScript[] getParserScripts(long sessionId) throws BadDataException {
		initBookMaker();
		checkPermission(sessionId, "script.read");
		List<ParserScript> list = bookMaker.getPersistence().getScripts(null);
		return list.toArray(new ParserScript[0]);
	}
	
	@Override
	public ParserScript saveParserScript(long sessionId, ParserScript script) throws BadDataException {
		initBookMaker();
		checkPermission(sessionId, "script.write");
		bookMaker.getPersistence().saveScript(script);
		return script;
	}

	@Override
	public void deleteParserScript(long sessionId, String scriptId) throws BadDataException {
		initBookMaker();
		checkPermission(sessionId, "script.delete");
		bookMaker.getPersistence().deleteScript(scriptId);
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
		checkPermission(sessionId, "create");
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
	public User getLogonUser(long sessionId) {
		User result = null;
		try {
			LogonUser user = checkPermission(sessionId, null);
			if (user != null) {
				result = user.getUser();
			}
		} catch (BadDataException ex) {
			// session has expired or user is not logged in
		}
		return result;
	}
	
	@Override
	public User login(String userName, String password) {
		initBookMaker();
		LogonUser result = null;
		User user = bookMaker.getPersistence().getUser(userName);
		if (user != null) {
			// encrypt password and compare
			String hash = hash(password);
			if (hash.equals(user.getPassword())) {
				long sessionId = System.currentTimeMillis();
				result = new LogonUser(user);
				result.getUser().setSessionId(sessionId);
				synchronized (sessionMap) {
					sessionMap.put(sessionId, result);
				}
				log.info(result.getUser().getName() + " has logged in, sessionId: " + result.getUser().getSessionId());				
			} else {
				log.log(Level.INFO, userName + " has attempted to log in with wrong password!"
						+ ", password:" + password
						+ ", hashPassword: " + hash
						+ ", expected hashPassword: " + user.getPassword());
			}
		}
		return (result != null ? result.getUser() : null);
	}

	@Override
	public void logout(long sessionId) {
		synchronized (sessionMap) {
			sessionMap.remove(sessionId);
		}
	}

	private LogonUser checkPermission(long sessionId, String permission)
			throws BadDataException {
		synchronized (sessionMap) {
			// remove expired session Id
			Iterator<Entry<Long, LogonUser>> iter = sessionMap.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Long, LogonUser> entry = iter.next();
				if (System.currentTimeMillis() - entry.getValue().getLastAccessTime() >= SESSION_TIMEOUT_MS) {
					log.info("remove expired session: " + entry.getKey() + "/" + entry.getValue().getUser().getName());
					iter.remove();
				}
			}			
			//log.info("#sessions in cache: " + sessionMap.size());
			LogonUser user = sessionMap.get(sessionId);
			if (user == null) {
				throw new BadDataException("Session " + sessionId + " has expired or is not valid!");
			} else {
				user.setLastAccessTime(System.currentTimeMillis());
				sessionMap.put(sessionId, user);
				if (permission != null && permission.startsWith("script") && !user.getUser().getRole().equals("administrator")) {
					throw new BadDataException("User does not have required permissions!");
				}
			}	
			return user;
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

	private class LogonUser {
		private long lastAccessTime;
		private User user;
		
		public LogonUser(User user) {
			this.user = user;
			lastAccessTime = System.currentTimeMillis();
		}
		public long getLastAccessTime() {
			return lastAccessTime;
		}
		public void setLastAccessTime(long lastAccessTime) {
			this.lastAccessTime = lastAccessTime;
		}		
		public User getUser() {
			return user;
		}
	}
}
