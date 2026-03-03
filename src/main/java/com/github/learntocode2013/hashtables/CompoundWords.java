package com.github.learntocode2013.hashtables;

import java.util.*;

public class CompoundWords {
    private static List<String> readInputs() {
        var words = new ArrayList<String>();
        Scanner scn = new Scanner(System.in);
        while(scn.hasNextLine()) {
            var word = scn.nextLine();
            if (word.trim().equals("")) {
                break;
            }
            words.add(word);
        }
        scn.close();
        return words;
    }

    private static boolean isInWordDict(Map<String, Boolean> wordDict, String word, int startIndex, int endIndex) {
       var searchWord = word.substring(startIndex, endIndex);
       return wordDict.containsKey(searchWord);
    }

    private static void identifyCompoundWords(List<String> words, Map<String, Boolean> wordDict) {
        int totalWords = words.size();
        for(int i = 0; i < totalWords; i++) {
            int len = words.get(i).length();
            for(int j = 1; j < len; j++) {
                if (isInWordDict(wordDict, words.get(i), 0, j) &&
                        isInWordDict(wordDict, words.get(i), j,len)) {
                    System.out.printf("%s %n", words.get(i));
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        var words = readInputs();
        var wordDict = new HashMap<String, Boolean>();
        words.forEach(w -> wordDict.put(w, Boolean.TRUE));
        identifyCompoundWords(words, wordDict);
    }
}
