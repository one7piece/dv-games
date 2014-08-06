package com.dv.gtusach.server.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;

public class TruyenHixxParser extends BookParser {

  public TruyenHixxParser() {
    super();
  }
  
  @Override
  public String getDomainName() {
    return "truyen.hixx.info";
  }

  public boolean checkSupport(String url) {
    return (url.indexOf("hixx") != -1);
  }    
    
  @Override
  public String getNextPageUrl(String targetURL, String currentPageURL, String html) {
    String result = null;
    int index = html.indexOf("function page_next()");
    if (index != -1) {
      StringBuffer buf = new StringBuffer(html.length());
      int index1 = html.indexOf("http://", index);
      int index2 = html.indexOf("}", index);
      if (index1 != -1 && index1 < index2) {
        for (int j=index1; j<index2; j++) {
          char c = html.charAt(j);
          if (c != '+' && c != '\'' && c != ' ' && c != ';') {
            buf.append(c);
          }
          if (c == ';') {
            break;
          }
        }
      }
      result = buf.toString();
    }      
    
    return result;
  }

  
  @Override
  public ChapterHtml extractChapterHtml(String targetStr, String requestStr, String book) {
    // extract data from <body> element
    Document doc = Jsoup.parse(book);
    Element body = doc.getElementsByTag("body").first();
    Elements list = body.getElementsByTag("td");
    String textStr = "";
    for (int i=list.size()-1; i>=0; i--) {
      Element elm = list.get(i);
      if (elm.className().equals("chi_tiet")) {        
        for (TextNode child: elm.textNodes()) {
          //log.info("found text node: [" + child.outerHtml() + "]");
          textStr += child.getWholeText() + "<br/>";
        }        
        break;
      }
    }
    ChapterHtml chapterHtml = null;
    if (textStr != null) {
      chapterHtml = new ChapterHtml();
      int index = bookTemplate.indexOf("</body>");
      chapterHtml.setHtml(bookTemplate.substring(0, index-1) + textStr + "</body></html>");           
    }
    
    return chapterHtml;
  }  

  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    String result = "";
    if (formatHtml != null && formatHtml.length() > 500) {
      try {
        int index = rawHtml.indexOf("class=\"chi_tiet\"");
        List<String> htmlList = new ArrayList<String>();
        htmlList.add(formatHtml.substring(0, formatHtml.length() > 1000 ? 1000 : formatHtml.length()));
        if (index != -1) {
          if (rawHtml.length() > (index + 500)) {
            htmlList.add(rawHtml.substring(0, index+500));
          } else {
            htmlList.add(rawHtml.substring(0, index));
          }
        } 
                        
        for (int i=0; i<htmlList.size() && result.length() == 0; i++) {
          String html = htmlList.get(i);
          for (String prefix: all_PREFIXES) {
            String regex = prefix + "\\s*\\d+";
            String title = findChapterTitle(html, regex);
            if (title.length() > 0) {
              result = title;
              break;
            }
          }          
        }
      } catch (Exception ex) {
        log.log(Level.WARNING, "Error getting chapter title", ex);
      }
      log.info("Found chapter title: [" + result + "]");
    }
    return result;
  }  

}
