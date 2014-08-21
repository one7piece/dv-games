package com.dv.gtusach.server.common;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.dv.gtusach.shared.BadDataException;

public class BookParser {
  protected static final Logger log = Logger.getLogger(BookParser.class.getCanonicalName());
  
  static String[] DOMAIN_NAMES = {".com/", ".name/", ".info/", ".org/", ".vn/"};
    
  protected HttpService httpService;
  protected SiteConfiguration siteConfiguration;
  protected String bookTemplate = "";
  protected String domainName;
  private ScriptEngine engine;
  private String error;
  private String[] scripts = new String[0];
  private ScriptException scriptError;
  
  public BookParser() {
    siteConfiguration = new SiteConfiguration();
    siteConfiguration.setProxy(getProxy());
    httpService = new HttpService();
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("JavaScript");
		if (engine == null) {
			throw new RuntimeException("No javascript engine!");
		}
  }
  
  public void init(String domainName, String[] scripts) throws ScriptException {
  	this.domainName = domainName;
  	if (!Arrays.equals(this.scripts, scripts)) {
  		log.info("BookParser.init() - " + domainName + ", re-evaluating scripts...");
  		try {
  			scriptError = null;
      	for (String script: scripts) {
        	engine.eval(script);  	
      	}
  		} catch (ScriptException ex) {
  			log.log(Level.SEVERE, "Parser script error: " + ex.getMessage());
  			scriptError = ex;
  			throw ex;  			
  		}
  	}
  }
  
  public ScriptException getScriptError() {
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
      
  public String getBookTemplate() {
    return bookTemplate;
  }
  public void setBookTemplate(String bookTemplate) {
    this.bookTemplate = bookTemplate;
  }  
  
  public String getTargetUrl(String pUrl) {
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
    
  public ChapterHtml extractChapterHtml(String target, String request, String rawChapterHtml) throws BadDataException {
  	if (scriptError != null) {
  		throw new BadDataException(scriptError.getClass() + ": " + scriptError.getMessage());
  	}
  	
  	ChapterHtml result = null;
  	try {
			Invocable inv = (Invocable)engine;
			setError(null);
			Object retval = inv.invokeFunction("extractChapterHtml", target, request, rawChapterHtml);
			if (error != null) {
				throw new BadDataException(error);
			}
			if (retval instanceof ChapterHtml) {
				result = (ChapterHtml)retval;
			} else {
				throw new BadDataException("extractChapterHtml() - Bad value return from javascript: " + retval);
			}
  	} catch (ScriptException ex) {
			throw new BadDataException("extractChapterHtml() - Script error! " + ex.getMessage());
  	} catch (NoSuchMethodException ex) {
			throw new BadDataException("extractChapterHtml() - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  public String getNextPageUrl(String target, String currentPageURL, String rawChapterHtml) {
  	if (scriptError != null) {
  		return null;
  	}
  	
  	String result = null;
  	try {
			Invocable inv = (Invocable)engine;
			setError(null);
			Object retval = inv.invokeFunction("getNextPageUrl", target, currentPageURL, rawChapterHtml);
			if (error != null) {
				log.log(Level.WARNING, error);
				return null;
			}
			if (retval instanceof String) {
				result = (String)retval;
			} else {
				log.log(Level.WARNING, "getNextPageUrl() - Bad value return from javascript: " + retval);
				return null;
			}
  	} catch (ScriptException ex) {
  		log.log(Level.WARNING, "getNextPageUrl - Script error! " + ex.getMessage());
  	} catch (NoSuchMethodException ex) {
  		log.log(Level.WARNING, "getNextPageUrl - Script error! " + ex.getMessage());
		}
  	return result;
  }
  
  public String getChapterTitle(String rawHtml, String formatHtml) {
  	if (scriptError != null) {
  		return null;
  	}
  	
  	String result = null;
  	try {
			Invocable inv = (Invocable)engine;
			setError(null);
			Object retval = inv.invokeFunction("getChapterTitle", rawHtml, formatHtml);
			if (error != null) {
				log.log(Level.WARNING, error);
				return null;
			}
			if (retval instanceof String) {
				result = (String)retval;
			} else {
				log.log(Level.WARNING, "getChapterTitle() - Bad value return from javascript: " + retval);
				return null;
			}
  	} catch (ScriptException ex) {
  		log.log(Level.WARNING, "getChapterTitle() - Script error! " + ex.getMessage());
  	} catch (NoSuchMethodException ex) {
  		log.log(Level.WARNING, "getChapterTitle() - Script error! " + ex.getMessage());
		}
  	return result;
  }

  public Proxy getProxy() {
    Proxy proxy = null;
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      if (InetAddress.getLocalHost().getHostAddress().startsWith("10.45")) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("Zen.tyco.com.au", 9400));
        //proxy = new HttpHost("Zen.tyco.com.au", 9400, "http");
        Authenticator authenticator = new Authenticator() {
          public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication("dvan", "Spidey56".toCharArray()));
          }
        };
        Authenticator.setDefault(authenticator);       
      }
    } catch (Exception ex) {
      log.log(Level.WARNING, "Error creating proxy", ex);
    }
    return proxy;
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
