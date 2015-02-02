importPackage(Packages.java.util);
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
  try {
  	var index = rawHtml.indexOf("title=\"Băng Hỏa Phá Phôi Thần\"");
  	if (index != -1) {
      var html = rawHtml.substring(index, index+200);    		
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
  } catch (ex) {
    logError("Error getting chapter title. " + ex);
  }
  logInfo("Found chapter title: [" + result + "]");
  return result;
}

function getNextPageUrl(target, currentPageURL, rawChapterHtml) {  
  var result = null;
  var doc = Jsoup.parse(rawChapterHtml);
  var body = doc.getElementsByTag("body").first();    
  var list = body.getElementsByTag("a");
  for (var i=0; i<list.size(); i++) {
    var elm = list.get(i);      
    if (elm.className().equals("next")) {
      //logInfo("found a: " + elm.html());
      result = elm.attr("href");
      break;
    }
  }
  logInfo("found next page url: " + result + ", current page URL: " + currentPageURL);
  if (result != null && currentPageURL != null 
  		&& (result.indexOf(currentPageURL) != -1 || currentPageURL.indexOf(result) != -1)) {
    logInfo("Bad html causing infinite loop!!!\n");
    result = null;
  } 
  return result;
}


function extractChapterHtml(target, request, rawChapterHtml) {
  // extract data from <body> element
  var doc = Jsoup.parse(rawChapterHtml);
  var body = doc.getElementsByTag("body").first();  
  var list = body.select("div#chapter_content");
  if (list == null || list.size() == 0) {
  	throwError("Cannot find chapter marker: div#chapter_content");
  }
  
  var buffer = new java.lang.StringBuilder(5000);
  for (var i=0; i<list.size(); i++) {  	
  	extractNodeText(list.get(i), buffer);
  	//log.info("found chapter text: [" + buffer.toString() + "]");
  }
  var chapterHtml = null;
  //note that toString() return an instance of java.lang.String
  var bufferStr = String(buffer.toString());	
  if (bufferStr.length > 0) {
    chapterHtml = new ChapterHtml();
    var index = context.getBookTemplate().indexOf("</body>");
    chapterHtml.setHtml(context.getBookTemplate().substring(0, index-1) + bufferStr + "</body></html>");           
  }
  
  if (!isValidChapterHtml(chapterHtml)) {
  	throwError("No chapter content found in html");
  }
         
  //log.info("Chapter data:\n----------------------\n" + chapterHtml + "\n----------------\n");
  return chapterHtml;
}

function extractNodeText(node, buffer) {
	if (node instanceof TextNode) {
		var text = node.getWholeText();
		if (text.toLowerCase().indexOf("ads by google") == -1) {
  		buffer.append(text + "<br/>");
		}
	} else {
		for (var i=0; i<node.childNodes().size(); i++) {
			var child = node.childNodes().get(i);		
			extractNodeText(child, buffer);
		}
	}
}