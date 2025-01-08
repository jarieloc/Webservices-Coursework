package org.example;

import java.util.ArrayList;
import java.util.List;

public class StringCalculator {

    // Method to add numbers from a comma-separated string
    public int add(String numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0; // Return 0 for empty input
        }

        // filter
        String delimiterPattern = ",|\n"; // Default delimiters

        // Check for custom delimiter
        if (numbers.startsWith("//")) {
            int delimiterEndIndex = numbers.indexOf("\n");
            if (delimiterEndIndex != -1) {
                String customDelimiter = numbers.substring(2, delimiterEndIndex);

                // Handle multi-character delimiters
                if (customDelimiter.startsWith("[") && customDelimiter.endsWith("]")) {
                    customDelimiter = customDelimiter.substring(1, customDelimiter.length() - 1);
                    // Escape special regex characters and allow multiple delimiters
                    customDelimiter = customDelimiter.replace("][", "|").replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
                } else {
                    // Escape special regex characters for single-character delimiters
                    customDelimiter = customDelimiter.replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
                }

                delimiterPattern = delimiterPattern + "|" + customDelimiter;
                numbers = numbers.substring(delimiterEndIndex + 1); // Remove delimiter line
            }
        }

/*         String normalizedNumbers = numbers.replace("\n", ","); */

        // Split the string into an array of numbers
        String[] numArray = numbers.split(delimiterPattern);
        
        List<Integer> negatives = new ArrayList<>();

        // Convert each number to an integer and sum them
        int sum = 0;
        for (String numStr : numArray) {
            if (!numStr.trim().isEmpty()) {
                int num = Integer.parseInt(numStr.trim());
                if (num < 0) {
                    negatives.add(num); // Collect negatives
                }
                sum += num; // Add valid numbers
            }
        }

        // Handle negative numbers
        if (!negatives.isEmpty()) {
            throw new IllegalArgumentException("Negatives not allowed: " + negatives);
        }

        return sum;
    }
}
