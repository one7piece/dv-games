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

import com.dv.gtusach.server.common.AttachmentData;
import com.dv.gtusach.server.common.ChapterHtml;
import com.dv.gtusach.server.common.Persistence;
import com.dv.gtusach.server.common.SectionData;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.SystemInfo;
import com.dv.gtusach.shared.User;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;

public class GAEPersistence extends Persistence implements Serializable {
  public static final String SYSTEM_INFO_KIND = "SystemInfo";
  public static final String SCRIPT_KIND = "Script";
  public static final String USER_KIND = "User";
  public static final String LIBRARY_KIND = "Libary";
  public static final String BOOK_KIND = "Book";
  public static final String LIBRARY_NAME = "dv";
  public static final String SECTION_KIND = "Section";
  public static final String CHAPTER_KIND = "Chapter";
  public static final String CHAPTER_ATTACHMENT_KIND = "ChapterAttachment";
  private static Logger log = Logger.getLogger(GAEPersistence.class.getCanonicalName());
  private long numBookReads = 0;
  private long numChapterReads = 0;
  private long numAttachmentReads = 0;
  private long numSectionReads = 0;
  private List<Book> cacheBooks = new ArrayList<Book>();
  private SystemInfo systemInfo = null;
  
  public GAEPersistence() {
  	SystemInfo info = getSystemInfo();
  	// initialise system info with just the id
  	systemInfo = new SystemInfo();
  	systemInfo.setId(info.getId());  	
  }
    
  protected Key getLibraryKey() {
    return KeyFactory.createKey(LIBRARY_KIND, LIBRARY_NAME);
  }

  private String null2empty(String s) {
    return (s != null ? s : "");
  }
  
  @Override
  public SystemInfo getSystemInfo() {
  	SystemInfo result = null;
    Query query = new Query(SYSTEM_INFO_KIND, getLibraryKey());
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
    if (list != null && list.size() > 0) {
    	Entity entity = list.get(0);
    	result = new SystemInfo();
    	result.setId(KeyFactory.keyToString(entity.getKey()));
    	result.setBookLastUpdateTime((Date)entity.getProperty("bookLastUpdateTime"));
    	result.setUserLastUpdateTime((Date)entity.getProperty("userLastUpdateTime"));
    	result.setScriptLastUpdateTime((Date)entity.getProperty("scriptLastUpdateTime"));
    	String s = (String)entity.getProperty("editingScript");
    	result.setEditingScript(s != null && s.equalsIgnoreCase("true"));
      //log.info("loaded system info: " + result);
    } else {
    	result = new SystemInfo();
    	Date now = new Date();
    	result.setBookLastUpdateTime(now);
    	result.setUserLastUpdateTime(now);
    	result.setScriptLastUpdateTime(now);
    	result.setEditingScript(Boolean.FALSE);
   		saveSystemInfo(result);
    }
    return result;
  }
  
  public void saveSystemInfo(SystemInfo info) {
  	log.info("saveSystemInfo() - " + info); 
    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity entity = null;
      if (info.getId() == null) {
        entity = new Entity(SYSTEM_INFO_KIND, getLibraryKey());
      } else {
        Key key = KeyFactory.stringToKey(info.getId());
        entity = new Entity(key);
      }    	    	
      // only save the property which was set
      if (info.getBookLastUpdateTime() != null) {
        entity.setProperty("bookLastUpdateTime", info.getBookLastUpdateTime());
      }
      if (info.getUserLastUpdateTime() != null) {
        entity.setProperty("userLastUpdateTime", info.getUserLastUpdateTime());
      }
      if (info.getScriptLastUpdateTime() != null) {
        entity.setProperty("scriptLastUpdateTime", info.getScriptLastUpdateTime());
      }
      if (info.isEditingScript() != null) {
        entity.setProperty("editingScript", info.isEditingScript().toString());
      }
      Key key = datastore.put(entity);
      log.log(Level.INFO, "Saved System Info: " + info);
      info.setId(KeyFactory.keyToString(key));
            
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving system info: " + info, ex);
      throw new RuntimeException("Error saving system info: " + ex.getMessage());
    }
  }
  
  @Override
	public void saveScript(ParserScript script) {
  	Date now = new Date();
    try {
      Entity entity = null;
      //ParserScript oldScript = getScript(script.getDomainName());
      if (script.getId() == null) {
        entity = new Entity(SCRIPT_KIND, getLibraryKey());
      } else {
        Key key = KeyFactory.stringToKey((String) script.getId());
        entity = new Entity(key);
      }    	    	
      entity.setProperty("domainName", script.getDomainName());
      entity.setProperty("script", new Text(script.getScript()));
      script.setTimestamp(now);
      entity.setProperty("timestamp", script.getTimestamp());

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key key = datastore.put(entity);
      //log.log(Level.INFO, "Saved script: " + script);
      script.setId(KeyFactory.keyToString(key));
      
      systemInfo.setScriptLastUpdateTime(now);
      saveSystemInfo(systemInfo);
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving script: " + script, ex);
      throw new RuntimeException("Error saving script: " + ex.getMessage());
    }
	}

  @Override
	public void deleteScript(String scriptId) {
  	Date now = new Date();
    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.delete(KeyFactory.stringToKey(scriptId));
      systemInfo.setScriptLastUpdateTime(now);
      saveSystemInfo(systemInfo);
      
      log.log(Level.INFO, "deleted script: " + scriptId);      
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error deleting script: " + scriptId, ex);
      throw new RuntimeException("Error deleting script: " + ex.getMessage());
    }
	}
  
  @Override
	public ParserScript getScript(String domainName) {
		Filter nameFilter = new FilterPredicate("domainName", FilterOperator.EQUAL, domainName);
    Query query = new Query(SCRIPT_KIND, getLibraryKey()).setFilter(nameFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(100));
    if (list.size() > 0) {
    	Entity entity = list.get(0);
    	ParserScript script = new ParserScript();
    	script.setId(KeyFactory.keyToString(entity.getKey()));
    	script.setDomainName((String) entity.getProperty("domainName"));
    	script.setScript(((Text) entity.getProperty("script")).getValue());
      //log.info("loaded script: " + script);
    	return script;
    }
    return null;
	}
		
	@Override
	public List<ParserScript> getScripts(Date timestamp) {
    //log.info("get scripts after: " + timestamp);
		List<ParserScript> result = new ArrayList<ParserScript>();			
    Query query = new Query(SCRIPT_KIND, getLibraryKey());
    if (timestamp != null) {
  		Filter filter = new FilterPredicate("timestamp", FilterOperator.GREATER_THAN, timestamp);
  		query.setFilter(filter);
    }
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(100));
    for (Entity entity: list) {
    	ParserScript script = new ParserScript();
    	script.setId(KeyFactory.keyToString(entity.getKey()));
    	script.setDomainName((String) entity.getProperty("domainName"));
    	script.setScript(((Text) entity.getProperty("script")).getValue());
      //log.info("loaded script: " + script);
      result.add(script);
    }
    return result;
	}
	
	public void saveUser(User user) {
    log.info("saving user: " + user);
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

      systemInfo.setUserLastUpdateTime(new Date());
      saveSystemInfo(systemInfo);
      
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
      //log.info("loaded user: " + user);
    	return user;
    }
    return null;
	}
	  
  @Override
  public void saveBook(Book book) {
    //log.info("saving book: " + book);
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
      //log.log(Level.INFO, "Saved book key: " + key);
      book.setId(KeyFactory.keyToString(key));
      
      systemInfo.setBookLastUpdateTime(now);
      saveSystemInfo(systemInfo);
            
      updateCache(book, now, false);
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error saving book: " + book, ex);
      throw new RuntimeException("Error saving book: " + ex.getMessage());
    }
  }
  
  @Override
  public List<Book> loadBooks(BookStatus[] statusList) {
  	// read the timestamp and see if it has changed    
    SystemInfo newInfo = getSystemInfo();
        
  	synchronized (cacheBooks) {
      if ((systemInfo.getBookLastUpdateTime() == null)
      		|| (newInfo.getBookLastUpdateTime() != null       		
      		&& systemInfo.getBookLastUpdateTime().before(newInfo.getBookLastUpdateTime()))) {
      	// load books from datastore
    		List<Book> loadedBooks = doLoadBooks();
    		cacheBooks.clear();
    		cacheBooks.addAll(loadedBooks);
    		newInfo.setBookLastUpdateTime(newInfo.getBookLastUpdateTime());
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

  private List<Book> doLoadBooks() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query(BOOK_KIND, getLibraryKey());
    query.addSort("lastUpdatedTime", Query.SortDirection.DESCENDING);
    List<Entity> list = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(500));
    numBookReads++;    
    //log.info("doLoadBooks() - Found " + list.size() + " books in library. numBookReads=" + numBookReads
    //		+ ", numChapterReads=" + numChapterReads + ", numAttachmentReads=" + numAttachmentReads
    //		+ ", numSectionReads=" + numSectionReads);
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
    //log.info("Found " + list.size() + " chapter IDs for book: " + bookId);
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
    //log.info("Found " + list.size() + " chapters for book: " + bookId);
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
      //log.info("Saving book chapter: " + chapter);
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
      //log.info("Saved book chapter key: " + chapterKey);

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
    //log.info("Found " + list.size() + " sections for book: " + bookId);
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
    //log.info("Saved book data: " + bookId);
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
      //log.info("Saved book section key: " + key);
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
        
    systemInfo.setBookLastUpdateTime(now);
    saveSystemInfo(systemInfo);
    
    updateCache(book, now, true);
  }

  private void updateCache(Book book, Date timestamp, boolean deleting) {
  	String searchId = (String)book.getId(); 
    synchronized (cacheBooks) {
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
