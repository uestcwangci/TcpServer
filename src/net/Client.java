package net;

import message.BaseMessage;
import protocol.EmergencyProtocol;
import protocol.UnPackEmergencyProtocol;

import java.io.*;
import java.net.*;
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
    private MulticastSocket multicastSocket = null;
    private int udpPort = 8888; // 组播侦听端口
    private String mulIp = "244.0.0.12";//组播地址 使用D类地址
    private byte[] buffer; // 缓存




    private long lastSendTime; //最后一次发送数据的时间


    public Client(String tcpServer, int tcpPort) {
        this.serverIP = tcpServer;
        this.serverPort = tcpPort;
        this.mac = mac;
        this.buffer = new byte[4096];

    }

    public void start() {
        tcpStart();
        udpStart();
    }

    public void stop() {
        tcpClose();
        udpClose();
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

                            System.out.println("心跳接收：\t" + line);
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
                // 接收数据时需要指定监听的端口号
                try {
                    multicastSocket = new MulticastSocket(udpPort);
                    // 创建组播ID地址
                    InetAddress address = InetAddress.getByName(mulIp);
                    // 加入地址
                    multicastSocket.joinGroup(address);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isUdpRun = true;
                if (multicastSocket == null) {
                    return;
                }
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                while (isUdpRun) {
                    try {
                        // 接收数据，同样会进入阻塞状态
                        multicastSocket.receive(datagramPacket);
                        System.out.println("UDP from " + datagramPacket.getAddress().getHostAddress() + " : " + datagramPacket.getPort());
                        byte[] receiveBytes = datagramPacket.getData();
                        EmergencyProtocol protocol = UnPackEmergencyProtocol.unPack(receiveBytes,
                                datagramPacket.getOffset());
                        if (protocol != null && protocol.getBody().getT() instanceof BaseMessage) {
                            BaseMessage baseMessage = (BaseMessage) protocol.getBody().getT();
                            switch ((baseMessage.getDataType())) {
                                case 0x01:
                                    System.out.println(mac + " 收到音频消息: ");
                                    break;
                                case 0x02:
                                    System.out.println("收到视频消息");
                                    break;
                                case 0x03:
                                    System.out.println("收到文字消息: " + new String(baseMessage.getData()));
                                    break;
                                case 0x04:
                                    System.out.println(mac + " 收到图片消息");
                                    break;
                                default:
                                    System.out.println("收到未知消息");
                                    break;
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        try {
                            if (multicastSocket != null) {
                                multicastSocket.leaveGroup(InetAddress.getByName(mulIp));
                                multicastSocket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void udpClose() {
        isUdpRun = false;
        System.out.println("udp客户端关闭");
    }

}
