package bouncyCastleExamples;

import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;

import java.io.*;
import java.security.*;
import java.util.Iterator;

import org.bouncycastle.bcpg.*;
import org.bouncycastle.jce.provider.*;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.examples.KeyBasedFileProcessor;
import org.bouncycastle.openpgp.examples.KeyBasedLargeFileProcessor;

import blogMrjaredpowell.KeyReader;

/**
 * Basic class to use PGP. 
 * This class is done using the PGP examples at BouncyCastle and 
 * http://cephas.net/blog/2004/04/01/pgp-encryption-using-bouncy-castle/
 */
public class SimplePGPTest
{
    public static void main(String[] args) throws Exception
    {

    	// --------------------------------------------------
    	// ENCRIPTAR
    	// --------------------------------------------------

    	//Ficheros y carpetas
    	String folder = "Meter aqui la ruta donde estén los ficheros";
    	String publicKey = "Nombre del fichero public key.asc";
    	String privateKey = "Private Key(Fingerprint).asc";
    	String ficheroAEncriptar = "FICHERO_A_ENCRIPTAR.TXT";
    	String ficheroEncriptado = "FICHERO_ENCRIPTADO.TXT";
    	String ficheroDesencriptado = "FICHERO_DESENCRIPTADO.TXT";
    	String msgParaSerEncriptado = "EsToooo M3nsaJèèéü a ENCRIPTAR c0N caract333rès êspeciales!!!&%";
    	String pass = "1chh31ss3p13rr3";
    	
    	// Public Key
    	String publicKeyFilePath = folder + publicKey;
    	
    	// Provider
    	Security.addProvider(new BouncyCastleProvider());
    	
    	//File to hold the message that I want to encrypt:
    	File outputfile = new File(folder + ficheroAEncriptar);
    	FileWriter writer = new FileWriter(outputfile);
    	writer.write(msgParaSerEncriptado.toCharArray());
    	writer.close();

    	//Read the public keyring file into a FileInputStream and then call the readPublicKey() method 
    	//that was provided for us by the KeyBasedFileProcessor:

    	FileInputStream in = new FileInputStream(publicKeyFilePath);
    	PGPPublicKey key = readPublicKey(in);

    	//At this point it’s important to note that the PGPPublicKeyRing class 
    	//(at least in the version I was using) appears to have a bug where it only recognizes the first key in the keyring. 
    	//If you use the getUserIds() method of the object returned you’ll only see one key.
    	
    	//This could cause you problems if you have multiple keys in your keyring and if the first key is not an RSA or El Gamal key. 	
    	
    	for (java.util.Iterator iterator = key.getUserIDs(); iterator.hasNext();) {
    		System.out.println((String)iterator.next());
    		}
    	
    	//Finally, create an armored ASCII text file and call the encryptFile() method 
    	//(again provided us by the KeyBasedFileProcessor example:
    	
    	// Scott 17.1.2006
    	OutputStream destino = new FileOutputStream(folder+ficheroEncriptado);
    	//OutputStream destino = new FileOutputStream(outputfile.getAbsolutePath() + "ENCRIPTADO_ESTO.TXT");
    	boolean ASCII_ARMOR = false;
    	boolean WITH_INTEGRITY_CHECK = true;
    	encryptFile(destino,folder+ficheroAEncriptar,key, ASCII_ARMOR, WITH_INTEGRITY_CHECK);
    	
    	System.out.println("TERMINADO ENCRIPTAR");
    	
    	// --------------------------------------------------
    	// DESENCRIPTAR
    	// --------------------------------------------------
    	
    	// fichero a ser desencriptado
    	File f1 = new File(folder+ficheroEncriptado);
    	InputStream isToDecrypt = new FileInputStream(f1);
    	
    	//private key a usar
    	File f2 = new File(folder+privateKey);
    	InputStream isPrivateKey = new FileInputStream(f2);
    	
    	// Password
    	char[] pwd = pass.toCharArray();
    	
    	decryptFile(isToDecrypt,isPrivateKey,pwd,folder+ficheroDesencriptado);

    	System.out.println("TERMINADO DESENCRIPTAR");

    	
    }
    
    
    private static void encryptFile(OutputStream out, String fileName,
            PGPPublicKey encKey, boolean armor,
            boolean withIntegrityCheck) throws IOException,
            NoSuchProviderException {
        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
                    PGPCompressedData.ZIP);

            PGPUtil.writeFileToLiteralData(comData.open(bOut),
                    PGPLiteralData.BINARY, new File(fileName));

            comData.close();

            PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
                    PGPEncryptedData.CAST5, withIntegrityCheck,
                    new SecureRandom(), "BC");

            cPk.addMethod(encKey);

            byte[] bytes = bOut.toByteArray();

            OutputStream cOut = cPk.open(out, bytes.length);

            cOut.write(bytes);

            cOut.close();

            out.close();
        } catch (PGPException e) {
            System.err.println(e);
            if (e.getUnderlyingException() != null) {
                e.getUnderlyingException().printStackTrace();
            }
        }
    }

    
	   /**
	    * Decrpypts files with a private key. 
	    * @param in InputStream containing file to decrypt
	    * @param keyIn InputStream containing the private keyring
	    * @param passwd passphrase needed to decode the keyring
	    * @throws Exception
	    */
	   public static void decryptFile(InputStream in, InputStream keyIn, char[] passwd, String rutaDestino) throws Exception {
	       in = PGPUtil.getDecoderStream(in);
	       PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
	       try {
	           PGPObjectFactory pgpF = new PGPObjectFactory(in);
	           PGPEncryptedDataList enc;
	           Object o = pgpF.nextObject();
	           if (o instanceof  PGPEncryptedDataList) {
	               enc = (PGPEncryptedDataList) o;
	           } else {
	               enc = (PGPEncryptedDataList) pgpF.nextObject();
	           }
	           System.out.println(enc.size() + " enc size.");
	           

	           Iterator it = enc.getEncryptedDataObjects();
	           PGPPrivateKey sKey = null;
	           PGPPublicKeyEncryptedData pbe = null;
	   
	           while (sKey == null && it.hasNext()) {
	               pbe = (PGPPublicKeyEncryptedData) it.next();
	               sKey = KeyReader.findSecretKey(pgpSec, pbe.getKeyID(), passwd);
	           }
	   
	           if (sKey == null) {
	               throw new IllegalArgumentException("Failed to find private key with ID " + pbe.getKeyID());
	           }
	   
	           InputStream clear = pbe.getDataStream(sKey, "BC");
	   
	           PGPObjectFactory plainFact = new PGPObjectFactory(clear);
	   
	           PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();
	   
	           InputStream compressedStream = new BufferedInputStream(cData.getDataStream());
	           PGPObjectFactory pgpFact = new PGPObjectFactory(compressedStream);
	   
	           Object message = pgpFact.nextObject();
	   
	           if (message instanceof  PGPLiteralData) {
	               PGPLiteralData ld = (PGPLiteralData) message;
	   
	               FileOutputStream fOut = new FileOutputStream(rutaDestino);
	               BufferedOutputStream bOut = new BufferedOutputStream(fOut);
	   
	               InputStream unc = ld.getInputStream();
	               int ch;
	   
	               while ((ch = unc.read()) >= 0) {
	                   bOut.write(ch);
	               }
	   
	               bOut.close();
	           } else if (message instanceof  PGPOnePassSignatureList) {
	               throw new PGPException("encrypted message contains a signed message - not literal data.");
	           } else {
	               throw new PGPException("message is not a simple encrypted file - type unknown.");
	           }
	   
	           if (pbe.isIntegrityProtected()) {
	               if (!pbe.verify()) {
	                   System.err.println("message failed integrity check");
	               } else {
	                   System.err.println("message integrity check passed");
	               }
	           } else {
	               System.err.println("no message integrity check");
	           }
	       } catch (PGPException e) {
	           System.err.println(e);
	           if (e.getUnderlyingException() != null) {
	               e.getUnderlyingException().printStackTrace();
	           }
	       }
	   }

    /**
     * Load a secret key ring collection from keyIn and find the secret key corresponding to
     * keyID if it exists.
     * 
     * @param keyIn input stream representing a key ring collection.
     * @param keyID keyID we want.
     * @param pass passphrase to decrypt secret key with.
     * @return
     * @throws IOException
     * @throws PGPException
     * @throws NoSuchProviderException
     */
    private static PGPPrivateKey findSecretKey(InputStream keyIn,
            long keyID, char[] pass) throws IOException, PGPException,
            NoSuchProviderException {
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(keyIn));

        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null) {
            return null;
        }

        return pgpSecKey.extractPrivateKey(pass, "BC");
    }

    
    /**
     * A simple routine that opens a key ring file and loads the first available key suitable for
     * encryption.
     * 
     * @param in
     * @return
     * @throws IOException
     * @throws PGPException
     */
    private static PGPPublicKey readPublicKey(InputStream in)
            throws IOException, PGPException {
        in = PGPUtil.getDecoderStream(in);

        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                in);

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //
        PGPPublicKey key = null;

        //
        // iterate through the key rings.
        //
        Iterator rIt = pgpPub.getKeyRings();

        while (key == null && rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();
            boolean encryptionKeyFound = false;

            while (key == null && kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();

                if (k.isEncryptionKey()) {
                    key = k;
                }
            }
        }

        if (key == null) {
            throw new IllegalArgumentException(
                    "Can't find encryption key in key ring.");
        }

        return key;
    }

    
}

