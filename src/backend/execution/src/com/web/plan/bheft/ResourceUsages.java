package com.web.plan.bheft;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class ResourceUsages {
    ArrayList<res> resources = new ArrayList<>();
    HashMap<String,Double> transRate = new HashMap<>();

    public ResourceUsages(File f) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Element doc = db.parse(f).getDocumentElement();

        NodeList tRateNodes = doc.getElementsByTagName("trate");
        NodeList rCostNodes = doc.getElementsByTagName("rcost");
        NodeList resCountNodes = doc.getElementsByTagName("rescount");
        ArrayList<Double> tempDList = new ArrayList<>();
        res currRes = null;
        res lastRes = new res();

        int resCount =  Integer.parseInt(resCountNodes.item(0).getTextContent());
        int resIndex = 0;

        for (int i = 0; i < resCount; i++) {
                String tRateRaw = tRateNodes.item(0).getTextContent();
                ArrayList<String> tRate = new ArrayList<>();
                Collections.addAll(tRate, tRateRaw.split(","));
                for (int j = i; j < tRate.size() -1; j++) {
                    transRate.put(i + "," + (j +1), Double.parseDouble(tRate.get(resIndex)));
                    resIndex += 1;
                }

                String rCostRaw = rCostNodes.item(0).getTextContent();
                ArrayList<String> rCost = new ArrayList<>();
                Collections.addAll(rCost, rCostRaw.split(","));



                currRes = new res(i, Double.parseDouble(rCost.get(i)), transRate);
//                lastRes.setNextRes(currRes);
                resources.add(currRes);

//                lastRes = currRes;

        }
    }

    public ArrayList<res> getResources() { return resources; }

    public double getTransRate(String key) {
        return transRate.get(key);
    }

    public Collection<Double> getTransRates() {
        return transRate.values();
    }
}

class res{
    int ID;
    double cost;
    HashMap<String,Double> transRates;
    res nextRes;

    res() {}

    res(int IDin, double costIn, HashMap<String,Double> transRatesIn){
        ID = IDin;
        cost = costIn;
        transRates = transRatesIn;
    }

    void setNextRes(res nextResIn) {
        nextRes = nextResIn;
    }

    Double getTransRateFor(int IDin) {
        for (String keys: transRates.keySet()) {
            ArrayList<Integer> parentOrChild =
                    Arrays.stream(keys.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toCollection(ArrayList::new));
            if (parentOrChild.contains(ID) && parentOrChild.contains(IDin)) {
                return transRates.get(keys);
            }
        }
        return -1.0;
    }

    Double getCost() { return cost;}
    int getID() { return ID;}
}