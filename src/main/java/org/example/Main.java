package org.example;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        StringCalculator sc = new StringCalculator();
        System.out.println(sc.add("1\n2,3")); // Output: 6

        System.out.println(sc.add("//;\n1;2"));
    }
}