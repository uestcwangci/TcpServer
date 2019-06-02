package net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//tcp服务器，单线程和多线程
public class TcpServer {
    // 存储设备的UDP监听端口
    private static List<Integer> portList = new ArrayList<>();

    // 监测当前TCP连接数量
//    private static AtomicInteger count = new AtomicInteger(0);
    private static Set<String> macSet;
    private static ConcurrentHashMap<String, Boolean> macOnlineMap;
    // 存储UDP端口与mac地址的映射
    private static final ConcurrentHashMap<Integer, String> portMacMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        macSet = new HashSet<>();
        macOnlineMap = new ConcurrentHashMap<>();
        macSet.add("A4:50:46:18:ED:05");
        macSet.add("B7:2D:1F:21:02:B5");
        macSet.add("F4:5B:00:2E:EB:83");
        macSet.add("E8:ED:39:DE:3D:BF");
        macSet.add("56:EF:87:82:0F:F6");
        for (String mac : macSet) {
            macOnlineMap.put(mac, false);
        }
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
                            } else if ("applyMac".equalsIgnoreCase(line)) {
                                clientMac = alignMap(os);
                            } else if ("heart".equalsIgnoreCase(line)) {
                                sendHeart(os);
                            } else if ("queryMap".equalsIgnoreCase(line)){
                                sendMap(os);
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

        private void sendMap(OutputStream os) {
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                for (Map.Entry<String, Boolean> entry : macOnlineMap.entrySet()) {
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
            macOnlineMap.put(mac, false);
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String alignMap(OutputStream os) {
            BufferedWriter bw = null;
            try {
                String mac = null;
                bw = new BufferedWriter(new OutputStreamWriter(os));
                for (Map.Entry<String, Boolean> entry : macOnlineMap.entrySet()) {
                    if (!entry.getValue()) {
                        macOnlineMap.put(entry.getKey(), true);
                        mac = entry.getKey();
                        bw.write(mac);
                        bw.newLine();
                        bw.flush();
                        break;
                    }
                }
                System.out.println(macOnlineMap);
                return mac;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }



    }

}




