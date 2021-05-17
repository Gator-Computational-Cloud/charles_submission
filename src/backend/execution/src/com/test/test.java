package com.test;
import com.web.ExecDriver;
import com.web.node.NodeADT;
import com.web.plan.Planner;
import com.web.plan.bheft.CommandLineTable;
import com.web.plan.bheft.ResourceUsages;
import com.web.plan.bheft.Workflow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;


public class test {

    public static void main(String[] args) throws TransformerException, ParserConfigurationException, IOException, SAXException {

        String src_dir;
        if (args.length == 3) {
            String user = args[0];
            String wf_name = args[1];
            String wfBHEFT_name = args[2];

            String iterName = wf_name + System.nanoTime();

            wfBHEFT_name = wfBHEFT_name.equals("test") ? wf_name + "BHEFT" : wfBHEFT_name;

            String temp = System.getProperty("user.dir");
            System.out.println(temp);
            src_dir = temp.substring(0, temp.lastIndexOf("\\"));

            String wf_path = src_dir + "/USERS/" + user + "/workflows/" + wf_name + "/" + wf_name + ".xml";
            String wfBHEFT_path = src_dir + "/USERS/" + user + "/workflows/" + wf_name + "/" + wfBHEFT_name + ".xml";
            String wfRes_path =
                    src_dir + "/USERS/" + user + "/executions/" + wf_name + "/results/";
//            String pem_path = src_dir + "/USERS/" + user + "/executions/" + wf_name + "/pem/" + iterName +
//                    ".pem";

            File wf = new File(wf_path);
//            File wfBHEFT = new File(wfBHEFT_path);


            ResourceUsages resUsages = new ResourceUsages(new File(src_dir + "/USERS/resUsage.xml"));

            Workflow BHEFT = new Workflow(user, wf,
                    new File(wfRes_path + wf_name + "Results.xml"),
                    new ArrayList<>(resUsages.getTransRates()));

            BHEFT.calcBHEFT(resUsages);

            CommandLineTable taskTable = BHEFT.createTaskTable(resUsages);
            CommandLineTable transferTable = BHEFT.createTransTable(resUsages);
            CommandLineTable calcTable = BHEFT.createCalcTable();
            CommandLineTable costTable = BHEFT.createCostTable(resUsages);

            BHEFT.getContents().stream().forEach(System.out::println);



            costTable.printTable();
            transferTable.printTable();
            taskTable.printTable();
            calcTable.printTable();



            BHEFT.createNodeOrderXML(wfBHEFT_path);
        }

        String temp = System.getProperty("user.dir");
        src_dir = temp.substring(0, temp.lastIndexOf("/"));

        String wfRes_path = src_dir + "/USERS/misback/executions/wf1/results/wf1Results" +
                ".xml";
        Planner pr = new Planner("", "", "", src_dir);
        ArrayList<NodeADT> nadTL = new ArrayList<>();
        nadTL.add(new NodeADT(0, "0", null, null, null, null, null, null, null, null, null, src_dir));

        nadTL.add(new NodeADT(1, "1", null, null, null, null, null, null, null, null, null, src_dir));

        nadTL.add(new NodeADT(2, "2", null, null, null, null, null, null, null, null, null, src_dir));

        nadTL.add(new NodeADT(3, "0", null, null, null, null, null, null, null, null, null, src_dir));

        pr.setNadtl(nadTL);
        pr.setBudget(150);

        ArrayList<Double> node1Durations = new ArrayList<>() {{ add(10.0); add(25.0); add(38.0); }};
        ArrayList<Double> node2Durations = new ArrayList<>() {{ add(15.0); add(27.0); add(32.0); }};
        ArrayList<Double> node3Durations = new ArrayList<>() {{ add(13.0); add(25.0); add(34.0); }};
        ArrayList<Double> node4Durations = new ArrayList<>() {{ add(15.0); add(22.0); add(38.0); }};
        ArrayList<ArrayList<Double>> durations = new ArrayList<>();
        durations.add(node1Durations); durations.add(node2Durations);
        durations.add(node3Durations); durations.add(node4Durations);



        ArrayList<String> node1Weights = new ArrayList<>();
        ArrayList<String> node2Weights = new ArrayList<>() {{ add(10.0 + "");}};
        ArrayList<String> node3Weights = new ArrayList<>() {{ add(25.0 + "");}};
        ArrayList<String> node4Weights = new ArrayList<>() {{ add(10.0 + ""); add(25.0 + "");}};
        ArrayList<ArrayList<String>> weights = new ArrayList<>();
        weights.add(node1Weights); weights.add(node2Weights);
        weights.add(node3Weights); weights.add(node4Weights);

//        NodeADT.createNodeOrderXML(wfRes_path, 4, pr, durations,
//                weights);

        ArrayList<String> flatWeights = new ArrayList<>();
        int m = 0;
        for (ArrayList<String> weightList: weights) {
            for (String weight: weightList) {
                flatWeights.add(m, weight);
            }
        }

        flatWeights.forEach(System.out::println);

    }
}
