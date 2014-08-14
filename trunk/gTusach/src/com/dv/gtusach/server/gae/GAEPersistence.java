package com.dv.gtusach.server.gae;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.dv.gtusach.server.common.AttachmentData;
import com.dv.gtusach.server.common.ChapterHtml;
import com.dv.gtusach.server.common.Persistence;
import com.dv.gtusach.server.common.SectionData;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.dv.gtusach.shared.User;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class GAEPersistence extends Persistence implements Serializable {
  public static final String USER_KIND = "User";
  public static final String LIBRARY_KIND = "Libary";
  public static final String BOOK_KIND = "Book";
  public static final String LIBRARY_NAME = "dv";
  public static final String SECTION_KIND = "Section";
  public static final String CHAPTER_KIND = "Chapter";
  public static final String CHAPTER_ATTACHMENT_KIND = "ChapterAttachment";
  private static Logger log = Logger.getLogger(GAEPersistence.class.getCanonicalName());
  private transient ServletContext context;
  private long numBookReads = 0;
  private long numChapterReads = 0;
  private long numAttachmentReads = 0;
  private long numSectionReads = 0;
  private List<Book> cacheBooks = new ArrayList<Book>();
  private Date libraryTimestamp = null;

  public ServletContext getContext() {
    return context;
  }

  public void setContext(ServletContext context) {
    this.context = context;
  }

  protected Key getLibraryKey() {
    return KeyFactory.createKey(LIBRARY_KIND, LIBRARY_NAME);
  }

  private String null2empty(String s) {
    return (s != null ? s : "");
  }

	public void saveUser(User user) {
    log.info("saving user: " + user.getName());
    try {
      Entity entity = null;
    	User oldUser = getUser(user.getName());
      if (oldUser == null) {
        entity = new Entity(USER_KIND, getLibraryKey());
      } else {
        Key key = KeyFactory.stringToKey((String) oldUser.getId());
        entity = new Entity(key);
      }
    	    	
      entity.setProperty("name", user.getName());
      entity.setProperty("role", user.getRole());
      entity.setProperty("password", user.getPassword());
      entity.setProperty("lastLoginTime", user.getLastLoginTime());

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key key = datastore.put(entity);
      log.log(Level.INFO, "Saved user key: " + key);
      user.setId(KeyFactory.keyToString(key));
            
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving user: " + user, ex);
      throw new RuntimeException("Error saving user: " + ex.getMessage());
    }
	}
		    
	public User getUser(String name) {
		Filter nameFilter = new FilterPredicate("name", FilterOperator.EQUAL, name);
    Query query = new Query(USER_KIND, getLibraryKey()).setFilter(nameFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
    if (list.size() > 0) {
    	Entity entity = list.get(0);
    	User user = new User();
    	user.setId(KeyFactory.keyToString(entity.getKey()));
      user.setName((String) entity.getProperty("name"));
      user.setRole((String) entity.getProperty("role"));
      user.setPassword((String) entity.getProperty("password"));
      user.setLastLoginTime((Date)entity.getProperty("lastLoginTime"));    	
    	return user;
    }
    return null;
	}
	
  public Date getLastUpdateTime() {
  	return libraryTimestamp;
  }
  
  @Override
  public void saveBook(Book book) {
    log.info("saving book: " + book);
    Date now = new Date();
    try {
    	
      // all book entity belong to parent library key
      Entity entity = null;
      if (book.getId() == null) {
        entity = new Entity(BOOK_KIND, getLibraryKey());
      } else {
        Key key = KeyFactory.stringToKey((String) book.getId());
        entity = new Entity(key);
      }

      entity.setProperty("user", book.getCreatedBy());
      entity.setProperty("date", book.getCreatedTime());
      entity.setProperty("title", book.getTitle());
      entity.setProperty("status", null2empty(book.getStatusStr()));
      entity.setProperty("author", null2empty(book.getAuthor()));
      entity.setProperty("currentPageNo", book.getCurrentPageNo() + "");
      entity.setProperty("buildTime", book.getBuildTimeSec() + "");
      entity.setProperty("currentPageUrl", null2empty(book.getCurrentPageUrl()));
      entity.setProperty("errorMsg", null2empty(book.getErrorMsg()));
      entity.setProperty("lastUpdatedTime", now);
      entity.setProperty("maxNumPages", book.getMaxNumPages() + "");
      entity.setProperty("startPageUrl", book.getStartPageUrl());
      entity.setProperty("epubCreated", (book.isEpubCreated() ? "true" : "false"));

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key key = datastore.put(entity);
      log.log(Level.INFO, "Saved book key: " + key);
      book.setId(KeyFactory.keyToString(key));
      
      Entity libraryEntity = new Entity(getLibraryKey());
      libraryEntity.setProperty("lastUpdatedTime", now);
      datastore.put(libraryEntity);
      
      updateCache(book, now, false);
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving book: " + book, ex);
      throw new RuntimeException("Error saving book: " + ex.getMessage());
    }
  }

  private void updateCache(Book book, Date timestamp, boolean deleting) {
  	String searchId = (String)book.getId(); 
    synchronized (cacheBooks) {    	
    	if (libraryTimestamp != null) {
    		libraryTimestamp = timestamp;
    		int index = -1;
    		for (int i=0; i<cacheBooks.size(); i++) {
    			String bookId = (String)cacheBooks.get(i).getId(); 
    			if (bookId.equals(searchId)) {
    				index = i;
    				break;
    			}
    		}
    		if (deleting) {
    			if (index != -1) {
    				cacheBooks.remove(index);
    			}
    		} else {
    			if (index != -1) {
    				cacheBooks.set(index, book);
    			} else {
    				cacheBooks.add(0, book);
    			}
    		}
    	}
    }
  }
  
  @Override
  public List<Book> loadBooks(BookStatus[] statusList) {
  	// read the timestamp and see if it has changed
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    
    Query proj = new Query(LIBRARY_KIND, null);
    proj.addProjection(new PropertyProjection("lastUpdatedTime", Date.class));
    List<Entity> list = datastore.prepare(proj).asList(FetchOptions.Builder.withLimit(1));
    Date timestamp = null;
    if (list.size() == 1) {
    	timestamp = (Date)list.get(0).getProperty("lastUpdatedTime");
    }
  	synchronized (cacheBooks) {
      if (timestamp == null || libraryTimestamp == null || libraryTimestamp.before(timestamp)) {
    		List<Book> loadedBooks = loadBooks();
    		cacheBooks.clear();
    		cacheBooks.addAll(loadedBooks);
    		libraryTimestamp = timestamp;
      }
  	}
  	
    List<Book> books = new ArrayList<Book>();
  	synchronized (cacheBooks) {
  		for (Book book : cacheBooks) {
				boolean adding = true;
  			if (statusList != null && statusList.length > 0) {
  				adding = false;
  				// only add if status matched
  				for (BookStatus status: statusList) {
  					if (book.getStatus() == status) {
  						adding = true;
  						break;
  					}
  				}
  			}
  			if (adding) {
  				books.add(book);
  			}
  		}
  	}
    
    return books;
  }

  private List<Book> loadBooks() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query(BOOK_KIND, getLibraryKey());
    query.addSort("lastUpdatedTime", Query.SortDirection.DESCENDING);
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(500));
    numBookReads++;
    log.info("Found " + list.size() + " books in library. numBookReads=" + numBookReads
    		+ ", numChapterReads=" + numChapterReads + ", numAttachmentReads=" + numAttachmentReads
    		+ ", numSectionReads=" + numSectionReads);
    List<Book> books = new ArrayList<Book>();
    for (Entity entity : list) {
      Book book = new Book();
      entity2Book(entity, book);
      books.add(book);
      // log.info("Loaded book: " + book);
    }
    return books;    
  }
  
  private void entity2Book(Entity entity, Book book) {
    book.setId(KeyFactory.keyToString(entity.getKey()));
    book.setTitle((String) entity.getProperty("title"));
    book.setCreatedBy((String) entity.getProperty("user"));
    book.setCreatedTime((Date) entity.getProperty("date"));
    book.setStatusStr((String) entity.getProperty("status"));
    book.setAuthor((String) entity.getProperty("author"));
    book.setCurrentPageNo(getIntAttr(entity, "currentPageNo", -1));
    book.setBuildTimeSec(getIntAttr(entity, "buildTime", 0));
    book.setCurrentPageUrl((String) entity.getProperty("currentPageUrl"));
    book.setErrorMsg((String) entity.getProperty("errorMsg"));
    book.setLastUpdatedTime((Date) entity.getProperty("lastUpdatedTime"));
    book.setMaxNumPages(getIntAttr(entity, "maxNumPages", -1));
    book.setStartPageUrl((String) entity.getProperty("startPageUrl"));
    String epubCreatedStr = (String) entity.getProperty("epubCreated");
    book.setEpubCreated(epubCreatedStr != null && "true".equalsIgnoreCase(epubCreatedStr));
  }

  private int getIntAttr(Entity entity, String attrName, int defaultValue) {
    int result = defaultValue;
    if (entity.getProperty(attrName) != null) {
      try {
        result = Integer.parseInt(String.valueOf(entity.getProperty(attrName)));
      } catch (Exception ex) {
        log.log(Level.WARNING, "Error extracting attribute " + attrName + "("
            + entity.getProperty(attrName) + ")");
      }
    }
    return result;
  }

  @Override
  public Book findBook(Object id) {
    if (id == null) {
      return null;
    }
        
    Entity entity = null;
    Key key = KeyFactory.stringToKey((String) id);
    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      entity = datastore.get(key);
      numBookReads++;
    } catch (EntityNotFoundException e) {
      // ignore
    }

    if (entity == null) {
      return null;
    }
    Book book = new Book();
    entity2Book(entity, book);
    // log.info("Found book: " + book);
    return book;
  }
  
  @Override
  public List<Object> loadChapterIDs(Object bookId) {
  	FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5000).chunkSize(100);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key bookKey = KeyFactory.stringToKey((String) bookId);
    Query query = new Query(CHAPTER_KIND, bookKey).setKeysOnly();
    //query.addSort("chapterNo", Query.SortDirection.ASCENDING);
    List<Entity> list = datastore.prepare(query).asList(fetchOptions);
    log.info("Found " + list.size() + " chapter IDs for book: " + bookId);
    List<Object> result = new ArrayList<Object>();
    for (Entity entity: list) {
    	result.add(KeyFactory.keyToString(entity.getKey()));    	
    }
  	return result;
  }
  
  @Override
  public List<ChapterHtml> loadChapters(Object bookId) {
  	FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5000).chunkSize(100);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key bookKey = KeyFactory.stringToKey((String) bookId);
    Query query = new Query(CHAPTER_KIND, bookKey);
    query.addSort("chapterNo", Query.SortDirection.ASCENDING);
    List<Entity> list = datastore.prepare(query).asList(fetchOptions);
    numChapterReads++;
    log.info("Found " + list.size() + " chapters for book: " + bookId);
    List<ChapterHtml> chapters = new ArrayList<ChapterHtml>();
    for (Entity entity : list) {
      ChapterHtml chapter = new ChapterHtml();
      chapter.setId(KeyFactory.keyToString(entity.getKey()));
      chapter.setBookId(bookId);
      chapter.setChapterNo(getIntAttr(entity, "chapterNo", 0));
      chapter.setChapterTitle((String) entity.getProperty("chapterTitle"));
      Blob htmlBlob = (Blob) entity.getProperty("html");
      if (htmlBlob != null) {
        try {
          chapter.setHtml(new String(htmlBlob.getBytes(), "UTF8"));
        } catch (UnsupportedEncodingException e) {
          log.log(Level.WARNING, "Failed to read chapter html: " + e.getMessage());
        }
      }

      int numAttachments = getIntAttr(entity, "numAttachments", -1);
      if (numAttachments > 0 || (numAttachments > -1 && chapter.getHtml().length() < 10000)) {
        query = new Query(CHAPTER_ATTACHMENT_KIND, entity.getKey());
        query.addSort("sectionNo", Query.SortDirection.ASCENDING);
        list = datastore.prepare(query).asList(fetchOptions);
        numAttachmentReads++;
        //log.info("Found " + list.size() + " attachments for chapter: " + chapter.getId());
        
        Map<String, List<SectionData>> attachmentMap = new HashMap<String, List<SectionData>>();
        for (Entity attachmentEntity : list) {
          List<SectionData> sections = attachmentMap.get(attachmentEntity.getProperty("href"));
          if (sections == null) {
            sections = new ArrayList<SectionData>();
            attachmentMap.put((String) attachmentEntity.getProperty("href"), sections);
          }
          SectionData section = new SectionData();
          section.setSectionNo(getIntAttr(attachmentEntity, "sectionNo", -1));
          section.setData(((Blob) attachmentEntity.getProperty("data")).getBytes());
          sections.add(section);
        }
        Iterator<Map.Entry<String, List<SectionData>>> iter = attachmentMap.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<String, List<SectionData>> entry = iter.next();
          chapter.getAttachments().add(new AttachmentData(entry.getKey(), mergeSections(entry.getValue())));
        }
      }      
      
      chapters.add(chapter);
    }
    return chapters;
  }

  @Override
  public void saveChapter(ChapterHtml chapter) {
    try {
      log.info("Saving book chapter: " + chapter);
      if (chapter.getBookId() == null) {
        log.log(Level.WARNING, "Missing book id in chapter!");
        return;
      }

      Key bookKey = KeyFactory.stringToKey((String) chapter.getBookId());
      Entity chapterEntity = new Entity(CHAPTER_KIND, chapter.getChapterNo(), bookKey);
      chapterEntity.setProperty("chapterNo", chapter.getChapterNo());
      chapterEntity.setProperty("chapterTitle", chapter.getChapterTitle());
      chapterEntity.setProperty("html", new Blob(chapter.getHtml().getBytes("UTF8")));
      chapterEntity.setProperty("numAttachments", chapter.getAttachments().size());

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key chapterKey = datastore.put(chapterEntity);
      chapter.setId(KeyFactory.keyToString(chapterKey));
      log.info("Saved book chapter key: " + chapterKey);

      if (chapter.getAttachments().size() > 0) {
        List<Entity> attachmentEntities = new ArrayList<Entity>();
        for (AttachmentData attachmentData : chapter.getAttachments()) {
          List<SectionData> sections = splitIntoSections(chapter.getBookId(), attachmentData
              .getData(), 700);
          for (SectionData section : sections) {
            Entity attachmentEntity = new Entity(CHAPTER_ATTACHMENT_KIND, chapterKey);
            attachmentEntity.setProperty("href", attachmentData.getHref());
            attachmentEntity.setProperty("sectionNo", section.getSectionNo());
            attachmentEntity.setProperty("data", new Blob(section.getData()));
            attachmentEntities.add(attachmentEntity);
          }
        }
        datastore.put(attachmentEntities);
      }

    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving book chapter: " + chapter, ex);
      throw new RuntimeException("Error saving book chapter: " + ex.getMessage());
    }
  }

  public List<Object> loadBookSectionIDs(Object bookId) {
  	FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5000).chunkSize(100);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key bookKey = KeyFactory.stringToKey((String) bookId);
    Query query = new Query(SECTION_KIND, bookKey).setKeysOnly();
    //query.addSort("sectionNo", Query.SortDirection.ASCENDING);
    List<Entity> list = datastore.prepare(query).asList(fetchOptions);
    List<Object> result = new ArrayList<Object>();
    for (Entity entity: list) {
    	result.add(KeyFactory.keyToString(entity.getKey()));    	
    }
  	return result;
  }
  
  @Override
  public List<SectionData> loadBookSections(Object bookId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key bookKey = KeyFactory.stringToKey((String) bookId);
    Query query = new Query(SECTION_KIND, bookKey);
    query.addSort("sectionNo", Query.SortDirection.ASCENDING);
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5000).chunkSize(100));
    numSectionReads++;
    log.info("Found " + list.size() + " sections for book: " + bookId);
    List<SectionData> sections = new ArrayList<SectionData>();
    for (Entity entity : list) {
      SectionData section = new SectionData();
      section.setId(KeyFactory.keyToString(entity.getKey()));
      section.setBookId(bookId);
      section.setSectionNo(getIntAttr(entity, "sectionNo", 0));
      Blob blob = (Blob) entity.getProperty("data");
      if (blob != null) {
        section.setData(blob.getBytes());
      }
      sections.add(section);
    }
    return sections;
  }

  @Override
  public void saveBookData(Object bookId, byte[] data) {
    log.info("Saved book data: " + bookId);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // remove all old sections
    List<Key> keys = new ArrayList<Key>();
    List<SectionData> oldSections = loadBookSections(bookId);
    for (SectionData section : oldSections) {
      keys.add(KeyFactory.stringToKey((String) section.getId()));
    }
    datastore.delete(keys);

    Key bookKey = KeyFactory.stringToKey((String) bookId);
    List<SectionData> newSections = splitIntoSections(bookId, data, 500);
    for (SectionData section : newSections) {
      Entity entity = new Entity(SECTION_KIND, bookKey);
      entity.setProperty("sectionNo", section.getSectionNo());
      entity.setProperty("data", new Blob(section.getData()));
      Key key = datastore.put(entity);
      log.info("Saved book section key: " + key);
      section.setId(KeyFactory.keyToString(key));
    }
  }

  @Override
  public void removeBook(Object id) {
    log.info("Removing book: " + id);
    Book book = findBook(id);
    if (book == null) {
      log.info("Cannot find book: " + id);
      return;
    }

  	Date now = new Date();
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Object> chapterIDs = loadChapterIDs(book.getId());
    Iterator<Object> chapterIDsIter = chapterIDs.iterator();
    List<Key> keys = new ArrayList<Key>();
    while (chapterIDsIter.hasNext()) {
      Object chapterId = chapterIDsIter.next();
      chapterIDsIter.remove();      
      keys.add(KeyFactory.stringToKey((String) chapterId));
      if (keys.size() >= 200) {
        datastore.delete(keys);
        keys.clear();
      }
    }    
    if (keys.size() > 0) {
      datastore.delete(keys);
      keys.clear();
    }
           
    List<Object> sections = loadBookSectionIDs(book.getId());
    Iterator<Object> sectionIter = sections.iterator();
    while (sectionIter.hasNext()) {
      Object sectionId = sectionIter.next();
      sectionIter.remove();
      keys.add(KeyFactory.stringToKey((String) sectionId));
      if (keys.size() >= 200) {
        datastore.delete(keys);
        keys.clear();
      }
    }
    if (keys.size() > 0) {
      datastore.delete(keys);
      keys.clear();
    }
    
    datastore.delete(KeyFactory.stringToKey((String) book.getId()));
    
    Entity libraryEntity = new Entity(getLibraryKey());
    libraryEntity.setProperty("lastUpdatedTime", now);
    datastore.put(libraryEntity);
    
    updateCache(book, now, true);
  }

}
