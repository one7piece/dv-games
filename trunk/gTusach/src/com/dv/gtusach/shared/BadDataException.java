package com.dv.gtusach.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BadDataException extends Exception implements IsSerializable {
	
	public BadDataException() {		
	}
  
	public BadDataException(String msg) {
    super(msg);
  }
}
