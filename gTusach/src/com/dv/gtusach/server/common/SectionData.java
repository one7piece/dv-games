package com.dv.gtusach.server.common;

public class SectionData {
  private Object id;
  private int sectionNo;
  private byte[] data;
  private Object bookId;
    
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

  public String getIdStr() {
    return (id != null ? (String)id : "");
  }
  
  public void setIdStr(String s) {
    this.id = s;
  }
  
  public int getSectionNo() {
    return sectionNo;
  }
  public void setSectionNo(int sectionNo) {
    this.sectionNo = sectionNo;
  }
  public byte[] getData() {
    return data;
  }
  public void setData(byte[] data) {
    this.data = data;
  }
}
