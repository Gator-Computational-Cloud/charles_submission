package com.web;

import java.io.*;
import java.util.*;

public class wc2 {
  public static void main(String[] args) {
    try {
      File in = new File("../data/in/0/wc1_1.txt");
      BufferedReader br = new BufferedReader(new FileReader(in));

      ArrayList<String> lines = new ArrayList<String>();
      String st;

      while ((st = br.readLine()) != null) {
        lines.add(st);
      }

      br.close();

      HashMap<String, Integer> hm = new HashMap<String, Integer>();

      for (int i = 0; i < lines.size(); i++) {
        for (int j = 0; j < lines.get(i).length(); j++) {
          // lines.get(i).charAt(j)
          String charStr = Character.toString(lines.get(i).charAt(j));
          if (hm.containsKey(charStr)) {
            hm.put(charStr, (hm.get(charStr) + 1));
          } else {
            hm.put(charStr, 1);
          }
        }
      }

      String hmStr = hm.toString();
      File out = new File("../data/out/3/wc2.txt");
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      bw.write(hmStr);
      bw.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
