package server;

import java.net.InetAddress;

public class Client {

    private String name;
    private InetAddress address;
    private int port;

    public String getName() {
        return name;
    }
    public InetAddress getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }

    public Client(String name, InetAddress address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String toString() {
        return "Uzytkownik polaczyl sie z" + address.toString() + ":" + port + ". Imie : " + name + ".";
    }

}

