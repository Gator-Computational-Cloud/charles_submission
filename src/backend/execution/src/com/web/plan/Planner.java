package com.web.plan;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import com.web.exec.*;
import com.web.aws.*;
import com.web.node.*;
import com.web.jsch.*;

public class Planner {
  private static HashMap<Integer, ArrayList<NodeADT>> hm = new HashMap<Integer, ArrayList<NodeADT>>();
  private static ArrayList<NodeADT> nadtl = new ArrayList<NodeADT>();
  private static HashMap<Integer, NodeADT> ledger = new HashMap<Integer, NodeADT>();
  private final String access_key;
  private final String secret_key;
  private final String token;
  private  Double budget;

  final String src_dir;

  public Planner(String access_key, String secret_key, String token, String src_dir) {
    // Constructor
    this.access_key = access_key;
    this.secret_key = secret_key;
    this.token = token;

    this.src_dir = src_dir;
  }

  public void parseXml(File f1, File f2, String user, String wf_name, String pem_path, boolean isTest) {


    try {

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      Document doc1 = dbf.newDocumentBuilder().parse(f1);
      doc1.getDocumentElement().normalize();

      NodeList nl = doc1.getElementsByTagName("task");
      NodeList bn = doc1.getElementsByTagName("budget");
      NodeList n2 = null;

      if (!isTest) {
        Document doc2 = dbf.newDocumentBuilder().parse(f2);
        doc2.getDocumentElement().normalize();
        n2 = doc2.getElementsByTagName("task");
      }

      budget = Double.parseDouble(bn.item(0).getTextContent());

      for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) n;

          int id = Integer.parseInt(e.getElementsByTagName("id").item(0).getTextContent());
          ArrayList<Integer> deps = new ArrayList<>();
          ArrayList<String> weights = null;

          if (!(e.getElementsByTagName("deps").item(0).getTextContent().equals(""))) {
            ArrayList<String> depsHelper = new ArrayList<>(Arrays.asList(e.getElementsByTagName("deps").item(0).getTextContent().split(",")));

            deps = depsHelper.stream()
                    .mapToInt(Integer::parseInt)
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));
          }
          int service =
           n2 != null ? Integer.parseInt(((Element) n2.item(i))
                           .getElementsByTagName("service")
                           .item(0).getTextContent()) : -1;

          String node_path =
                  src_dir + "/USERS/" + user + "/workflows/" + wf_name + "/nodes/n" + (id+1) +
                          "/n" + (id+1) + ".zip";
          NodeADT nadt = new NodeADT(id, serviceInfo.matchService(service), node_path, pem_path, deps, weights,
                  access_key,
                  secret_key, token, user, wf_name, src_dir);
          nadtl.add(nadt);
          ledger.put(id, nadt);
        }
      }
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public DirectedAcyclicGraph<NodeADT, DefaultEdge> plan() {
    DirectedAcyclicGraph<NodeADT, DefaultEdge> pl = new DirectedAcyclicGraph<>(null, SupplierUtil.createDefaultEdgeSupplier(), true);

    while(true) {
      ArrayList<NodeADT> lint = new ArrayList<>();
      ArrayList<Integer> compIds = new ArrayList<>();
      int max = 0;

      for (Integer key : hm.keySet()) {
        max = key;
        for (NodeADT nt : hm.get(key)) {
          lint.add(nt);
          compIds.add(nt.getId());
        }
      }

      if (lint.equals(nadtl)) {
        break;
      }

      for (NodeADT t : nadtl) {
        if (t.getDeps() == null) {
          if (hm.get(0) != null) {
            if (!(hm.get(0).contains(t))) {
              hm.get(0).add(t);
            }
          } else {
            ArrayList<NodeADT> temp = new ArrayList<NodeADT>();
            temp.add(t);
            hm.put(0, temp);
          }
        } else {
          int ct = 0;

          if (!(lint.contains(t))) {
            if (compIds.containsAll(t.getDeps())) {
              if (hm.get((max + 1)) != null) {
                hm.get((max + 1)).add(t);
              } else {
                ArrayList<NodeADT> temp = new ArrayList<NodeADT>();
                temp.add(t);
                hm.put((max + 1), temp);
              }
            }
          }
        }
      }
    }

    try {
      ArrayList<NodeADT> nl;
      ArrayList<String> dl;
      DefaultEdge e;

      for (Integer key : hm.keySet()) {
        nl = hm.get(key);
        for (int i = 0; i < nl.size(); i++) {
          pl.addVertex(nl.get(i));
          if (nl.get(i).getDeps() != null) {
            for (int j = 0; j < nl.get(i).getDeps().size(); j++) {
              pl.addEdge(ledger.get(nl.get(i).getDeps().get(j)), nl.get(i));
            }
          }
        }
      }
    } catch (Exception a) {
      a.printStackTrace();
    }

    return pl;
  }

  public void setNadtl(ArrayList<NodeADT> nadtl) {
    Planner.nadtl = nadtl;
  }

  public void setBudget(double budget) {
    this.budget = budget;
  }

  public HashMap<Integer, ArrayList<NodeADT>> getHmap() {
    return hm;
  }

  public ArrayList<NodeADT> getNadtl() {
    return nadtl;
  }

  public HashMap<Integer, NodeADT> getLedger() {
    return ledger;
  }

  public double getBudget() {
    return budget;
  }
}
