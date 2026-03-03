package com.github.learntocode2013;

// Add code comments for the code below
public class HelloRecords {
    record Point(int x, int y) { }

    public static void main(String[] args) {
        var aPoint = new Point(1, 2);

        System.out.println(aPoint);

        System.out.printf("""
                x = %s
                y = %s
                """, aPoint.x(), aPoint.y());
    }
}
