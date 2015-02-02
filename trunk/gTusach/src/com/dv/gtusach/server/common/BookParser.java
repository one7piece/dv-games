package com.dv.gtusach.server.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.dv.gtusach.shared.BadDataException;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;

public class BookParser {
  protected static final Logger log = Logger.getLogger(BookParser.class.getCanonicalName());
  
  static String[] DOMAIN_NAMES = {".com/", ".name/", ".info/", ".org/", ".vn/", ".co/"};
    
  protected HttpService httpService;
  protected SiteConfiguration siteConfiguration;
  protected String bookTemplate = "";
  protected String domainName;
  private String error;
  private String[] scripts = new String[0];
  private Script compiledScript;
  private Exception scriptError;
  
  public BookParser() {
    siteConfiguration = new SiteConfiguration();
    httpService = new HttpService();
  }
  
  public void init(String domainName, String[] scripts) throws BadDataException {
  	this.domainName = domainName;
  	if (!Arrays.equals(this.scripts, scripts)) {
  		log.info("BookParser.init() - " + domainName + ", re-compiling scripts...");
  		try {
  			compiledScript = null;
  			scriptError = null;
  			StringBuffer buf = new StringBuffer(); 
      	for (String script: scripts) {
      		buf.append(script);
      	}
    		Context ctx = Context.enter();
  			compiledScript = ctx.compileString(buf.toString(), domainName, 1, null);      	
  		} catch (Exception ex) {
  			scriptError = ex;
  			compiledScript = null;
  			log.log(Level.SEVERE, "Parser script compile error: " + ex.getMessage());
  			throw new BadDataException(ex.getMessage());  			
  		} finally {
  			Context.exit();
  		}
  	}
  }
  
  public Exception getScriptError() {
  	return scriptError;
  }
  
  public Logger getLogger() {
  	return log;
  }
  
  public void setError(String error) {
  	this.error = error;
  }
  
  public String getUrl(String target, String request) {
    return httpService.getUrl(target, request);
  }
    
  public byte[] loadResource(String url) {
    return httpService.executeRequest(url, siteConfiguration);
  }
  
  public String executeRequest(String target, String request) {
    return httpService.executeRequestStr(target, request, siteConfiguration);
  }
  
  public SiteConfiguration getSiteConfiguration() {
    return siteConfiguration;
  }
  
  public HttpService getHttpService() {
		return httpService;
	}

	public String getBookTemplate() {
    return bookTemplate;
  }
  public void setBookTemplate(String bookTemplate) {
    this.bookTemplate = bookTemplate;
  }  
  
  public String getTargetUrl(String pUrl) {
  	if (pUrl == null) {
  		return null;
  	}
    for (String name: DOMAIN_NAMES) {
      int index = pUrl.toLowerCase().indexOf(name);
      if (index != -1) {
        if (pUrl.toLowerCase().startsWith("http://")) {
          return pUrl.substring("http://".length(), index+name.length());
        }
        return pUrl.substring(0, index+name.length());        
      }
    }
    return null;
  }
  
  public String getRequestUrl(String pUrl) {
  	if (pUrl == null) {
  		return null;
  	}
    String target = getTargetUrl(pUrl);
    if (target != null) {
      if (target.equalsIgnoreCase(pUrl)) {
        return "/";
      }
      int index = pUrl.toLowerCase().indexOf(target.toLowerCase());
      if (index != -1) {
        return pUrl.substring(index+target.length());
      }
    }
    return null;
  }
  
  public String getDomainName() {
  	return domainName;
  }
  
  public List<AttachmentData> createAttachments(String ref, byte[] imageData, int pageHeight) {
  	List<AttachmentData> result = new ArrayList<AttachmentData>();
  	ImagesService imagesService = ImagesServiceFactory.getImagesService();  	
  	Image image = ImagesServiceFactory.makeImage(imageData);
  	float h = (float)image.getHeight();
		log.info("createAttachments - " + "image height=" + h);
  	float y = 0.0f;
  	int count = 0;
  	while (y < h) {
    	Transform crop = ImagesServiceFactory.makeCrop(0.0f, y/h, 1.0, (y+pageHeight) >= h ? 1.0f : (y+pageHeight)/h );
    	Image newImage = imagesService.applyTransform(crop, image);
    	String partRef = count + "-" + ref;
    	AttachmentData attachment = new AttachmentData(partRef, newImage.getImageData());
    	result.add(attachment);
    	y += pageHeight;
    	count++;
  	}
  	return result;
  }
  
  public ChapterHtml extractChapterHtml(String target, String request, String rawChapterHtml) throws BadDataException {
  	Object retval = executeJSFunction("extractChapterHtml", new Object[] {target, request, rawChapterHtml});
  	if (!(retval instanceof ChapterHtml)) {
  		throw new BadDataException("extractChapterHtml() - Bad value return from javascript: " + retval);
  	}
  	return (ChapterHtml)retval;
  }

  public int getBatchSize() {
  	int result = 100;
  	try {
    	Object retval = executeJSFunction("getBatchSize", null);
    	if (retval != null && (retval instanceof String || retval instanceof Number)) {
    		Double d = Double.parseDouble("" + retval);
    		result = d.intValue();
    	}
  	} catch (Exception ex) {
  		log.log(Level.WARNING, "getBatchSize - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  
  public int getDelayTimeSec() {
  	int result = 0;
  	try {
    	Object retval = executeJSFunction("getDelayTimeSec", null);
    	if (retval != null && (retval instanceof String || retval instanceof Number)) {
    		Double d = Double.parseDouble("" + retval);
    		result = d.intValue();
    	}
  	} catch (Exception ex) {
  		log.log(Level.WARNING, "getDelayTimeSec - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  public String getNextPageUrl(String target, String currentPageURL, String rawChapterHtml) {
  	String result = null;
  	try {
    	Object retval = executeJSFunction("getNextPageUrl", new Object[] {target, currentPageURL, rawChapterHtml});
    	if (retval != null && !(retval instanceof String)) {
    		throw new BadDataException("Bad value return from javascript: " + retval);
    	}
    	result = (String)retval;  		
  	} catch (Exception ex) {
  		log.log(Level.WARNING, "getNextPageUrl - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  public String getChapterTitle(String rawHtml, String formatHtml) {
  	String result = null;
  	try {
    	Object retval = executeJSFunction("getChapterTitle", new Object[] {rawHtml, formatHtml});
    	if (retval != null && !(retval instanceof String)) {
    		throw new BadDataException("Bad value return from javascript: " + retval);
    	}
    	result = (String)retval;  		
  	} catch (Exception ex) {
  		log.log(Level.WARNING, "getChapterTitle - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  private Object executeJSFunction(String functionName, Object[] args) throws BadDataException {
  	if (scriptError != null) {
  		throw new BadDataException(scriptError.getClass() + ": " + scriptError.getMessage());
  	}
  	
  	Context ctx = Context.enter();
  	try {
  		Scriptable scope = new ImporterTopLevel(ctx);  		
  		compiledScript.exec(ctx, scope);
  		// set global variable context pointing to this isntance
  		Object wrappedThis = Context.javaToJS(this, scope);
			ScriptableObject.putProperty(scope, "context", wrappedThis);
  		  		
			setError(null);
			Object fct = scope.get(functionName, scope);
			if (!(fct instanceof Function)){
				throw new BadDataException("Function " + functionName + " is not defined!");
			}
			Object retval = ((Function)fct).call(ctx, scope, scope, args);			
			if (error != null) {
				throw new BadDataException(error);
			}
			if (retval instanceof NativeJavaObject) {
				return ((NativeJavaObject)retval).unwrap();
			}
			return retval;						
  	} catch (BadDataException ex) {
  		throw ex;
  	} catch (Exception ex) {
			throw new BadDataException("extractChapterHtml() - Script error! " + ex.getMessage());
		} finally {
			Context.exit();
		}
  }
  
/*    
  protected void extractNodeText(Node node, StringBuffer buffer) {
  	if (node instanceof TextNode) {
  		buffer.append(((TextNode)node).getWholeText() + "<br/>");
  	} else {
  		for (Node child: node.childNodes()) {
  			extractNodeText(child, buffer);
  		}
  	}
  }
*/  
}
