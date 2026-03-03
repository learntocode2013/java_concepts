package com.github.learntocode2013.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class StudentSerializer extends StdSerializer<Student> {
    public StudentSerializer() {
        this(null);
    }

    protected StudentSerializer(Class<Student> t) {
        super(t);
    }

    @Override
    public void serialize(Student student, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("first_name", student.getFirstName());
        gen.writeStringField("last_name", student.getLastName());
        gen.writeStringField("age", student.getDob().toString());
        gen.writeStringField("graduation", student.getGraduation().toString());
        gen.writeEndObject();
    }
}
