package com.dv.gtusach.server.common;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpService {
  protected static final Logger log = Logger.getLogger(HttpService.class.getCanonicalName());
    
  public String getUrl(String target, String request) {    
    String url = (target.endsWith("/") ? target.substring(0, target.length()-1) : target);
    url += "/" + (request.startsWith("/") ? request.substring(1) : request);
    if (!url.startsWith("http://")) {
      url = "http://" + url;
    }
    return url;
  }
  
  public String executeRequestStr(String target, String request, SiteConfiguration siteConfiguration) {
    String urlStr = getUrl(target, request);
    byte[] responseData = executeRequest(urlStr, siteConfiguration);
    if (responseData != null) {
      try {
        return new String(responseData, "UTF8");
      } catch (UnsupportedEncodingException e) {
        log.log(Level.WARNING, "Error converting response to string.", e);
      }
    }
    return null;
  }
  
  public byte[] executeRequest(String urlStr, SiteConfiguration siteConfiguration) {
    byte[] result = null;
    for (int i = 0; i < siteConfiguration.getNumTries(); i++) {
      result = null;
      BufferedInputStream reader = null;
      try {
        log.info("Attempt#" + (i + 1) + " to load from: " + urlStr);
                        
        URL url = new URL(urlStr);
        HttpURLConnection connection = null;
        if (siteConfiguration.getProxy() != null) {
          connection = (HttpURLConnection) url.openConnection(siteConfiguration.getProxy());
        } else {
          connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2" );        
        //connection.setRequestProperty(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);        
        //connection.setUseCaches(true);
        if (siteConfiguration.getReferer() != null && siteConfiguration.getReferer().length() > 0) {
          connection.setRequestProperty("Referer", siteConfiguration.getReferer());
          log.info("setting referer: " + siteConfiguration.getReferer());          
        }
        if (siteConfiguration.getCookie() != null && siteConfiguration.getCookie().length() > 0) {
          connection.setRequestProperty("Cookie", siteConfiguration.getCookie());          
          log.info("setting cookie: " + siteConfiguration.getCookie());          
        }        
        //connection.setConnectTimeout(10000);
        connection.setReadTimeout(siteConfiguration.getTimeoutSec()*1000);
        
        reader = new BufferedInputStream(connection.getInputStream());
        int index = 0;
        int count = 0;
        byte[] buf = new byte[1000];
        while ((count = reader.read(buf, 0, buf.length)) != -1) {
          byte[] tempBuf = result; 
          result = new byte[index+count];
          if (tempBuf != null) {
            System.arraycopy(tempBuf, 0, result, 0, tempBuf.length);
          }
          System.arraycopy(buf, 0, result, index, count);
          index += count;
        }
        
/*        
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));
        String line;
        while ((line = reader.readLine()) != null) {
          if (responseHtml == null) {
            responseHtml = line;
          } else {
            responseHtml += "\n" + line;
          }
        }
*/            
        log.info("Completed loading page from: " + urlStr);
        break;
      } catch (IOException ex) {
        result = null;
        log.log(Level.INFO, "Error loading book data from " + urlStr, ex);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      } catch (Exception ex) {
        result = null;
        log.log(Level.WARNING, "Error loading book data from " + urlStr, ex);
        break;
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (Exception ex) {
          }
        }
      }
    }

    return result;
  }
    
}
