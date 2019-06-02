package net;

public class Main {
    public static void main(String[] args) {
        // 启动服务器
//        new net.TcpServer().tcpStart(10041);
        // 模拟客户端
        try {
            Client client1 = new Client("localhost", 10041);
            Client client2 = new Client("localhost", 10041);
            Client client3 = new Client("localhost", 10041);
            Client client4 = new Client("localhost", 10041);
            Client client5 = new Client("192.168.0.116", 10041);

//            client1.start();
//            client2.start();
//            client3.start();
//            client4.start();
            client5.start();
//            client1.udpStart();
//            client2.udpStart();
//            client3.udpStart();
//            client4.udpStart();
//            client5.udpStart();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
