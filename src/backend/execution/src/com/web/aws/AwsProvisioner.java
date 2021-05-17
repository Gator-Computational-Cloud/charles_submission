package com.web.aws;

import java.util.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;

public class AwsProvisioner {
  private final String access_key;
  private final String secret_key;
  private final String token;

  public AwsProvisioner(String access_key, String secret_key, String token) {
    // Constructor
    this.access_key = access_key;
    this.secret_key = secret_key;
    this.token = token;
  }

  public void startInstance(String instanceId) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    ArrayList<String> instanceIdsToStart = new ArrayList<String>();
    try {
      instanceIdsToStart.add(instanceId);
      StartInstancesRequest startRequest = new StartInstancesRequest(instanceIdsToStart);
      ec2.startInstances(startRequest);
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public void stopInstance(String instanceId) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    ArrayList<String> instanceIdsToStop = new ArrayList<String>();
    try {
      instanceIdsToStop.add(instanceId);
      StopInstancesRequest stopRequest = new StopInstancesRequest(instanceIdsToStop);
      ec2.stopInstances(stopRequest);
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public void stopInstances() {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    Set<Instance> instances = new HashSet<Instance>();
    DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
    List<Reservation> reservations = describeInstancesRequest.getReservations();
    for (Reservation reservation : reservations) {
      instances.addAll(reservation.getInstances());
    }

    ArrayList<String> instanceIdsToStop = new ArrayList<String>();
    try {
      for (Instance ins : instances) {
        String instanceId = ins.getInstanceId();
        InstanceState is = ins.getState();
        if (is.getName().equalsIgnoreCase("running")) {
          instanceIdsToStop.add(instanceId);
        }
      }
      if (instanceIdsToStop.size() > 0) {
        StopInstancesRequest stopRequest = new StopInstancesRequest(instanceIdsToStop);
        ec2.stopInstances(stopRequest);
      }
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public void terminateInstance(String instanceId) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    ArrayList<String> instanceIdsToTerminate = new ArrayList<String>();
    try {
      instanceIdsToTerminate.add(instanceId);
      TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceIdsToTerminate);
      ec2.terminateInstances(terminateRequest);
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public void terminateInstances() {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    Set<Instance> instances = new HashSet<Instance>();
    DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
    List<Reservation> reservations = describeInstancesRequest.getReservations();
    for (Reservation reservation : reservations) {
      instances.addAll(reservation.getInstances());
    }

    ArrayList<String> instanceIdsToTerminate = new ArrayList<String>();
    try {
      for (Instance ins : instances) {
        String instanceId = ins.getInstanceId();
        instanceIdsToTerminate.add(instanceId);
      }
      if (instanceIdsToTerminate.size() > 0) {
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceIdsToTerminate);
        ec2.terminateInstances(terminateRequest);
      }
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public ArrayList<ArrayList<String>> listInstances() {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    Set<Instance> instances = new HashSet<Instance>();
    DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
    List<Reservation> reservations = describeInstancesRequest.getReservations();
    for (Reservation reservation : reservations) {
      instances.addAll(reservation.getInstances());
    }

    ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
    for (Instance ins : instances) {
      ArrayList<String> hmap = new ArrayList<String>();
      hmap.add(ins.getInstanceId());
      hmap.add(ins.getState().getName());
      hmap.add(ins.getKeyName());
      hmap.add(ins.getPublicIpAddress());
      hmap.add(ins.getSecurityGroups().get(0).getGroupId());
      al.add(hmap);
    }

    return al;
  }

  public String createKeyPair(String kpName) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();



    CreateKeyPairRequest kpObj = new CreateKeyPairRequest();
    kpObj.withKeyName(kpName);

    try {
      CreateKeyPairResult kpRes = ec2.createKeyPair(kpObj);
      String mat = kpRes.getKeyPair().getKeyMaterial();
      return mat;
    } catch(Exception x) {
      x.printStackTrace();
    }

    return null;
  }

  public void deleteKeyPair(String kpName) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    DeleteKeyPairRequest kpObj = new DeleteKeyPairRequest();
    kpObj.withKeyName(kpName);

    try {
      DeleteKeyPairResult kpRes = ec2.deleteKeyPair(kpObj);
    } catch(Exception x) {
      x.printStackTrace();
    }
  }

  public ArrayList<ArrayList<String>> listKeyPairs() {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    DescribeKeyPairsResult dKpRes = ec2.describeKeyPairs();

    ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
    for(KeyPairInfo kp : dKpRes.getKeyPairs()) {
      ArrayList<String> hmap = new ArrayList<String>();
      hmap.add(kp.getKeyName());
      hmap.add(kp.getKeyFingerprint());
      al.add(hmap);
    }

    return al;
  }

  public void createSecurityGroup(String sgName) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
    csgr.withGroupName(sgName).withDescription("SG generated by GCC");

    CreateSecurityGroupResult createSecurityGroupResult =
    ec2.createSecurityGroup(csgr);

    IpRange ip_range = new IpRange()
    .withCidrIp("0.0.0.0/0");

    IpPermission ip_perm = new IpPermission()
    .withIpProtocol("tcp")
    .withToPort(80)
    .withFromPort(80)
    .withIpv4Ranges(ip_range);

    IpPermission ip_perm2 = new IpPermission()
    .withIpProtocol("tcp")
    .withToPort(22)
    .withFromPort(22)
    .withIpv4Ranges(ip_range);

    AuthorizeSecurityGroupIngressRequest auth_request = new
    AuthorizeSecurityGroupIngressRequest()
    .withGroupName(sgName)
    .withIpPermissions(ip_perm, ip_perm2);

    AuthorizeSecurityGroupIngressResult auth_response =
    ec2.authorizeSecurityGroupIngress(auth_request);
  }

  public void deleteSecurityGroup(String sgName) {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    DeleteSecurityGroupRequest dsgr = new DeleteSecurityGroupRequest();
    dsgr.withGroupName(sgName);

    try {
      DeleteSecurityGroupResult deleteSecurityGroupResult = ec2.deleteSecurityGroup(dsgr);
    } catch(Exception x) {
      x.printStackTrace();
    }
  }

  public ArrayList<ArrayList<String>> listSecurityGroups() {
    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    DescribeSecurityGroupsRequest dsgr = new DescribeSecurityGroupsRequest();
    DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(dsgr);

    ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
    for(SecurityGroup sg : response.getSecurityGroups()) {
      ArrayList<String> hmap = new ArrayList<String>();
      hmap.add(sg.getGroupName());
      hmap.add(sg.getGroupId());
      hmap.add(sg.getVpcId());
      al.add(hmap);
    }

    return al;
  }

  public String createInstances(int num, String insType, String insImage, String kpName, String sgName) {
    String vm_id = null;

    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
    .withInstanceType(insType)
    .withImageId(insImage)
    .withMinCount(num)
    .withMaxCount(num)
    .withKeyName(kpName)
    .withSecurityGroups(sgName);

    RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
    Reservation re = runInstances.getReservation();
    List<Instance> insl = re.getInstances();
    for (Instance ins : insl) {
      vm_id = ins.getInstanceId();
    }

    return vm_id;
  }

  public Instance describeInstance(String instanceId) {
    Instance i = null;

    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(access_key, secret_key, token)))
    .withRegion(Regions.US_EAST_1)
    .build();

    DescribeInstancesRequest request = new DescribeInstancesRequest()
    .withInstanceIds(instanceId);

    DescribeInstancesResult response = ec2.describeInstances(request);
    for(Reservation reservation : response.getReservations()) {
      for(Instance instance : reservation.getInstances()) {
        i = instance;
      }
    }

    return i;
  }
}
