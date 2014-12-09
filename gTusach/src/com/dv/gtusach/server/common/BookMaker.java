package com.dv.gtusach.server.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;

import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;

public abstract class BookMaker {
  private static final int MAX_PENDING_BOOKS = 3;
  private static Logger log = Logger.getLogger(BookMaker.class.getCanonicalName());
  private String templateHtml = null;

  public BookMaker() {
  }
      
  public BookParser findParser(String url) throws BadDataException {
  	BookParser result = BookParserFactory.getInstance().getParser(getPersistence(), url);
    if (result != null) {
    	if (result.getScriptError() != null) {
  			throw new BadDataException("Parser script error! " + result.getScriptError().getMessage());
    	}
      result.setBookTemplate(getTemplateHtml());
    }
    return result;
  }

  protected String getTemplateHtml() {
    if (templateHtml == null) {
      try {
        //log.info("loading template.html...");
        byte[] buf = loadResource("template.html");
        if (buf == null) {
          throw new RuntimeException("Cannot find template file: template.html");
        }
        templateHtml = new String(buf, "UTF8");
      } catch (Exception ex) {
        throw new RuntimeException("Failed to load template file: template.html", ex);
      }
    }
    return templateHtml;
  }
  
  public abstract void scheduleJob(final Book book);

  public abstract Persistence getPersistence();

  public void create(String url, String title, int numPages, String author) throws BadDataException {
    log.info("create book, url: " + url + ", title: " + title + ", numPages: " + numPages + ", author: " + author);
    // get the parser for the url
    BookParser parser = findParser(url);
    if (parser == null) {
      throw new BadDataException("No parser found for book url: " + url);
    }
    List<Book> pendingList = getPersistence().loadBooks(new BookStatus[] {BookStatus.WORKING});
    if (pendingList.size() > MAX_PENDING_BOOKS) {
      throw new BadDataException("Too many books in progress");
    }
    // create the book & save status
    Book newBook = new Book();
    newBook.setTitle(title);
    newBook.setStartPageUrl(url);
    newBook.setCurrentPageNo(0);
    newBook.setCurrentPageUrl("");
    newBook.setMaxNumPages(numPages);
    newBook.setAuthor(author);
    newBook.setStatus(BookStatus.WORKING);
    getPersistence().saveBook(newBook);

    scheduleJob(newBook);
  }

  public void delete(Object bookId) throws BadDataException {
    getPersistence().removeBook(bookId);
  }
  
  public void resume(Object bookId) throws BadDataException {
    log.info("resuming book: " + bookId);
    Book book = getPersistence().findBook(bookId);
    if (book == null) {
      throw new BadDataException("Cannot find book: " + bookId + " from database!");
    }
    if (book.getStatus() != BookStatus.WORKING) {
      book.setErrorMsg("");
      book.setStatus(BookStatus.WORKING);
      getPersistence().saveBook(book);
      scheduleJob(book);
    } else {
      throw new BadDataException("Cannot resume book: " + bookId + "(" + book.getStatusStr() + ")");
    }
  }

  public void abort(Object bookId) throws BadDataException {
    log.info("aborting book: " + bookId);
    Book book = getPersistence().findBook(bookId);
    if (book == null) {
      throw new BadDataException("Cannot find book: " + bookId + " from database!");
    }
    if (book.getStatus() != BookStatus.COMPLETED && book.getStatus() != BookStatus.ERROR) {
    	book.setStatus(BookStatus.ABORTED);
      getPersistence().saveBook(book);
    } 
  }

  public void createPages(Book book, int numPages) {
  	try {
      String url = book.getCurrentPageUrl();
      if (url == null || url.length() == 0) {
        url = book.getStartPageUrl();
      }
      if (url == null || url.length() == 0) {
        setBookError(book, "No url found!");
        return;
      }

      BookParser parser = findParser(url);
      if (parser == null) {
        setBookError(book, "Cannot find parser for book url: " + url);
        return;
      }
      
      log.info("createPages() - book: " + book + ", batch numPages=" 
      + numPages + ", parser=" + parser.getClass().getCanonicalName() + ", URL=" + url);
      String target = parser.getTargetUrl(url);
      String request = parser.getRequestUrl(url);
      if (target == null || request == null) {
        setBookError(book, "Invalid book url: " + url);
        return;
      }
      int count = 0;    
      boolean completed = false;
      
      List<ChapterHtml> chapters = new ArrayList<ChapterHtml>();
      String currentPageUrl = book.getCurrentPageUrl();
      int currentPageNo = book.getCurrentPageNo();
      
      try {
        for (int i=0; i<numPages && !completed; i++) {        
          //log.info("loading page: " + (book.getCurrentPageNo()+1) + ", numPages=" + numPages);
          String rawChapterHtml = parser.executeRequest(target, request);
          if (rawChapterHtml == null || rawChapterHtml.length() == 0) {
            throw new BadDataException("Failed to loaded chapter html from target=" + target 
                + ", request=" + request);
          }
          
          ChapterHtml chapterHtml = null;
          try {
            chapterHtml = parser.extractChapterHtml(target, request, rawChapterHtml);        
          } catch (BadDataException ex) {
            log.warning("Error extracting chapter content from: " + request + " - " + ex.getMessage());
            throw new BadDataException("Error extracting chapter content from: " + request + " - " + ex.getMessage());
          }
          
          String pageUrl = parser.getUrl(target, request);        
          if (book.getCurrentPageUrl() == null || !book.getCurrentPageUrl().equals(pageUrl)) {
            int chapterNo = book.getCurrentPageNo() + 1;
            // save the chapter
            chapterHtml.setBookId(book.getId());
            chapterHtml.setChapterNo(chapterNo);
            chapterHtml.setChapterTitle(parser.getChapterTitle(rawChapterHtml, chapterHtml.getHtml()));
            if (count == 0 && (chapterHtml.getChapterTitle() == null || chapterHtml.getChapterTitle().length() == 0)) {
              //log.info("Cannot parse chapter title. Raw Chapter\n" 
              //    + rawChapterHtml + "\nFormat Chapter\n" + chapterHtml.getHtml());
            }
            getPersistence().saveChapter(chapterHtml);
            //chapters.add(chapterHtml);
            
            book.setCurrentPageNo(chapterNo);
            book.setCurrentPageUrl(pageUrl);
            count++;
          } else {
            // ignore duplicate, ie. when we're continuing
          }
          
          // get the next page
          String nextPageUrl = parser.getNextPageUrl(target, request, rawChapterHtml);
          if (nextPageUrl != null && nextPageUrl.toLowerCase().startsWith("http")) {
            request = parser.getRequestUrl(nextPageUrl);
          } else {
            request = nextPageUrl;
          }
          
          if (((request == null || request.length() == 0) 
              || (book.getMaxNumPages() > 0 && book.getCurrentPageNo() >= book.getMaxNumPages()))) {
          	completed = true;
          }
          
          // check if book was aborted
          Book loadedBook = getPersistence().findBook(book.getId());
          if (loadedBook != null && loadedBook.getStatus() == BookStatus.ABORTED) {
            log.info("Book " + loadedBook + " has been aborted!");
            book.setStatus(BookStatus.ABORTED);
          }        
          getPersistence().saveBook(book);
          if (book.getStatus() == BookStatus.ABORTED) {
            break;
          }
        }
        if (completed) {
        	book.setStatus(BookStatus.COMPLETED);
        }
      } catch (BadDataException ex) {
        log.warning("Error creating pages. " + ex.getMessage());
        book.setErrorMsg(ex.getClass().getName() + ":" + ex.getMessage());
        book.setStatus(BookStatus.ERROR);      
      } catch (Throwable ex) {
        log.log(Level.WARNING, "Error creating pages.", ex);
        book.setErrorMsg(ex.getClass().getName() + ":" + ex.getMessage());
        book.setStatus(BookStatus.ERROR);
      }
              
      if (completed || book.getStatus() == BookStatus.ERROR || book.getStatus() == BookStatus.ABORTED) {
        if (count > 0 || (!book.isEpubCreated() && book.getCurrentPageNo() > 0)) {
          createEpub(book);        
        } else {
          getPersistence().saveBook(book);
        }
      }
  	} catch (Throwable ex) {
  		log.log(Level.WARNING, "Error creating pages.", ex);
      book.setErrorMsg(ex.getClass().getName() + ":" + ex.getMessage());
      book.setStatus(BookStatus.ERROR);
      getPersistence().saveBook(book);
  	}
  }
  
  public void createEpub(Book book) {
    log.info("Start creating epub book: " + book.getTitle() + ", status=" + book.getStatusStr());
        
    try {
      nl.siegmann.epublib.domain.Book epubBook = new nl.siegmann.epublib.domain.Book();
      epubBook.getMetadata().addTitle(book.getTitle());
      epubBook.getMetadata().addAuthor(new Author(book.getTitle()));
      epubBook.getMetadata().setLanguage("vi");
      
      // add style css resource
      byte[] cssData = loadResource("stylesheet.css");
      Resource styleRes = new Resource("stylesheet.css", cssData, "stylesheet.css", MediatypeService.CSS);
      epubBook.getResources().add(styleRes);

      String[] timesFont = new String[] {"Times-New-Roman.ttf", "Times-New-Roman-Bold.ttf", "Times-New-Roman-Italic.ttf", "Times-New-Roman-Bold-Italic.ttf"};
      String[] verdanaFont = new String[]{"verdana.ttf", "verdanab.ttf", "verdanai.ttf", "verdanaz.ttf"};

      // add embedded font
      String[] selectedFont = verdanaFont;
      for (String name : selectedFont) {
        byte[] font = loadResource(name);
        epubBook.getResources().add(new Resource(name, font, name, MediatypeService.TTF));
      }    
    	
    	
      java.util.List<ChapterHtml> chapters = getPersistence().loadChapters(book.getId());
      for (int i=0; i<chapters.size(); i++) {
        ChapterHtml chapter = chapters.get(i);
        Resource chapterRes = new Resource(chapter.getHtml().getBytes("UTF8"), MediatypeService.XHTML);
        String chapterTitle = chapter.getChapterTitle();        
        if (chapterTitle == null || chapterTitle.trim().length() == 0) {
          chapterTitle = "Chapter " + chapter.getChapterNo();
        }
        epubBook.addSection(chapterTitle, chapterRes);
        for (AttachmentData attachment: chapter.getAttachments()) {
          epubBook.getResources().add(new Resource(attachment.getData(), attachment.getHref()));
        }
      }
      
      // Create EpubWriter
      EpubWriter epubWriter = new EpubWriter();
      // Write the Book as Epub
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10000);
      epubWriter.write(epubBook, outputStream);
      byte[] epubData = outputStream.toByteArray();      
      log.info("Finished creating epub book, size=" + epubData.length + " bytes");
      getPersistence().saveBookData(book.getId(), epubData);      
      book.setEpubCreated(true);
      getPersistence().saveBook(book);
    } catch (Throwable ex) {
      log.log(Level.WARNING, "Error creating epub book. ", ex);
      setBookError(book, "Failed to create epub book. " + ex.getMessage());
      return;
    }    
  }

  protected void setBookError(Book book, String errorMsg) {
    log.log(Level.WARNING, book + ": " + errorMsg);
    book.setErrorMsg(errorMsg);
    book.setStatus(BookStatus.ERROR);
    getPersistence().saveBook(book);
  }

  protected byte[] loadResource(String filename) throws IOException {
    byte[] result = new byte[0];
    String path = filename;
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    InputStream is = getResourceAsStream(path);
    if (is == null) {
      return null;
    }

    try {
      byte[] buf = new byte[1000];
      int count = 0;
      while ((count = is.read(buf)) != -1) {
        byte[] tmp = new byte[result.length + count];
        System.arraycopy(result, 0, tmp, 0, result.length);
        System.arraycopy(buf, 0, tmp, result.length, count);
        result = tmp;
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
    return result;
  }

  protected InputStream getResourceAsStream(String path) {
    //return getClass().getResourceAsStream(path);
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
  }
}
