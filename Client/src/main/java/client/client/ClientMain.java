package client;
 
 
public class ClientMain {

    public static void main(String[] args) {

    	boolean enc =false;

    if (args.length == 4) {
    	if (args[3].equals("encrypt")) {
        System.out.println("Szyfrowanie : TAK");
        enc = true;
    	}
    	else {
    	System.out.println("Podano niepoprawne parametry. Podaj prawidlowe parametry: [username] [ip_serwera] [port_serwera] (opcjonalnie) Szyfrowanie[wpisz szyfruj]");  
        System.exit(0);
    }}    	
    else if (args.length == 3){
        System.out.println("Szyfrowanie : NIE");
        }
    else if (args.length <= 2){
    	System.out.println("Brakuje wszystkich parametrow. Podaj prawidlowe parametry: [username] [ip_serwera] [port_serwera] (opcjonalnie) Szyfrowanie[wpisz enc]");    	 
    }
    Client client = new Client(args[0], args[1], Integer.parseInt(args[2]), enc);
    	if (!client.openConnection()) {
            System.out.println("Nie udalo sie nawiazac polaczenia z serwerem!");
        } else {
            client.start();
        }
    }
}