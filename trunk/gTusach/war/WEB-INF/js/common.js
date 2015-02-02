importPackage(Packages.java.util.logging);
importPackage(Packages.com.dv.gtusach.server.common);
importPackage(Packages.com.dv.gtusach.shared);

var Chuong_PREFIX = "\u0043\u0068\u01B0\u01A1\u006E\u0067"; // Chuong
var CHUONG_PREFIX = "\u0043\u0048\u01AF\u01A0\u004E\u0047"; // CHUONG 
var chuong_PREFIX = "\u0063\u0068\u01B0\u01A1\u006E\u0067"; // chuong
var Quyen_PREFIX = "\u0051\u0075\u0079\u1EC3\u006E"; // Quyển
var QUYEN_PREFIX = "\u0051\u0055\u0059\u1EC2\u004E"; // QUYỂN  
var quyen_PREFIX = "\u0071\u0075\u0079\u1EC3\u006E"; // quyển
  
var chuong_PREFIXES = [Chuong_PREFIX, CHUONG_PREFIX, chuong_PREFIX, "Chuong", "CHUONG"];
var quyen_PREFIXES = [Quyen_PREFIX, QUYEN_PREFIX, quyen_PREFIX, "Quyen", "QUYEN"];
var all_PREFIXES = chuong_PREFIXES.concat(quyen_PREFIXES); 

String.prototype.endsWith = function(suffix) {
  return (this.indexOf(suffix, this.length - suffix.length) !== -1);
};

function throwError(message) {
	context.setError(message);
	throw message;
}

function logInfo(message) {
	context.getLogger().log(Level.INFO, context.getDomainName() + ": " + message);
}

function logError(message) {
	context.getLogger().log(Level.WARNING, context.getDomainName() + ": " + message);
}
  
function getDefaultChapterTitle(rawHtml, formatHtml) {
	var result = "";
	try {
		var html = formatHtml.substring(0, 1000);
		for (var i=0; i<all_PREFIXES.length; i++) {
			var prefix = all_PREFIXES[i];
			var regex = prefix + "\\s*\\d+";
			var title = findChapterTitle(html, regex);
			if (title.length > 0) {
				result = title;
				break;
			}
		}
	} catch (ex) {
		logError("Error getting chapter title. " + ex.message);
	}
	logInfo("Found chapter title: [" + result + "]");
	return result;
}

function findChapterTitle(html, regex) {
	var title = "";
	var index0 = html.search(regex);
	
	if (index0 >= 0) {
		//log.info("Found chapter prefix: " + regex + " at index: " + index0);
		// find the first : after the chapter prefix
		var index1 = html.indexOf(":", index0);
		//log.info("Found chapter prefix: " + regex + " at index0=" + index0
			//  + ", index1=" + index1);
		if (index1 != -1 && index1-index0 <= 20) {
			var index2 = html.indexOf("<", index1);
			if (index2 - index1 < 150) {
				title = html.substring(index0, index2);
			} else {
				logInfo("Cannot find < after chapter title!");
			}
		}      
	} else {
		//log.info("Not found chapter prefix: " + regex);
	}
	
	return title;
}

function isValidChapterHtml(chapterHtml) {
	// note: getHtml() return the java string (not javascript) so length() must be use
	// instead of length
	var valid = (chapterHtml != null && chapterHtml.getHtml() != null)
			&& (chapterHtml.getHtml().length() > 300 || chapterHtml.getAttachments().size() > 0);
	if (valid) {
		// strip out the embed tag
		var doc = Jsoup.parse(chapterHtml.getHtml());
    var list = doc.getElementsByTag("embed");
    if (list.size() > 0) {
      list.remove();
      chapterHtml.setHtml(doc.toString());
    }
	}
	
	return valid;
}

function createChapterImageHtml(imageArr) {
  var result = new ChapterHtml();
  
  var text = "<div cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">";
  for (var i=0; i<imageArr.length; i++) {
    // load image from server
    var image = context.loadResource(imageArr[i]);
    if (image != null && image.length > 0) {    
      logInfo("Loaded chapter image: " + imageArr[i]); 
      var index = imageArr[i].lastIndexOf("/");
      var href = imageArr[i];
      if (index != -1) {
        href = href.substring(index+1);
      }
      var attachment = new AttachmentData(href, image);
      result.add(attachment);
      // create html element
      var imgHtml = "<img style=\"width:100%\" src=\"" + attachment.getHref() + "\" />";
      text += imageHtml      
    } else {
      logError("Failed to load image resource: " + imageArr[i]);
    }    
  }
  text += "</div>";  
  logInfo("createChapterImageHtml: html=" + text);
  
  var index = context.getBookTemplate().indexOf("</body>");
  result.setHtml(context.getBookTemplate().substring(0, index-1) + text + "</body></html>");           
  
  return result;
}



// line: 130