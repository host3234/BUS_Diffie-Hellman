package client;  

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import org.json.*;

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
    private boolean encrypting = false;
    private String mimeEncodedString;
    

    public Client(String name, String address, int port, boolean encrypting) {
        this.clientName = name;
        this.serverAddressString = address;
        this.serverPort = port;
        this.encrypting = encrypting;
        randGenerator = new Random();
        clientSecret = randGenerator.nextInt(100);
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
               else
               {         	             	   
            	   try {
                 //  String base64encodedString = Base64.getMimeEncoder().encodeToString(msg.getBytes("utf-8"));
            		   if (encrypting) {
                		   msg = CaesarEncrypt(msg, secretValue.intValue());
                       }
            		   byte[] mimeBytes = msg.getBytes("utf-8");
                       mimeEncodedString = Base64.getMimeEncoder().encodeToString(mimeBytes);              	   
            	//   msg = base64encodedString;
            	   }

            	   catch (UnsupportedEncodingException e) {
            		   e.printStackTrace();
            	   }
                   byte endByte = Byte.parseByte("0");
                   String toSend = "\\message " + mimeEncodedString + " " + endByte;
                   try {
                       byte[] bytes = toSend.getBytes("UTF-8");
                       byte[] newBytes = Arrays.copyOf(bytes, bytes.length + 1);
                       newBytes[newBytes.length - 1] = new Byte("0");
                       send(newBytes);
                   } catch (UnsupportedEncodingException e) {
                       e.printStackTrace();
                   }
               } } }, "Client");
        clientThr.start(); }
   
    public void send(final byte[] data) {
        Thread send = new Thread("SendData") {
            public void run() {
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }};
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
                 catch (NullPointerException e){
                	 System.out.println("Błąd");
                 }
                processData(packet);
            }
        }, "Receive");
        receiver.start();
    }

    private void processData(DatagramPacket packet) {
   	
        String extraInfo = null;
        try {
        	extraInfo = new String(packet.getData(), "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (extraInfo.startsWith("\\keStep1")) {
            System.out.println("Polaczono z serwerem! Zalogowano jako " + clientName + ".");
            try {
                socket.setSoTimeout(1000000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            printMenu();
            String[] splittedMessage = extraInfo.split(" ");
            numP = new BigInteger(splittedMessage[1].trim());
            numG = new BigInteger(splittedMessage[2].trim());
            BigInteger A = numG.pow(clientSecret).mod(numP);
            int checkEncrypt = encrypting ? 1 : 0;
            String toSend = "\\keStep2 " + A + " " + checkEncrypt ;
            send(toSend.getBytes());
        } else if (extraInfo.startsWith("\\wrong")) {
            System.out.println("Ten nick juz istnieje na serwerze! Wybierz inny.");
            System.exit(0);
        } else if (extraInfo.startsWith("\\keStep3")) {
            BigInteger B = new BigInteger(extraInfo.split(" ")[1].trim());
            secretValue = B.pow(clientSecret).mod(numP);
            
        } 
        	else if (extraInfo.startsWith("\\message")) {
        		
            String source = extraInfo.split(" ", 3)[1];
            String message = extraInfo.split(" ", 3)[2].trim();
            
            if (encrypting) {
                message = CaesarDecrypt(message, secretValue.intValue());
            }
            System.out.println(source + " : " + message);
        }   
         
    }
    
    private String CaesarEncrypt(String text, int key) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) 
        {
            chars[i] += key;
        }
        return String.valueOf(chars);
    }

    private String CaesarDecrypt(String text, int key) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) 
        {
            chars[i] -= key;
        }
        return String.valueOf(chars);
    }
}