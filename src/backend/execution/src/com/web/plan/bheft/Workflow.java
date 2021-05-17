package com.web.plan.bheft;
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
import java.io.File;
import java.io.IOException;
import java.util.*;


public class Workflow {
    String user;
    ArrayList<BNode> contents;
    ArrayList<Double> serviceTransTimes;
    ArrayList<BNode> ogOrder;
    double Budget;

    public Workflow(String userIn, File fNodes, File fResults, ArrayList<Double> serviceTransTimesIn) throws IOException,
            SAXException,
            ParserConfigurationException {
        this.user = userIn;
        serviceTransTimes = serviceTransTimesIn;
        CreateNodes(fNodes, fResults);

    }

    public ArrayList<BNode> getContents() {
        return contents;
    }

    private void CreateNodes(File fNodes, File fResults) throws ParserConfigurationException, IOException,
            SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbNodes = dbf.newDocumentBuilder();

        Element docNodes = dbNodes.parse(fNodes).getDocumentElement();
        Element docResults;
        NodeList weightsNodes, RTsNodes, labelNodes, DepsNodes, budgetNode;
        RTsNodes = null; labelNodes = null; DepsNodes = null; budgetNode = null;
        weightsNodes = null;
        boolean hasRealTimes = false;

        if (fResults != null) {
            DocumentBuilder dbResults = dbf.newDocumentBuilder();
            docResults = dbResults.parse(fResults).getDocumentElement();

            weightsNodes = docResults.getElementsByTagName("weights");
            RTsNodes = docResults.getElementsByTagName("rts");
            labelNodes = docResults.getElementsByTagName("label");

            hasRealTimes = RTsNodes.getLength() != 0;
        }

        contents = new ArrayList<>();
        BNode currNode;

        DepsNodes = docNodes.getElementsByTagName("deps");
        budgetNode = docNodes.getElementsByTagName("budget");

        Budget = Double.parseDouble( budgetNode.item(0).getTextContent());

        for (int i = 0; i < DepsNodes.getLength(); i++) {

            if (labelNodes.getLength() != 0) {
                currNode  = new BNode(i, labelNodes.item(i).getTextContent());
            } else {
                currNode  = new BNode(i, null);
            }
            contents.add( currNode);

            ArrayList<String> RTs = new ArrayList<>();
            ArrayList<String> weights = new ArrayList<>();

            if (hasRealTimes) {
                String rtsRaw = RTsNodes.item(i).getTextContent();
                Collections.addAll(RTs, rtsRaw.split(","));

                for (String rt : RTs) {
                    double time = Double.parseDouble(rt);
                    currNode.addRTimeForRes(time);
                }

                if (i == 0) { continue;}

                String weightsRaw = weightsNodes.item(i).getTextContent();
                Collections.addAll(weights, weightsRaw.split(","));

            }

            String depsRaw = DepsNodes.item(i).getTextContent();
            ArrayList<String> deps = new ArrayList<>();
            Collections.addAll(deps, depsRaw.split(","));
            if (deps.get(0).equals("")) {
                deps.clear();
            }

            for (int j = 0; j < deps.size(); j++) {
                int parID = Integer.parseInt(deps.get(j));
                if (hasRealTimes) {
                    System.out.println(i);
                    currNode.addDep(contents.get(parID), Double.parseDouble(weights.get(j)));
                } else {
                    currNode.addDep(contents.get(parID));
                }
            }
        }

        if (hasRealTimes) {
            contents.forEach(BNode::setAvgTime);
            contents.forEach(aNode -> aNode.setChildren(contents));
            Collections.reverse(contents);
            contents.forEach(BNode::setRank);
            Collections.reverse(contents);
            ogOrder = new ArrayList<>(contents);
            contents.sort(Comparator.comparing(BNode::getRank));
            Collections.reverse(contents);
        }
    }

    public void calcBHEFT(ResourceUsages resUsage) {
        contents.forEach( node ->  node.setAvgCost(resUsage.getResources()));
        for (int i = 0; i < contents.size(); i++) {
           contents.get(i).calcBHEFT(i, Budget, contents, resUsage.getResources());
        }
        BNode originNode = ogOrder.get(0);


        findSTandFT(null, originNode, 0);

    }

    void findSTandFT(BNode prevNode, BNode currNode, double prevNodeFT) {
        double prevTransTime = 0;
        double currNodeFT = prevNodeFT + currNode.getElapTime();

        if (prevNode != null && prevNode.getService().getID() != currNode.getService().getID()) {
            prevTransTime = prevNode.getService().getTransRateFor(currNode.getService().getID()) * currNode.getEdgeWeightFrom(prevNode);
        }

//        System.out.println("FT:" + currNodeFT);
//        System.out.println("TT:" + prevTransTime);
        if (currNode.replaceMax(currNodeFT + prevTransTime)) {
            currNode.setST(prevNodeFT + prevTransTime);
            currNode.setFT(currNodeFT + prevTransTime);
        }

        System.out.println(currNode.getST());
        System.out.println(currNode.getFT());

        for (BNode aChild : currNode.getChildren()) {
            findSTandFT(currNode, aChild, currNode.getFT());
        }
    }

    public CommandLineTable createCalcTable() {
        CommandLineTable table = new CommandLineTable();

        table.setShowVerticalLines(true);
        table.setHeaders("Task", "Rank", "SAB", "CTB", "AF", "SK*", "S", "ST", "FT", "Cost");
        for (BNode node: contents) {
            table.addRow(node.getID(), node.getRank(), node.getSAB(), node.getCTB(), node.getAF(), node.getSKSet(),
                    node.getService().getID(), node.getST(), node.getFT(), node.getRealCost());
        }

        return table;
    }

    public CommandLineTable createTaskTable(ResourceUsages resUsage) {
        CommandLineTable table = new CommandLineTable();
        ArrayList<String> headerArray = new ArrayList<>();

        headerArray.add("Task");
        for (res R: resUsage.getResources()) {
            headerArray.add("R" + R.ID);
        }
        headerArray.add("Avg Time"); headerArray.add("Avg Cost");


        int index;
        ArrayList<Object> tempRow = new ArrayList<>();

        table.setShowVerticalLines(true);
        table.setHeaders(headerArray.toArray(new String[0]));
        for (BNode node: ogOrder) {
            index = node.getID();
            tempRow.add("T" + index);
            tempRow.addAll(node.getRTimes());
            tempRow.add(node.getAvgTime()); tempRow.add(node.getAvgCost());
            table.addRow(tempRow.toArray(new Object[0]));
            tempRow.clear();
        }

        return table;
    }

    public CommandLineTable createTransTable(ResourceUsages mainResUsage) {
        CommandLineTable table = new CommandLineTable();
        table.setShowVerticalLines(true);

        table.setHeaders("Res","Time");

        int n = mainResUsage.getResources().size();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (j > i) {
                    table.addRow("R" + i + " - " + "R" + j, mainResUsage.getTransRate(i + "," + j));
                }
            }
        }

        return table;
    }

    public CommandLineTable createCostTable(ResourceUsages mainResUsage) {
        CommandLineTable table = new CommandLineTable();
        table.setShowVerticalLines(true);

        table.setHeaders("Res","Cost");

        for (res aRes: mainResUsage.getResources()) {
            table.addRow(aRes.getID(), aRes.getCost());
        }

        return table;
    }

    public void createNodeOrderXML(String outputPath) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();


        Element rootElement = doc.createElement("workflow");
        doc.appendChild(rootElement);

        String temp;
        Element aTask = null;

        for (BNode aNode: ogOrder) {
            aTask = doc.createElement("task");
            Element budget, ID, label, service;
            ID = doc.createElement("id");
            ID.appendChild( doc.createTextNode( aNode.getID() + ""));
            aTask.appendChild(ID);
            label = null;
            if (ogOrder.get(0).getLabel() != null) {
                label = doc.createElement("label");
                label.appendChild( doc.createTextNode( aNode.getLabel()));
                aTask.appendChild(label);
            }
            service = doc.createElement("service");
            service.appendChild( doc.createTextNode( aNode.getService().ID + ""));
            aTask.appendChild(service);

            rootElement.appendChild(aTask);

        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        Result output = new StreamResult(new File(outputPath));
        Source input = new DOMSource(doc);

        transformer.transform(input, output);
    }



    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for ( BNode node : contents ) {
            stringBuilder.append(node.toString());
        }



        return stringBuilder.toString();
    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        String temp = System.getProperty("user.dir");
        String src_dir = temp.substring(0, temp.lastIndexOf("/"));

        String wfNum = "WF2";
        File WF = new File("input\\workflows\\" + wfNum + ".xml");
        File RES = new File("input\\resources\\resUsage" + wfNum + ".xml");
        File WFResults = new File("input\\results\\" + wfNum + "Results.xml");
        ResourceUsages mainResUsage = new ResourceUsages(RES);
        ArrayList<Double> serviceTransRates = new ArrayList<>(mainResUsage.getTransRates());
        Workflow mainWorkFlow = new Workflow("tim", WF, WFResults, serviceTransRates);
        mainWorkFlow.calcBHEFT( mainResUsage);

        CommandLineTable taskTable = mainWorkFlow.createTaskTable(mainResUsage);
        CommandLineTable transferTable = mainWorkFlow.createTransTable(mainResUsage);
        CommandLineTable calcTable = mainWorkFlow.createCalcTable();
        CommandLineTable costTable = mainWorkFlow.createCostTable(mainResUsage);

        mainWorkFlow.createNodeOrderXML( wfNum);

        mainWorkFlow.contents.stream().forEach(System.out::println);



        costTable.printTable();
        transferTable.printTable();
        taskTable.printTable();
        calcTable.printTable();
    }




}
