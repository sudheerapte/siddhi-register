package com.sapte.sn;


import java.util.Map;

public interface RecordReceiver {
	public void receiveHeader(String[] header);  // File opened; here are the column names
	public void receiveRecord(Map<String,String> record); // This is the next record
	public void done();                          // No more records. 
	public void badRecord();                       // Bad record, but will continue scanning.
	public void error();                         // I/O error -- no more records.
}
