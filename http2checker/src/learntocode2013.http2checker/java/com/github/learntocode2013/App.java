package com.github.learntocode2013;

import java.util.stream.IntStream;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        printDelimiter();
        IntStream.rangeClosed(1, 10).forEach(i -> System.out.print(" "));
        System.out.println( "Hello World from new module" );
        printDelimiter();
    }

    private static void printDelimiter() {
        IntStream.rangeClosed(1, 50).forEach(i -> System.out.print("-"));
        System.out.println();
    }
}
