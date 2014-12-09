importPackage(Packages.java.util);
importPackage(Packages.com.dv.gtusach.server.common);
importPackage(Packages.org.jsoup);
importPackage(Packages.org.jsoup.nodes);
importPackage(Packages.org.jsoup.select);

function getBatchSize() {
	return 50;
}

function getDelayTimeSec() {
	return 125;
}

function getChapterTitle(rawHtml, formatHtml) {
  var result = "";
  try {
  	var index = rawHtml.indexOf("id_noidung_chuong");
  	if (index > 1000) {
      var html = rawHtml.substring(index-300, index);    		
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
  var list = doc.select("div.mobi-chuyentrang");
  if (list.size() > 0) {    	    
    var elm = list.get(0);
    var refs = elm.select("a[href]");
    for (var i=0; i<refs.size(); i++) {
    	var ref = refs.get(i);
      //log.info("getNextPageUrl() - found href: " + ref.attr("href") + ", text:" +  ref.text());
    	if (ref.text().trim().toLowerCase().equals("sau")) {
      	result = ref.attr("href");
    		break;
    	}
    }      
  } else {
  	logError("getNextPageUrl() - Could not find next page marker: div.mobi-chuyentrang");
  }
  
  logInfo("found next page url: " + result + ", current page URL: " + currentPageURL);
  if (result != null && currentPageURL != null) {
  	if (result.indexOf(currentPageURL) != -1 || currentPageURL.indexOf(result) != -1) {
	    logError("Bad html causing infinite loop!!!\n");
	    result = null;
  	}
		// check for same title
  	var index1a = currentPageURL.indexOf("doc-truyen/");
  	var index2a = result.indexOf("doc-truyen/");
  	if (index1a != -1 && index2a != -1) {
    	var index1b = currentPageURL.indexOf("/", index1a + "doc-truyen/".length);
    	var index2b = result.indexOf("/", index2a + "doc-truyen/".length);  		
    	if (index1b != -1 && index2b != -1 
    			&& currentPageURL.substring(index1a, index1b) != result.substring(index2a, index2b)) {
  	    logError("Bad next page URL, title mismatched!!! Dumped html:\n"
  	    		+ "-------------------------------------------------"
  	    		+ rawChapterHtml
  	    		+ "-------------------------------------------------\n");
  	    result = null;
    	}
  	}
  } 
  
  return result;
}


function extractChapterHtml(target, request, rawChapterHtml) {
  // extract data from <body> element
  var doc = Jsoup.parse(rawChapterHtml);
  var body = doc.getElementsByTag("body").first();  
  var list = body.select("div#id_noidung_chuong");
  if (list == null || list.size() == 0) {
  	throwError("Cannot find chapter marker: div#id_noidung_chuong");
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