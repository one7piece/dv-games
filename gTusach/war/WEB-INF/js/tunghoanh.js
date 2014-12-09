
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
	return getDefaultChapterTitle(rawHtml, formatHtml);
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
  var bookText = rawChapterHtml;
  if (request.indexOf("/chapter/") == -1) {
    var marker = "FetchChapter(\"";    
    var index = rawChapterHtml.indexOf(marker);    
    if (index != -1) {      
      var index1 = index + marker.length;
      var index2 = rawChapterHtml.indexOf("\"", index1);
      var chapterId = rawChapterHtml.substring(index1, index2);      
      logInfo("Found chapter id: " + chapterId);
      if (!chapterId.toLowerCase().endsWith(".html")) {
        chapterId += ".html";
      }
      bookText = context.executeRequest(target, "/chapter/" + chapterId);
      if (bookText == null || bookText.trim().length == 0) {
        throwError("Failed to load request: " + "/chapter/" + chapterId);
      }        
    } else {
    	throwError("Cannot find FetchChapter marker in:\n" + rawChapterHtml);
    }
  }
  
  // get the html between the body tags
  var index = bookText.indexOf("<body>");
  var index2 = bookText.indexOf("</body>");
  if (index != -1 && index2 != -1) {
    bookText = bookText.substring(index+"<body>".length, index2);
  }
  
  index = context.getBookTemplate().indexOf("</body>");
  var formatChapterHtml = context.getBookTemplate().substring(0, index-1) + bookText + "</body></html>";
  var chapterHtml = new ChapterHtml();
  
  // check for chapter with image
  if (bookText.length < 2000) {
  	logInfo("chapter content is too small, attempt to extract image chapter...");
    var doc = Jsoup.parse(formatChapterHtml);
    var body = doc.getElementsByTag("body").first();    
    var list = body.getElementsByTag("img");
    for (var i=0; i<list.size(); i++) {
      var elm = list.get(i);
      var src = elm.attr("src");
      if (src != null && src.indexOf("/chapter/") != -1) {          
        var image = context.loadResource(src);
        if (image != null && image.length > 0) {
          //String srcHtml = elm.outerHtml();
          logInfo("Found chapter image: " + src);
          // split the image            
          var href = src.substring(src.indexOf("/chapter/") + "/chapter/".length);            
          var attachments = createAttachments(href, image);            
          chapterHtml.getAttachments().addAll(attachments);
          var newHtml = "";
          for (var j=0; j<attachments.size(); j++) {
          	var att = attachments.get(j);
            if (newHtml.length > 0) {
              newHtml += "<br/>";                
            }
            newHtml += "<div class=\"chapterImage\"><img src=\"" + att.getHref() + "\"</div>";
          }
          var index1 = formatChapterHtml.indexOf(src);
          if (index1 != -1) {
            index1 = formatChapterHtml.lastIndexOf("<img", index1);
            index2 = formatChapterHtml.indexOf(">", index1);
            var oldHtml = formatChapterHtml.substring(index1, index2+1);
            formatChapterHtml = formatChapterHtml.replace(oldHtml, newHtml);
          } else {
            logError("Cannot find '" + src + "'");
          }
        } else {
          logError("Failed to load image resource: " + src);
        }
      }
    }
  } 
  chapterHtml.setHtml(formatChapterHtml);
  //logInfo("\n############ Chapter HTML #####################(" + chapterHtml.getHtml().length() + ")\n" 
  //		+ chapterHtml.getHtml() 
  //		+ "\n#################################################\n");
  
  if (!isValidChapterHtml(chapterHtml)) {
  	throwError("No chapter content found in html");
  }
  
  //log.info("Chapter data:\n----------------------\n" + result + "\n----------------\n");
  return chapterHtml;    
}

function createAttachments(href, imageData) {        
  var result = new ArrayList();  
  result.add(new AttachmentData(href, imageData));
  return result;    
}

