import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.util.Vector;
import java.io.*;
import java.net.UnknownHostException;

/**
 * This is a simple wrapper around the Jakarta Commons FTP
 * library. It has just been added a few convenience methods to the
 * class to suit the ftp requirements.
 * 
 */

public class FtpClientService extends FTPClient {
	
	
	/**
	 * A convenience method for connecting and logging in
	 * @param host The host name.
	 * @param userName The user name to log in.
	 * @param password The password to log ing.
	 * @return boolean Return true if successfully connected and false in any other case.
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws FTPConnectionClosedException
	 */
	public boolean connectAndLogin (String host, String userName, String password)
			throws  IOException, UnknownHostException, FTPConnectionClosedException {
		
		boolean success = false;
		connect(host);
		int reply = getReplyCode();
		if (FTPReply.isPositiveCompletion(reply))
			success = login(userName, password);
		if (!success)
			disconnect();
		return success;
	}
	
	
	/**
	 * Turn passive transfer mode on or off. 
	 * If Passive mode is active, a PASV command will be issued 
	 * and interpreted before data transfers;
	 * otherwise, a PORT command will be used for data transfers. 
	 * If you're unsure which one to use, you probably want Passive mode to be on.
	 * @param setPassive Flag indicating whether to use the PASV mode or PORT command.
	 */
	public void setPassiveMode(boolean setPassive) {

		if (setPassive) {
			enterLocalPassiveMode();
		} else {
			enterLocalActiveMode();
		}
	}
	
	/**
	 * Use ASCII mode for file transfers.
	 */
	public boolean ascii () throws IOException {
		
		return setFileType(FTP.ASCII_FILE_TYPE);
	}
	
	/**
	 * Use Binary mode for file transfers.
	 */
	public boolean binary () throws IOException {
		
		return setFileType(FTP.BINARY_FILE_TYPE);
	}
	
	/**
	 * Download a file from the server, and save it to the specified local file.
	 * @param serverFile The server file name.
	 * @param localFile The absolute filepath of the local file where the server file will be downloaded.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public boolean downloadFile (String serverFile, String localFile)
			throws IOException, FTPConnectionClosedException {
		
		FileOutputStream out = new FileOutputStream(localFile);
		boolean result = retrieveFile(serverFile, out);
		out.close();
		return result;
	}
		
	
	/**
	 * Upload a file to the server. 
	 * @param localFile The absolute filepath of the local file to be uploaded.
	 * @param serverFile The server file name.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public boolean uploadFile (String localFile, String serverFile) 
			throws IOException, FTPConnectionClosedException {

		FileInputStream in = new FileInputStream(localFile);
		boolean result = storeFile(serverFile, in);
		in.close();
		return result;
	}

	/**
	 * Get the list of files in the current directory as a Vector of Strings 
	 * (excludes subdirectories).
	 * @return Vector List of the files in the current directory.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public Vector listFileNames () 
			throws IOException, FTPConnectionClosedException {
		
		FTPFile[] files = listFiles();
		Vector v = new Vector();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory())
				v.addElement(files[i].getName());
		}
		return v;
	}
	
	/**
	 * Get the list of files in the current directory as a Vector of Strings 
	 * (excludes subdirectories).
	 * @return Vector List of the files in the current directory.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public Vector listFileNames (String pathname) 
			throws IOException, FTPConnectionClosedException {
		
		FTPFile[] files = listFiles(pathname);
		Vector v = new Vector();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory())
				v.addElement(files[i].getName());
		}
		return v;
	}
	
	/**
	 * Get the list of files in the current directory as a single String,
	 * delimited by \n (char '10') (excludes subdirectories).
	 * @return String List of the files in the current directory.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public String listFileNamesString () 
			throws IOException, FTPConnectionClosedException {
		
		return vectorToString(listFileNames(), "\n");
	}
	
	/**
	 * Get the list of subdirectories in the current directory as a Vector of Strings 
	 * (excludes files).
	 * @return Vector The list of subdirectories in the current directory.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public Vector listSubdirNames () 
			throws IOException, FTPConnectionClosedException {
		
		FTPFile[] files = listFiles();
		Vector v = new Vector();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				v.addElement(files[i].getName());
		}
		return v;
	}
	
	/**
	 * Get the list of subdirectories in the current directory as a single String,
	 * delimited by \n (char '10') (excludes files)
	 * @return String The list of subdirectories in the current directory.
	 * @throws IOException
	 * @throws FTPConnectionClosedException
	 */
	public String listSubdirNamesString () 
			throws IOException, FTPConnectionClosedException {
		
		return vectorToString(listSubdirNames(), "\n");
	}
	
	/**
	 * Convert a Vector to a delimited String.
	 * @param v The vector to be converted.
	 * @param delim The delimiter used to separate the vector items.
	 * @return String A string with all the given vector items separated by a given delimiter. 
	 */
	private String vectorToString (Vector v, String delim) {

		StringBuffer sb = new StringBuffer();
		String s = "";
		for (int i = 0; i < v.size(); i++) {
			sb.append(s).append((String)v.elementAt(i));
			s = delim;
		}
		return sb.toString();
	}
		
}
