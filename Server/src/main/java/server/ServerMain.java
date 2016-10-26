package server;

public class ServerMain {
    public static void main(String[] args) {
        if (args.length == 1) {
                Server serverInstance = new Server(Integer.parseInt(args[0]));
                serverInstance.start();
            } else {
                System.out.println("Nieprawidlowe dane. Podaj prawidlowe parametry dla wartoœci [port] )");
            }
           }
    
    
}
