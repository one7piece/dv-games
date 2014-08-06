package com.dv.gtusach.server.parser;

import java.util.List;
import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.AttachmentData;
import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;

public class TungHoanhParser extends BookParser {

  public TungHoanhParser() {
    super();
    siteConfiguration.setCookie("location.href=1; PHPSESSID=soaolelru0jpgj9qca02v25rc2; ann_show=1; __RC=5; __R=3; ver=; _sm_au_c=iVVPTtMPs615vPnr0d; ad_play_index=38; __utma=180264865.723063477.1358501601.1360277004.1360279684.15; __utmb=180264865.6.10.1360279684; __utmc=180264865; __utmz=180264865.1360279684.15.9.utmcsr=tunghoanh.com|utmccn=(referral)|utmcmd=referral|utmcct=/; _azs=; cpcSelfServ=");
    siteConfiguration.setTimeoutSec(20);
  }
  
  @Override
  public String getDomainName() {
    return "tunghoanh";
  }
    
  @Override
  public String getNextPageUrl(String target, String currentPageURL, String rawChapterHtml) {  
    String result = null;
    Document doc = Jsoup.parse(rawChapterHtml);
    Element body = doc.getElementsByTag("body").first();    
    Elements list = body.getElementsByTag("a");
    for (int i=0; i<list.size(); i++) {
      Element elm = list.get(i);      
      if (elm.className().equals("next")) {
        //log.info("found a: " + elm.html());
        result = elm.attr("href");
        break;
      }
    }
    log.info("found next page url: " + result + ", current page URL: " + currentPageURL);
    if (result != null && currentPageURL != null && result.equals(currentPageURL)) {
      log.info("Bad html causing infinite loop!!!\n");
      result = null;
    } 
    return result;
  }

  
  @Override
  public ChapterHtml extractChapterHtml(String target, String request, String rawChapterHtml) {
    
    String bookText = rawChapterHtml;
    if (request.indexOf("/chapter/") == -1) {
      String marker = "FetchChapter(\"";    
      int index = rawChapterHtml.indexOf(marker);    
      if (index != -1) {      
        int index1 = index + marker.length();
        int index2 = rawChapterHtml.indexOf("\"", index1);
        String chapterId = rawChapterHtml.substring(index1, index2);      
        log.info("Found chapter id: " + chapterId);
        if (!chapterId.toLowerCase().endsWith(".html")) {
          chapterId += ".html";
        }
        bookText = executeRequest(target, "/chapter/" + chapterId);
        if (bookText == null || bookText.trim().length() == 0) {
          throw new RuntimeException("Failed to load request: " + "/chapter/" + chapterId);
        }        
      } else {
        log.info("Cannot find FetchChapter marker in:\n" + rawChapterHtml);
        return null;
      }
    }
    
    // get the html between the body tags
    int index = bookText.indexOf("<body>");
    int index2 = bookText.indexOf("</body>");
    if (index != -1 && index2 != -1) {
      bookText = bookText.substring(index+"<body>".length(), index2);
    }
    
    index = bookTemplate.indexOf("</body>");
    String formatChapterHtml = bookTemplate.substring(0, index-1) + bookText + "</body></html>";
    ChapterHtml chapterHtml = new ChapterHtml();
    
    // check for chapter with image
    if (bookText.length() < 2000) {
      Document doc = Jsoup.parse(formatChapterHtml);
      Element body = doc.getElementsByTag("body").first();    
      Elements list = body.getElementsByTag("img");
      for (int i=0; i<list.size(); i++) {
        Element elm = list.get(i);
        String src = elm.attr("src");
        if (src != null && src.indexOf("/chapter/") != -1) {          
          byte[] image = loadResource(src);
          if (image != null && image.length > 0) {
            //String srcHtml = elm.outerHtml();
            log.info("Found chapter image: " + src);
            // split the image            
            String href = src.substring(src.indexOf("/chapter/") + "/chapter/".length());            
            List<AttachmentData> attachments = createAttachments(href, image);            
            chapterHtml.getAttachments().addAll(attachments);
            String newHtml = "";
            for (AttachmentData att: attachments) {
              if (newHtml.length() > 0) {
                newHtml += "<br/>";                
              }
              newHtml += "<div class=\"chapterImage\"><img src=\"" + att.getHref() + "\"</div>";
            }
            int index1 = formatChapterHtml.indexOf(src);
            if (index1 != -1) {
              index1 = formatChapterHtml.lastIndexOf("<img", index1);
              index2 = formatChapterHtml.indexOf(">", index1);
              String oldHtml = formatChapterHtml.substring(index1, index2+1);
              formatChapterHtml = formatChapterHtml.replace(oldHtml, newHtml);
            } else {
              log.log(Level.WARNING, "Cannot find '" + src + "'");
            }
          } else {
            log.log(Level.WARNING, "Failed to load image resource: " + src);
          }
        }
      }
    } 
    
    chapterHtml.setHtml(formatChapterHtml);
    //log.info("Chapter data:\n----------------------\n" + result + "\n----------------\n");
    return chapterHtml;    
  }

  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    // TODO Auto-generated method stub
    return super.getChapterTitle(rawHtml, formatHtml);
  }

}
