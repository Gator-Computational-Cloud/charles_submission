package com.web;

import com.web.aws.AwsProvisioner;
import com.web.exec.PlanExecutor;
import com.web.node.NodeADT;
import com.web.plan.Planner;
import com.web.plan.bheft.ResourceUsages;
import com.web.plan.bheft.Workflow;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ExecDriver {
  public static void main(String[] args) {
    /**
     Code to execute workflows in the cloud
     **/
    final String BHEFTcode_path = "execution/src/com/web/plan/bheft/";


    final String user;
    final String wf_name;
    String wfBHEFT_name;
    final String wf_path;
    final String wfRes_path;
    final String wfBHEFT_path;
    final String pem_path;
    final String access_key;
    final String secret_key;
    final String pem_mat;
    final String token;

    final String src_dir;

    String iterName;

    if (args.length != 6) {
      System.exit(-1);
    } else {
      // ====================================================================
      try {
        // ==================================================================
        System.out.println("test");
        user = args[0];
        wf_name = args[1];
        wfBHEFT_name = args[2];
        access_key = args[3];
        secret_key = args[4];
        token = args[5];

        iterName = wf_name + System.nanoTime();

        wfBHEFT_name = wfBHEFT_name.equals("test") ? wf_name + "BHEFT" : wfBHEFT_name;

        String temp = System.getProperty("user.dir");
        System.out.println(temp);
        src_dir = temp.substring(0, temp.lastIndexOf("\\"));

        wf_path = src_dir + "/USERS/" + user + "/workflows/" + wf_name + "/" + wf_name + ".xml";
        wfBHEFT_path = src_dir + "/USERS/" + user + "/workflows/" + wf_name + "/" + wfBHEFT_name + ".xml";
        wfRes_path =
                src_dir + "/USERS/" + user + "/executions/" + wf_name + "/results/";
        pem_path = src_dir + "/USERS/" + user + "/executions/" + wf_name + "/pem/" + iterName +
                ".pem";

        File wf = new File(wf_path);
        File wfBHEFT = new File(wfBHEFT_path);
        File pem = new File(pem_path);

        System.out.println(wf_path);
        if (wf.exists()) {
          Planner pr = new Planner(access_key, secret_key, token, src_dir);
          pr.parseXml(wf, wfBHEFT, user, wf_name, iterName, args[2].equals("test"));

          ArrayList<NodeADT> nadtl = pr.getNadtl();
          HashMap<Integer, NodeADT> ledger = pr.getLedger();

          AwsProvisioner aws = new AwsProvisioner(access_key, secret_key, token);
          pem_mat = aws.createKeyPair(iterName);
          aws.createSecurityGroup(iterName);

          if (pem.createNewFile()) {
            System.out.println("file created : " + pem.getName());
          }

          BufferedWriter bw = new BufferedWriter(new FileWriter(pem, false));
          bw.write(pem_mat);
          bw.close();

          for (NodeADT n : nadtl) {
            n.ei.setPemPath(pem_path);
            n.ei.setKpName(iterName);
            n.ei.setSgName(iterName);
            n.setLedger(ledger);
          }

          DirectedAcyclicGraph<NodeADT, DefaultEdge> pl = pr.plan();
          HashMap<Integer, ArrayList<NodeADT>> hm = pr.getHmap();
          PlanExecutor pex = new PlanExecutor();
          pex.executePlan(pl, hm);

          for (NodeADT node : nadtl) {
            aws.terminateInstance(node.getEi().getInsId());
          }

          while (!nadtl.stream().allMatch(x -> aws.describeInstance(x.getEi().getInsId()).getState().getCode() == 48)) {
            Thread.sleep(5000);
          }

          aws.deleteKeyPair(iterName);
          aws.deleteSecurityGroup(iterName);

          ArrayList<ArrayList<Double>> durations = new ArrayList<>();
          ArrayList<ArrayList<String>> weights = new ArrayList<>();

          int i = 0;
          for (NodeADT n : nadtl) {
            BufferedReader reader = new BufferedReader(new FileReader(wfRes_path + "/n" + n.getId() + "/stats.txt"));
            String line;
            int lineIndex = 0;
            durations.add(new ArrayList<>());
            weights.add(new ArrayList<>());

            while ((line = reader.readLine()) != null) {
              if (line.contains("s")) {
                durations.get(i).add(NodeADT.parseDuration(line));
              }
              if (line.matches("[^a-zA-Z]+")) {
                weights.get(i).add(line);
              }
            }
            reader.close();
            i++;
          }

          ArrayList<ArrayList<String>> sortedListsofWeights = new ArrayList<>();
          ArrayList<String> flatWeights = new ArrayList<>();
          ArrayList<String> sortedWeights;

          int m = 0;
          for (ArrayList<String> weightList : weights) {
            for (String weight : weightList) {
              flatWeights.add(m, weight);
            }
          }

          for (NodeADT nodeADT : nadtl) {
            sortedWeights = new ArrayList<>();
            for (int k = 0; k < nodeADT.getDeps().size(); k++) {
              sortedWeights.add(flatWeights.get(m++));
            }
            sortedListsofWeights.add(sortedWeights);
          }

          NodeADT.createNodeOrderXML(wfRes_path + wf_name + "Results.xml", nadtl.size(), pr, durations, sortedListsofWeights);

          ResourceUsages resUsages = new ResourceUsages(new File(src_dir + "/USERS/resUsage.xml"));

          Workflow BHEFT = new Workflow(user, wf,
                  new File(wfRes_path + wf_name + "Results.xml"),
                  new ArrayList<>(resUsages.getTransRates()));

          BHEFT.calcBHEFT(resUsages);

          BHEFT.createNodeOrderXML(wfBHEFT_path);

        } else {
          System.out.println("Invalid Workflow");
        }
        // ==================================================================
      } catch (Exception e) {
        e.printStackTrace();
      }
      // ======================================================================
    }
  }
}
