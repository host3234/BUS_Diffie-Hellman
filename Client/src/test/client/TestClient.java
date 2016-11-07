package client;
import static org.junit.Assert.*;



public class TestClient {

	Client client = new Client("Client","localhost",8080, true);

	/*
	 * Testing CesearEcnrypt method
	 */
	
	//Positive -> return true if result matches expected value
			
	@org.junit.Test
	public void TestCesearCryptPositive() {
        String result = client.CaesarEncrypt("abc",4);
        assertEquals("efg", result);
        result = client.CaesarEncrypt("aa bb cc",2);
        assertEquals("cc\"dd\"ee", result);
	}
	
	//Negative -> return true if result doesn't match expected value
	@org.junit.Test
	public void TestCesearCryptNegative() {
        String result = client.CaesarEncrypt("abc",1);
        assertFalse(result.equals("cde"));
        result = client.CaesarEncrypt("aa bb",2);
        assertFalse(result.equals("aa bb"));
	}
	
	/*
	 * Testing CesearDecrypt method
	 */
	//Positive -> return true if result matches expected value
	@org.junit.Test
	public void TestCesearDecryptPositive() {
        String result = client.CaesarDecrypt("ccc",2);
        assertEquals("aaa", result);
	}
	
	//Negative -> return true if result doesn't match expected value
	@org.junit.Test
	public void TestCesearDecryptNegative() {
        String result = client.CaesarDecrypt("cde",1);
        assertFalse(result.equals("bcc"));
	}
	
	
	
	
}
	

