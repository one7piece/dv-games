package com.dv.gtusach.server.gae;

import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.dv.gtusach.server.common.BookMaker;
import com.dv.gtusach.server.common.Persistence;
import com.dv.gtusach.shared.Book;
import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

public class BookMakerGAE extends BookMaker implements Serializable {
  private static Logger log = Logger.getLogger(BookMakerGAE.class.getCanonicalName());
  private static int NUM_BATCH_PAGES = 100;
  protected Persistence persistence;
  private transient ServletContext context;
  
  public BookMakerGAE() {
    super();
    this.persistence = new GAEPersistence();
  }

  public ServletContext getContext() {
    return context;
  }

  public void setContext(ServletContext context) {
    this.context = context;
    //((GAEPersistence)persistence).setContext(context);
  }
  
  public Persistence getPersistence() {
    return persistence;
  }
    
  @Override
  protected InputStream getResourceAsStream(String path) {
    if (context != null) {
      String prefix = "/WEB-INF";
      String newPath = path;
      if (!path.startsWith(prefix)) {
        newPath = prefix + (path.startsWith("/") ? "" : "/") + path;
      }
      return context.getResourceAsStream(newPath);
    }    
    return super.getResourceAsStream(path);    
  }
  
  public void scheduleJob(final Book book) {   
    try {
      log.info("Create backend job for: " + book);
      // Create Task and push it into Task Queue
      Queue queue = QueueFactory.getQueue("CreateBookQueue");
      TaskOptions taskOptions = TaskOptions.Builder.withUrl("/createBook")
          .param("bookId", (String)book.getId())
          .header("Host", BackendServiceFactory.getBackendService().getBackendAddress("book-backend"))
          .method(Method.POST);
      
      taskOptions.retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));
      queue.add(taskOptions);
      log.info("Successfully add task to backends for: " + book);
      // delay for task to start
      Thread.sleep(2000);
    } catch (Throwable ex) {
      log.log(Level.WARNING, "Error add task to backend. ", ex);
    }       
  }  
}
