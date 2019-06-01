package net;

import message.BaseMessage;
import protocol.EmergencyProtocol;
import protocol.UnPackEmergencyProtocol;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    // TCP
    private String serverIP = "192.168.0.116";
    private int serverPort = 10041;
    private Socket clientSocket = null;
    private InputStream is = null;
    private OutputStream os = null;
    private String mac;
    private boolean isTcpRun = false;// 连接状态

    // UDP
    private boolean isUdpRun = true;
    public int udpPort = 6787;//数据监听绑定端口
    private DatagramSocket udpSocket = null;


    private long lastSendTime; //最后一次发送数据的时间


    public Client(String tcpServer, int tcpPort, String mac, int udpPort) {
        this.serverIP = tcpServer;
        this.serverPort = tcpPort;
        this.mac = mac;
        this.udpPort = udpPort;
    }

    public void start() {
        tcpStart();
        udpStart();
    }


    private void tcpStart(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isTcpRun) {
                        return;
                    }
                    clientSocket = new Socket(serverIP, serverPort);
                    is = clientSocket.getInputStream();
                    os = clientSocket.getOutputStream();
                    lastSendTime = System.currentTimeMillis();
                    isTcpRun = true;
                    clientSocket.setKeepAlive(true);
                    System.out.println(mac + " TCP连接成功");
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                    bw.write(udpPort + "|" + mac);
                    bw.newLine();
                    bw.flush();
                    new Thread(new KeepAliveWatchDog()).start();  //保持长连接的线程，每隔5秒项服务器发一个一个保持连接的心跳消息
                    new Thread(new ReceiveWatchDog()).start();    //接受消息的线程，处理消息
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    class KeepAliveWatchDog implements Runnable {
        long checkDelay = 10;
        long keepAliveDelay = 5000;

        public void run() {
            while (isTcpRun) {
                if (System.currentTimeMillis() - lastSendTime > keepAliveDelay) {
                    try {
                        Client.this.sendHeartPack("heart");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Client.this.tcpClose();
                    }
                    lastSendTime = System.currentTimeMillis();
                } else {
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Client.this.tcpClose();
                    }
                }
            }
        }
    }

    private void sendHeartPack(String str) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("heart");
        bw.newLine();
//        System.out.println(mac + " 发送心跳包");
        bw.flush();
    }


    class ReceiveWatchDog implements Runnable {
        public void run() {
            while (isTcpRun) {
                try {
                    InputStream in = clientSocket.getInputStream();
                    if (in.available() > 0) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            // ignore
//                            System.out.println("心跳接收：\t" + line);
                        }
                    } else {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Client.this.tcpClose();
                }
            }
        }
    }


    public void tcpClose() {
        try {
            System.out.println(mac + " 断开连接");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            bw.write("quit" + udpPort);
            bw.newLine();
            bw.flush();
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (isTcpRun) {
                isTcpRun = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void udpStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(mac + " UDP开始监听");
                runUdpClient();
            }
        }).start();
    }


    private void runUdpClient() {
        try {
            byte[] receiveBuffer = new byte[4096];//数据缓冲区
            udpSocket = new DatagramSocket(udpPort);//绑定端口进行数据监听
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);//数据接收包囊
            while (isUdpRun) {
                udpSocket.receive(receivePacket);//接收数据.阻塞式
                System.out.println("UDP from " + receivePacket.getAddress().getHostAddress() + " : " + receivePacket.getPort());
                byte[] receiveBytes = receivePacket.getData();
                System.out.println(receivePacket.getLength());
                EmergencyProtocol protocol = UnPackEmergencyProtocol.unPack(receiveBytes,
                        receivePacket.getOffset());
                System.out.println(protocol);
                if (protocol != null && protocol.getBody().getT() instanceof BaseMessage) {
                    BaseMessage baseMessage = (BaseMessage) protocol.getBody().getT();
                    switch (((BaseMessage) protocol.getBody().getT()).getDataType()) {
                        case 0x01:
                            System.out.println("收到音频消息");
                            break;
                        case 0x02:
                            System.out.println("收到视频消息");
                            break;
                        case 0x03:
                            System.out.println(mac + " 收到文字消息: " + new String(baseMessage.getData()));
                            break;
                        case 0x04:
                            System.out.println("收到图片消息");
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void udpClose() {
        isUdpRun = false;
        udpSocket.close();
        System.out.println("udp客户端关闭");
    }

}
