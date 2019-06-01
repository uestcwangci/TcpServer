package net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

//tcp服务器，单线程和多线程
public class TcpServer {
    // 存储设备的UDP监听端口
    private static List<Integer> portList = new ArrayList<>();

    // 监测当前TCP连接数量
//    private static AtomicInteger count = new AtomicInteger(0);


    // 存储UDP端口与mac地址的映射
    private static final ConcurrentHashMap<Integer, String> portMacMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        portList.add(6787);
        portList.add(6788);
        portList.add(6789);
        portList.add(6790);
        portList.add(6791);
        for (Integer port : portList) {
            portMacMap.put(port, "0");
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
            int udpPort = 0;
            String mac = null;
            System.out.println("检测到： " + socket.getInetAddress() + ":" + socket.getPort() + " 接入");
            while (isRun && run && socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown()) {
                try {
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    if (is.available() > 0) {
                        String line = null;
                        while ((line = br.readLine()) != null) {
//                            if (!"heart".equalsIgnoreCase(line)) {
//                                System.out.println("收到消息: " + line);
//                            }
                            if ("quit".equalsIgnoreCase(line.substring(0, 4))) {
                                disconnect(Integer.parseInt(line.substring(4)));
                                break;
                            } else if ("map".equalsIgnoreCase(line.substring(0,3))) {
                                sendMap(os, Integer.parseInt(line.substring(3)));
                            } else if ("heart".equalsIgnoreCase(line)) {
                                sendHeart(os);
                            } else {
                                String[] strings = line.split("\\|");
                                udpPort = Integer.parseInt(strings[0]);
                                mac = strings[1];
                                portList.add(udpPort);
                                portMacMap.put(udpPort, mac);
//                                count.incrementAndGet();
//                                System.out.println("现共有" + count + "条Tcp连接");
                            }
                        }
                    } else {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnect(udpPort);
                }
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


        private void disconnect(int udpPort) {
            run = false;
            if (!portList.contains(udpPort)) {
                return;
            }
            System.out.println(portMacMap.get(udpPort) + " 断开连接");
            portMacMap.put(udpPort, "0");
            portList.remove((Integer) udpPort);
//            count.decrementAndGet();
//            System.out.println("现共有" + count + "条Tcp连接");
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMap(OutputStream os, int udpPort) {
            BufferedWriter bw = null;
            try {
                System.out.println(portMacMap);
                bw = new BufferedWriter(new OutputStreamWriter(os));
                for (Map.Entry<Integer, String> entry : portMacMap.entrySet()) {
                    if (entry.getKey() != udpPort) {
                        String msg = entry.getKey() + "|" + entry.getValue();
                        bw.write(msg);
                        bw.newLine();
                    }
                }
                bw.write("mapDone");
                bw.newLine();
                bw.flush();
//                System.out.println("sendMap");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

}




