package com.dv.gtusach.server.parser;

import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;
import com.dv.gtusach.shared.BadDataException;

public class TruyenyyParser extends BookParser {

  public TruyenyyParser() {
    super();
    //siteConfiguration.setCookie("location.href=1; PHPSESSID=soaolelru0jpgj9qca02v25rc2; ann_show=1; __RC=5; __R=3; ver=; _sm_au_c=iVVPTtMPs615vPnr0d; ad_play_index=38; __utma=180264865.723063477.1358501601.1360277004.1360279684.15; __utmb=180264865.6.10.1360279684; __utmc=180264865; __utmz=180264865.1360279684.15.9.utmcsr=tunghoanh.com|utmccn=(referral)|utmcmd=referral|utmcct=/; _azs=; cpcSelfServ=");
    //siteConfiguration.setTimeoutSec(20);
  }
  
  @Override
  public String getDomainName() {
    return "truyenyy";
  }
    
  @Override
  public String getNextPageUrl(String target, String currentPageURL, String rawChapterHtml) {  
    String result = null;
    Document doc = Jsoup.parse(rawChapterHtml);
    Elements list = doc.select("div.mobi-chuyentrang");
    if (list.size() > 0) {    	    
      Element elm = list.get(0);
      Elements refs = elm.select("a[href]");
      for (Element ref: refs) {
        //log.info("getNextPageUrl() - found href: " + ref.attr("href") + ", text:" +  ref.text());
      	if (ref.text().trim().toLowerCase().equals("sau")) {
        	result = ref.attr("href");
      		break;
      	}
      }      
    } else {
    	log.warning("getNextPageUrl() - Could not find next page marker: div.mobi-chuyentrang");
    }
    
    log.info("found next page url: " + result + ", current page URL: " + currentPageURL);
    if (result != null && currentPageURL != null && result.equals(currentPageURL)) {
      log.info("Bad html causing infinite loop!!!\n");
      result = null;
    } 
    return result;
  }

  
  @Override
  public ChapterHtml extractChapterHtml(String target, String request, String rawChapterHtml) throws BadDataException {
    // extract data from <body> element
    Document doc = Jsoup.parse(rawChapterHtml);
    Elements list = doc.select("div#id_noidung_chuong");
    if (list == null || list.size() == 0) {
    	throw new BadDataException("Cannot find chapter marker: div#id_noidung_chuong");
    }
    StringBuffer buffer = new StringBuffer(5000);
    for (Element elm: list) {   
    	extractNodeText(elm, buffer);
    	//log.info("found chapter text: [" + buffer.toString() + "]");
    }
    ChapterHtml chapterHtml = null;
    if (buffer.length() > 0) {
      chapterHtml = new ChapterHtml();
      int index = bookTemplate.indexOf("</body>");
      chapterHtml.setHtml(bookTemplate.substring(0, index-1) + buffer.toString() + "</body></html>");           
    }
    
    if (!isValidChapterHtml(chapterHtml)) {
    	throw new BadDataException("No chapter content found in html");
    }
                   
    //log.info("Chapter data:\n----------------------\n" + chapterHtml + "\n----------------\n");
    return chapterHtml;
  }
  
  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    String result = "";
    try {
    	int index = rawHtml.indexOf("id_noidung_chuong");
    	if (index > 300) {
        String html = rawHtml.substring(index-300, index);    		
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
      log.log(Level.WARNING, "Error getting chapter title.", ex);
    }
    log.log(Level.INFO, "Found chapter title: [" + result + "]");
    return result;
  }
  
  @Override
  protected void extractNodeText(Node node, StringBuffer buffer) {
  	if (node instanceof TextNode) {
  		String text = ((TextNode)node).getWholeText();
  		if (text.toLowerCase().indexOf("ads by google") == -1) {
    		buffer.append(text + "<br/>");
  		}
  	} else {
  		for (Node child: node.childNodes()) {
  			extractNodeText(child, buffer);
  		}
  	}
  }
  
}
