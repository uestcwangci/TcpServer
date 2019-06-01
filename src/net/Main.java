package net;

public class Main {
    public static void main(String[] args) {
        // 启动服务器
//        new net.TcpServer().tcpStart(10041);
        // 模拟客户端
        try {
            Client client1 = new Client("192.168.0.116", 10041, "1", 6787);
            Client client2 = new Client("192.168.0.116", 10041, "2", 6788);
            Client client3 = new Client("192.168.0.116", 10041, "1C:2D:FF:11:02:B2", 6789);
            Client client4 = new Client("192.168.0.116", 10041, "1C:2D:1F:21:02:B5", 6790);
            Client client5 = new Client("192.168.0.116", 10041, "1C:2D:2F:31:02:C4", 6791);

//            client1.start();
//            client2.start();
            client3.start();
            client4.start();
            client5.start();
//            client1.udpStart();
//            client2.udpStart();
//            client3.udpStart();
//            client4.udpStart();
//            client5.udpStart();
            Thread.sleep(10000);
            client3.tcpClose();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
