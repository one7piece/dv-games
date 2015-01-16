package com.dv.gtusach.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.gae.BookMakerGAE;
import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;

/**
 * This servlet responds to the request corresponding to orders made from task queue and processes
 * them. It also sends a mail to the customer who placed the order.
 * 
 * @author
 */
@SuppressWarnings("serial")
public class CreateBookServlet extends HttpServlet {

  private static final Logger log = Logger.getLogger(CreateBookServlet.class.getCanonicalName());
  private static long recentId;
  private static final int NUM_BATCH_PAGES = 100;
  private BookMakerGAE bookMaker = null;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    createBook(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    createBook(req, resp);
  }
  
  /**
   * The method creates orders by unmarshalling the XML string based on the schema order-trial.xsd.
   * it logs an exception if the uploaded XML schema doesn't match the schema for this project.
   * 
   * @param req : HTTP request from the task queue
   * @param resp : HTTP response
   * @throws IOException
   */
  private void createBook(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/json; charset=utf-8");
    resp.setHeader("Cache-Control", "no-cache");
    final String bookId = req.getParameter("bookId");
    log.info("createBookServlet - create book: " + bookId);
    if (bookId == null || bookId.length() == 0) {
      log.log(Level.WARNING, "Missing bookId!");
      return;
    }
    if (bookMaker == null) {
    	log.info("CreateBookServlet - creating book maker...");
    	bookMaker = new BookMakerGAE();
    }
    bookMaker.setContext(getServletContext());
    Book book = bookMaker.getPersistence().findBook(bookId);
    if (book != null) { 
    	try {
    		BookParser parser = bookMaker.findParser(book.getStartPageUrl());
    		int batchSize = (parser != null ? parser.getBatchSize() : 100);
        bookMaker.createPages(book, batchSize);
        if (book.getStatus() == BookStatus.WORKING) {
        	if (parser != null) {
  					int sec = parser.getDelayTimeSec();
  					if (parser.getDelayTimeSec() > 0) {
  						log.info("Delaying for " + sec + " seconds before reading next batch");
  						try {
  							Thread.sleep(parser.getDelayTimeSec()*1000);
  						} catch (InterruptedException e) {
  						}
  					}
        	}        	
          bookMaker.scheduleJob(book);
        }          	
    		
			} catch (BadDataException e) {
	      log.log(Level.WARNING, e.getMessage());
			}    	
    } else {
      log.log(Level.WARNING, "Cannot find book: " + bookId);
    }
  }
    
}