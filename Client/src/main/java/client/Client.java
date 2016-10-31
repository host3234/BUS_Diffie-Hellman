package client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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
    private String extraInfo =null;
    private BigInteger secret = null;
    private BigInteger numP, numG;
    private int clientSecret;
    private Random randGenerator;
    private BigInteger secretValue;


    public Client(String name, String address, int port) {
        this.clientName = name;
        this.serverAddressString = address;
        this.serverPort = port;
        randGenerator = new Random();
        clientSecret = randGenerator.nextInt(50);
    }

    public boolean openConnection() {
        try {
            socket = new DatagramSocket(); 
            socket.setSoTimeout(3000);
            serverAddress = InetAddress.getByName(serverAddressString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        String handshakeMsg = "\\hello " + clientName; // this prefix will be use to  start connection with server
    	// System.out.println("Test 1C");
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
                String msg = scan.nextLine();
               if (msg.startsWith("exit")) {
                    System.out.println("Aplikacja zakonczona.");
                    System.exit(0);
                }
               else {
                   byte endByte = Byte.parseByte("0");
                   String toSend = "\\message " + msg + " " + endByte;
                   try {
                       byte[] bytes = toSend.getBytes("UTF-8");
                       byte[] newBytes = Arrays.copyOf(bytes, bytes.length + 1);
                       newBytes[newBytes.length - 1] = new Byte("0");
                       send(newBytes);
                   } catch (UnsupportedEncodingException e) {
                       e.printStackTrace();
                   }
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
    
    
    private void runReceiver() {

        Thread receiver = new Thread(() -> {
            while (running) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    System.out.println("Nie nawiazano polaczenia z serwerem!!!");
                    socket.close();
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                processData(packet);
            }
        }, "Receive");
        receiver.start();
    }

    private void processData(DatagramPacket packet) {

        String extraMsg = null; //extra message, using to exchange values of P,G and A,B
        try {
        	extraMsg = new String(packet.getData(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (extraMsg.startsWith("\\wrong")){ //Client name is already in use, pick another one
            System.out.println("Taki Klient jest ju¿ na serwerze! Wybierz inn¹ nazwê.");
            System.exit(0);
        }
        else if (extraMsg.startsWith("\\keStep1")) // KeyExchange - step 1, using this prefix allows client to receive values P & G
        {
            System.out.println("Polaczono z serwerem! Jesteœ zalogowany jako " + clientName + ".");
            try {
                socket.setSoTimeout(1000000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            printMenu();
            String[] splittedMessage = extraMsg.split(" ");
            numP = new BigInteger(splittedMessage[1].trim());
            numG = new BigInteger(splittedMessage[2].trim());
            BigInteger A = numG.pow(clientSecret).mod(numP); //calculate the A value and send it to server in response
            String toSend = "\\keStep2 " + A; // KeyExchange- step 2. Client sends value of A, using special prefix
            send(toSend.getBytes());
        } 
        else if (extraInfo.startsWith("\\keStep3")) // KeyExchange - step 3. Client gets message with value of B.
        {
            BigInteger B = new BigInteger(extraInfo.split(" ")[1].trim());
            secretValue = B.pow(clientSecret).mod(numP);
            System.out.println("Tajna wartoœæ to - " + secretValue);
        } 
    }
    


}
