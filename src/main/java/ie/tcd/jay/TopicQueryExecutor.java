package ie.tcd.jay;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;

import java.io.*;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopicQueryExecutor {

    private static String absPathToQueries =  System.getProperty("user.dir") + File.separator + "topics";;
    private static String absPathToSearchResults = System.getProperty("user.dir") + File.separator + "GeneratedResults.txt";
    private static Analyzer analyzer = new EnglishAnalyzer(); // Instantiate your analyzer here
    private static Similarity similarityModel = new BM25Similarity() ; // Define your Similarity model here
    private static final int MAX_RETURN_RESULTS = 1000;
    private static final String ITER_NUM = " Q0 ";

    // Load queries from the file
    public static List<QueryObject> loadQueriesFromFile() {
        List<QueryObject> queries = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(absPathToQueries))) {
            String queryLine;
            QueryObject queryObject = new QueryObject();
            String tempTag = QueryTags.TOP_START.getTag();
            String topTag = QueryTags.TOP_START.getTag();
            int counter = 0;

            while ((queryLine = bf.readLine()) != null) {
                String queryLineTag = checkIfDocLineHasTag(queryLine);
                if (queryLineTag != null && queryLineTag.equals(topTag)) {
                    counter++;
                    tempTag = queryLineTag;
                    queries.add(queryObject);
                    queryObject = new QueryObject();
                } else if (queryLineTag != null && !queryLineTag.equals(topTag)) {
                    tempTag = queryLineTag;
                }
                populateQueryFields(tempTag, queryLine, queryObject, counter);
            }
            queries.add(queryObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queries;
    }

    // Check if a line contains a specific query tag
    private static String checkIfDocLineHasTag(String docLine) {
        for (QueryTags tag : QueryTags.values()) {
            if (docLine.contains(tag.getTag())) {
                return tag.getTag();
            }
        }
        return null;
    }

    // Populate query fields based on the tags
    private static void populateQueryFields(String queryLineTag, String queryLine, QueryObject queryObject, int counter) {
        if (queryLineTag.equals(QueryTags.QUERY_NUMBER.getTag())) {
            queryObject.setQueryNum(queryLine.replaceAll(QueryTags.QUERY_NUMBER.getTag(), "").trim());
        } else if (queryLineTag.equals(QueryTags.QUERY_TITLE.getTag())) {
            queryObject.setTitle(queryObject.getTitle() + " " + queryLine.replaceAll(QueryTags.QUERY_TITLE.getTag(), "").trim());
        } else if (queryLineTag.equals(QueryTags.QUERY_DESCRIPTION.getTag())) {
            queryObject.setDescription(queryObject.getDescription() + " " + queryLine.replaceAll(QueryTags.QUERY_DESCRIPTION.getTag(), "").trim());
        } else if (queryLineTag.equals(QueryTags.QUERY_NARRATIVE.getTag())) {
            queryObject.setNarrative(queryObject.getNarrative() + " " + queryLine.replaceAll(QueryTags.QUERY_NARRATIVE.getTag(), "").trim());
        } else {
            queryObject.setQueryId(String.valueOf(counter));
        }
    }

    // Split narrative into relevant and non-relevant parts
    private static List<String> splitNarrIntoRelNotRel(String narrative) {
        StringBuilder relevantNarr = new StringBuilder();
        StringBuilder irrelevantNarr = new StringBuilder();
        List<String> splitNarrative = new ArrayList<>();

        BreakIterator bi = BreakIterator.getSentenceInstance();
        bi.setText(narrative);
        int index = 0;
        while (bi.next() != BreakIterator.DONE) {
            String sentence = narrative.substring(index, bi.current()).trim();

            if (!sentence.contains("not relevant") && !sentence.contains("irrelevant")) {
                relevantNarr.append(sentence.replaceAll(
                        "a relevant document identifies|a relevant document could|a relevant document may|a relevant document must|a relevant document will|a document will|to be relevant|relevant documents|a document must|relevant|will contain|will discuss|will provide|must cite",
                        "").trim()).append(" ");
            } else {
                irrelevantNarr.append(sentence.replaceAll("are also not relevant|are not relevant|are irrelevant|is not relevant|not|NOT", "").trim()).append(" ");
            }
            index = bi.current();
        }
        splitNarrative.add(relevantNarr.toString().trim());
        splitNarrative.add(irrelevantNarr.toString().trim());
        return splitNarrative;
    }

    // Execute the queries against the index
    public static void executeQueries(Directory directory) throws ParseException {
        try (IndexReader indexReader = DirectoryReader.open(directory);
             PrintWriter writer = new PrintWriter(absPathToSearchResults, "UTF-8")) {

            IndexSearcher indexSearcher = createIndexSearcher(indexReader, similarityModel);
            Map<String, Float> boost = createBoostMap();
            QueryParser queryParser = new MultiFieldQueryParser(new String[]{"headline", "text"}, analyzer, boost);

            List<QueryObject> loadedQueries = loadQueriesFromFile();

            for (QueryObject queryData : loadedQueries) {
                List<String> splitNarrative = splitNarrIntoRelNotRel(queryData.getNarrative());
                String relevantNarr = splitNarrative.get(0).trim();

                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                if (queryData.getTitle().length() > 0) {
                    Query titleQuery = queryParser.parse(QueryParser.escape(queryData.getTitle()));
                    Query descriptionQuery = queryParser.parse(QueryParser.escape(queryData.getDescription()));
                    Query narrativeQuery = null;
                    if (relevantNarr.length() > 0) {
                        narrativeQuery = queryParser.parse(QueryParser.escape(relevantNarr));
                    }

                    booleanQuery.add(new BoostQuery(titleQuery, 4.0f), BooleanClause.Occur.SHOULD);
                    booleanQuery.add(new BoostQuery(descriptionQuery, 1.7f), BooleanClause.Occur.SHOULD);
                    if (narrativeQuery != null) {
                        booleanQuery.add(new BoostQuery(narrativeQuery, 1.2f), BooleanClause.Occur.SHOULD);
                    }

                    ScoreDoc[] hits = indexSearcher.search(booleanQuery.build(), MAX_RETURN_RESULTS).scoreDocs;

                    for (int hitIndex = 0; hitIndex < hits.length; hitIndex++) {
                        ScoreDoc hit = hits[hitIndex];
                        Document doc = indexSearcher.doc(hit.doc);
                        writer.println(queryData.getQueryNum() + ITER_NUM + doc.get("docno") + " " + hitIndex + " " + hit.score + " BM25");
                    }
                }
            }

            System.out.println("Queries executed and results saved to " + absPathToSearchResults);

        } catch (IOException e) {
            System.err.println("ERROR: An error occurred while executing queries.");
            e.printStackTrace();
        }
    }

    private static IndexSearcher createIndexSearcher(IndexReader indexReader, Similarity similarityModel) {
        IndexSearcher searcher = new IndexSearcher(indexReader);
        searcher.setSimilarity(similarityModel);
        return searcher;
    }

    private static Map<String, Float> createBoostMap() {
        // Define field boosts, if needed
        // For example: return Map.of("headline", 2.0f, "text", 1.0f);
        return Map.of("headline", 2.0f, "text", 1.0f); // Adjust as needed
    }
}

