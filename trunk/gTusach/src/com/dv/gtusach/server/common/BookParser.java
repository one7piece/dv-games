package com.dv.gtusach.server.common;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.dv.gtusach.shared.BadDataException;

public abstract class BookParser {
  protected static final Logger log = Logger.getLogger(BookParser.class.getCanonicalName());
  
  static String[] DOMAIN_NAMES = {".com/", ".name/", ".info/", ".org/", ".vn/"};
  protected static String Chuong_PREFIX = "\u0043\u0068\u01B0\u01A1\u006E\u0067"; // Chuong
  protected static String CHUONG_PREFIX = "\u0043\u0048\u01AF\u01A0\u004E\u0047"; // CHUONG 
  protected static String chuong_PREFIX = "\u0063\u0068\u01B0\u01A1\u006E\u0067"; // chuong

  protected static String Quyen_PREFIX = "\u0051\u0075\u0079\u1EC3\u006E"; // Quyển
  protected static String QUYEN_PREFIX = "\u0051\u0055\u0059\u1EC2\u004E"; // QUYỂN  
  protected static String quyen_PREFIX = "\u0071\u0075\u0079\u1EC3\u006E"; // quyển
  
  protected static List<String> chuong_PREFIXES = new ArrayList<String>();
  protected static List<String> quyen_PREFIXES = new ArrayList<String>();
  protected static List<String> all_PREFIXES = new ArrayList<String>();
  static {
    try {
      chuong_PREFIXES.add(new String(Chuong_PREFIX.getBytes("UTF-8"), "UTF-8"));
      chuong_PREFIXES.add(new String(CHUONG_PREFIX.getBytes("UTF-8"), "UTF-8"));
      
      quyen_PREFIXES.add(new String(Quyen_PREFIX.getBytes("UTF-8"), "UTF-8"));
      quyen_PREFIXES.add(new String(QUYEN_PREFIX.getBytes("UTF-8"), "UTF-8"));  
      
      all_PREFIXES.addAll(chuong_PREFIXES);
      all_PREFIXES.addAll(quyen_PREFIXES);
      all_PREFIXES.add("Chuong");
      all_PREFIXES.add("CHUONG");
    } catch (Exception ex) {      
    }
  }
  
  protected HttpService httpService;
  protected SiteConfiguration siteConfiguration;
  protected String bookTemplate = "";
  
  public BookParser() {
    siteConfiguration = new SiteConfiguration();
    //siteConfiguration.setProxy(getProxy());
    httpService = new HttpService();
  }
  
  public List<AttachmentData> createAttachments(String href, byte[] imageData) {        
    List<AttachmentData> result = new ArrayList<AttachmentData>();
    
/*  
  	// break image into into multiple pages
    final int MAX_IMG_HEIGHT = 700;    
    try {
      int extIndex = href.lastIndexOf(".");
      final BufferedImage image = Sanselan.getBufferedImage(imageData);
      if (image.getHeight() > MAX_IMG_HEIGHT) {
        int i = 0;
        while (i*MAX_IMG_HEIGHT < image.getHeight()) {
          int h = MAX_IMG_HEIGHT;
          if (i*MAX_IMG_HEIGHT + MAX_IMG_HEIGHT > image.getHeight()) {
            h = image.getHeight() - i*MAX_IMG_HEIGHT; 
          }
          BufferedImage newImage = new BufferedImage(image.getWidth(), h, image.getType());
          Graphics2D g2 = newImage.createGraphics();
          g2.drawImage(image, 0, 0, image.getWidth(), h, 
              0, i*MAX_IMG_HEIGHT, image.getWidth(), i*MAX_IMG_HEIGHT+h, null);

          String newHref = href.substring(0, extIndex) + "_" + (i+1) + ".gif";
          result.add(new AttachmentData(newHref, Sanselan.writeImageToBytes(newImage, ImageFormat.IMAGE_FORMAT_GIF, null)));
          g2.dispose();
          i++;          
        }
      }            
    } catch (Exception e) {
      log.log(Level.WARNING, "Error splitting chapter image.", e);
    }
*/    
    if (result.size() == 0) {
      result.add(new AttachmentData(href, imageData));
    }    
    return result;    
  }
  
  public String getUrl(String target, String request) {
    return httpService.getUrl(target, request);
  }
  
  public byte[] loadResource(String url) {
    return httpService.executeRequest(url, siteConfiguration);
  }
  
  public String executeRequest(String target, String request) {
    return httpService.executeRequestStr(target, request, siteConfiguration);
  }
  
  public SiteConfiguration getSiteConfiguration() {
    return siteConfiguration;
  }
      
  public String getBookTemplate() {
    return bookTemplate;
  }
  public void setBookTemplate(String bookTemplate) {
    this.bookTemplate = bookTemplate;
  }
  
  public boolean checkSupport(String url) {
    return (url.indexOf(getDomainName()) != -1);
  }
  
  public String getTargetUrl(String pUrl) {
    for (String name: DOMAIN_NAMES) {
      int index = pUrl.toLowerCase().indexOf(name);
      if (index != -1) {
        if (pUrl.toLowerCase().startsWith("http://")) {
          return pUrl.substring("http://".length(), index+name.length());
        }
        return pUrl.substring(0, index+name.length());        
      }
    }
    return null;
  }
  
  public String getRequestUrl(String pUrl) {
    String target = getTargetUrl(pUrl);
    if (target != null) {
      if (target.equalsIgnoreCase(pUrl)) {
        return "/";
      }
      int index = pUrl.toLowerCase().indexOf(target.toLowerCase());
      if (index != -1) {
        return pUrl.substring(index+target.length());
      }
    }
    return null;
  }
  
  public abstract String getDomainName();
  
  public abstract ChapterHtml extractChapterHtml(String target, String request, String rawChapterHtml) throws BadDataException;
  
  public abstract String getNextPageUrl(String target, String currentPageURL, String rawChapterHtml);
  
  protected String findChapterTitle(String html, String regex) {
    String title = "";
    Matcher matcher = Pattern.compile(regex).matcher(html);
    if (matcher.find()) {
      int index0 = matcher.start();
      //log.info("Found chapter prefix: " + regex + " at index: " + index0);
      // find the first : after the chapter prefix
      int index1 = html.indexOf(":", index0);
      //log.info("Found chapter prefix: " + regex + " at index0=" + index0
        //  + ", index1=" + index1);
      if (index1 != -1 && index1-index0 <= 20) {
        int index2 = html.indexOf("<", index1);
        if (index2 - index1 < 150) {
          title = html.substring(index0, index2);
        } else {
          log.info("Cannot find < after chapter title!");
        }
      }      
    } else {
      //log.info("Not found chapter prefix: " + regex);
    }
    return title;
  }
  
  protected String getChapterTitle(String rawHtml, String formatHtml) {
    String result = "";
    try {
      String html = formatHtml.substring(0, 1000);
      for (String prefix: all_PREFIXES) {
        String regex = prefix + "\\s*\\d+";
        String title = findChapterTitle(html, regex);
        if (title.length() > 0) {
          result = title;
          break;
        }
      }
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error getting chapter title.", ex);
    }
    log.log(Level.INFO, "Found chapter title: [" + result + "]");
    return result;
  }
  
  public boolean isValidChapterHtml(ChapterHtml chapterHtml) {
    return (chapterHtml != null && chapterHtml.getHtml() != null 
        && (chapterHtml.getHtml().length() > 300 || chapterHtml.getAttachments().size() > 0));
  }
    
  protected void extractNodeText(Node node, StringBuffer buffer) {
  	if (node instanceof TextNode) {
  		buffer.append(((TextNode)node).getWholeText() + "<br/>");
  	} else {
  		for (Node child: node.childNodes()) {
  			extractNodeText(child, buffer);
  		}
  	}
  }
  
  public Proxy getProxy() {
    Proxy proxy = null;
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      if (hostname.toLowerCase().startsWith("anz")) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("Zen.tyco.com.au", 9400));
        // proxy = new HttpHost("Zen.tyco.com.au", 9400, "http");
        Authenticator authenticator = new Authenticator() {
          public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication("dvan", "Spidey52".toCharArray()));
          }
        };
        Authenticator.setDefault(authenticator);       
      }        
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error creating proxy", ex);
    }
    return proxy;
  }
}
