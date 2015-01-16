importPackage(Packages.java.util);
importPackage(Packages.com.dv.gtusach.server.common);
importPackage(Packages.org.jsoup);
importPackage(Packages.org.jsoup.nodes);
importPackage(Packages.org.jsoup.select);

var chapterTitle = "";
var rawChapterHtml = "";

function getBatchSize() {
	return 100;
}

function getDelayTimeSec() {
	return 10;
}

function getChapterTitle(rawHtml, formatHtml) {
  if (rawHtml.length() == rawChapterHtml.length()) {
    return chapterTitle;
  }
  return "";
}

function getNextPageUrl(targetURL, currentPageURL, html) { 
    var result = null;
    var doc = Jsoup.parse(html);
    var body = doc.getElementsByTag("body").first();
    var list = body.getElementsByTag("a");
    for (var i=0; i<list.size(); i++) {
      var elm = list.get(i);
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


function extractChapterHtml(targetStr, requestStr, html) {
  chapterTitle = "";
  rawChapterHtml = html;
  var title1 = "";
  var title2 = "";
  var textStr = "";  	
  
  var index = 0;
  var i, j;
  while ((index = html.indexOf("post_message_", index)) != -1) {
    i = html.indexOf("class=\"hiddentext\"", index);
    index++;
    var index2 = html.indexOf("post_message_", index);
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
        var str = html.substring(i+1, j);
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
  
  chapterTitle = title1 + "/" + title2;
  if (chapterTitle.length() > 64) {
    chapterTitle = chapterTitle.substring(0, 64);
  }
  
  var chapterHtml = null;
  if (textStr.length() > 0) {
    chapterHtml = new ChapterHtml();
    index = bookTemplate.indexOf("</body>");
    chapterHtml.setHtml(bookTemplate.substring(0, index-1) + textStr + "</body></html>");           
  }
  
  return chapterHtml;
}
