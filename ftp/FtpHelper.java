import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import FtpClient;

/**
 * Helper class for FTP purposes.
 * It uses the <b>FtpClient</b> utility class for uploading and
 * downloading files.
 * 
 */
public class FtpHelper {

	// Message constants 
    private static final MessageFormat _ATTEMPTS_EXHAUSTED_ERROR_MSG 
          = new MessageFormat("Intentos Agotados.");
    private static final MessageFormat _FILE_SENT_SUCCESSFULLY_MSG 
          = new MessageFormat("El fichero {0} ha sido subido correctamente.");
    private static final MessageFormat _FILE_DOWNLOAD_SUCCESSFULLY_MSG 
          = new MessageFormat("El fichero {0} ha sido descargado correctamente.");
    private static final MessageFormat _FILE_DOWNLOAD_FAILURE_MSG 
          = new MessageFormat("Ha ocurrido un error mientras se descargaba el fichero {0}.");
    private static final MessageFormat _DIRECTORY_NOT_EXISTING_MSG 
          = new MessageFormat("El directorio {0} no existe.");

    // Class parameters
	private String ftpAddress;
	private String userName;
	private String password;
	private String localPath;
	private String remotePath;	
	
	// Timeout values
	// Timeout que se usa para conectarse. Esto también afecta a setSoTimeout que se llamaría una vez conectado.
	private final int defaultTimeout = 60000; // 60s
	//Timeout para la conexión abierta
	private final int soTimeout = 360000; //360s
	// Timeout para read y writes.
	private final int dataTimeout = 300000; //300s
	
	// Ftp Handler
	FtpClientService ftpClient = null;

	/**
	 * Public constructor.
	 * @param ftpAddress IP Address of remote server
	 * @param userName	User name
	 * @param password	User password
	 * @param localPath	Path where are the files to be sent
	 * @param remotePath	Path in the server to put the files
	 */
	public FtpHelper(String ftpAddress, String userName, String password, String localPath, String remotePath) {
		this.ftpAddress = ftpAddress;
		this.userName = userName;		
		this.password = password;
		this.localPath = localPath;
		this.remotePath = remotePath;		
	}
	
	/*********************************************************************************************
	 *  
	 *                                      Independent Methods
	 *                   (You don't need to use the login and disconnect methods to use these) 
	 * 
	 * 
	 *********************************************************************************************/

	/*********************************************************************************************
	 * 
	 *                                      Upload methods
	 * 
	 *********************************************************************************************/
				
		
	
	/**
	 * Sends a single file via FTP.
	 * 
	 * @param file	The file to upload.
	 * @param maxRetries	maximum retries.
	 * @param processId	the process id.
	 * @param className	the class name.
	 * @throws DefineSomeException	If any error is produced.
	 */
	public long uploadFile(File file, int maxRetries, int processId, String className)
		throws DefineSomeException {

		long numFilesUploaded = 0;
		List fileList = new ArrayList();
		
		fileList.add(file);
		numFilesUploaded = uploadFileList(fileList, maxRetries, processId, className);

		return numFilesUploaded;
	}
	
	/**
	 * Convenience method for uploading a list of files
	 * @param fileList
	 * @param maxRetries
	 * @param processId
	 * @param className
	 * @return
	 * @throws DefineSomeException if any error is produced
	 */
	public long uploadFileList(List fileList, int maxRetries, int processId, String className)
			throws DefineSomeException {

		long numFilesUploaded = 0;
		FtpClientService ftp = new FtpClientService();
		Iterator ite = fileList.iterator();
		File fileSend = null;
		int dotPos = 0;
		String fileExtension = null;
		int retryCount = 1;
		boolean sendingPerformed = false;

		try {

			ftp.setDefaultTimeout(defaultTimeout);
			
			if (ftp.connectAndLogin(ftpAddress, userName, password)) {

				ftp.setSoTimeout(soTimeout);
				ftp.setDataTimeout(dataTimeout);
				
				// Set PASV mode.
				ftp.setPassiveMode(true);

				// Iterate on the files to be sent to the server.
				while (ite.hasNext() == true) {

					fileSend = (File) ite.next();
					
					// Set the retries parameters.
					retryCount = 1;
					sendingPerformed = false;

					// Set the transfer mode according to the file extension.
					String fileName = fileSend.getName();
					dotPos = fileName.lastIndexOf(".");
					fileExtension = fileName.substring(dotPos + 1);

					// Here you can add binary files (like ZIP)
					if ("RAR".equalsIgnoreCase(fileExtension)) {
						ftp.binary();
					} else { // WL, GL, BL and CEI Files goes in ASCII mode.
						ftp.ascii();

					}

					String serverFile = "";
					String localFile = "";
					
					// Change the work directory in the server side.
					if (ftp.changeWorkingDirectory(remotePath)){
					// Upload the file (retrial policy by maximum number of send attempts).
						serverFile = fileName;
						
						localFile = fileSend.getAbsolutePath();
						while (retryCount <= maxRetries && !sendingPerformed) {
							sendingPerformed = ftp.uploadFile(localFile, serverFile);
							// Increment the number of attemps only when the files are not correctly sent.
							if (!sendingPerformed) {
								retryCount ++;
							} else {
								// The sending has been performed correctly.
								Object[] logArgs = new Object[] {fileName};
								DefineSomeLoggerClass.insertError(processId,  _FILE_SENT_SUCCESSFULLY_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_INFORMATION, className);			
	
							}
						}
					}
					else{
						Object[] logArgs = new Object[] {remotePath};
						throw new DefineSomeException(_DIRECTORY_NOT_EXISTING_MSG.format(logArgs));
					}
					if (!sendingPerformed && retryCount > maxRetries) {

						Object[] logArgs = new Object[] {localFile};
						DefineSomeLoggerClass.insertError(processId,  _ATTEMPTS_EXHAUSTED_ERROR_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_CRITICAL, className);	
					} else {
						numFilesUploaded = numFilesUploaded + 1;
					}
				} // End of the while that iterates on the list of the files to upload.
			} else {
				throw new DefineSomeException("Unable to connect to " + ftpAddress);
			}
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		} catch (IOException ioe) {
			throw new DefineSomeException(ioe.getMessage());
		} finally {
			try {
				if (ftp.isConnected() == true) {
					ftp.logout();
					ftp.disconnect();
				}
			} catch (IOException ioe) {
				throw new DefineSomeException(ioe.getMessage());
			}
		}

		return numFilesUploaded;
	}
	

	/*********************************************************************************************
	 * 
	 * 
	 *                                      Download methods
	 * 
	 * 
	 *********************************************************************************************/
			
	
	
	
    /**
     * Returns the absolute local path of the downloaded file.
     * Downloads the given filename through FTP to the given local path.
     * @param remoteFileName The remote filename of the file to download.
     * @param processId The process identifier.
     * @param className The class name.
     * @return String The absolute local path of the downloaded file.
     * @throws DefineSomeException If any error is produced.
     */
	public String downloadFile(String remoteFileName, int processId, String className) throws DefineSomeException {
		
		String localFileName = null;
		FtpClientService ftp = new FtpClientService();
		int dotPos = 0;
		String fileExtension = null;
		boolean sendingPerformed = false;

		try {
			
			ftp.setDefaultTimeout(defaultTimeout);
			
			if (ftp.connectAndLogin(ftpAddress, userName, password)) {

				ftp.setSoTimeout(soTimeout);
				ftp.setDataTimeout(dataTimeout);	
				
				// Set PASV mode.
				ftp.setPassiveMode(true);

				dotPos = remoteFileName.lastIndexOf(".");
				fileExtension = remoteFileName.substring(dotPos + 1);

				// Here your can define Binary Files (Like ZIP)
				if ("RAR".equalsIgnoreCase(fileExtension)) {
					ftp.binary();
				} else { // Other file formats goes in ASCII mode.
					ftp.ascii();
				}

				// Change the work directory in the server side.
				ftp.changeWorkingDirectory(remotePath);
			
				// Downloads the file.
				localFileName = localPath + remoteFileName;
				
				sendingPerformed = ftp.downloadFile(remoteFileName, localFileName);
				// Increment the number of attempts only when the files are not correctly sent.
				if (!sendingPerformed) {
					Object[] logArgs = new Object[] {remoteFileName};
					DefineSomeLoggerClass.insertError(processId,  _FILE_DOWNLOAD_FAILURE_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_CRITICAL, className);	
				} else {
					// The sending has been performed correctly.
					Object[] logArgs = new Object[] {remoteFileName};
					DefineSomeLoggerClass.insertError(processId,  _FILE_DOWNLOAD_SUCCESSFULLY_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_INFORMATION, className);			
				}
			} else {
				throw new DefineSomeException("Unable to connect to " + ftpAddress);
			}
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		} catch (IOException ioe) {
			throw new DefineSomeException(ioe.getMessage());
		} finally {
			try {
				if (ftp.isConnected() == true) {
					ftp.logout();
					ftp.disconnect();
				}
			} catch (IOException ioe) {
				throw new DefineSomeException(ioe.getMessage());
			}
		}
		return localFileName;
	}
	
	
	
	/**
	 *
	 * Downloads the last modified file of a remote path to the local path in ASCII Mode.
	 * Also includes a Check that indicates if the remote paht has to be deleted
	 * Note: You need commons-net-1.4.1.jar to make this work.
	 * 
	 * @param deleteFile
	 * @return boolean	True if all went okey
	 * @throws DefineSomeException If any error is produced.
	 * 
	 */
	public boolean downloadLatestModifiedFile(boolean deleteFile)
			throws DefineSomeException {
		
		String lastModifiedFile = null;
				
			FtpClientService ftp = new FtpClientService();

			try {
				
				ftp.setDefaultTimeout(defaultTimeout);

				if (ftp.connectAndLogin(ftpAddress, userName, password)) {
					    
					ftp.setSoTimeout(soTimeout);
					ftp.setDataTimeout(dataTimeout);
					
					// List the files in the directory
					ftp.changeWorkingDirectory(remotePath);	
					FTPFile[] files = ftp.listFiles();
	
	
					//DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
					DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
					Date beginDate = dfm.parse("2000-01-01");
	
					// For every File in the directory
					for (int i = 0; i < files.length; i++) {
						if (!files[i].isDirectory()) {
		
							// If the modification date is the most recent we keep it
							// as the last modified file
							Date fileDate = files[i].getTimestamp().getTime();
							if (fileDate.compareTo(beginDate) >= 0) {
		
								// The File was modified more recently
								lastModifiedFile = files[i].getName();
								beginDate = files[i].getTimestamp().getTime();
							}
						}
					}
	
					// If we have found a file we download it
					if (!(lastModifiedFile == null)) {
						File file = new File(localPath + File.separator
								+ lastModifiedFile);
						FileOutputStream fos = new FileOutputStream(file);
						ftp.type(FTPClient.ASCII_FILE_TYPE);
						ftp.retrieveFile(lastModifiedFile, fos);
	
					    //Check that indicates if the remote file has to be deleted
						if (deleteFile){
							//We delete all the files, not only the last modified
							//so that we do not download twice the same file in the next cicle
							for (int i = 0; i < files.length; i++) {
								if (!files[i].isDirectory()) {
									ftp.deleteFile(files[i].getName());
								}
							}
						}
						fos.flush();	
						fos.close();
					}

				}
				 else {
						throw new DefineSomeException("Unable to connect to " + ftpAddress);
					}
			
			} catch (UnknownHostException e) {
				throw new DefineSomeException(e.getMessage());
			} catch (IOException ioe) {
				throw new DefineSomeException(ioe.getMessage());
			} catch (Exception e) {
					throw new DefineSomeException("Excepción: " + e.getMessage());
			} finally {
				try {

					if (ftp.isConnected() == true) {
						ftp.logout();
						ftp.disconnect();
					}
				} catch (IOException ioe) {
					throw new DefineSomeException(ioe.getMessage());
				}
			}

		if (lastModifiedFile == null) {
			return false;
		} else{
			return true;}

	}
	
	/*********************************************************************************************
	 * 
	 * 
	 *                                      Delete methods
	 * 
	 * 
	 *********************************************************************************************/
			
	/**
	 * Deletes a remote file given its name. 
	 * 
	 * @param remoteFilename The remote filename to delete.
	 * @param processId Invoking process identifier.
	 * @param className Invoking class name.
         * @return boolean Flag indicating if the file was deleted successfully or not.
	 * @throws DefineSomeException If any error is produced.
	 */
	public boolean deleteRemoteFile(String remoteFilename, int processId, String className)
		throws DefineSomeException {

		FtpClientService ftp = new FtpClientService();
		boolean wasDeleted = false;

		try {
			
			ftp.setDefaultTimeout(defaultTimeout);
			
			if (ftp.connectAndLogin(ftpAddress, userName, password)) {

				ftp.setSoTimeout(soTimeout);
				ftp.setDataTimeout(dataTimeout);
				
				// Set PASV mode.
				ftp.setPassiveMode(true);

				// Change the work directory in the server side.
				ftp.changeWorkingDirectory(remotePath);
			
				// Delete the remote file.
				wasDeleted = ftp.deleteFile(remoteFilename);
			} else {
				throw new DefineSomeException("Unable to connect to " + ftpAddress);
			}
		} catch (UnknownHostException e) {
			throw new DefineSomeException(processId, e.getMessage(), className);
		} catch (IOException ioe) {
			throw new DefineSomeException(processId, ioe.getMessage(), className);
		} finally {
			try {
				if (ftp.isConnected() == true) {
					ftp.logout();
					ftp.disconnect();
				}
			} catch (IOException ioe) {
				throw new DefineSomeException(processId, ioe.getMessage(), className);
			}
		}
		return wasDeleted;
	}


	
	/*********************************************************************************************
	 * 
	 * 
	 *                                Dependent Methods (all named as i*)
	 *                   (You need to use the login and disconnect methods to use these) 
	 * 
	 * 
	 *********************************************************************************************/
	
	/**
	 *
	 * Connects to the remote server
	 * 
	 * @return boolean
	 * @throws DefineSomeException If any error is produced.
	 */
	public boolean iConnectAndLogin()
			throws DefineSomeException {
				
		boolean connectionOK = false;
			
		ftpClient = new FtpClientService();

			try {
				
				ftpClient.setDefaultTimeout(defaultTimeout);
				
				if (ftpClient.connectAndLogin(ftpAddress, userName, password)) {
					
					ftpClient.setSoTimeout(soTimeout);
					ftpClient.setDataTimeout(dataTimeout);	
					
					connectionOK = true;
				}			 	
			} catch (UnknownHostException e) {
				throw new DefineSomeException(e.getMessage());
			} catch (IOException ioe) {
				throw new DefineSomeException("Excepción: " + ioe.getMessage());
			} catch (Exception e) {
					throw new DefineSomeException("Excepción: " + e.getMessage());
			}

		return connectionOK;
	}
	
		/**
		 *
		 * Disconnects from the remote server
		 * 
		 * @return boolean
		 * @throws DefineSomeException If any error is produced.
		 */
		public boolean iLogoutAndDisconnect()
				throws DefineSomeException {
		
			boolean disconnectOK = false;

				try {
					if (ftpClient.isConnected() == true) {
						ftpClient.logout();
						ftpClient.disconnect();
						disconnectOK = true;		
					}
				}catch (UnknownHostException e) {
					throw new DefineSomeException(e.getMessage());}
				catch (Exception e) {
					throw new DefineSomeException(e.getMessage());
				}
				
			return disconnectOK;
		}
	
		/**
		 *
		 * Reconnects to the remote server
		 * 
		 * @return boolean
		 * @throws DefineSomeException If any error is produced.
		 */
		public boolean iReconnectAndLogin()
				throws DefineSomeException {
					
			boolean connectionOK = false;
			
			try{
				if (!ftpClient.isConnected()){ // reconnect
						
					ftpClient.setDefaultTimeout(defaultTimeout);
				
					if (ftpClient.connectAndLogin(ftpAddress, userName, password)) {
					
						ftpClient.setSoTimeout(soTimeout);
						ftpClient.setDataTimeout(dataTimeout);	
					
						connectionOK = true;
					}			 	
				}
				else{ // already connected
					connectionOK = true;
				}
			} catch (UnknownHostException e) {
				throw new DefineSomeException(e.getMessage());
			} catch (IOException ioe) {
				throw new DefineSomeException("Excepción: " + ioe.getMessage());
			} catch (Exception e) {
				throw new DefineSomeException("Excepción: " + e.getMessage());
			}

			return connectionOK;
	}
		
	/**
	 * List Files on the remote server. 
	 * REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 *  
	 * @param processId Invoking process identifier.
	 * @param className Invoking class name.
         * @return boolean Flag indicating if the file exists or not.
	 * @throws DefineSomeException If any error is produced.
	 */
	public Vector iListFilesOfDirectory(int processId, String className)
		throws DefineSomeException {

		try {
			if (ftpClient.isConnected()) {

				// Set PASV mode.
				ftpClient.setPassiveMode(true);

				// Change the work directory in the server side.
				ftpClient.changeWorkingDirectory(remotePath);
			
				// List FileNames of the Directory
				return ftpClient.listFileNames();
						
			} 
			else {
				throw new DefineSomeException("Error en checkIfFileExists, No conectado en " + ftpAddress);
			}
		} catch (UnknownHostException e) {
			throw new DefineSomeException(processId, e.getMessage(), className);
		} catch (IOException ioe) {
			throw new DefineSomeException(processId, ioe.getMessage(), className);
		} 
	}
	
	/**
	 *
	 * Deletes the contents of the "remotePath" directory configured in the constructor
	 * REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 * 
	 * @return boolean	True si todo ha ido bien
	 * @throws DefineSomeException If any error is produced.
	 * 
	 */
	
	public boolean iDeleteDirectoryContents()
			throws DefineSomeException {		
				
		try {
			if (ftpClient.isConnected()) {
				
					// Set PASV mode.
					ftpClient.setPassiveMode(true);
	
					// Change the work directory in the server side.
					ftpClient.changeWorkingDirectory(remotePath);
					    
					// List the files in the directory
					ftpClient.changeWorkingDirectory(remotePath);	
					FTPFile[] files = ftpClient.listFiles();
	
					for (int i = 0; i < files.length; i++) {
						if (!files[i].isDirectory()) {
							ftpClient.deleteFile(files[i].getName());
						}
					}
					return true;
				}
			else {
				throw new DefineSomeException("Not connected to the server " + ftpAddress);
			}
			
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		} catch (IOException ioe) {
			throw new DefineSomeException(ioe.getMessage());
		} catch (Exception e) {
				throw new DefineSomeException("Excepción: " + e.getMessage());
		}
	}
	
	/**
	 *
	 * Deletes the files passed in the "remotePath" configured in the constructor
	 * REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 * 
	 * @return boolean	True si todo ha ido bien
	 * @throws DefineSomeException If any error is produced.
	 * 
	 */
	
	public boolean iDeleteFilesFromDirectory(Vector v)
			throws DefineSomeException {		
				
		try {
			if (ftpClient.isConnected()) {	
				
				// Set PASV mode.
				ftpClient.setPassiveMode(true);

				// Change the work directory in the server side.
				ftpClient.changeWorkingDirectory(remotePath);
				
					for (int i = 0; i < v.size(); i++) {
						String fileName = (String) v.get(i);
						ftpClient.deleteFile(fileName);							
					}
					return true;								
				}
			else {
				throw new DefineSomeException("No se ha podido borrar los ficheros ya que no se esta conectado al servidor FTP " + ftpAddress);
			}
			
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		}
		catch (Exception e) {
			throw new DefineSomeException("Excepción: " + e.getMessage());
		}
	}
	
	/**
	 *
	 * Deletes the files passed in the vector except the giving fileNotToBeDeleteded in the "remotePath" directory previously configured in the constructor
	 * @REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 * 
	 * @return boolean	True si todo ha ido bien
	 * @throws DefineSomeException If any error is produced.
	 * 
	 */
	
	public boolean iDeleteFilesFromDirectory(Vector v, String fileNotToBeDeleteted)
			throws DefineSomeException {		
				
		try {
			if (ftpClient.isConnected()) {
				
				// Set PASV mode.
				ftpClient.setPassiveMode(true);

				// Change the work directory in the server side.
				ftpClient.changeWorkingDirectory(remotePath);
				
				for (int i = 0; i < v.size(); i++) {
					String fileName = (String) v.get(i);
					if (!fileName.equalsIgnoreCase(fileNotToBeDeleteted)){
						ftpClient.deleteFile(fileName);	}						
				}
				return true;								
			}
			else {
				throw new DefineSomeException("No se ha podido borrar los ficheros ya que no se esta conectado al servidor FTP " + ftpAddress);
			}
			
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		}
		catch (Exception e) {
			throw new DefineSomeException("Excepción: " + e.getMessage());
		}
	}
	
	/**
	 * Deletes a remote file given its name. 
	 * 
	 * @REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 * 
	 * @param remoteFilename The remote filename to delete.
	 * @param processId Invoking process identifier.
	 * @param className Invoking class name.
         * @return boolean Flag indicating if the file was deleted successfully or not.
	 * @throws DefineSomeException If any error is produced.
	 */
	public boolean iDeleteRemoteFile(String remoteFilename, int processId, String className)
		throws DefineSomeException {

		boolean wasDeleted = false;

		try {
			if (ftpClient.isConnected()) {

				// Set PASV mode.
				ftpClient.setPassiveMode(true);

				// Change the work directory in the server side.
				ftpClient.changeWorkingDirectory(remotePath);
			
				// Delete the remote file.
				wasDeleted = ftpClient.deleteFile(remoteFilename);
			} else {
				throw new DefineSomeException("Imposible borrar fichero "+remoteFilename+", Not connected to " + ftpAddress);
			}
			
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		}
		catch (Exception e) {
			throw new DefineSomeException("Excepción: " + e.getMessage());
		}

		return wasDeleted;	
	}
		
	
    /**
	 * Downloads the given filename through FTP to the given local path.
	 * 
	 * @REQUISITES TO USE THIS METHOD: 
	 * (1) Use iConnectAndLogin() before using this method
	 * (2) Use iLogoutAndDisconnect() after using this method
	 * 
	 * @param remoteFileName The remote filename of the file to download.
	 * @param processId The process identifier.
	 * @param className The class name.
	 * @return String The absolute local path of the downloaded file.
     * @throws DefineSomeException If any error is produced.
     */
	public String iDownloadFile(String remoteFileName, int processId, String className) throws DefineSomeException {
		
		String localFileName = null;
		int dotPos = 0;
		String fileExtension = null;
		boolean sendingPerformed = false;

		try {
			if (ftpClient.isConnected()) {

				// Set PASV mode.
				ftpClient.setPassiveMode(true);

				dotPos = remoteFileName.lastIndexOf(".");
				fileExtension = remoteFileName.substring(dotPos + 1);

				// Here you can define Binary Files, like ZIP
				if ("RAR".equalsIgnoreCase(fileExtension)) {
					ftpClient.binary();
				} else { // Other file formats goes in ASCII mode.
					ftpClient.ascii();
				}

				// Change the work directory in the server side.
				ftpClient.changeWorkingDirectory(remotePath);
			
				// Downloads the file.
				localFileName = localPath + remoteFileName;
				
				sendingPerformed = ftpClient.downloadFile(remoteFileName, localFileName);
				// Increment the number of attempts only when the files are not correctly sent.
				if (!sendingPerformed) {
					Object[] logArgs = new Object[] {remoteFileName};
					DefineSomeLoggerClass.insertError(processId,  _FILE_DOWNLOAD_FAILURE_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_CRITICAL, className);	
				} else {
					// The sending has been performed correctly.
					Object[] logArgs = new Object[] {remoteFileName};
					DefineSomeLoggerClass.insertError(processId,  _FILE_DOWNLOAD_SUCCESSFULLY_MSG.format(logArgs), DefineSomeLoggerClass._ERROR_INFORMATION, className);			
				}
			} else {
				throw new DefineSomeException("Not connected to " + ftpAddress);
			}
		} catch (UnknownHostException e) {
			throw new DefineSomeException(e.getMessage());
		} catch (IOException ioe) {
			throw new DefineSomeException(ioe.getMessage());
		}
		return localFileName;
	}
	

}
