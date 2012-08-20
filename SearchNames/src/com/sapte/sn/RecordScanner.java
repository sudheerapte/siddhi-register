package com.sapte.sn;


import java.io.*;
import java.util.*;


public class RecordScanner {
	
	public void addReceiver(RecordReceiver r) {
		_receivers.add(r);
	}
	
	private ArrayList<RecordReceiver> _receivers;
	
	private String[] _header;
	private BufferedReader _reader;
	private StringBuffer _errBuf;
	
	private File _inFile;
	
	public RecordScanner(File f) { _inFile = f; _receivers = new ArrayList<RecordReceiver>(); }
	
	public boolean scan() {
		int lineNum = 0;
		if (scanHeader()) {
			for (RecordReceiver r: _receivers) {
				r.receiveHeader(_header);
			}
			String line;
			lineNum++;
			try {
				while ((line = _reader.readLine())!= null) {
					String words[] = line.split(",");
					if (words.length != _header.length) {
						_errBuf.append("Line " + lineNum + ": found " + words.length +
								" words; expecting " + _header.length + "\n");
						for (RecordReceiver r: _receivers) {
							r.badRecord();
						}
						continue;
					}
					Map<String,String> map = new HashMap<String,String>();
					for (int i=0; i<words.length; i++) {
						map.put(_header[i], words[i]);
					}
					for (RecordReceiver r: _receivers) {
						r.receiveRecord(map);
					}
				}
				_reader.close();
				for (RecordReceiver r: _receivers) {
					r.done();
				}
			} catch (IOException e) {
				_errBuf.append("Exception when reading: " + e + "\n");
				for (RecordReceiver r: _receivers) {
					r.error();
				}
				return false;
			}
		} else {
			_errBuf.append("Failed scanning header.\n");
			for (RecordReceiver r: _receivers) {
				r.error();
			}
			return false;
		}
		return true;
	}
	private boolean scanHeader() {
		_errBuf = new StringBuffer();
		try {
			_reader = new BufferedReader(new FileReader(_inFile));
			String line;
			if ((line = _reader.readLine())!= null) {
				_header = line.split(",");
			}
		} catch(IOException e) {
			_errBuf.append("Failed reading " + _inFile.getAbsolutePath() + ": " + e + "\n");
			return false;
		}
		return true;
	}
	
	public String getError() { if (_errBuf == null) { return null; } else { return _errBuf.toString(); }}
	
	public static void main(String[] args) {
		RecordReceiver r = new RecordReceiver() {
			public void receiveHeader(String[] h) {
				System.out.println(" " + h[0] + " " + h[1] + " " + h[2]);
			}
			public void receiveRecord(Map<String,String> rec) {
				System.out.println(" " + rec.get("One") + " " + rec.get("Two") + " " + rec.get("Three"));
			}
			public void done() { }
			public void badRecord() { System.out.println("BAD RECORD."); }
			public void error() { System.out.println("ERROR."); }
		};
		File file = new File("one.csv");
		RecordScanner scanner = new RecordScanner(file);
		scanner.addReceiver(r);
		scanner.scan();
	}
}
