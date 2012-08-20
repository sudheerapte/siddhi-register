package com.sapte.sn;

import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;

public class TestSearch {
	private static Directory _ramDir = new RAMDirectory();
	private static IndexReader _reader = null;
	
	private static void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
		IndexWriter iWriter = new IndexWriter(_ramDir, config);
		Document doc = new Document();
		doc.add(new Field("lastname", "Barucha", Field.Store.YES, Field.Index.NOT_ANALYZED));
		iWriter.addDocument(doc);
		iWriter.close();
	}
	
	private static IndexReader getReader() throws CorruptIndexException, IOException {
		if (_reader == null) { _reader = IndexReader.open(_ramDir); }
		return _reader;
	}
	
	private static Query getQuery() {
		// return new TermQuery(new Term("lastname", "Barucha"));
		return new PrefixQuery(new Term("lastname", "Bar"));
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws LockObtainFailedException 
	 * @throws CorruptIndexException 
	 */
	public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException {
		// TODO Auto-generated method stub
		createIndex();
		System.out.println("added one doc.\n");
		IndexSearcher iSearcher = new IndexSearcher(getReader());
		TopDocs topdocs = iSearcher.search(getQuery(), 10);
		System.out.println("Got " + String.valueOf(topdocs.totalHits) + " hits.\n"); 
		
	}

}
