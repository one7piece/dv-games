package com.dv.gtusach.server.common;

import java.util.ArrayList;
import java.util.List;

public class ChapterHtml {
  private Object id;
  private String chapterTitle;
  private int chapterNo;
  private String html;
  private Object bookId;
  private List<AttachmentData> attachments = new ArrayList<AttachmentData>();
    
  public List<AttachmentData> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<AttachmentData> attachments) {
    this.attachments = attachments;
  }

  public Object getBookId() {
    return bookId;
  }

  public void setBookId(Object bookId) {
    this.bookId = bookId;
  }

  public int getBookIdInt() {
    return (bookId != null ? (Integer)bookId : 0); 
  }
  
  public void setBookIdInt(int bookId) {
    this.bookId = bookId;
  }
  
  public Object getId() {
    return id;
  }
  
  public void setId(Object id) {
    this.id = id;
  }

  public int getIdInt() {
    return (id != null ? (Integer)id : 0);
  }

  public void setIdInt(int id) {
    this.id = id;
  }
  
  public String getChapterTitle() {
    return chapterTitle;
  }
  
  public void setChapterTitle(String chapterTitle) {
    this.chapterTitle = chapterTitle;
  }
  
  public int getChapterNo() {
    return chapterNo;
  }
  
  public void setChapterNo(int chapterNo) {
    this.chapterNo = chapterNo;
  }
  
  public String getHtml() {
    return html;
  }
  
  public void setHtml(String html) {
    this.html = html;
  }      
  
  public String toString() {
  	return "ID:" + getId() + ", chapter: " + chapterNo + " - " + chapterTitle;
  }
}
