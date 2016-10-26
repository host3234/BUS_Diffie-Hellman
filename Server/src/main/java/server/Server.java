package server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.*;

public class Server {
    private BigInteger numP, numG;
    SecureRandom secRandGenerator = new SecureRandom();
    private DatagramSocket socket;
    private boolean running = false;

    public Server(int port) {
        numP = BigInteger.probablePrime(32, secRandGenerator);
        numG = gFinder(numP.longValue());
        System.out.println("Wartosc p - " + numP);
        System.out.println("Wartosc g - " + numG);
        try {
            socket = new DatagramSocket(port);
            System.out.println("Serwer uruchomiony na porcie : " + port + ".");
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Method to generate the value of G 
     */
    private BigInteger gFinder(long p) {
        List<Long> probableG = new ArrayList<>();
        long extraVar = p - 1, n = extraVar;
        for (long i = 2; i * i <= n; i++)
            if (n % i == 0) {
                probableG.add(i);
                while (n % i == 0)
                    n /= i;
            }
        if (n > 1){
            probableG.add(n);	
        }
        for (int result = 2; result <= p; ++result) {
            boolean accept = true;
            for (int i = 0; i < probableG.size() && accept; i++) {
                BigInteger iBI = BigInteger.valueOf(result);
                iBI = iBI.modPow(BigInteger.valueOf(extraVar / probableG.get(i)), BigInteger.valueOf(p));
                accept &= iBI.intValue() != 1;
            }
            if (accept) return BigInteger.valueOf(result);
        }
        return BigInteger.valueOf((-1));
    }

    public void start() {
        Thread mainThr = new Thread(() -> {
            running = true;
            startReceive();
        }, "Main");
        mainThr.start();
    }

    private void startReceive() {
        Thread receiver = new Thread(() -> {
            while (running) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // TODO write method to process the data
            }
        }, "Receive");
        receiver.start();
    }



}
