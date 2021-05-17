package com.web;

import java.io.*;
import java.util.*;

public class wc1 {
  public static void main(String[] args) {
    try {
      System.out.println("counting lines...");

      File words = new File("../data/in/words.txt");
      BufferedReader br = new BufferedReader(new FileReader(words));

      ArrayList<String> lines = new ArrayList<String>();
      String st;

      while ((st = br.readLine()) != null) {
        lines.add(st);
      }

      System.out.println("\t[-] # of lines: " + lines.size());
      System.out.println("splitting file...");

      File out1 = new File("../data/out/1/wc1_1.txt");
      File out2 = new File("../data/out/2/wc1_2.txt");

      BufferedWriter bw1 = new BufferedWriter(new FileWriter(out1));
      BufferedWriter bw2 = new BufferedWriter(new FileWriter(out2));

      for (int i = 0; i < lines.size() / 2; i++) {
        if (i == ((lines.size() / 2) - 1)) {
          bw1.write(lines.get(i));
        } else {
          bw1.write(lines.get(i) + "\n");
        }
      }

      for (int i = lines.size() / 2; i < lines.size(); i++) {
        if (i == (lines.size() - 1)) {
          bw2.write(lines.get(i));
        } else {
          bw2.write(lines.get(i) + "\n");
        }
      }

      br.close();
      bw1.close();
      bw2.close();

      System.out.println("\t[-] done!");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
