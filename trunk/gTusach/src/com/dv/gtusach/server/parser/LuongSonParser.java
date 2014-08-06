package com.dv.gtusach.server.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;

public class LuongSonParser extends BookParser {

  public LuongSonParser() {
    super();
  }
  
  @Override
  public String getDomainName() {
    return "luongson";
  }

  public boolean checkSupport(String url) {
    return (url.indexOf("luongson") != -1 || url.indexOf("lsb-thuquan") != -1);
  }    
  
  @Override
  public String executeRequest(String target, String request) {
    siteConfiguration.setReferer("http://www.luongsonbac.com/luongsonbac.phtml?" + request);    
    return super.executeRequest(target, request);
  }  
  
  @Override
  public String getNextPageUrl(String targetURL, String currentPageURL, String html) {  
    String result = null;
    
    String expectedNextPageURL = null;
    int currentPageNo = 1;
    int index1 = currentPageURL.indexOf("&page=");
    if (index1 != -1) {
      int index2 = currentPageURL.indexOf("&", index1+1);
      if (index2 != -1) {
        String s = currentPageURL.substring(index1+"&page=".length(), index2);
        currentPageNo = Integer.parseInt(s);
        expectedNextPageURL = currentPageURL.substring(0, index1) 
            + "&page=" + (currentPageNo + 1) + currentPageURL.substring(index2);
      }
    }
    if (expectedNextPageURL == null) {
      expectedNextPageURL = currentPageURL + "&page=2"; 
    }
    
    index1 = expectedNextPageURL.indexOf("www.luongsonbac.com");
    if (index1 == -1) {
      index1 = expectedNextPageURL.indexOf("www.lsb-thuquan.com");
    }
    if (index1 != -1) {
      expectedNextPageURL = expectedNextPageURL.substring(index1, "www.luongsonbac.com".length());
    }
    
    log.info("Current page no: " + currentPageNo + ", expected next page: " + expectedNextPageURL);    
    Document doc = Jsoup.parse(html);
    Elements links = doc.getElementsByTag("a");
    for (int i=links.size()-1; i>=0; i--) {
      Element link = links.get(i);
      //log.info("found link: " + link.text() + ", class=" + link.className());
      if (link.attr("href").indexOf(expectedNextPageURL) != -1) {
        result = link.attr("href");
        break;
      }
    }
        
    return result;
  }

  
  @Override
  public ChapterHtml extractChapterHtml(String targetStr, String requestStr, String book) {
    // extract data from <body> element
    Document doc = Jsoup.parse(book);
    Elements list = doc.select("div.maincontent");
    String textStr = "";
    for (Element elm: list) {
      String chapterText = "";
      for (TextNode textNode: elm.textNodes()) {
        chapterText += textNode.getWholeText() + "<br/>";
      } 
      if (chapterText.length() > 500) {
        //log.info("found chapter text: [" + chapterText + "]");
        textStr += chapterText + "<br/>";
        break;
      }
    }
    
    //String result = null;
    if (textStr != null) {
      ChapterHtml chapterHtml = new ChapterHtml();
      int index = bookTemplate.indexOf("</body>");
      chapterHtml.setHtml(bookTemplate.substring(0, index-1) + textStr + "</body></html>");
    }
    return null;
  }  

  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    // TODO Auto-generated method stub
    return super.getChapterTitle(rawHtml, formatHtml);
  }

}
