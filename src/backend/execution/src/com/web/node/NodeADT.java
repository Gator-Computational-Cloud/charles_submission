package com.web.node;

        import java.io.*;
        import java.util.*;
        import java.util.concurrent.*;

        import com.web.aws.*;
        import com.web.exec.*;
        import com.amazonaws.AmazonClientException;
        import com.amazonaws.AmazonServiceException;
        import com.amazonaws.auth.*;
        import com.amazonaws.auth.profile.ProfileCredentialsProvider;
        import com.amazonaws.regions.*;
        import com.amazonaws.services.ec2.*;
        import com.amazonaws.services.ec2.model.*;

        import com.web.jsch.*;
        import com.web.plan.Planner;
        import org.w3c.dom.Document;
        import org.w3c.dom.Element;

        import javax.xml.parsers.DocumentBuilder;
        import javax.xml.parsers.DocumentBuilderFactory;
        import javax.xml.parsers.ParserConfigurationException;
        import javax.xml.transform.*;
        import javax.xml.transform.dom.DOMSource;
        import javax.xml.transform.stream.StreamResult;

public class NodeADT implements Runnable {
  private final int id;
  private final String service;
  private final String wf_name;
  private final String user;
  private final String path;
  private final ArrayList<Integer> deps;
  private final ArrayList<String> weights;
  private final String access_key;
  private final String secret_key;
  private final String token;
  private final String pem_path;
  private ArrayList<NodeADT> children;
  private ArrayList<NodeADT> parents;
  private HashMap<Integer, NodeADT> ledger;

  final String src_dir;

  public ExecInfo ei = new ExecInfo();

  public NodeADT(int id, String service, String path, String pem_path, ArrayList<Integer> deps,
                 ArrayList<String> weights,
                 String access_key,
                 String secret_key, String token, String user, String wf_name, String src_dir) {
    // Constructor
    this.id = id;
    this.service = service;
    this.path = path;
    this.pem_path = pem_path;
    this.deps = deps;
    this.weights = weights;
    this.access_key = access_key;
    this.secret_key = secret_key;
    this.token = token;
    this.user = user;
    this.wf_name = wf_name;

    this.src_dir = src_dir;
  }

  public int getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public ArrayList<Integer> getDeps() {
    return deps;
  }

  public ArrayList<String> getWeights() {
    return weights;
  }

  public void setLedger(HashMap<Integer, NodeADT> ledger) {
    this.ledger = ledger;
  }

  public void setChildren(ArrayList<NodeADT> children) {
    this.children = children;
  }

  public ArrayList<NodeADT> getChildren() {
    return children;
  }

  public void setParents(ArrayList<NodeADT> parents) {
    this.parents = parents;
  }

  public ArrayList<NodeADT> getParents() {
    return parents;
  }

  public void run() {
    System.out.println("executing : " + id);

    String ins_id;
    AwsProvisioner aws;
    JschProvisioner jsch;
    int done_ct;
    ArrayList<String> cmds;
    String logs;

    Instance ins = null;

    try {
      aws = new AwsProvisioner(access_key, secret_key, token);
      jsch = new JschProvisioner();

      if (ei.getInitialized() == false) {
        ins_id = aws.createInstances(1, "t2.micro", "ami-0885b1f6bd170450c", ei.getKpName(), ei.getSgName());
        ei.setInsId(ins_id);
        Thread.sleep(1500);
        while (true) {
          if (ei.getInitialized() == true) {
            break;
          } else {
            ins = aws.describeInstance(ei.getInsId());
            if (ins.getState().getCode() == 16) {
              ei.setIp(ins.getPublicIpAddress());
              ei.setInitialized(true);
            }
          }
          Thread.sleep(2500);
        }
      }
      Thread.sleep(25000);
      // UPLOAD NODE SRC TO AWS INSTANCE
      System.out.println("uploading node src to " + ei.getIp());
      jsch.upload(path, "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
      jsch.upload(ei.getPemPath(), "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
      System.out.println("upload complete");

      // INSTALL JAVA AND UNZIP .ZIP FILE
      System.out.println("initializing vm and unzipping src");
      cmds = new ArrayList<String>();
      cmds.add("sudo apt update -qq");
      cmds.add("sudo apt update -qq");
      cmds.add("echo Y | sudo apt install default-jre -qq");
      cmds.add("echo Y | sudo apt install default-jdk -qq");
      cmds.add("echo Y | sudo apt install unzip -qq");
      cmds.add("unzip n" + (id+1) + ".zip");
      cmds.add("ls");
      cmds.add("exit");
      logs = jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
      System.out.println("initialization and unzipping complete");
      cmds.clear();

      if (ei.getFilesReceived() == false) {
        for (int i = 0; i < parents.size(); i++) {
          System.out.println("transferring files to " + id + " from " + parents.get(i).getId());

          // TRANSFER INPUT FILES
          cmds.add("chmod 600 " + pem_path + ".pem");
          cmds.add("ls");
          cmds.add("cd n" + (id +1));
          if (id == 0) {
            cmds.add("cd data/in");
            cmds.add("scp -i ../../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
          } else {
            cmds.add("cd data/in/" + parents.get(i).getId());
            cmds.add("scp -i ../../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
          }
          cmds.add("exit");
          logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
          cmds.clear();

          parents.get(i).ei.setTransferCount((parents.get(i).ei.getTransferCount() + 1));
          if (parents.get(i).ei.getTransferCount() == parents.get(i).getChildren().size()) {
            System.out.println("transfers complete - terminating " + parents.get(i).getId());
            aws.terminateInstance(parents.get(i).ei.getInsId());
          }
        }
        ei.setFilesReceived(true);
      }

      // EXECUTE NODE
      System.out.println("executing code on " + id);
      cmds.add("ls");
      cmds.add("cd n" + (id+1));
      cmds.add("ls data/in");
      cmds.add("chmod +x ./build.sh");
      cmds.add("chmod +x ./run.sh");
      cmds.add("./build.sh");
      cmds.add("(time ./run.sh) 2> data/time.txt");
      cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- > data/stats" + (id+1) + ".txt");

      if (children.size() > 0) {
        for (NodeADT child : children) {
          cmds.add("du -l -s data/out/" + child.getId() + " | cut -d'd' -f1 | sed 's/.$//' >> data/stats" + (id + 1) + ".txt");
        }
      }

      if (service.equals("test")) {
        cmds.add("(time ./run.sh) 2> data/time.txt");
        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
        cmds.add("(time ./run.sh) 2> data/time.txt");
        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
      }
      cmds.add("exit");
      logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
      System.out.println("done executing code on " + id);
      cmds.clear();

      System.out.println("downloading stats from node " + id);
      File genOutFile = new File( src_dir + "USERS/" + user + "/executions/" + wf_name +
              "/results/n" + id);
      genOutFile.mkdirs();
      jsch.download("/home/ubuntu/n" + (id+1) + "/data/stats" + (id+1) + ".txt", genOutFile.getAbsolutePath() +
              "/stats.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);

      System.out.println("downloading results from node " + id);
      if (children.size() > 0) {
        for (NodeADT child : children) {
          File resFile =
                  new File( src_dir + "/USERS/" + user + "/executions/" + wf_name + "/results/n" + id + "/" + child.getId());
          resFile.mkdirs();
          jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/" + child.getId() + "/*",
                  resFile.getAbsolutePath() + "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
        }
      } else {
        jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/*", genOutFile.getAbsolutePath() +
                "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
      }
//      aws.terminateInstance(ei.getInsId());
//      while(true) {
//        ins = aws.describeInstance(ei.getInsId());
//        if (ins.getState().getCode() == 48) {
//          break;
//        }
//        Thread.sleep(2500);
//      }
//      Thread.sleep(5000);

      File log_file = new File(src_dir + "/USERS/" + user + "/executions/" + wf_name + "/logs" +
              "/" + id + "_logs.txt");

      if (log_file.createNewFile()) {
        System.out.println("file created : " + log_file.getName());
      }

      BufferedWriter bw = new BufferedWriter(new FileWriter(log_file, false));
      bw.write(logs);
      bw.close();

      System.out.println("done with : " + id);
      ei.setExecStatus(true); // !IMPORTANT!
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static double parseDuration(String strDuration) {
    char[] durationArray = strDuration.toCharArray();

    double duration = 0;

    String temp;

    for (int i = 1; i < durationArray.length; i++) {
      temp = "";
      int j = i - 1;
      switch (durationArray[i]) {
        case 'h':
          while (j != -1) {
            temp = durationArray[j] + temp;
            j--;
          }

          duration = Double.parseDouble(temp) * 3600;
          break;

        case 'm':
          do {
            temp = durationArray[j] + temp;
            j--;
          } while (j != -1 && durationArray[j] != 'h');

          duration += Double.parseDouble(temp) * 60;
          break;

        case 's':
          do {
            temp = durationArray[j] + temp;
            j--;
          } while (j != -1 && durationArray[j] != 'm');

          duration += Double.parseDouble(temp);
          break;
      }
    }
    return duration;
    //return duration;
  }

  public static void createNodeOrderXML(String resPath, int numNodes, Planner pr, ArrayList<ArrayList<Double>> durations,
                                        ArrayList<ArrayList<String>> edgeWeights) throws ParserConfigurationException,
          TransformerException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();


    Element rootElement = doc.createElement("workflow");
    doc.appendChild(rootElement);

    String temp;
    Element aTask = null;

    Element budget = doc.createElement("budget");
    budget.appendChild(doc.createTextNode(pr.getBudget() + ""));
    rootElement.appendChild(budget);

    for (int i = 0; i < numNodes; i++) {
      aTask = doc.createElement("task");

      Element ID, rts, weights;

      ID = doc.createElement("id");
      rts = doc.createElement("rts");
      weights = doc.createElement("weights");

      ID.appendChild( doc.createTextNode( i + ""));

      temp = "";
      for(double duration: durations.get(i)) {
        temp += duration + ",";
      }
      if (!temp.equals("")) {
        rts.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
      }

      temp = "";
      for (String weight : edgeWeights.get(i)) {
        temp += weight + ",";
      }
      if (!temp.equals("")) {
        weights.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
      }

      aTask.appendChild(ID);
      aTask.appendChild(rts);
      aTask.appendChild(weights);

      rootElement.appendChild(aTask);

    }

    Transformer transformer = TransformerFactory.newInstance().newTransformer();

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    Result output = new StreamResult(new File(resPath));
    Source input = new DOMSource(doc);

    transformer.transform(input, output);
  }

  public ExecInfo getEi() {
    return ei;
  }
}

//package com.web.node;
//
//import java.io.*;
//import java.util.*;
//import java.util.concurrent.*;
//
//import com.web.aws.*;
//import com.web.exec.*;
//import com.amazonaws.AmazonClientException;
//import com.amazonaws.AmazonServiceException;
//import com.amazonaws.auth.*;
//import com.amazonaws.auth.profile.ProfileCredentialsProvider;
//import com.amazonaws.regions.*;
//import com.amazonaws.services.ec2.*;
//import com.amazonaws.services.ec2.model.*;
//
//import com.web.jsch.*;
//import com.web.plan.Planner;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.transform.*;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//
//public class NodeADT implements Runnable {
//  private final int id;
//  private final String service;
//  private final String wf_name;
//  private final String user;
//  private final String path;
//  private final ArrayList<Integer> deps;
//  private final ArrayList<String> weights;
//  private final String access_key;
//  private final String secret_key;
//  private final String token;
//  private final String pem_path;
//  private ArrayList<NodeADT> children;
//  private ArrayList<NodeADT> parents;
//  private HashMap<Integer, NodeADT> ledger;
//
//  final String src_dir;
//
//  public ExecInfo ei = new ExecInfo();
//
//  public NodeADT(int id, String service, String path, String pem_path, ArrayList<Integer> deps,
//                 ArrayList<String> weights,
//                 String access_key,
//                 String secret_key, String token, String user, String wf_name, String src_dir) {
//    // Constructor
//    this.id = id;
//    this.service = service;
//    this.path = path;
//    this.pem_path = pem_path;
//    this.deps = deps;
//    this.weights = weights;
//    this.access_key = access_key;
//    this.secret_key = secret_key;
//    this.token = token;
//    this.user = user;
//    this.wf_name = wf_name;
//
//    this.src_dir = src_dir;
//  }
//
//  public int getId() {
//    return id;
//  }
//
//  public String getPath() {
//    return path;
//  }
//
//  public ArrayList<Integer> getDeps() {
//    return deps;
//  }
//
//  public ArrayList<String> getWeights() {
//    return weights;
//  }
//
//  public void setLedger(HashMap<Integer, NodeADT> ledger) {
//    this.ledger = ledger;
//  }
//
//  public void setChildren(ArrayList<NodeADT> children) {
//    this.children = children;
//  }
//
//  public ArrayList<NodeADT> getChildren() {
//    return children;
//  }
//
//  public void setParents(ArrayList<NodeADT> parents) {
//    this.parents = parents;
//  }
//
//  public ArrayList<NodeADT> getParents() {
//      return parents;
//  }
//
//  public void run() {
//    System.out.println("executing : " + id);
//
//    String ins_id;
//    AwsProvisioner aws;
//    JschProvisioner jsch;
//    int done_ct;
//    ArrayList<String> cmds;
//    String logs;
//
//    Instance ins = null;
//
//    try {
//      aws = new AwsProvisioner(access_key, secret_key, token);
//      jsch = new JschProvisioner();
//
//      if (ei.getInitialized() == false) {
//        ins_id = aws.createInstances(1, "t2.micro", "ami-0885b1f6bd170450c", ei.getKpName(), ei.getSgName());
//        ei.setInsId(ins_id);
//        Thread.sleep(1500);
//        while (true) {
//          if (ei.getInitialized() == true) {
//            break;
//          } else {
//            ins = aws.describeInstance(ei.getInsId());
//            if (ins.getState().getCode() == 16) {
//              ei.setIp(ins.getPublicIpAddress());
//              ei.setInitialized(true);
//            }
//          }
//          Thread.sleep(2500);
//        }
//      }
//      Thread.sleep(25000);
//      // UPLOAD NODE SRC TO AWS INSTANCE
//      System.out.println("uploading node src to " + ei.getIp());
//      jsch.upload(path, "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      jsch.upload(ei.getPemPath(), "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      System.out.println("upload complete");
//
//      // INSTALL JAVA AND UNZIP .ZIP FILE
//      System.out.println("initializing vm and unzipping src");
//      cmds = new ArrayList<String>();
//      cmds.add("sudo apt update -qq");
//      cmds.add("sudo apt update -qq");
//      cmds.add("echo Y | sudo apt install default-jre -qq");
//      cmds.add("echo Y | sudo apt install default-jdk -qq");
//      cmds.add("echo Y | sudo apt install unzip -qq");
//      cmds.add("unzip n" + (id+1) + ".zip");
//      cmds.add("ls");
//      cmds.add("exit");
//      logs = jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//      System.out.println("initialization and unzipping complete");
//      cmds.clear();
//
//      if (ei.getFilesReceived() == false) {
//        for (int i = 0; i < parents.size(); i++) {
//          System.out.println("transferring files to " + id + " from " + parents.get(i).getId());
//
//          // TRANSFER INPUT FILES
//          cmds.add("chmod 600 " + pem_path + ".pem");
//          cmds.add("ls");
//          cmds.add("cd n" + (id +1));
//          if (id == 0) {
//            cmds.add("cd data/in");
//            cmds.add("scp -i ../../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
//          } else {
//            cmds.add("cd data/in/" + parents.get(i).getId());
//            cmds.add("scp -i ../../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
//          }
//          cmds.add("exit");
//          logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//          cmds.clear();
//
//          parents.get(i).ei.setTransferCount((parents.get(i).ei.getTransferCount() + 1));
//          if (parents.get(i).ei.getTransferCount() == parents.get(i).getChildren().size()) {
//            System.out.println("transfers complete - terminating " + parents.get(i).getId());
//            aws.terminateInstance(parents.get(i).ei.getInsId());
//          }
//        }
//        ei.setFilesReceived(true);
//      }
//
//      // EXECUTE NODE
//      System.out.println("executing code on " + id);
//      cmds.add("ls");
//      cmds.add("cd n" + (id+1));
//      cmds.add("ls data/in");
//      cmds.add("chmod +x ./build.sh");
//      cmds.add("chmod +x ./run.sh");
//      cmds.add("./build.sh");
//      cmds.add("(time ./run.sh) 2> data/time.txt");
//      cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- > data/stats" + (id+1) + ".txt");
//
//      if (children.size() > 0) {
//        for (NodeADT child : children) {
//          cmds.add("du -l -s data/out/" + child.getId() + " | cut -d'd' -f1 | sed 's/.$//' >> data/stats" + (id + 1) + ".txt");
//        }
//      }
//
//      if (service.equals("test")) {
//        cmds.add("(time ./run.sh) 2> data/time.txt");
//        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
//        cmds.add("(time ./run.sh) 2> data/time.txt");
//        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
//      }
//      cmds.add("exit");
//      logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//      System.out.println("done executing code on " + id);
//      cmds.clear();
//
//      System.out.println("downloading stats from node " + id);
//      File genOutFile = new File( "USERS/" + user + "/executions/" + wf_name +
//              "/results/n" + id);
//      genOutFile.mkdirs();
//      jsch.download("/home/ubuntu/n" + (id+1) + "/data/stats" + (id+1) + ".txt", genOutFile.getAbsolutePath() +
//                      "/stats.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//
//      System.out.println("downloading results from node " + id);
//      if (children.size() > 0) {
//        for (NodeADT child : children) {
//          File resFile =
//                  new File( src_dir + "/USERS/" + user + "/executions/" + wf_name + "/results/n" + id + "/" + child.getId());
//          resFile.mkdirs();
//          jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/" + child.getId() + "/*",
//                  resFile.getAbsolutePath() + "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//        }
//      } else {
//        jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/*", genOutFile.getAbsolutePath() +
//                        "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      }
//      aws.terminateInstance(ei.getInsId());
//       while(true) {
//         ins = aws.describeInstance(ei.getInsId());
//         if (ins.getState().getCode() == 48) {
//           break;
//         }
//         Thread.sleep(2500);
//       }
//      Thread.sleep(5000);
//
//      File log_file = new File(src_dir + "/USERS/" + user + "/executions/" + wf_name + "/logs" +
//              "/" + id + "_logs.txt");
//
//      if (log_file.createNewFile()) {
//        System.out.println("file created : " + log_file.getName());
//      }
//
//      BufferedWriter bw = new BufferedWriter(new FileWriter(log_file, false));
//      bw.write(logs);
//      bw.close();
//
//      System.out.println("done with : " + id);
//      ei.setExecStatus(true); // !IMPORTANT!
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  public static double parseDuration(String strDuration) {
//    char[] durationArray = strDuration.toCharArray();
//
//    double duration = 0;
//
//    String temp;
//
//    for (int i = 1; i < durationArray.length; i++) {
//      temp = "";
//      int j = i - 1;
//      switch (durationArray[i]) {
//        case 'h':
//          while (j != -1) {
//            temp = durationArray[j] + temp;
//            j--;
//          }
//
//          duration = Double.parseDouble(temp) * 3600;
//          break;
//
//        case 'm':
//          do {
//            temp = durationArray[j] + temp;
//            j--;
//          } while (j != -1 && durationArray[j] != 'h');
//
//          duration += Double.parseDouble(temp) * 60;
//          break;
//
//        case 's':
//          do {
//            temp = durationArray[j] + temp;
//            j--;
//          } while (j != -1 && durationArray[j] != 'm');
//
//          duration += Double.parseDouble(temp);
//          break;
//      }
//    }
//    return duration;
//    //return duration;
//  }
//
//  public static void createNodeOrderXML(String resPath, int numNodes, Planner pr, ArrayList<ArrayList<Double>> durations,
//                                        ArrayList<ArrayList<String>> edgeWeights) throws ParserConfigurationException,
//          TransformerException {
//    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//    DocumentBuilder db = dbf.newDocumentBuilder();
//    Document doc = db.newDocument();
//
//
//    Element rootElement = doc.createElement("workflow");
//    doc.appendChild(rootElement);
//
//    String temp;
//    Element aTask = null;
//
//    Element budget = doc.createElement("budget");
//    budget.appendChild(doc.createTextNode(pr.getBudget() + ""));
//    rootElement.appendChild(budget);
//
//    for (int i = 0; i < numNodes; i++) {
//      aTask = doc.createElement("task");
//
//      Element ID, rts, weights;
//
//      ID = doc.createElement("id");
//      rts = doc.createElement("rts");
//      weights = doc.createElement("weights");
//
//      ID.appendChild( doc.createTextNode( i + ""));
//
//      temp = "";
//      for(double duration: durations.get(i)) {
//        temp += duration + ",";
//      }
//      if (!temp.equals("")) {
//        rts.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
//      }
//
//        temp = "";
//        for (String weight : edgeWeights.get(i)) {
//          temp += weight + ",";
//        }
//        if (!temp.equals("")) {
//          weights.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
//        }
//
//      aTask.appendChild(ID);
//      aTask.appendChild(rts);
//      aTask.appendChild(weights);
//
//      rootElement.appendChild(aTask);
//
//    }
//
//    Transformer transformer = TransformerFactory.newInstance().newTransformer();
//
//    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//
//    Result output = new StreamResult(new File(resPath));
//    Source input = new DOMSource(doc);
//
//    transformer.transform(input, output);
//  }
//}

//package com.web.node;
//
//        import java.io.*;
//        import java.util.*;
//        import java.util.concurrent.*;
//
//        import com.web.aws.*;
//        import com.web.exec.*;
//        import com.amazonaws.AmazonClientException;
//        import com.amazonaws.AmazonServiceException;
//        import com.amazonaws.auth.*;
//        import com.amazonaws.auth.profile.ProfileCredentialsProvider;
//        import com.amazonaws.regions.*;
//        import com.amazonaws.services.ec2.*;
//        import com.amazonaws.services.ec2.model.*;
//
//        import com.web.jsch.*;
//        import com.web.plan.Planner;
//        import org.w3c.dom.Document;
//        import org.w3c.dom.Element;
//
//        import javax.xml.parsers.DocumentBuilder;
//        import javax.xml.parsers.DocumentBuilderFactory;
//        import javax.xml.parsers.ParserConfigurationException;
//        import javax.xml.transform.*;
//        import javax.xml.transform.dom.DOMSource;
//        import javax.xml.transform.stream.StreamResult;
//
//public class NodeADT implements Runnable {
//  private final int id;
//  private final String service;
//  private final String wf_name;
//  private final String user;
//  private final String path;
//  private final ArrayList<Integer> deps;
//  private final ArrayList<String> weights;
//  private final String access_key;
//  private final String secret_key;
//  private final String token;
//  private final String pem_path;
//  private ArrayList<NodeADT> children;
//  private ArrayList<NodeADT> parents;
//  private HashMap<Integer, NodeADT> ledger;
//
//  final String src_dir;
//
//  public ExecInfo ei = new ExecInfo();
//
//  public NodeADT(int id, String service, String path, String pem_path, ArrayList<Integer> deps,
//                 ArrayList<String> weights,
//                 String access_key,
//                 String secret_key, String token, String user, String wf_name, String src_dir) {
//    // Constructor
//    this.id = id;
//    this.service = service;
//    this.path = path;
//    this.pem_path = pem_path;
//    this.deps = deps;
//    this.weights = weights;
//    this.access_key = access_key;
//    this.secret_key = secret_key;
//    this.token = token;
//    this.user = user;
//    this.wf_name = wf_name;
//
//    this.src_dir = src_dir;
//  }
//
//  public int getId() {
//    return id;
//  }
//
//  public String getPath() {
//    return path;
//  }
//
//  public ArrayList<Integer> getDeps() {
//    return deps;
//  }
//
//  public ArrayList<String> getWeights() {
//    return weights;
//  }
//
//  public void setLedger(HashMap<Integer, NodeADT> ledger) {
//    this.ledger = ledger;
//  }
//
//  public void setChildren(ArrayList<NodeADT> children) {
//    this.children = children;
//  }
//
//  public ArrayList<NodeADT> getChildren() {
//    return children;
//  }
//
//  public void setParents(ArrayList<NodeADT> parents) {
//    this.parents = parents;
//  }
//
//  public ArrayList<NodeADT> getParents() {
//    return parents;
//  }
//
//  public void run() {
//    System.out.println("executing : " + id);
//
//    String ins_id;
//    AwsProvisioner aws;
//    JschProvisioner jsch;
//    int done_ct;
//    ArrayList<String> cmds;
//    String logs;
//
//    Instance ins = null;
//
//    try {
//      aws = new AwsProvisioner(access_key, secret_key, token);
//      jsch = new JschProvisioner();
//
//      if (ei.getInitialized() == false) {
//        ins_id = aws.createInstances(1, "t2.micro", "ami-0885b1f6bd170450c", ei.getKpName(), ei.getSgName());
//        ei.setInsId(ins_id);
//        Thread.sleep(1500);
//        while (true) {
//          if (ei.getInitialized() == true) {
//            break;
//          } else {
//            ins = aws.describeInstance(ei.getInsId());
//            if (ins.getState().getCode() == 16) {
//              ei.setIp(ins.getPublicIpAddress());
//              ei.setInitialized(true);
//            }
//          }
//          Thread.sleep(2500);
//        }
//      }
//      Thread.sleep(25000);
//      // UPLOAD NODE SRC TO AWS INSTANCE
//      System.out.println("uploading node src to " + ei.getIp());
//      jsch.upload(path, "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      jsch.upload(ei.getPemPath(), "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      System.out.println("upload complete");
//
//      // INSTALL JAVA AND UNZIP .ZIP FILE
//      System.out.println("initializing vm and unzipping src");
//      cmds = new ArrayList<String>();
//      cmds.add("sudo apt update -qq");
//      cmds.add("sudo apt update -qq");
//      cmds.add("echo Y | sudo apt install default-jre -qq");
//      cmds.add("echo Y | sudo apt install default-jdk -qq");
//      cmds.add("echo Y | sudo apt install unzip -qq");
//      cmds.add("unzip n" + (id+1) + ".zip");
//      cmds.add("ls");
//      cmds.add("exit");
//      logs = jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//      System.out.println("initialization and unzipping complete");
//      cmds.clear();
//
//      if (ei.getFilesReceived() == false) {
//        for (int i = 0; i < parents.size(); i++) {
//          System.out.println("transferring files to " + id + " from " + parents.get(i).getId());
//
//          // TRANSFER INPUT FILES
//          cmds.add("chmod 600 " + pem_path + ".pem");
//          cmds.add("ls");
//          cmds.add("cd n" + (id +1));
//          if (id == 0) {
//            cmds.add("cd data/in");
//            cmds.add("scp -i ../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
//          } else {
//            cmds.add("cd data/in/" + parents.get(i).getId());
//            cmds.add("scp -i ../../../" + pem_path + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/n" + (parents.get(i).getId()+1) + "/data/out/" + id + "/* .");
//          }
//          cmds.add("exit");
//          logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//          cmds.clear();
//
//          parents.get(i).ei.setTransferCount((parents.get(i).ei.getTransferCount() + 1));
//          if (parents.get(i).ei.getTransferCount() == parents.get(i).getChildren().size()) {
//            System.out.println("transfers complete - terminating " + parents.get(i).getId());
//            aws.terminateInstance(parents.get(i).ei.getInsId());
//          }
//        }
//        ei.setFilesReceived(true);
//      }
//
//      // EXECUTE NODE
//      System.out.println("executing code on " + id);
//      cmds.add("ls");
//      cmds.add("cd n" + (id+1));
//      cmds.add("ls data/in");
//      cmds.add("chmod +x ./build.sh");
//      cmds.add("chmod +x ./run.sh");
//      cmds.add("./build.sh");
//      cmds.add("(time ./run.sh) 2> data/time.txt");
//      cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- > data/stats" + (id+1) + ".txt");
//
//      if (children.size() > 0) {
//        for (NodeADT child : children) {
//          cmds.add("du -l -s data/out/" + child.getId() + " | cut -d'd' -f1 | sed 's/.$//' >> data/stats" + (id + 1) + ".txt");
//        }
//      }
//
//      if (service.equals("test")) {
//        cmds.add("(time ./run.sh) 2> data/time.txt");
//        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
//        cmds.add("(time ./run.sh) 2> data/time.txt");
//        cmds.add("cut -d$'\n' -f2 data/time.txt | cut -b 6- >> data/stats" + (id+1) + ".txt");
//      }
//      cmds.add("exit");
//      logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
//      System.out.println("done executing code on " + id);
//      cmds.clear();
//
//      System.out.println("downloading stats from node " + id);
//      File genOutFile = new File( "USERS/" + user + "/executions/" + wf_name +
//              "/results/n" + id);
//      genOutFile.mkdirs();
//      jsch.download("/home/ubuntu/n" + (id+1) + "/data/stats" + (id+1) + ".txt", genOutFile.getAbsolutePath() +
//              "/stats.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//
//      System.out.println("downloading results from node " + id);
//      if (children.size() > 0) {
//        for (NodeADT child : children) {
//          File resFile =
//                  new File( src_dir + "/USERS/" + user + "/executions/" + wf_name + "/results/n" + id + "/" + child.getId());
//          resFile.mkdirs();
//          jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/" + child.getId() + "/*",
//                  resFile.getAbsolutePath() + "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//        }
//      } else {
//        jsch.download("/home/ubuntu/n" + (id + 1) + "/data/out/*", genOutFile.getAbsolutePath() +
//                "/res.txt", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
//      }
//      aws.terminateInstance(ei.getInsId());
//      while(true) {
//        ins = aws.describeInstance(ei.getInsId());
//        if (ins.getState().getCode() == 48) {
//          break;
//        }
//        Thread.sleep(2500);
//      }
//      Thread.sleep(5000);
//
//      File log_file = new File(src_dir + "/USERS/" + user + "/executions/" + wf_name + "/logs" +
//              "/" + id + "_logs.txt");
//
//      if (log_file.createNewFile()) {
//        System.out.println("file created : " + log_file.getName());
//      }
//
//      BufferedWriter bw = new BufferedWriter(new FileWriter(log_file, false));
//      bw.write(logs);
//      bw.close();
//
//      System.out.println("done with : " + id);
//      ei.setExecStatus(true); // !IMPORTANT!
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  public static double parseDuration(String strDuration) {
//    char[] durationArray = strDuration.toCharArray();
//
//    double duration = 0;
//
//    String temp;
//
//    for (int i = 1; i < durationArray.length; i++) {
//      temp = "";
//      int j = i - 1;
//      switch (durationArray[i]) {
//        case 'h':
//          while (j != -1) {
//            temp = durationArray[j] + temp;
//            j--;
//          }
//
//          duration = Double.parseDouble(temp) * 3600;
//          break;
//
//        case 'm':
//          do {
//            temp = durationArray[j] + temp;
//            j--;
//          } while (j != -1 && durationArray[j] != 'h');
//
//          duration += Double.parseDouble(temp) * 60;
//          break;
//
//        case 's':
//          do {
//            temp = durationArray[j] + temp;
//            j--;
//          } while (j != -1 && durationArray[j] != 'm');
//
//          duration += Double.parseDouble(temp);
//          break;
//      }
//    }
//    return duration;
//    //return duration;
//  }
//
//  public static void createNodeOrderXML(String resPath, int numNodes, Planner pr, ArrayList<ArrayList<Double>> durations,
//                                        ArrayList<ArrayList<String>> edgeWeights) throws ParserConfigurationException,
//          TransformerException {
//    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//    DocumentBuilder db = dbf.newDocumentBuilder();
//    Document doc = db.newDocument();
//
//
//    Element rootElement = doc.createElement("workflow");
//    doc.appendChild(rootElement);
//
//    String temp;
//    Element aTask = null;
//
//    Element budget = doc.createElement("budget");
//    budget.appendChild(doc.createTextNode(pr.getBudget() + ""));
//    rootElement.appendChild(budget);
//
//    for (int i = 0; i < numNodes; i++) {
//      aTask = doc.createElement("task");
//
//      Element ID, rts, weights;
//
//      ID = doc.createElement("id");
//      rts = doc.createElement("rts");
//      weights = doc.createElement("weights");
//
//      ID.appendChild( doc.createTextNode( i + ""));
//
//      temp = "";
//      for(double duration: durations.get(i)) {
//        temp += duration + ",";
//      }
//      if (!temp.equals("")) {
//        rts.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
//      }
//
//      temp = "";
//      for (String weight : edgeWeights.get(i)) {
//        temp += weight + ",";
//      }
//      if (!temp.equals("")) {
//        weights.appendChild(doc.createTextNode(temp.substring(0, temp.length() - 1)));
//      }
//
//      aTask.appendChild(ID);
//      aTask.appendChild(rts);
//      aTask.appendChild(weights);
//
//      rootElement.appendChild(aTask);
//
//    }
//
//    Transformer transformer = TransformerFactory.newInstance().newTransformer();
//
//    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//
//    Result output = new StreamResult(new File(resPath));
//    Source input = new DOMSource(doc);
//
//    transformer.transform(input, output);
//  }
//}