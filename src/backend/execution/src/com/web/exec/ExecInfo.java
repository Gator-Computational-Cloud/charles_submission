package com.web.exec;

public class ExecInfo {
  private String ip;
  private String pem_path;
  private String sg_name;
  private String kp_name;
  private String ins_id;
  private int transfer_ct;

  private boolean exec_complete = false;
  private boolean files_received = false;
  private boolean initialized = false;
  private boolean initializing = false;

  public ExecInfo() {}

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getIp() {
    return ip;
  }

  public void setPemPath(String pem_path) {
    this.pem_path = pem_path;
  }

  public String getPemPath() {
    return pem_path;
  }

  public void setExecStatus(boolean exec_complete) {
    this.exec_complete = exec_complete;
  }

  public boolean getExecStatus() {
    return exec_complete;
  }

  public void setSgName(String sg_name) {
    this.sg_name = sg_name;
  }

  public String getSgName() {
    return sg_name;
  }

  public void setKpName(String kp_name) {
    this.kp_name = kp_name;
  }

  public String getKpName() {
    return kp_name;
  }

  public void setInsId(String ins_id) {
    this.ins_id = ins_id;
  }

  public String getInsId() {
    return ins_id;
  }

  public void setFilesReceived(boolean files_received) {
    this.files_received = files_received;
  }

  public boolean getFilesReceived() {
    return files_received;
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  public boolean getInitialized() {
    return initialized;
  }

  public void setInitializing(boolean initializing) {
    this.initializing = initializing;
  }

  public boolean getInitializing() {
    return initializing;
  }

  public void setTransferCount(int transfer_ct) {
    this.transfer_ct = transfer_ct;
  }

  public int getTransferCount() {
    return transfer_ct;
  }
}
