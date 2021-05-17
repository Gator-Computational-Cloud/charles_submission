package com.web.plan.bheft;

import java.text.DecimalFormat;
import java.util.*;

public class BNode {
    int ID;
    String label;
    HashMap<BNode, Double> deps;
    ArrayList<BNode> children = new ArrayList<>();
    ArrayList<BNode> parents = new ArrayList<>();
    BNode maxChild;
    ArrayList<Double> RTimes;
    double rank, avgCost, avgTime, realCost, SAB, CTB, AF, elapTime, ST;
    double FT = 0;
    ArrayList<res> SKset = new ArrayList<>();
    res S;



    /*
    |Constructor|
    Inputs: ID               int
    Desc: Handles Node Creation.
     */
    BNode(int IDin, String labelIn) {
        ID = IDin;
        label = labelIn;

        deps = new HashMap<>();
        RTimes = new ArrayList<>();
    }

    /*

    Inputs: parent                          BNode
            weight                            int
    Desc: Creates an edge with weight from parent
     */
    void addDep(BNode parent, Double weight) {
        deps.put(parent, weight);
    }

    void addDep(BNode parent) {
        deps.put(parent, null);
    }

    /*
    Inputs: resIn                                   int
            time                                    int
    Desc: Creates a computation time for resIn resource
     */
    void addRTimeForRes(double time) {
        RTimes.add( time);
    }

    void calcBHEFT(int index, double Budget, ArrayList<BNode> ogOrder, ArrayList<res> SKsetin) {
        System.out.println(this.ID + ":" + Budget + "" + - sumOfPriorCosts(index, ogOrder) +  "" + -  sumOfFutureAvgCosts(index,
                ogOrder));
        this.SAB = Budget - sumOfPriorCosts(index, ogOrder) -  sumOfFutureAvgCosts(index, ogOrder);
        double tempAF = avgCost / sumOfFutureAvgCosts(index, ogOrder);
        this.AF = SAB > 0 ? tempAF : 0;
        this.CTB = avgCost + SAB * AF;
        SKset.addAll(SKsetin);
        S = findBestService(SKsetin);
        realCost = S.getCost() * RTimes.get(SKset.indexOf(S));
    }

    res findBestService(ArrayList<res> SKsetIn) {
        res bestService = null;
        double min;
        min = -1;
        for (int i = 0; i < SKsetIn.size(); i++) {
            double currCost = SKsetIn.get(i).cost * this.RTimes.get(i);
            double currTime = RTimes.get(i);
                if (currTime < min || min == -1 && currCost <= CTB) {
                    bestService = SKsetIn.get(i);
                    min = currTime;
                } else if (currCost >= CTB) {
                    SKset.remove(SKsetIn.get(i));
                }
        }
        if ( bestService == null){
            if (this.SAB >= 0) {
                min = RTimes.get(0) +1;
                for (int i = 0; i < RTimes.size(); i++) {
                    double currTime = RTimes.get(i);
                    if (currTime < min) {
                        bestService = currTime < min ? SKsetIn.get(i) : bestService;
                        min = currTime;
                    }
                }
            } else{
                min = SKsetIn.get(0).cost * this.RTimes.get(0) +1;
                for (int i = 0; i < RTimes.size(); i++) {
                    double currCost = SKsetIn.get(i).cost * this.RTimes.get(i);
                    if (this.SAB < 0) {
                        bestService = (RTimes.get(i) < min) && currCost <= CTB ? SKsetIn.get(i) : bestService;
                    } else {
                        if (currCost < min) {
                            bestService =  SKsetIn.get(i);
                            min = RTimes.get(i);
                        }
                    }
                }
                }
            }
        elapTime = min;
        return bestService;
    }

    void addParent(BNode parentinIn) {
        parents.add( parentinIn);
    }

    Boolean replaceMax(double maxIn) {
        System.out.println("Current FT:" + this.FT);
        System.out.println("Possible FT:" + maxIn);
        return this.FT == 0 || maxIn > this.FT;
    }

    void setST(double time) {
        ST = time;
    }
    void setFT(double time) {
        FT = time;
    }

    double sumOfFutureAvgCosts(int index, ArrayList<BNode> ogOrder) {
        double sum = 0;

        for (int i = index; i < ogOrder.size(); i++) {
            sum += ogOrder.get(i).avgCost;
            System.out.println(ogOrder.get(i).avgCost);
        }

        return sum;
    }

    //GET METHODS

    public String getLabel() {
        return label;
    }

    double getElapTime() { return elapTime; }
    double getST() { return ST;}
    double getFT() { return FT;}
    double getSAB() {
        return SAB;
    }
    double getCTB() {
        return CTB;
    }
    double getRealCost() {
        return realCost;
    }
    double getAvgCost() { return avgCost;}
    double getAvgTime() { return avgTime;}
    double getAF() {
        return AF;
    }
    double getEdgeWeightFrom(BNode parentIn) {
        return deps.get(parentIn);
    }
    double getRank() {
        return rank;
    }

    int getID() {
        return ID;
    }

    res getService() { return S;}

    ArrayList<BNode> getChildren() { return children; }
    ArrayList<res> getSKSet() {
        return SKset;
    }
    ArrayList<Double> getRTimes() { return RTimes;}
    HashMap<BNode, Double> getDeps() { return deps;}

    //SET METHODS

    //sets max child based off rank AND edge weight
    void setMaxChild() {
//        final BNode tempNode = this;
//        maxChild =  children.stream()
//                .reduce( (child1, child2) -> child1.getRank() + child1.getEdgeWeightFrom(tempNode) >= child2.getRank() + child2.getEdgeWeightFrom(tempNode) ?
//                        child1 : child2)
//                .orElse(children.get(0));
//
        int maxIndex = 0;
        double max = 0;
        double temp;

        for (int i = 0; i < children.size(); i++) {
            temp = children.get(i).getEdgeWeightFrom(this) * 0.578 +  children.get(i).getRank();
            if (temp > max) {
                maxIndex = i;
                max = temp;
            }
        }

        maxChild = children.get(maxIndex);
    }

//    //Sets max child based just off rank
//    void setMaxChild() {
//        maxChild =  children.stream()
//                .reduce( (child1, child2) -> child1.getRank() >= child2.getRank() ?
//                        child1 : child2)
//                .orElse(children.get(0));
//    }

    void setChildren(ArrayList<BNode> potChildren) {
        for (int i = this.ID; i < potChildren.size(); i++) {
            for (BNode dep: potChildren.get(i).deps.keySet()) {
                if (dep.ID == this.ID) {
                    potChildren.get(i).addParent(this);
                    children.add(potChildren.get(i));
                }
            }
        }
    }

    void setRealCost(double expCostIn) {
        realCost = expCostIn;
    }

    void setAvgCost(ArrayList<res> reses) {
        double sum = 0;
        for (int i = 0; i < reses.size(); i++) {
            sum += reses.get(i).cost * this.RTimes.get(i);
        }

        avgCost = sum/reses.size();
    }

    double sumOfPriorCosts(int index, ArrayList<BNode> ogOrder) {
        double sum = 0;

        for (int i = index -1; i >= 0; i--) {
            sum += ogOrder.get(i).realCost;
            System.out.println(ogOrder.get(i).realCost);
        }

        return sum;
    }

    void setRank() {
        if (children.size() != 0) {
            setMaxChild();
            rank = avgTime + maxChild.getEdgeWeightFrom(this) *
                    0.578 + maxChild.getRank();
        } else {
        rank = avgTime;
        }
    }

    void setAvgTime() {
        avgTime = RTimes.stream().mapToDouble(val -> val).average().orElse(0);
    }

    public double getMaxChildWeight() {
        if (maxChild == null) {
            return -1;
        }
        return maxChild.getEdgeWeightFrom(this);
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{").append(ID).append("} with Resource times: ");

        if (RTimes.size() == 0) {
            stringBuilder.append("None \n");
            for ( HashMap.Entry<BNode, Double> entry : deps.entrySet() ) {
                BNode dep = entry.getKey();
                stringBuilder.append("\t[").append(dep.ID).append("] with Weight: None\n");
            }
        } else {

            for (double time : RTimes) {
                stringBuilder.append(String.format("%,.2f", time)).append(" ");
            }
            stringBuilder.append("\n");
            stringBuilder.append("RANK: ").append(df.format(rank)).append("\n");
            stringBuilder.append("SERVICE: ").append(S.ID).append("\n");
            stringBuilder.append("Start time: ").append(ST).append("\n");
            stringBuilder.append("Finish time: ").append(FT).append("\n");

            double maxChildWeight = getMaxChildWeight();
            if (maxChildWeight != -1) {
                stringBuilder.append("\tMax child [").append(maxChild.ID).append("]: ").append(getMaxChildWeight()).append("\n");
            }

            if (SAB != 0) {
                stringBuilder.append("\tSAB: ").append(df.format(SAB)).append("\n");
                stringBuilder.append("\tCTB: ").append(df.format(CTB)).append("\n");
                stringBuilder.append("\tAF: ").append(df.format(AF)).append("\n");
            }

            for (HashMap.Entry<BNode, Double> entry : deps.entrySet()) {
                BNode dep = entry.getKey();
                stringBuilder.append("\t[").append(dep.ID).append("] with Weight: ").append(df.format(this.getEdgeWeightFrom(dep))).append("\n");
            }
            System.out.println();
        }
        return stringBuilder.toString();
    }
}