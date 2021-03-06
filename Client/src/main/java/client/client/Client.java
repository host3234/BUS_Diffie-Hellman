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
import com.google.gson.Gson;

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
    private String mimeDecodedString;
    private String ourMessage ="";

    
    /**
     * Instance of Client Class. Secret random value for Client is generate immediately.
     */
    
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
        
       
        String handshakeMsg = "\\request " + clientName; // this prefix will be use to  start connection with server
      //  JSONObject obj = new JSONObject();
      //  obj.put("message", handshakeMsg);
      
        send(handshakeMsg.getBytes());
       // send (obj.toString().getBytes());    
         
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
                    scan.close();
                    System.exit(0);
                }        
               else
               {                	   
            	   try {
            		   if (encrypting) {
                		   msg = CaesarEncrypt(msg, secretValue.intValue());
                       }
            		   byte[] mimeBytes = msg.getBytes("utf-8");
                       mimeEncodedString = Base64.getMimeEncoder().encodeToString(mimeBytes);              	   
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

    /**
     * Method to process all data between Client and Server. 
     * When Client tries to conenct with server he send a message[msg] with \hello prefix. 
     * If Client name is already used, server send msg with \wrong prefix. And now Client has to pick another name.
     * If everything is ok, Server create response with \keStep1 prefix and appends to the it values of P and G.
     * Client calculates the value of A and appends it to response, with /keStep2 prefix.
	 * Message with /keStep3 prefix contains value of B. Now Client is ready to compute the secret value.
     */
    
    private void processData(DatagramPacket packet) {
   	
        String extraInfo = null;
        try {
        	extraInfo = new String(packet.getData(), "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (extraInfo.startsWith("\\pgValues")) {
            //KeyExchange - step 1. Prefix pgValues informs client that sending data contains values P & G
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
            String toSend = "\\aValue " + A;
            //KeyExchange - step 2. Prepare data with value of A from client.
            send(toSend.getBytes());  

        } else if (extraInfo.startsWith("\\wrong")) {
            System.out.println("Ten nick juz istnieje na serwerze! Wybierz inny.");
            System.exit(0);
        } else if (extraInfo.startsWith("\\bValue")) {
            //KeyExchange - step 3. Client gets value of B. 
            BigInteger B = new BigInteger(extraInfo.split(" ")[1].trim());
            secretValue = B.pow(clientSecret).mod(numP);
            int checkEncrypt = encrypting ? 1 : 0;
            String toSend = "\\encrypt " + checkEncrypt;
            send(toSend.getBytes());
        } 
        	else if (extraInfo.startsWith("\\message")) {
 		
            String source = extraInfo.split(" ", 3)[1];
            String message = extraInfo.split(" ", 3)[2].trim();
    
            try {
            	byte[] mimeDecodedString = Base64.getMimeDecoder().decode(message);
            	ourMessage = new String(mimeDecodedString, "utf-8");
        	}
            catch (UnsupportedEncodingException e){
            	e.printStackTrace();
            }
            
            if (encrypting) {
            	ourMessage = CaesarDecrypt(ourMessage, secretValue.intValue());
            }
            System.out.println(source + " : " + ourMessage);
        }   
         
    }
    
    String CaesarEncrypt(String text, int key) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) 
        {
            chars[i] += key;
        }
        return String.valueOf(chars);
    }

    String CaesarDecrypt(String text, int key) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) 
        {
            chars[i] -= key;
        }
        return String.valueOf(chars);
    }
}