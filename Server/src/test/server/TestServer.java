package server;
import static org.junit.Assert.*;



public class TestServer {

	Server server = new Server(8000);

	/*
	 * Testing CesearEcnrypt method
	 */
	
	//Positive -> return true if result matches expected value
	@org.junit.Test
	public void TestCesearCryptPositive() {
        String result = server.CaesarEncrypt("abc",4);
        assertEquals("efg", result);
        result = server.CaesarEncrypt("aa bb cc",2);
        assertEquals("cc\"dd\"ee", result);
	}
	
	//Negative -> return true if result doesn't match expected value
	@org.junit.Test
	public void TestCesearCryptNegative() {
        String result = server.CaesarEncrypt("abc",1);
        assertFalse(result.equals("cde"));
        result = server.CaesarEncrypt("aa bb",2);
        assertFalse(result.equals("aa bb"));
	}
	
	/*
	 * Testing CesearDecrypt method
	 */
	//Positive -> return true if result matches expected value
	@org.junit.Test
	public void TestCesearDecryptPositive() {
        String result = server.CaesarDecrypt("ccc",2);
        assertEquals("aaa", result);
	}
	
	//Negative -> return true if result doesn't match expected value
	@org.junit.Test
	public void TestCesearDecryptNegative() {
        String result = server.CaesarDecrypt("cde",1);
        assertFalse(result.equals("bcc"));
	}
	
	
	
	
}
	

