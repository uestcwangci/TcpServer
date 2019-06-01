package net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class UdpServer {
    private DatagramSocket ds = null;

    private boolean isUdpRun = true;
    private String ip = "255.255.255.255";//广播地址
    private static List<Integer> portList = new ArrayList<>(); //指定广播接收数据端口

    public static void main(String[] args) {
        initList();
        UdpServer server = new UdpServer();
        server.start();
        try {
            Thread.sleep(20000);
            server.udpServerClose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void initList() {
        portList.add(6787);
        portList.add(6788);
        portList.add(6789);
        portList.add(6790);
        portList.add(6791);
    }


    public void start(){
        try {
            System.out.println("UDP 广播服务器启动");
            ds = new DatagramSocket();
            for (Integer udpPort : portList) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runServer(udpPort);
                    }
                }).start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    private void runServer(int udpPort){
        try {
            while(isUdpRun){
                String sendMessage = "message from UDP server,";
                InetAddress adds = InetAddress.getByName(ip);
                DatagramPacket dp = new DatagramPacket(sendMessage.getBytes(),sendMessage.length(), adds, udpPort);
                ds.send(dp);
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("UdpTestServer run Exception: " + e.toString());
        }
    }

    private void send(int udpPort){
        try{

        }catch (Exception e) {
            System.out.println("UdpTestServer send Exception: "+e.toString());
        }
    }

    public void udpServerClose() {
        isUdpRun = false;
        ds.close();
        System.out.println("UDP 服务器关闭");
    }
}
