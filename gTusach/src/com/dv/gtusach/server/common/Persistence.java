package com.dv.gtusach.server.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dv.gtusach.shared.Book;
import com.dv.gtusach.shared.Book.BookStatus;
import com.dv.gtusach.shared.ParserScript;
import com.dv.gtusach.shared.SystemInfo;
import com.dv.gtusach.shared.User;

public abstract class Persistence {
	abstract public void saveScript(ParserScript script); 
	abstract public void deleteScript(String scriptId); 
	abstract public ParserScript getScript(String domainName);
	abstract public List<ParserScript> getScripts(Date timestamp);
	
	abstract public void saveUser(User user);	
	abstract public User getUser(String userName);
	
  abstract public SystemInfo getSystemInfo();
  abstract public void saveSystemInfo(SystemInfo info);
	
  abstract public List<Book> loadBooks(BookStatus[] statusList);
  abstract public Book findBook(Object id);  
  abstract public void saveBook(Book book);
  abstract public void removeBook(Object id);  

  abstract public List<ChapterHtml> loadChapters(Object bookId);
  abstract public List<Object> loadChapterIDs(Object bookId);
  abstract public void saveChapter(ChapterHtml chapter);
  
  abstract public List<SectionData> loadBookSections(Object bookId);  
  abstract public void saveBookData(Object bookId, byte[] data);
  
  public byte[] loadBookData(Object bookId) {
    List<SectionData> sections = loadBookSections(bookId);
    return mergeSections(sections);
  }
  
  protected byte[] mergeSections(List<SectionData> sections) {
    if (sections.size() == 0) {
      return null;
    }
    int count = 0;
    for (SectionData section: sections) {
      count += section.getData().length;
    }
    byte[] data = new byte[count];
    if (count > 0) {
      int index = 0;
      for (SectionData section: sections) {
        System.arraycopy(section.getData(), 0, data, index, section.getData().length);
        index += section.getData().length;
      }
    }          
    return data;
  }
  
  protected List<SectionData> splitIntoSections(Object bookId, byte[] data, int maxSectionSizeKB) {
    List<byte[]> sections = new ArrayList<byte[]>();
    // break book into smaller sections
    int maxSize = maxSectionSizeKB*1024;
    if (data.length > maxSize) {
      int n = data.length/maxSize;
      int index = 0;
      for (int i=0; i<n; i++) {
        byte[] buf = new byte[maxSize];
        System.arraycopy(data, index, buf, 0, buf.length);
        sections.add(buf);
        index += buf.length;
      }
      if (index < data.length) {
        byte[] buf = new byte[data.length-index];
        System.arraycopy(data, index, buf, 0, buf.length);
        sections.add(buf);
      }
    } else {
      sections.add(data);
    }
    
    List<SectionData> result = new ArrayList<SectionData>();
    for (int i=0; i<sections.size(); i++) {
      SectionData section = new SectionData();
      section.setBookId(bookId);
      section.setData(sections.get(i));
      section.setSectionNo(i);
      result.add(section);
    }
    return result;
  }
}
