package com.github.learntocode2013.hashtables;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class IdenticalSnowflakesV2 {
    private static final String ANY_MATCH_MSG = "Twin snowflakes found.";
    private static final String NONE_MATCH_MSG = "No two snowflakes are alike.";
    private static final int SNOW_FLAKE_ARMS = 6;
    private static final int MAX_SNOWFLAKES = 100_000;

    // Note how we used "chaining" instead of "open addressing" for the hash buckets
    private static final Map<Integer, List<Integer>> sameSumDict = new HashMap<>(MAX_SNOWFLAKES);

    private static String identifyIdentical(int[][] snowflakes ,Map<Integer, List<Integer>> sumDict) {
        var entries = sumDict.entrySet();
        for(var entry : entries) {
            if (entry.getValue().size() >= 2) {
                var sameSumIndexList = entry.getValue();
                for(int i = 0; i < sameSumIndexList.size(); i++) {
                    for(int j = i+1; j < sameSumIndexList.size(); j++) {
                        if (areIdentical(
                                snowflakes[sameSumIndexList.get(i)],
                                snowflakes[sameSumIndexList.get(j)])
                        ) {
                            System.out.printf("Snowflakes: %s and %s are identical %n",
                                    Arrays.toString(snowflakes[sameSumIndexList.get(i)]),
                                    Arrays.toString(snowflakes[sameSumIndexList.get(j)]));
                            return ANY_MATCH_MSG;
                        }
                    }
                }
            }
        }
        return NONE_MATCH_MSG;
    }

    private static boolean areIdentical(int[] snow1, int[] snow2) {
        for (int start = 0; start < SNOW_FLAKE_ARMS; start++) {
            if (identicalRight(snow1, snow2, start)) {
                return true;
            }
            if (identicalLeft(snow1, snow2, start)) {
                return true;
            }
        }
        return false;
    }

    private static boolean identicalRight(int[] snow1, int[] snow2, int start) {
        for(int offset = 0; offset < SNOW_FLAKE_ARMS; offset++) {
            if (snow1[offset] == snow2[(offset + start) % SNOW_FLAKE_ARMS]) {
                return  true;
            }
        }
        return false;
    }

    private static boolean identicalLeft(int[] snow1, int[] snow2, int start) {
        int offset, snow2Index;
        for (offset = 0; offset < SNOW_FLAKE_ARMS; offset++) {
            snow2Index = start - offset;
            snow2Index = snow2Index < 0 ? snow2Index + SNOW_FLAKE_ARMS : snow2Index;
            if (snow1[offset] == snow2[snow2Index]) {
                return true;
            }
        }
        return false;
    }


    private static int hashCode(int[] snowflake) {
        int sum = 0;
        for (var elem : snowflake) {
            sum += elem;
        }
        return sum % MAX_SNOWFLAKES;
    }

    private static int[][] readInput(Map<Integer, List<Integer>> containerDict) throws Exception {
        try(BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            int n = Integer.parseInt(consoleReader.readLine());
            int[][] snowflakes = new int[n][6];
            for(int i = 0; i < n; i++) {
                int[] snowflake = toIntArray(consoleReader.readLine().split("\\s+"));
                containerDict.computeIfAbsent(
                        hashCode(snowflake), k -> new ArrayList<>()).add(i);
                snowflakes[i] = snowflake;
            }
            return snowflakes;
        }
    }

    private static int[] toIntArray(String[] arr) {
        int[] result = new int[arr.length];
        for(var i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(arr[i]);
        }
        return result;
    }

    private static void printArray(int[][] snowflakes) {
        System.out.printf("[ %n");
        for(var arr : snowflakes) {
            System.out.println(Arrays.toString(arr));
        }
        System.out.printf("]%n");
    }

    public static void main(String[] args) throws Exception {
        int[][] snowflakes = readInput(sameSumDict);
        printArray(snowflakes);
        System.out.println(sameSumDict);
        var result = identifyIdentical(snowflakes, sameSumDict);
        System.out.println(result);
    }
}
