package com.web.plan;

public class serviceInfo {
    static String matchService(int index) {
        return switch (index) {
            case 0 -> "t2.nano";
            case 1 -> "t2.micro";
            case 2 -> "t2.small";
            default -> "test";
        };
    }
}