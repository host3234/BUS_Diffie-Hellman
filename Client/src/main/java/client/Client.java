package client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Client {

    private String clientName;
    private String serverAddressString;
    private int serverPort;
    private InetAddress serverAddress;
    private DatagramSocket socket;
    private boolean running = false;

    public Client(String name, String address, int port) {
        this.clientName = name;
        this.serverAddressString = address;
        this.serverPort = port;
    }

    public boolean openConnection() {
        try {
            socket = new DatagramSocket(); 
            socket.setSoTimeout(2000);
            serverAddress = InetAddress.getByName(serverAddressString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        String handshakeMsg = "\\hello " + clientName; // this prefix will be use to  start connection with server
        send(handshakeMsg.getBytes());
        return true;
    }

    private void printMenu() {
        System.out.println("Witamy na serwerze");
        System.out.println("Aby wyjsc z serwera wpisz [exit] ");
    }

    public void start() {
        Thread clientThr = new Thread(() -> {
            running = true;
            runReceiver();
            Scanner scan = new Scanner(System.in);
            while (running) {
                String test = scan.nextLine();
               if (test.startsWith("exit")) {
                    System.out.println("Aplikacja zakonczona.");
                    System.exit(0);
                }
               }
        }, "Client");
        clientThr.start();
    }

    public void send(final byte[] data) {
        Thread send = new Thread("Send") {
            public void run() {
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        send.start();
    }
    
    private void processData() {
    	// TODO 
    }

	private void runReceiver() {

        Thread receiver = new Thread(() -> {
            while (running) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    System.out.println("Nie nawiazano polaczenia z serwerem!");
                    socket.close();
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
           //    TODO processData method
            }

        }, "Receive");
        receiver.start();
    }
}
