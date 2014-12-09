
importPackage(Packages.java.util);
importPackage(Packages.com.dv.gtusach.server.common);
importPackage(Packages.org.jsoup);
importPackage(Packages.org.jsoup.nodes);
importPackage(Packages.org.jsoup.select);

function getBatchSize() {
	return 100;
}

function getDelayTimeSec() {
	return 10;
}

function getChapterTitle(rawHtml, formatHtml) {
  var result = "";
  if (formatHtml != null && formatHtml.length > 500) {
    try {
      var index = rawHtml.indexOf("chi_tiet");
      var htmlList = new Array();
      if (index != -1) {
        if (index > 500) {
          htmlList.push(rawHtml.substring(index-500, index));
        } else {
          htmlList.push(rawHtml.substring(0, index));
        }        
      } else {
        htmlList.push(formatHtml.substring(0, formatHtml.length > 1000 ? 1000 : formatHtml.length));
      }
                      
      for (var i=0; i<htmlList.length && result.length == 0; i++) {
        var html = htmlList[i];
        for (var j=0; j<all_PREFIXES.length; j++) {
        	var prefix = all_PREFIXES[j];
          var regex = prefix + "\\s*\\d+";
          var title = findChapterTitle(html, regex);
          if (title.length > 0) {
            result = title;
            break;
          }
        }          
      }
    } catch (err) {
      logError("Error getting chapter title: " + err);
    }
    logInfo("Found chapter title: [" + result + "]");
  }
  return result;
}

function getNextPageUrl(target, currentPageURL, html) {  
  var result = null;
  var index = html.indexOf("function page_next()");
  if (index != -1) {
    var buf = "";
    var index1 = html.indexOf("http://", index);
    var index2 = html.indexOf("}", index);
    if (index1 != -1 && index1 < index2) {
      for (var j=index1; j<index2; j++) {
        var c = html.charAt(j);
        if (c != '+' && c != '\'' && c != ' ' && c != ';') {
          buf += c;
        }
        if (c == ';') {
          break;
        }
      }
    }
    result = buf;
  }      
  
  logInfo("found next page url: " + result + ", current page URL: " + currentPageURL);
  if (result != null && currentPageURL != null 
  		&& (result.indexOf(currentPageURL) != -1 || currentPageURL.indexOf(result) != -1)) {
    logError("Bad html causing infinite loop!!!\n");
    result = null;
  } 
  
  return result;
}

function extractChapterHtml(targetStr, requestStr, book) {  
  // extract data from <body> element
  var doc = Jsoup.parse(book);
  var body = doc.getElementsByTag("body").first();
  var list = body.getElementsByTag("td");
  var textStr = "";
  for (var i=list.size()-1; i>=0; i--) {
    var elm = list.get(i);
    if (elm.className().equals("chi_tiet")) {        
      for (var j=0; j<elm.textNodes().size(); j++) {
      	var child = elm.textNodes().get(j);
        //logInfo("found text node: [" + child.outerHtml() + "]");
        textStr += child.getWholeText() + "<br/>";
      }        
      break;
    }
  }
  
  var chapterHtml = null;
  if (textStr != null) {
    chapterHtml = new ChapterHtml();
    var index = context.getBookTemplate().indexOf("</body>");
    chapterHtml.setHtml(context.getBookTemplate().substring(0, index-1) + textStr + "</body></html>");           
  }
  if (!isValidChapterHtml(chapterHtml)) {
  	throw new BadDataException("No chapter content found in html");
  }
  
  return chapterHtml;
}


