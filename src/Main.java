import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Document;
import java.nio.file.Files;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



/**
 * This is a java Doc explanation of the code below
 * 1. Mapping each SGML tag <DOCNO>, <TEXT>, etc., into corresponding Lucene document fields.
 * Method parseFBISDocument - processes each singular document from a file in a fbis folder
 * Method parseLADocument - processes each singular document from a file in a latimes folder
 * Method rocessFbisLatimesFolder runs the processing for all
 */

public class Main {

    private static String INDEX_DIRECTORY = System.getProperty("user.dir") + File.separator + "index";
    private static String FILE_PATH = System.getProperty("user.dir") + File.separator + "documents";
    private static String FILE_PATH_FBIS = FILE_PATH + File.separator + "fbis";
    private static String FILE_PATH_LATIMES = FILE_PATH + File.separator + "latimes";

    // Method to parse a single FBIS file and return a Lucene document
    public static Document parseFBISDocument(Element docElement) {
        Document luceneDoc = new Document();

        Element docnoElement = docElement.selectFirst("DOCNO");
        if (docnoElement != null) {
            luceneDoc.add(new StringField("docno", docnoElement.text(), Field.Store.YES));
        }

        Element headerElement = docElement.selectFirst("HEADER");
        if (headerElement != null) {
            luceneDoc.add(new TextField("header", headerElement.text(), Field.Store.YES));
        }

        Element textElement = docElement.selectFirst("TEXT");
        if (textElement != null) {
            luceneDoc.add(new TextField("text", textElement.text(), Field.Store.YES));
        }

        Element dateElement = docElement.selectFirst("DATE1");
        if (dateElement != null) {
            luceneDoc.add(new StringField("date", dateElement.text(), Field.Store.YES));
        }
        return luceneDoc;
    }

    // Method to parse a single LATIMES file and return a Lucene document
    public static Document parseLADocument(Element docElement) {
        Document luceneDoc = new Document();

        Element docnoElement = docElement.selectFirst("DOCNO");
        if (docnoElement != null) {
            luceneDoc.add(new StringField("docno", docnoElement.text(), Field.Store.YES));
        }

        Element docidElement = docElement.selectFirst("DOCID");
        if (docidElement != null) {
            luceneDoc.add(new StringField("docid", docidElement.text(), Field.Store.YES));
        }

        Element headerElement = docElement.selectFirst("HEADLINE");
        if (headerElement != null) {
            luceneDoc.add(new TextField("HEADLINE", headerElement.text(), Field.Store.YES));
        }

        Element textElement = docElement.selectFirst("TEXT");
        if (textElement != null) {
            luceneDoc.add(new TextField("text", textElement.text(), Field.Store.YES));
        }

        Element dateElement = docElement.selectFirst("DATE");
        if (dateElement != null) {
            luceneDoc.add(new StringField("date", dateElement.text(), Field.Store.YES));
        }
        return luceneDoc;
    }

    // Method to process all files in the FBIS folder
    public static int processFbisLatimesFolder(IndexWriter writer, String folderPath,  boolean isFBIS) throws IOException {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        int documentCount = 0; // Counter to track the number of indexed documents

        if (files != null) {
            for (File file : files) {
                System.out.println("Processing file: " + file.getName());
                String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), "UTF-8");
                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());

                // extracts all the elements in the file that are enclosed within <DOC> tags,
                Elements docs = jsoupDoc.select("DOC");

                for (Element docElement : docs) {
                    Document luceneDoc = isFBIS ? parseFBISDocument(docElement) : parseLADocument(docElement);
                    if (luceneDoc != null) {
                        writer.addDocument(luceneDoc);
                        documentCount++;
                    }
                }
            }
        }
        return documentCount; // Return the total number of indexed documents
    }


    public static void main(String[] args) {
        try {
            // Initialize Lucene components for indexing
            Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
            Analyzer analyzer = new EnglishAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new BM25Similarity()); // Can change the Similarity here

            IndexWriter writer = new IndexWriter(indexDirectory, config);

            // Process the FBIS folder and index all documents, keeping track of the count
            int totalIndexedDocumentsFbis = processFbisLatimesFolder(writer, FILE_PATH_FBIS, true);
            // Process the LATIMES folder and index all documents
            int totalIndexedDocumentsLatimes = processFbisLatimesFolder(writer, FILE_PATH_LATIMES, false);

            writer.close();

            // Print total documents indexed from both folders
            System.out.println("Indexing complete!");
            System.out.println("Total FBIS documents indexed: " + totalIndexedDocumentsFbis);
            System.out.println("Total LATIMES documents indexed: " + totalIndexedDocumentsLatimes);
            System.out.println("Total documents indexed: " + (totalIndexedDocumentsFbis + totalIndexedDocumentsLatimes));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}