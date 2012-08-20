package com.sapte.sn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class NameIndexer implements RecordReceiver {
	private int _numRecords = 0;
	private int _badRecords = 0;
	
	private Directory _dir = new RAMDirectory();
	private IndexWriter _writer = null;
	private IndexSearcher _searcher = null;
	private IndexReader _reader = null;
	
	public NameIndexer(Directory dir) {
		_dir = dir;
		try {
			createIndex();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public IndexWriter getWriter() {
		return _writer;
	}
	
	public IndexSearcher getSearcher() throws CorruptIndexException, IOException {
		if (_searcher == null) {
			_searcher = new IndexSearcher(getReader());
		}
		return _searcher;
	}
	
	public IndexReader getReader() throws CorruptIndexException, IOException {
		if (_reader == null) {
			_reader = IndexReader.open(_dir);
		}
		return _reader;
	}
	
	public void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
		_writer = new IndexWriter(_dir, config);
		
	}	

	public void receiveHeader(String[] header) { // File opened; here are the column names
		System.out.println("Found " + header.length + " headers.\n");
	}
	public void receiveRecord(Map<String,String> record) {
		_numRecords ++;
		Document doc = new Document();
		doc.add(new Field("LAST", record.get("LAST"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("FIRST", record.get("FIRST"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("REGISTER", record.get("REGISTER"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("YEAR", record.get("YEAR"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("DEPARTMENT", record.get("DEPARTMENT"), Field.Store.YES, Field.Index.NOT_ANALYZED));
		try {
			_writer.addDocument(doc);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void done() {
		System.out.println("Successful.  Read " + _numRecords + " names.\n");
		try {
			_writer.close();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void badRecord() {
		_badRecords ++;
	}
	public void error() {
		System.err.println("I/O error.  Stopped reading.\n");
		try {
			_writer.close();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String, Integer> getFrequencies(TopDocs topdocs) throws CorruptIndexException, IOException {
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		for (int i=0; i<topdocs.totalHits; i++) {
			Document doc = getReader().document(topdocs.scoreDocs[i].doc);
			String lastname = doc.get("LAST");
			if (! map.containsKey(lastname)) { map.put(lastname, new Integer(1)); }
		}
		for (String lastname : map.keySet()) {
			TermDocs termDocs = getReader().termDocs(new Term("LAST", lastname));
			int i = 0;
			for (;termDocs.next();) {
				i += termDocs.freq();
			}
			map.put(lastname, new Integer(i));
			termDocs.close();
		}
		return map;
	}
	
	TopDocs searchLastPrefix(String prefix, int maxNames) throws IOException {
		IndexSearcher iSearcher = getSearcher();
		TopDocs topdocs = iSearcher.search(new PrefixQuery(new Term("LAST", prefix)), maxNames);
		return topdocs;
	}
	
	
	
	
	List<Map<String,String>> getAllByLast(String last) throws CorruptIndexException, IOException {
		TopDocs topdocs = getSearcher().search(new TermQuery(new Term("LAST", last)), _numRecords);
		ArrayList<Map<String,String>> list = new ArrayList<Map<String,String>>();
		final String[] fields = { "LAST", "FIRST", "REGISTER", "YEAR", "DEPARTMENT" };
		for (int i=0; i<topdocs.totalHits; i++) {
			HashMap<String,String> map = new HashMap<String,String>();
			Document doc = getReader().document(topdocs.scoreDocs[i].doc);
			for (String f : fields) { map.put(f, doc.get(f)); }
			list.add(map);
		}
		return list;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 */
	public static void main(String[] args) throws CorruptIndexException, IOException {
		
		Directory ramDir = new RAMDirectory();
		NameIndexer nameIndexer = new NameIndexer(ramDir);
		File file = new File("2012-05-31.csv");
		RecordScanner scanner = new RecordScanner(file);
		scanner.addReceiver(nameIndexer);
		scanner.scan();
		
		
		String prefix = "Ch";
		int maxNames = 1000;
		
		System.out.println("Doing a search for LAST = " + prefix);
		
		TopDocs topdocs = nameIndexer.searchLastPrefix(prefix, maxNames);
		System.out.println("Got " + String.valueOf(topdocs.totalHits) + " hits.\n");
		if (topdocs.totalHits > maxNames) {
			System.out.println("Too many to show.");
		}
		Map<String,Integer> map = nameIndexer.getFrequencies(topdocs);
		System.out.println("Unique names found = " + map.size());
		for (String last : map.keySet()) {
			System.out.println("\t" + last + " : " + map.get(last));
		}
		
		
		List<Map<String,String>> list = nameIndexer.getAllByLast("Choudhary");
		for (Map<String,String> m : list) {
			System.out.println("\t" + m.get("LAST") + ", " + m.get("FIRST") + " : " + m.get("REGISTER") + "/" + m.get("YEAR"));
		}
	}
}
