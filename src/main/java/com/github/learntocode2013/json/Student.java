package com.github.learntocode2013.json;

import java.time.LocalDate;

public final class Student extends Person{
    private final LocalDate graduation;


    private Student(String firstName, String lastName, LocalDate dob, LocalDate graduation) {
        super(firstName, lastName, dob);
        this.graduation = graduation;
    }

    public static Student of(String firstName, String lastName, LocalDate dob, LocalDate graduation) {
        return new Student(firstName, lastName, dob, graduation);
    }

    public LocalDate getGraduation() {
        return graduation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Student student = (Student) o;

        return graduation.equals(student.graduation);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + graduation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Student{");
        sb.append("firstName=").append(getFirstName()).append(", ");
        sb.append("lastName=").append(getLastName()).append(", ");;
        sb.append("graduation=").append(graduation);
        sb.append('}');
        return sb.toString();
    }
}
