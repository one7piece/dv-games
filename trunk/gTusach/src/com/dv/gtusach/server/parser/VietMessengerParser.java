package com.dv.gtusach.server.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;
import com.dv.gtusach.shared.BadDataException;

public class VietMessengerParser extends BookParser {

  public VietMessengerParser() {
    super();
    siteConfiguration.setCookie("JPLUG=; WIDTH=1600; HEIGHT=900; gotCookies=Yes; _sm_au_c=iVVWSW1VPZVZPFFq11; REFERER=http://vietmessenger.com/books/?title%3Dtheogiong; STAT=Active; MEMBERID=one7piece; ONLINE=one7piece; GENDER=Male; AGE=30-39; COUNTRY=Australia; VIP=1; gotCookies=Yes; WIDTH=1600");
    //Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36
  }
  
  @Override
  public String getDomainName() {
    return "vietmessenger";
  }

  public boolean checkSupport(String url) {
    return (url.indexOf("vietmessenger") != -1);
  }    
    
  @Override
  public String getNextPageUrl(String targetURL, String currentPageURL, String html) {
    String result = null;
    Document doc = Jsoup.parse(html);
    Element body = doc.getElementsByTag("body").first();    
    Elements list = body.getElementsByTag("a");
    for (int i=0; i<list.size(); i++) {
      Element elm = list.get(i);
      Elements children = elm.getElementsByAttributeValueEnding("src", "next1.gif");      
      if (children != null && children.size() == 1) {
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
  public ChapterHtml extractChapterHtml(String targetStr, String requestStr, String book) throws BadDataException {
    // extract data from <body> element
    Document doc = Jsoup.parse(book);
    Element body = doc.getElementsByTag("body").first();
    Elements list = body.getElementsByTag("td");
    String textStr = "";
    for (int i=list.size()-1; i>=0; i--) {
      Element elm = list.get(i);
      if (elm.className().equals("hfont")) {
        textStr = elm.html();
/*        
        rawHtml = rawHtml.replaceAll("</br\\s*>", "");
        rawHtml = rawHtml.replaceAll("</p\\s*>", "");
        rawHtml = rawHtml.replaceAll("<br.*>", "br2n");
        rawHtml = rawHtml.replaceAll("<p.*>", "br2n");        
        textStr = Jsoup.parse(rawHtml).text().replaceAll("br2n", "\n");
*/        
        break;
      }
    }
    ChapterHtml chapterHtml = null;
    if (textStr != null) {
      chapterHtml = new ChapterHtml();
      int index = bookTemplate.indexOf("</body>");
      chapterHtml.setHtml(bookTemplate.substring(0, index-1) + textStr + "</body></html>");           
    }
    
    if (!isValidChapterHtml(chapterHtml)) {
    	throw new BadDataException("No chapter content found in html");
    }
    
    return chapterHtml;
  }  

  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    return " ";
  }  

}
