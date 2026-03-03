package com.github.learntocode2013.hashtables;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// Refer: https://dmoj.ca/problem/cco07p2
public class IdenticalSnowflakes {
    private static final String ANY_MATCH_MSG = "Twin snowflakes found.";
    private static final String NONE_MATCH_MSG = "No two snowflakes are alike.";
    private static String identifyIdentical(int n, int[] values) {
        Map<Integer, Boolean> numDict = new HashMap<>();
        for (int value : values) {
            if (numAlreadyExists(value, numDict)) {
                return ANY_MATCH_MSG;
            }

        }
        return NONE_MATCH_MSG;
    }

    private static int sortedNum(int num) {
        int remainder = num;
        var digits = new ArrayList<Integer>();
        while(remainder > 0) {
            digits.add(remainder % 10);
            remainder = remainder / 10;
        }
        return Integer.parseInt(
                digits.stream()
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(""))
        );
    }

    private static boolean numAlreadyExists(int num, Map<Integer, Boolean> numDict) {
        var sortedNum = sortedNum(num);
        if (numDict.containsKey(sortedNum)) {
            return true;
        }

        numDict.put(sortedNum, Boolean.TRUE);
        return false;
    }

    private static int[] readInput() {
        Scanner scn = new Scanner(System.in);
        int n = Integer.parseInt(scn.nextLine());
        int[] snowflakes = new int[n];

        for(int i = 0; i < n; i++) {
            var line = scn.nextLine();
            StringBuilder sb = new StringBuilder();
            for(int j = 0; j < line.length(); j++) {
                var s = String.valueOf(line.charAt(j));
                if (!s.isBlank()) {
                    sb.append(s);
                }
            }
            snowflakes[i] = Integer.parseInt(sb.toString());
        }
        scn.close();
        return snowflakes;
    }

    public static void main(String[] args) {
        var snowflakes = readInput();
        var start = Instant.now();
        var result = identifyIdentical(snowflakes.length, snowflakes);
        var timeTaken = Duration.between(Instant.now(), start);
        if (timeTaken.getSeconds() >= 120) {
            throw new RuntimeException("Exceeded max execution time");
        }

        System.out.println(result);
    }
}
