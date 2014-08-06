package com.dv.gtusach.server.common;

import java.net.Proxy;

public class SiteConfiguration {
  private String referer = "";
  private String cookie = "";
  private int numTries = 1;
  private int timeoutSec = 10;
  private Proxy proxy;
  
  public String getReferer() {
    return referer;
  }
  public void setReferer(String referer) {
    this.referer = referer;
  }
  public String getCookie() {
    return cookie;
  }
  public void setCookie(String cookie) {
    this.cookie = cookie;
  }
  public int getNumTries() {
    return numTries;
  }
  public void setNumTries(int numTries) {
    this.numTries = numTries;
  }
  public int getTimeoutSec() {
    return timeoutSec;
  }
  public void setTimeoutSec(int timeoutSec) {
    this.timeoutSec = timeoutSec;
  }
  public Proxy getProxy() {
    return proxy;
  }
  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }   
}
