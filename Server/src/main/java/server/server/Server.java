package server; 

import java.io.IOException;    
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.plaf.synth.SynthSpinnerUI;
import org.json.*;
import com.google.gson.*;



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
		
        System.out.println("Wartosc P - " + numP);
        System.out.println("Wartosc G - " + numG);
        
        try {
            socket = new DatagramSocket(port);
            System.out.println("Serwer uruchomiony na porcie : " + port + ".");
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
    }
    
    /**
     Method to find primitive root modulo P
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
     * When Client tries to connect with server he send a message[msg] with \request prefix. 
     * If Client name is already used, server send msg with \wrong prefix. And now Client has to pick another name.
     * If everything is ok, Server creates response with \pgVavlues prefix and appends to it values of P and G.
     * After that Client send reply containing value of A with \aValue prefix.
     * Server calculated the value of B and finally the secret value.
     */
    
   /* private String convertTojson(String str , String key){
    	JSONObject jObject = new JSONObject(str.trim());
    	// System.out.println(jObject.getString(key));
    	return jObject.getString(key);}
    */
    
    private void processData(DatagramPacket packet) {
    	
            String extraMsg = null; //extra message, using to process the incoming data
          //  JSONObject jsonObj = null;

            try 
            {
              extraMsg = new String(packet.getData(), "UTF-8");  
            //  jsonObj =  new JSONObject(extraMsg.toString().trim())

            } 
            catch (UnsupportedEncodingException | JSONException e) 
            {
                e.printStackTrace();
            }  
            
            if (extraMsg.startsWith("\\request")) 
            {
                String clientName = extraMsg.split(" ")[1].trim();
                Client newClient = new Client(clientName.trim(), packet.getAddress(), packet.getPort());
                if (findClient(clientName) != null) {
                    String response = "\\wrong"; //Client name is already in use, pick another one
                    send(response.getBytes(), newClient.getAddress(), newClient.getPort());
                } else 
                {
                    clients.put(newClient, null);
                    String response = "\\pgValues " + numP + " " + numG; 
                    //KeyExchange - step 1. Using prefix pgValues will allow client to receive values P & G
                    send(response.getBytes(), newClient.getAddress(), newClient.getPort());
                }
            }             
            else if (extraMsg.startsWith("\\aValue")) 
            	{     	
           //KeyExchange - step 2. Server receives value of A from client.
            Client senderClient = getClient(packet.getAddress(), packet.getPort());
            BigInteger A = new BigInteger(extraMsg.split(" ")[1].trim());
            BigInteger B = numG.pow(serverSecret).mod(numP);  
            String response = "\\bValue " + B;
            //KeyExchange - step 3. Prepare messasge with value of B. 
            send(response.getBytes(), senderClient.getAddress(), senderClient.getPort());
            secretValue = A.pow(serverSecret).mod(numP); 
            //Server calculates the secret value
            // System.out.println("Secret value for " + senderClient.getName() + " is: " + secretValue);

            clients.put(senderClient, secretValue); //ascription secret Value to the client
            	}
            else if (extraMsg.startsWith("\\encrypt")){
            	 if (extraMsg.split(" ")[1].trim().equals("1")){
                 	encrypting = true;}
            }
            else if (extraMsg.startsWith("\\message"))//this prefix allows both sides to exchange messages. 
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
                 scan.close();
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
    
    /*
     * Method used to check if name for Client is still available
     */
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