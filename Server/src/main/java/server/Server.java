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

public class Server {
    private static final int BIT_LENGTH = 32;
    SecureRandom srandGenerator = new SecureRandom();
    private Map<Client, BigInteger> clients = new HashMap<>();
    private DatagramSocket socket;
    private BigInteger numP, numG;
    private int serverSecret;
    private boolean running = false;

    public Server(int port) {
        serverSecret = srandGenerator.nextInt(100);
        numP = BigInteger.probablePrime(BIT_LENGTH, srandGenerator);
        numG = findPrimitive(numP.longValue());
        System.out.println(numP);
        System.out.println(numG);
        try {
            socket = new DatagramSocket(port);
            System.out.println("Serwer uruchomiony na porcie : " + port + ".");
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

    }

    private BigInteger findPrimitive(long p) {
        List<Long> facts = new ArrayList<>();
        long phi = p - 1, n = phi;
        for (long i = 2; i * i <= n; i++)
            if (n % i == 0) {
                facts.add(i);
                while (n % i == 0)
                    n /= i;
            }
        if (n > 1)
            facts.add(n);

        for (int res = 2; res <= p; ++res) {
            boolean ok = true;
            for (int i = 0; i < facts.size() && ok; i++) {
                BigInteger iBI = BigInteger.valueOf(res);
                iBI = iBI.modPow(BigInteger.valueOf(phi / facts.get(i)), BigInteger.valueOf(p));
                ok &= iBI.intValue() != 1;
            }
            if (ok) return BigInteger.valueOf(res);
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

    private void processData(DatagramPacket packet) {
        String extraMsg = null; //extra message, using to exchange values of P,G and A,B
        try {
        	extraMsg = new String(packet.getData(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (extraMsg.startsWith("\\hello")) {
            String clientName = extraMsg.split(" ")[1].trim();
            Client newClient = new Client(clientName.trim(), packet.getAddress(), packet.getPort());
            if (findClient(clientName) != null) {
                String response = "\\wrong"; //Client name is already in use, pick another one
                send(response.getBytes(), newClient.getAddress(), newClient.getPort());
            } else {
                clients.put(newClient, null);
                String response = "\\keStep1 " + numP + " " + numG; 
                // keStep1 => KeyExchange - step 1. Using this prefix will allow client to receive values P & G
                send(response.getBytes(), newClient.getAddress(), newClient.getPort());
            }
        } else if (extraMsg.startsWith("\\keStep2")) {
            Client senderClient = getClient(packet.getAddress(), packet.getPort());
            BigInteger A = new BigInteger(extraMsg.split(" ")[1].trim());
            BigInteger B = numG.pow(serverSecret).mod(numP);
            String response = "\\keStep3 " + B;
            send(response.getBytes(), senderClient.getAddress(), senderClient.getPort());
            BigInteger secret = A.pow(serverSecret).mod(numP);
            clients.put(senderClient, secret);
            System.out.println("Tajna wartoœæ to - " + secret);
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


}
