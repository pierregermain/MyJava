
public class JavaTest {

	public static void main(String[] args) {
		
		
	    try  {
	    	System.out.println("START");
	        org.apache.commons.net.ftp.FTPSClient FTPs =
	                new org.apache.commons.net.ftp.FTPSClient(false);
	        FTPs.connect("127.0.0.1");
	        FTPs.getReplyCode();
	        FTPs.execPBSZ(0);
	        FTPs.execPROT("P");
	        FTPs.login("user","password");//user,password
	        java.io.FileInputStream fileStream = new java.io.FileInputStream("XXXX.txt");
	        FTPs.enterLocalPassiveMode();
	        FTPs.storeFile("JavaTest.java", fileStream);
	        fileStream.close();
	        FTPs.disconnect();
	    	System.out.println("END");
	    } catch(Exception e) {
	        System.out.println("FTP FAILED, ERROR: " + e.getMessage());
	    }
	    
	    
	}
}
