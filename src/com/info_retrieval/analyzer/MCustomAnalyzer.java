package com.info_retrieval.analyzer;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.miscellaneous.TrimFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
//import org.apache.lucene.analysis.ngram.ShingleFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MCustomAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();  // Splits input text into tokens
        TokenStream tokenStream = tokenizer;

        tokenStream = new ASCIIFoldingFilter(tokenStream); // Normalise accented or special characters into their plain forms "café becomes cafe"

        // Delimiter Filter (splits or combines words based on special characters or "formatting high-speed")
        tokenStream = new WordDelimiterGraphFilter(
                tokenStream,
                WordDelimiterGraphFilter.SPLIT_ON_NUMERICS |
                        WordDelimiterGraphFilter.GENERATE_WORD_PARTS |
                        WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS |
                        WordDelimiterGraphFilter.PRESERVE_ORIGINAL,
                null
        );


        // Chain filters
        tokenStream = new LowerCaseFilter(tokenStream); // Converts text to lowercase
        tokenStream = new TrimFilter(tokenStream); // Removes extra whitespace
       //  tokenStream = new SynonymGraphFilter(tokenStream, createSynonymMap(), true);
       // tokenStream = new ShingleFilter(tokenStream, 2, 3); // Handle n-grams min 2-grams max 3-grams
        tokenStream = new StopFilter(tokenStream, loadStopwords()); // Removes stop words
        tokenStream = new SnowballFilter(tokenStream, new EnglishStemmer());

        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    // Load stop words from a file and using Lucene's built-in set.
    private CharArraySet loadStopwords() {
        CharArraySet stopWords = new CharArraySet(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET, true);

        try (BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                stopWords.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }
}
