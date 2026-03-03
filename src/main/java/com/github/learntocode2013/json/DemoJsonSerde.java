package com.github.learntocode2013.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.Month;

public class DemoJsonSerde {
    private static void demoSerialization() {
        var dob = LocalDate.of(1984, Month.FEBRUARY, 20);
        var graduation = LocalDate.of(2006, Month.MAY, 21);
        var dibakar = Student.of("Dibakar", "Sen", dob, graduation);
        var mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule());

        try {
            var jsonPayload = mapper.writeValueAsString(dibakar);
            System.out.printf("%s %n",jsonPayload);
        } catch (Exception cause) {
            cause.printStackTrace();
        }

    }

    private static void demoDeserialization() {
        var jsonPayload = """
                {
                  "firstName" : "Dibakar",
                  "lastName" : "Sen",
                  "dob" : [ 1984, 2, 20 ],
                  "graduation" : [ 2006, 5, 21 ]
                }
                """;
        var mapper = new ObjectMapper();

        try {
            var dibakar = mapper.readValue(jsonPayload, Student.class);
            System.out.printf("%s %n", dibakar);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void demoCustomDeserializer() {
        var module = new SimpleModule();
        module.addDeserializer(Student.class, new StudentDeserializer());
        var mapper = new ObjectMapper().registerModule(module);

        var jsonPayload = """
                {
                  "first_name" : "Dibakar",
                  "last_name" : "Sen",
                  "age" : "1984-02-20",
                  "graduation" : "2006-05-21"
                }
                """;

        try {
            var dibakar = mapper.readValue(jsonPayload, Student.class);
            System.out.printf("%s %n", dibakar);
        } catch (Exception cause) {
            cause.printStackTrace();
        }
    }

    private static void demoCustomSerializer() {
        var module = new SimpleModule();
        module.addSerializer(Student.class, new StudentSerializer());
        var dob = LocalDate.of(1984, Month.FEBRUARY, 20);
        var graduation = LocalDate.of(2006, Month.MAY, 21);
        var dibakar = Student.of("Dibakar", "Sen", dob, graduation);
        var mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(module);

        try {
            var jsonPayload = mapper.writeValueAsString(dibakar);
            System.out.printf("%s %n", jsonPayload);
        } catch (Exception cause) {
            cause.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        demoSerialization();
//        demoDeserialization();
//        demoCustomSerializer();
        demoCustomDeserializer();
    }
}
