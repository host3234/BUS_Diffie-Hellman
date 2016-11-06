package server; 

import java.io.IOException; 
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import org.json.*;

public class Server {
    SecureRandom srandGenerator = new SecureRandom();
    private Map<Client, BigInteger> clients = new HashMap<>();
    private DatagramSocket socket;
    private BigInteger numP, numG;
    private int serverSecret;
    private boolean running = false;
    private  boolean encrypting = false;
    private BigInteger secretValue;
    private String mimeDecodedString;
    private String mimeEncodedString;
    
    
    /**
     * Instance of Server Class. Show the public value of P and G
     */
    
    public Server(int port) {
        serverSecret = srandGenerator.nextInt(100);
        numP = BigInteger.probablePrime(32, srandGenerator);
        numG = findPrimitive(numP.longValue());
		
        System.out.println("Wartość P - " + numP);
        System.out.println("Wartość G - " + numG);
       
        try {
            socket = new DatagramSocket(port);
            System.out.println("Serwer uruchomiony na porcie : " + port + ".");
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
    }
    
    
    /**
     Method to find G number. 
     */
    
    private BigInteger findPrimitive(long valueP) {
        List<Long> results = new ArrayList<>();
        long phi = valueP - 1,n; 
        n = phi;
        for (long i = 2; i*i <= n; i++)
            if (n%i == 0) 
            {
            	results.add(i);
                while (n % i == 0)
                    n /= i;
            }
        if (n > 1){
        	results.add(n);
        }
        for (int j = 2; j <= valueP; ++j) {
            boolean check = true;
            for (int i = 0; i < results.size() && check; i++) {
                BigInteger iBI = BigInteger.valueOf(j);
                iBI = iBI.modPow(BigInteger.valueOf(phi / results.get(i)), BigInteger.valueOf(valueP));
                check &= iBI.intValue() != 1;
            }
            if (check){
            	return BigInteger.valueOf(j);
            }
        }
        return BigInteger.valueOf((-1));
    }

   
    public void start() {
        Thread mainThread = new Thread(() -> {
            running = true;
            runReceiver();
        }, "Main");
        mainThread.start();
    }
    
    
   
    private void runReceiver() {
        Thread receiver = new Thread(() -> {
            while (running) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                processData(packet);
            }
        }, "Receive");
        receiver.start();
    }

    /**
     * Method to process all data between Server and Client. 
     * When Client tries to conenct with server he send a message[msg] with \hello prefix. 
     * If Client name is already used, server send msg with \wrong prefix. And now Client has to pick another name.
     * If everything is ok, Server create response with \keStep1 prefix and appends to the it value of P and G.
     * After that Client send reply containing value of A with \keStep2 prefix.
     * Server calculated the value of B and finally the secret value.
     */
    private void processData(DatagramPacket packet) {
            String extraMsg = null; //extra message, using to process the incoming data
            try 
            {
            	extraMsg = new String(packet.getData(), "UTF-8"); //receive the data
            } 
            catch (UnsupportedEncodingException e) 
            {
                e.printStackTrace();
            }     
            if (extraMsg.startsWith("\\hello")) 
            {
                String clientName = extraMsg.split(" ")[1].trim();
                Client newClient = new Client(clientName.trim(), packet.getAddress(), packet.getPort());
                if (findClient(clientName) != null) {
                    String response = "\\wrong"; //Client name is already in use, pick another one
                    send(response.getBytes(), newClient.getAddress(), newClient.getPort());
                } else 
                {
                    clients.put(newClient, null);
                    String response = "\\keStep1 " + numP + " " + numG; 
                    // keStep1 => KeyExchange - step 1. Using this prefix will allow client to receive values P & G
                    send(response.getBytes(), newClient.getAddress(), newClient.getPort());
                }
            }             
            else if (extraMsg.startsWith("\\keStep2")) 
            	{     	
           // keStep2 => KeyExchange - step 2. Data received from client.
            Client senderClient = getClient(packet.getAddress(), packet.getPort());
            BigInteger A = new BigInteger(extraMsg.split(" ")[1].trim());
            BigInteger B = numG.pow(serverSecret).mod(numP);  
            if (extraMsg.split(" ")[2].trim().equals("1")){
            	encrypting = true;}
            String response = "\\keStep3 " + B;
            // keStep2 => KeyExchange - step 3. Prepare messasge with value of B. 
            send(response.getBytes(), senderClient.getAddress(), senderClient.getPort());
            secretValue = A.pow(serverSecret).mod(numP); 
            //calculated the secret value, which will be used to encryption
          //System.out.println("Secret value for " + senderClient.getName() + " is: " + secretValue);

            clients.put(senderClient, secretValue); //ascription secret Value to the client
            	}
            else if (extraMsg.startsWith("\\message"))//this prefix allows both sides to exchange messages
            	{
            try {
            	extraMsg = extraMsg.trim();
            	extraMsg = extraMsg.substring(0, extraMsg.length() - 1);
                String[] splittedMessage = extraMsg.split(" ", 2);

                byte[] mimeDecoded = Base64.getMimeDecoder().decode(splittedMessage[1]);
                mimeDecodedString = new String(mimeDecoded, "utf-8");
            }
            catch (UnsupportedEncodingException e){
            	e.printStackTrace();
            } 
        	Client senderClient = getClient(packet.getAddress(), packet.getPort());
            if (encrypting) 
            {
            	mimeDecodedString = CaesarDecrypt(mimeDecodedString.trim(), clients.get(senderClient).intValue());             
            }
            System.out.println(senderClient.getName() + ": " +mimeDecodedString);
            sendMessage(senderClient.getName(), mimeDecodedString);
            Scanner scan = new Scanner(System.in);
            		String msg = scan.nextLine();
            if (msg.startsWith("exit")) {
                 System.out.println("Aplikacja zakończona.");
                 System.exit(0);
            	}    
            
            sendMessage("Server: ", msg);            
            	}
    }

    private void send(final byte[] data, final InetAddress address, final int port) {
        Thread sender = new Thread(() -> {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "Send");
        sender.start();
    }
    
    private Client findClient(String name) {
        for (Client cli : clients.keySet())
        {
            if (cli.getName().equals(name))
                return cli;
        }
        return null;
    }
    
    private Client getClient(InetAddress address, int port) {
        for (Client cli : clients.keySet()) 
        {
            if (cli.getAddress().toString().equals(address.toString()) && cli.getPort() == port)
                return cli;
        }
        return null;
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
        
    private void sendMessage(String sender, String message) {
        String messageToSend;
        for (Client cli : clients.keySet()) {
            if (encrypting) 
            {
            	messageToSend = CaesarEncrypt(message, clients.get(cli).intValue());
            } 
            else 
            {
            	messageToSend = message;
            }
            try
            {
            	byte[] mimeBytes = messageToSend.getBytes("utf-8");
                mimeEncodedString = Base64.getMimeEncoder().encodeToString(mimeBytes);
            } 
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String ourMessage = "\\message " + sender + " " + mimeEncodedString;
            byte[] bytes = new byte[0];
            try
            {
                bytes = ourMessage.getBytes("UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            byte[] newBytes = Arrays.copyOf(bytes, bytes.length + 1);
            newBytes[newBytes.length - 1] = new Byte("0");
            send(newBytes, cli.getAddress(), cli.getPort());
        }
    }
}