package com.github.learntocode2013.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

public class StudentDeserializer extends StdDeserializer<Student> {
    public StudentDeserializer() {
        this(null);
    }

    protected StudentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Student deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
        JsonNode node = jp.getCodec().readTree(jp);
        var fName = node.get("first_name").asText();
        var lName = node.get("last_name").asText();
        var ageDateStrArr =  node.get("age").asText().split("-");
        var gradDateStrArr = node.get("graduation").asText().split("-");
        var dob = LocalDate.of(
                Integer.parseInt(ageDateStrArr[0]),
                Month.of(Integer.parseInt(ageDateStrArr[1])),
                Integer.parseInt(ageDateStrArr[2]));
        var graduation = LocalDate.of(
                Integer.parseInt(gradDateStrArr[0]),
                Month.of(Integer.parseInt(gradDateStrArr[1])),
                Integer.parseInt(gradDateStrArr[2]));
        return Student.of(fName, lName, dob, graduation);

    }
}
