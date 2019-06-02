package net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//tcp服务器，单线程和多线程
public class TcpServer {
    // 存储设备的UDP监听端口

    // 监测当前TCP连接数量
//    private static AtomicInteger count = new AtomicInteger(0);
    private static Set<String> macSet;
    // 存储UDP端口与mac地址的映射

    private static  ConcurrentHashMap<String, String> mac2IPandPortMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        macSet = new HashSet<>();
        macSet.add("A4:50:46:18:ED:05"); // mix2s
        macSet.add("9C:2E:A1:C6:93:B3"); // mix2
        macSet.add("F4:5B:00:2E:EB:83");
        macSet.add("E8:ED:39:DE:3D:BF");
        macSet.add("56:EF:87:82:0F:F6");
        for (String mac : macSet) {
            mac2IPandPortMap.put(mac, "0|0");
        }

        new TcpServer().start(10041);
    }

    private volatile boolean isRun = false;

    private long receiveTimeDelay = 3000;

    //多线程服务器
    public void start(int port) {
        if (isRun) {
            return;
        }
        isRun = true;
        ServerSocket server = null;
        try {
            System.out.println("开始监听...............");
            server = new ServerSocket(port);
//            ExecutorService service = Executors.newFixedThreadPool(10);
            while (isRun) {
                //阻塞，直到有客户连接
                Socket socket = server.accept();
                socket.setKeepAlive(true);
                //启动服务线程
                new ServerThread(socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //使用线程为多个客户端服务
    class ServerThread extends Thread {
        private Socket socket;

        private boolean run = true;

        long lastReceiveTime = System.currentTimeMillis();

        ServerThread(Socket socket) {
          this.socket = socket;
        }

        private void closeSelf() {
            run = false;
        }

        //线程运行实体
        @Override
        public void run() {
            System.out.println("检测到： " + socket.getInetAddress() + ":" + socket.getPort() + " 接入");
            while (isRun && run && socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown()) {
                String clientMac = null;
                try {
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    if (is.available() > 0) {
                        String line = null;
                        while ((line = br.readLine()) != null) {
//                            System.out.println("line: " + line);
                            if ("quit".equalsIgnoreCase(line.substring(0, 4))) {
                                disconnect(line.substring(4));
                                break;
                            } else if ("heart".equalsIgnoreCase(line)) {
                                sendHeart(os);
                            } else if ("queryMap".equalsIgnoreCase(line)) {
                                sendMap(os);
                            } else {
                                // 收到mac，ip，port
                                updateMap(line);
                            }
                        }
                    } else {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnect(clientMac);
                }
            }
        }

        private void updateMap(String line) {
            String[] strings = line.split("\\|");
            String mac = strings[0];
            String ip = strings[1];
            String udpPort = strings[2];
            for (Map.Entry<String, String> entry : mac2IPandPortMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(mac)) {
                    mac2IPandPortMap.put(mac, ip + "|" + udpPort);
                    break;
                }
            }
        }

        private void sendMap(OutputStream os) {
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                for (Map.Entry<String, String> entry : mac2IPandPortMap.entrySet()) {
                    bw.write(entry.getKey() + "|" + entry.getValue());
                    bw.newLine();
                }
                bw.write("mapDone");
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendHeart(OutputStream os) {
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write("server heart pack");
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void disconnect(String mac) {
            run = false;
            if (!macSet.contains(mac)) {
                return;
            }
            System.out.println(mac + " 断开连接");
            mac2IPandPortMap.put(mac, "0|0");
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




    }

}




