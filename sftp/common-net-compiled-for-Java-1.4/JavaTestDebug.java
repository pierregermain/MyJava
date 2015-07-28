// Net Commons FTPS: http://commons.apache.org/net/api/org/apache/commons/net/ftp/FTPClient.html

class JavaTestDebug {
    public static void main(String[] args) {
    	System.out.println("START");
	System.setProperty("javax.net.debug", "ssl,handshake,keymanager,trustmanager"); // Set verbosity. For some reason, sslctx kills most all output.
	/*	DEBUG TRACING:
		FTPS debug can be set on command line for Java: "-Djavax.net.debug=ssl,handshake,keymanager,trustmanager", in \jboss\server\Printellect\conf\wrapper.conf or \jboss\bin\run_Printellect.bat,
		or in code before FTPS is used (runtime changes did not seem to apply after org.apache.commons.net.ftp.FTPSClient was used once).
		sys.setProperty("javax.net.debug", "ssl,handshake,keymanager,trustmanager");
		sys.setProperty("javax.net.debug", "all");
			ssl - ssl stuff, with refinements (use ",", ":", ";" [maybe others] as delimiter):
				handshake - 
				sslctx - Print information about the SSL context (seems to kill all output)
				sessioncache - Print information about the SSL session cache.
				keymanager - Print information about calls to the key manager.
				trustmanager - Print information about calls to the trust manager.
		see: http://onjava.com/pub/a/onjava/excerpt/java_security_ch1/index.html?page=5
	*/
			
	/*	KEYS/CERTS - Passive vs Active:
		Passive FTP: does not seem to require any keys or certifcations (it makes its own tmp key)
		Active FTP:
		   You are required to make your own key and add it to the keystore.
		   If the FTP server your are hitting provides a public key certified by a well known company like verisign, the default truststore cacerts will likely work.
		   However, if you the server has a self signed cert, you need to put its public key in your truststore or the data channel will fail with invalid certification chain.
		 Note: My testing indicated you can't change the stores once the apache commons SSL code is initiialized.
		 System.setProperty("javax.net.ssl.trustStore", "X:\\JavaTest\\jdk\\jre\\lib\\security\\cacerts"); // Active FTP: This file is the default trust "cert chain" store
		 System.setProperty("javax.net.ssl.trustStorePassword", "changeit"); // Active FTP: This is the detault password for the deftaul keystore.
		 System.setProperty("javax.net.ssl.keyStore", "extras\KeyStore_MyPrivKey.jks"); // Active FTP: my private key so I can encipher the data channel. There is no "default" file.
		 System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
	*/

	// DEBUG: Confirm you environment.
	System.out.println("javax.net.debug = " + System.getProperty("javax.net.debug"));
	System.out.println("java.home = " + System.getProperty("java.home")); // Used to search for truststore. java-home and jre-home are synonyms.
	System.out.println("user.home = " + System.getProperty("user.home")); // Used to search for keystore. http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4787931
	System.out.println("javax.net.ssl.trustStore = '" + System.getProperty("javax.net.ssl.trustStore") + "' else '" + System.getProperty("java.home") +  "\\lib\\security\\jssecacerts' else '" + System.getProperty("java.home") + "\\lib\\security\\cacerts'"); // Defaults empty. On failure to find it falls back to <java-home>/lib/security/jssecacerts, and if that fails then <java-home>/lib/security/cacerts (java-home and jre-home are synonyms) 
	System.out.println("javax.net.ssl.trustStorePassword = " + System.getProperty("javax.net.ssl.trustStorePassword")); // <java-home>/lib/security/cacerts defailts to "changeit"
	System.out.println("javax.net.ssl.keyStore = '" + System.getProperty("javax.net.ssl.keyStore")  + "' else '" + System.getProperty("user.home") +  "\\.keystore'"); // Defaults empty, On failure to find falls back to <user.home>/.keystore.
	System.out.println("javax.net.ssl.keyStorePassword = " + System.getProperty("javax.net.ssl.keyStorePassword"));

	// DEBUG: What libraries are going to be used? They better all be compatible with org/apache/commons/net v2.0.
	System.out.println("java.library.path = " + System.getProperty("java.library.path"));
	System.out.println("java.class.path = " + System.getProperty("java.class.path"));

	System.out.println("FTPSClient.class is " + Thread.currentThread().getContextClassLoader().getResource("org/apache/commons/net/ftp/FTPSClient.class"));
	System.out.println("FTPClient.class is " + Thread.currentThread().getContextClassLoader().getResource("org/apache/commons/net/ftp/FTPClient.class"));
	System.out.println("FTP.class is " + Thread.currentThread().getContextClassLoader().getResource("org/apache/commons/net/ftp/FTP.class"));
	System.out.println("SocketClient.class is " + Thread.currentThread().getContextClassLoader().getResource("org/apache/commons/net/SocketClient.class"));
	System.out.println("KeyStore.class is " + Thread.currentThread().getContextClassLoader().getResource("java/security/KeyStore.class"));
	System.out.println("KeyManagerFactory.class is " + Thread.currentThread().getContextClassLoader().getResource("javax/net/ssl/KeyManagerFactory.class"));
	System.out.println("TrustManagerFactory.class is " + Thread.currentThread().getContextClassLoader().getResource("javax/net/ssl/TrustManagerFactory.class"));
	System.out.println("SSLContext.class is " + Thread.currentThread().getContextClassLoader().getResource("javax/net/ssl/SSLContext.class"));
	System.out.println("SSLSocketFactory.class is " + Thread.currentThread().getContextClassLoader().getResource("javax/net/ssl/SSLSocketFactory.class"));
	System.out.println("SSLServerSocketImpl.class is " + Thread.currentThread().getContextClassLoader().getResource("com/sun/net/ssl/internal/ssl/SSLServerSocketImpl.class"));

	// Do the FTPS stuff:
	java.io.ByteArrayOutputStream Log = new java.io.ByteArrayOutputStream();
	java.io.PrintStream printLog = new java.io.PrintStream(Log);
	java.io.PrintWriter LogPrinter = new java.io.PrintWriter(Log);
	try  {
		org.apache.commons.net.ftp.FTPSClient FTPs = new org.apache.commons.net.ftp.FTPSClient(false);
		FTPs.addProtocolCommandListener(new org.apache.commons.net.PrintCommandListener(LogPrinter));
		FTPs.setDefaultTimeout(10000);
		final String ip = "127.0.0.1";
		FTPs.connect(ip); // ftp://ftpstest.forus.com 100MB, bandwidth limited, no MkDir, supports FTP Active, and FTPS Active & Passive. Please delete files
		// FTPs.connect("www.secureftp-test.com"); // https://www.secureftp-test.com (cert expired/invalid). For testing, capture in IE and put in your testing truststore.
		FTPs.setSoTimeout(900000); // 15 minutes, a massive file transfer.
		FTPs.getReplyCode();
		FTPs.execPBSZ(0); // RFC2228 requires that the PBSZ subcommand be issued prior to the PROT subcommand. However, TLS/SSL handles blocking of data, so '0' is the only value accepted.
		FTPs.execPROT("P"); // P(rivate) data channel, which needs certs if "Active". E and S: '536 Requested PROT level not supported by mechanism.'. C is default, but has clear text data channel - http://www.nabble.com/TLS-for-FTP-td6645485.html
		final String user = "user";
		final String pass = "pwd";
		FTPs.login(user,pass);
		FTPs.changeWorkingDirectory("/");
		final String localPath = "Ruta al local.txt";
		java.io.FileInputStream fileStream = new java.io.FileInputStream(localPath);
		FTPs.setDataTimeout(5000);
		FTPs.enterLocalPassiveMode(); // Active is the default, which very few clients can suppart in SSL (firewalls can't detect "PORT" command, and thus cant open/map local port). Active will also require keys/certs.
		printLog.println("(call store file...)");
		FTPs.storeFile("test.txt", fileStream);
		fileStream.close();
		FTPs.disconnect();
		System.out.println("");
		System.out.println("FTP COMMAND LOG:");
		System.out.println(Log.toString());
	} catch(Exception e) {
		System.out.println("");
		System.out.println("FTP COMMAND LOG:");
		System.out.println(Log.toString());
		System.out.println("");
		System.out.println("FTP FAILED, ERROR: " + e.getMessage());
	}
	System.out.println("END");
    }
}
