package com.web.exec;

        import java.util.*;
        import java.util.concurrent.*;
        import java.io.*;

        import org.jgrapht.*;
        import org.jgrapht.graph.*;
        import org.jgrapht.util.*;

        import com.web.node.*;

public class PlanExecutor {
  public PlanExecutor() {}

  public void executePlan(DirectedAcyclicGraph<NodeADT, DefaultEdge> pl, HashMap<Integer, ArrayList<NodeADT>> hm) {
    ExecutorService executor = Executors.newFixedThreadPool(pl.vertexSet().size());
    int done_ct;

    try {
      for (ArrayList<NodeADT> nl : hm.values()) {
        done_ct = 0;
        for (int i = 0; i < nl.size(); i++) {
          ArrayList<NodeADT> children = new ArrayList<NodeADT>();
          ArrayList<NodeADT> parents = new ArrayList<NodeADT>();

          for (DefaultEdge e : pl.outgoingEdgesOf(nl.get(i))) {
            children.add(pl.getEdgeTarget(e));
          }
          for (DefaultEdge e : pl.incomingEdgesOf(nl.get(i))) {
            parents.add(pl.getEdgeSource(e));
          }

          if (pl.incomingEdgesOf(nl.get(i)).size() == 0) {
            nl.get(i).ei.setFilesReceived(true);
          }

          nl.get(i).setChildren(children);
          nl.get(i).setParents(parents);
          executor.execute(nl.get(i));
        }
        while (true) {
          if (done_ct == nl.size()) {
            break;
          } else {
            done_ct = 0;
            for (int i = 0; i < nl.size(); i++) {
              if (nl.get(i).ei.getExecStatus() == true) {
                done_ct += 1;
              }
            }
          }
          Thread.sleep(500);
        }
      }
      //CREATE RESULTS XML HERE
      //CREATE RESULTS XML HERE
      //CREATE RESULTS XML HERE
      //CREATE RESULTS XML HERE
      //CREATE RESULTS XML HERE
      //CREATE RESULTS XML HERE
    } catch (Exception e) {
      e.printStackTrace();
    }

    executor.shutdown();
  }
}

//package com.web.exec;
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.io.*;
//
//import org.jgrapht.*;
//import org.jgrapht.graph.*;
//import org.jgrapht.util.*;
//
//import com.web.node.*;
//
//public class PlanExecutor {
//  public PlanExecutor() {
//  }
//
//  public void executePlan(DirectedAcyclicGraph<NodeADT, DefaultEdge> pl, HashMap<Integer, ArrayList<NodeADT>> hm) {
//    ExecutorService executor = Executors.newFixedThreadPool(pl.vertexSet().size());
//
//    try {
//      long start_time = System.nanoTime();
//
//      ExecutorService threadPool = Executors.newFixedThreadPool(4);
//      for (ArrayList<NodeADT> nl : hm.values()) {
//        threadPool.submit(new Runnable() {
//          public void run() {
//            int done_ct = 0;
//            for (int i = 0; i < nl.size(); i++) {
//              ArrayList<NodeADT> children = new ArrayList<NodeADT>();
//              ArrayList<NodeADT> parents = new ArrayList<NodeADT>();
//
//              for (DefaultEdge e : pl.outgoingEdgesOf(nl.get(i))) {
//                children.add(pl.getEdgeTarget(e));
//              }
//              for (DefaultEdge e : pl.incomingEdgesOf(nl.get(i))) {
//                parents.add(pl.getEdgeSource(e));
//              }
//
//              if (pl.incomingEdgesOf(nl.get(i)).size() == 0) {
//                nl.get(i).ei.setFilesReceived(true);
//              }
//
//              nl.get(i).setChildren(children);
//              nl.get(i).setParents(parents);
//              executor.execute(nl.get(i));
//            }
//            while (true) {
//              if (done_ct == nl.size()) {
//                break;
//              } else {
//                done_ct = 0;
//                for (int i = 0; i < nl.size(); i++) {
//                  if (nl.get(i).ei.getExecStatus() == true) {
//                    done_ct += 1;
//                  }
//                }
//              }
//              try {
//                Thread.sleep(500);
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              }
//            }
//          }
//        });
//      }
//      threadPool.shutdown();
//      while (true) {
//        if (threadPool.isTerminated()) {
//          System.out.println("#Completed uploading...");
//          long end_time = System.nanoTime();
//          long total_time = end_time - start_time;
//          System.out.println("Total time taken:" + total_time);
//          break;
//        }
//      }
//    } catch (Exception ex) {
//      System.out.println(ex.toString());
//      System.out.println("upload error");
//    }
//  }
//}

