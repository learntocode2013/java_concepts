package com.github.learntocode2013;

import java.util.List;

/**
 * Hello world!
 * /Users/dsen/.sdkman/candidates/java/8.0.342-amzn/jre/lib/rt.jar
 */
// ---- This results in compilation error -------//
// javac com/github/learntocode2013/JavaSourceTargetDemo.java -source 8 -target 8 -bootclasspath /Users/dsen/.sdkman/candidates/java/8.0.342-amzn/jre/lib/rt.jar
//--------------------------------//
public class JavaSourceTargetDemo
{
    public static void main( String[] args )
    {
        System.out.println(List.of("Hello", "World", "!"));
    }
}
