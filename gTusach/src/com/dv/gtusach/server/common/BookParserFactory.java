package com.dv.gtusach.server.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.dv.gtusach.shared.BadDataException;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.SystemInfo;

public class BookParserFactory {
	private static final BookParserFactory instance = new BookParserFactory();
	private List<ParserScript> scripts = new ArrayList<ParserScript>();
	private HashMap<String, BookParser> parserMap = new HashMap<String, BookParser>();
	private Date lastUpdateTime = null;
	
	private BookParserFactory() {		
	}
	
	public static BookParserFactory getInstance() {
		return instance;
	}
	
	public BookParser getParser(Persistence persistence, String domain) throws BadDataException {
		SystemInfo info = persistence.getSystemInfo();
		if (!info.isEditingScript()) {
			if (lastUpdateTime == null || (info.getScriptLastUpdateTime() != null 
					&& lastUpdateTime.before(info.getScriptLastUpdateTime()))) {
				lastUpdateTime = info.getScriptLastUpdateTime();
				// need to reload the scripts
				List<ParserScript> list = persistence.getScripts(null);
				synchronized (this) {
					scripts.clear();
					scripts.addAll(list);
				}
			}
		}
		
		BookParser result = null;
		synchronized (this) {
			result = getParser(domain);
		}
		return result;					
	}
	
	private BookParser getParser(String url) throws BadDataException {
		ParserScript common = null;
		ParserScript script = null;
		for (ParserScript s: scripts) {
			if (s.getDomainName().equals("common")) {
				common = s;
			} else if (script == null && url.toLowerCase().indexOf(s.getDomainName().toLowerCase()) != -1) {
				script = s;				
			}
		}
		if (common == null || script == null) {
			return null;
		}
		
		String key = script.getDomainName();
		BookParser parser = parserMap.get(key);
		if (parser == null) {
			parser = new BookParser();
			parserMap.put(key, parser);
		}
		parser.init(script.getDomainName(), new String[] {common.getScript(), script.getScript()});
		return parser;
	}
}
