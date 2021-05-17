package com.web;

import java.io.*;
import java.util.*;

public class wc4 {
  public static void main(String[] args) {
    try {
      File in1 = new File("../data/in/1/wc2.txt");
      File in2 = new File("../data/in/2/wc3.txt");

      BufferedReader br1 = new BufferedReader(new FileReader(in1));
      BufferedReader br2 = new BufferedReader(new FileReader(in2));

      String st1;
      String st2;
      String[] arr1;
      String[] arr2;
      String[] vals1;
      String[] vals2;

      HashMap<String, Integer> hm = new HashMap<String, Integer>();

      st1 = br1.readLine();
      st1 = st1.substring(1, (st1.length() - 1));

      st2 = br2.readLine();
      st2 = st2.substring(1, (st2.length() - 1));

      arr1 = st1.split(", ");
      arr2 = st2.split(", ");

      for (String s : arr1) {
        vals1 = s.split("=");
        if (hm.containsKey(vals1[0])) {
          hm.put(vals1[0], (hm.get(vals1[0]) + Integer.parseInt(vals1[1])));
        } else {
          hm.put(vals1[0], Integer.parseInt(vals1[1]));
        }
      }

      for (String s : arr2) {
        vals2 = s.split("=");
        if (hm.containsKey(vals2[0])) {
          hm.put(vals2[0], (hm.get(vals2[0]) + Integer.parseInt(vals2[1])));
        } else {
          hm.put(vals2[0], Integer.parseInt(vals2[1]));
        }
      }

      String hmStr = hm.toString();
      File out = new File("../data/out/results.txt");
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      bw.write(hmStr);
      bw.close();
      br1.close();
      br2.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
