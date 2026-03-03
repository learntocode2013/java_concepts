package com.github.learntocode2013;

import java.lang.reflect.RecordComponent;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;

public class DemoRecords {
  record Location(double latitude, double longitude) implements Json {
    // Custom canonical constructor
    public Location {
      if (latitude <= -90 || latitude > 90 ||
          longitude <= -180 || longitude > 180) {
        throw new IllegalArgumentException("Your location is out of this world");
      }
    }
    // Custom non-canonical constructor
    public Location(String position) {
      this(
          Double.parseDouble(position.split(":")[0]),
          Double.parseDouble(position.split(":")[1])
      );
    }

    @Override
    public String generateJson() {
      return """
          {
            "latitude": %s,
            "longitude": %s
          }
          """.formatted(latitude(), longitude());
    }
  }
  record Author(String name, List<String> books) {}

  static class Stocks {
    public static int simulatePrice(String ticker) {
      return 200 + ticker.chars().sum();
    }
  }

  @SneakyThrows
  private static void printRecordMetaData(Class<? extends Record> klaz) {
    System.out.println("------ Record meta data ------");
    System.out.printf("%s is a record: %s \n", klaz.getSimpleName(), klaz.isRecord());
    var bangalore = new Location(12.3, 24.3);
    for(RecordComponent component : klaz.getRecordComponents()) {
      var name = component.getName();
      var type = component.getType();
      var value = component.getAccessor().invoke(bangalore);
      System.out.printf("%s %s has value: %s \n", type, name, value);
    }
    System.out.println();
  }

  private static void designForImmutability() {
    System.out.println("---------- ensure component immutability ---------");
    var subject = new Author(
        "Venket Subramaniam",
        List.of(
            "Cruising along with Java",
            "Seven Databases in seven weeks")
    );
    for(var component : subject.getClass().getRecordComponents()) {
      var name = component.getName();
      var type = component.getType();
      System.out.printf("%s is of type : %s \n", name, type);
    }
    subject.books().add("Streaming with Java");
    System.out.println();
  }

  private static void basicUsage() {
    System.out.println("---------- basic usage ---------");
    var loc1 = new Location(12.3, 24.3);
    var loc2 = new Location(12.3, 24.3);
    var loc3 = new Location(15.3, 25.3);
    var loc4 = new Location("17.3:29.3");
    System.out.println(loc1);
    System.out.println(loc1.hashCode());
    System.out.println(loc1.generateJson());
    System.out.printf("%s equals %s: %s \n", loc1, loc2, loc1.equals(loc2));
    System.out.printf("%s equals %s: %s \n", loc1, loc3, loc1.equals(loc3));
    System.out.println(loc4.generateJson());
    System.out.println();
  }

  private static void useAsTuple(List<String> tickers) {
    System.out.println("---------- use as tuple --------");
    record Stock(String ticker, int price) {
      @Override
      @NonNull
      public String toString() {
        return "Ticker: " + ticker + " Price: $ " + price;
      }
    }
    tickers.stream()
        .map(ticker -> new Stock(ticker, Stocks.simulatePrice(ticker)))
        .filter(stock -> stock.price() >= 500)
        .forEach(System.out::println);
    System.out.println();
  }

  public static void main(String[] args) {
    printRecordMetaData(Location.class);
    basicUsage();
    useAsTuple(List.of(
        "GOOG",
        "AMZ",
        "TLSA",
        "NVDA",
        "TWLO",
        "CSCO"
    ));
    designForImmutability();
  }
}
