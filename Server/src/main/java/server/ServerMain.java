 package server;

public class ServerMain {
    public static void main(String[] args) {
    	System.out.println();
    	System.out.println("Server");
        if (args.length == 1) 
        {       	
                Server serverInstance = new Server(Integer.parseInt(args[0]));
                serverInstance.start();
        }
        else 
        {
            System.out.println("Nieprawidlowe dane.Podaj poprawne parametry port oraz (opcjonalnie) 'szyfruj' ");
            System.exit(0);

        }
           }
   
    }
   
