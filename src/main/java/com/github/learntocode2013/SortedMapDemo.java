package com.github.learntocode2013;

import com.google.common.primitives.Bytes;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class SortedMapDemo {

  public static void main(String[] args) {
    SortedMap<String, String> engineers = new ConcurrentSkipListMap<>();
    engineers.put("Venket", "US");
    engineers.put("Dibakar", "India");
    engineers.put("Alexander", "Estonia");

    //Related data in the form of two level map
    HashMap<String,  SortedMap<String, String>> employeeInfo = new HashMap<>();
    employeeInfo.put("Engineers", engineers);
    employeeInfo.put("Operating_Group", new ConcurrentSkipListMap<>(){{
      put("Trisha Gee", "US");
      put("Jessica Kerr", "US");
      put("Cal Newport", "US");
    }});

    System.out.printf("Two level map -> %s %n", employeeInfo);
    System.out.println();
    System.out.println("---- Simpler use cases ---");

    // Named set
    HashMap<String,  SortedMap<String, String>> namedSets = new HashMap<>();
    namedSets.put("business_units", new ConcurrentSkipListMap<>() {{
      put("Data and application", "");
      put("Communication", "");
      put("Emerging Technology", "");
    }});
    System.out.printf("Business units %n %s %n", namedSets.get("business_units").keySet().stream().collect(
        Collectors.joining("\n")));
  }
}
