package com.github.learntocode2013;


import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TriFunctionTest {
    @Test
    void test() {
        TriFunction<Integer, Integer, Integer, Integer> multiplyAndThenAdd = (x, y, z) -> x * y + z;
        TriFunction<Integer, Integer, Integer, Integer> multiplyThenAddAndThenDivideByTen = multiplyAndThenAdd
                .andThen(x -> x /10);
        Integer result = multiplyThenAddAndThenDivideByTen.apply(2, 5, 0);
        assertEquals(1, result.intValue());
    }

    @Test
    void testSortedMap() {
       SortedMap<String, String> sortedKVPair = new ConcurrentSkipListMap<>();
        sortedKVPair.put("Venket", "US");
        sortedKVPair.put("Dibakar", "India");
        sortedKVPair.put("Alexander", "Estonia");
        System.out.printf("%s %n", sortedKVPair);
    }
}
