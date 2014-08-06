package com.dv.gtusach.server.common;

public class AttachmentData {
  private String href;
  private byte[] data;
  
  public AttachmentData() {   
  }
  
  public AttachmentData(String href, byte[] data) {
    this.href = href;
    this.data = data;
  }
  
  public String getHref() {
    return href;
  }
  public void setHref(String href) {
    this.href = href;
  }
  public byte[] getData() {
    return data;
  }
  public void setData(byte[] data) {
    this.data = data;
  }  
}
