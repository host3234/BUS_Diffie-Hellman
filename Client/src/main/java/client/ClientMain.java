package client;
public class ClientMain {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Brakuje wszystkich paraemtrów. Podaj prawidlowe parametry: [username] [ip_serwera] [port_serwera]");
            System.exit(0);
        }
        Client client = new Client(args[0], args[1], Integer.parseInt(args[2]));
        if (!client.openConnection()) {
            System.out.println("Nie udalo sie nawiazac polaczenia z serwerem!");
        } else {
            client.start();
        }
    }
}
