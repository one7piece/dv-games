package com.dv.gtusach.server.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dv.gtusach.server.common.BookParser;
import com.dv.gtusach.server.common.ChapterHtml;
import com.dv.gtusach.shared.BadDataException;

public class TangThuVienParser extends BookParser {
	private String chapterTitle;
	private String rawChapterHtml;
	
  public TangThuVienParser() {
    super();
  }
  
  @Override
  public String getDomainName() {
    return "tangthuvien.vn";
  }

  public boolean checkSupport(String url) {
    return (url.indexOf("tangthuvien") != -1);
  }    
    
  @Override
  public String getNextPageUrl(String targetURL, String currentPageURL, String html) {
    String result = null;
    Document doc = Jsoup.parse(html);
    Element body = doc.getElementsByTag("body").first();
    Elements list = body.getElementsByTag("a");
    for (int i=0; i<list.size(); i++) {
      Element elm = list.get(i);
      if (elm.attr("rel").equals("next")) {
      	result = elm.attr("href");
      	break;
      }
    }
    if (result != null) {
    	if (result.startsWith("showthread")) {
    		result = "forum/" + result;
    	} else if (result.startsWith("/showthread")) {
    		result = "/forum" + result;    		
    	}
    }
    return result;
  }

  
  @Override
  public ChapterHtml extractChapterHtml(String targetStr, String requestStr, String html) throws BadDataException {
  	this.chapterTitle = "";
  	this.rawChapterHtml = html;
  	String title1 = "";
  	String title2 = "";
    String textStr = "";  	
  	
    int index = 0;
    int i, j;
  	while ((index = html.indexOf("post_message_", index)) != -1) {
  		i = html.indexOf("class=\"hiddentext\"", index);
  		index++;
  		int index2 = html.indexOf("post_message_", index);
  		if (index2 == -1) {
  			index2 = index + 5000;
  		}
  		if ((i != -1) && (i < index2)) {
  			i = html.indexOf(">", i);
  			j = html.indexOf("</div>", i);
				if (j != -1) {
  				textStr += html.substring(i+1, j);  						
				} else {
					break;
				}
  			// get the title
				j = html.indexOf("<", i);  			
  			if (j != -1 && j - i < 100) {
  				String str = html.substring(i+1, j);
  				if (str.indexOf(":") != -1) {
    				if (title1.length() == 0) {
    					title1 = html.substring(i+1, j);
    				} else {
    					title2 = html.substring(i+1, j);
    				}  				  				
  				}
  			} 
  		} else {
  			continue;
  		}
  	} 
  	this.chapterTitle = title1 + "/" + title2;
  	if (this.chapterTitle.length() > 64) {
  		this.chapterTitle = chapterTitle.substring(0, 64);
  	}
  	
    ChapterHtml chapterHtml = null;
    if (textStr.length() > 0) {
      chapterHtml = new ChapterHtml();
      index = bookTemplate.indexOf("</body>");
      chapterHtml.setHtml(bookTemplate.substring(0, index-1) + textStr + "</body></html>");           
    }
    
    if (!isValidChapterHtml(chapterHtml)) {
    	throw new BadDataException("No chapter content found in html");
    }
    
    return chapterHtml;
  }  

  @Override
  public String getChapterTitle(String rawHtml, String formatHtml) {
    if (rawHtml.length() == rawChapterHtml.length()) {
    	return this.chapterTitle;
    }
    return "";
  }  

}
