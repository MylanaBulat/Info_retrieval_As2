package com.info_retrieval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ResultWriter {

    public static void writeToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
            System.out.println("Results written to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
