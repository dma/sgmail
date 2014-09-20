package com.subgraph.sgmail.ui.dialogs;

import com.subgraph.sgmail.autoconf.ServerInformation;
import com.subgraph.sgmail.autoconf.ServerInformation.Protocol;
import com.subgraph.sgmail.autoconf.ServerInformation.SocketType;
import com.sun.mail.util.MailConnectException;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.IProgressMonitor;

import javax.mail.*;

import java.util.Properties;
import java.util.logging.Logger;

public class IMAPServerConfigurationProbeTask implements IRunnableWithProgress {

	private final static Logger logger = Logger.getLogger(IMAPServerConfigurationProbeTask.class.getName());
		
	private final Properties sessionProperties;
	private final String server;
	private final String login;
	private final String password;
	private final boolean debug;

	private boolean isSuccess;
	private String errorMessage = "";

	private boolean isDone;
	private int port;
	private int startTLSport;
	
	private ServerInformation.Protocol protocol;

	private SocketType socketType = null;

	private boolean connectSuccess = false;
	
	private boolean autodetect;

	private boolean sslConnectSuccess;
	private int sslPort;

	private boolean startTLSConnectSuccess;

	IMAPServerConfigurationProbeTask(String hostname, String login, String password, boolean debug) {
		this.server = hostname;
		this.login = login;
		this.password = password;
	    this.debug = debug;
		this.sessionProperties = new Properties();
		this.autodetect = true;
	}
	
	IMAPServerConfigurationProbeTask(String hostname, String login, String password, int port, SocketType socketType, boolean debug) {
		this.server = hostname;
		this.socketType = socketType;
		this.autodetect = false;
		this.port = port;
		this.login = login;
		this.password = password;
	    this.debug = debug;
		this.sessionProperties = new Properties();
	}

	public boolean isSuccess() {
		return isSuccess;
	}
		
	public String getErrorMessage() {
		return errorMessage;
	}
	
		
	@Override
	public void run(IProgressMonitor monitor) {
		monitor.beginTask("Test login credentials", IProgressMonitor.UNKNOWN);
		
		Session session = Session.getInstance(sessionProperties);
	       if(debug) {
	           session.setDebug(true);
	       }
			
	    if (this.autodetect == false) {
	    	
	    	/* If we have a user-provided config, we try that. 
	    	 *  
	    	 */
		    
	    	if (socketType == SocketType.SSL) {
	    		set("mail.store.protocol", "imaps");
	    		set("mail.imaps.port", Integer.toString(port));
		    	set("mail.imaps.host", server);

	    	}
	    	else if (socketType == SocketType.STARTTLS) {
	    		set("mail.store.protocol", "imap");
	    		set("mail.imap.starttls.enable", "true");
	    		set("mail.imap.starttls.required", "true");
	    		set("mail.imap.port", Integer.toString(port));
		    	set("mail.imap.host", server);

	    	}
	    	
	    	
	    	Store store;

	    	try {
	    		store = session.getStore(sessionProperties.getProperty("mail.store.protocol"));
	    		store.connect(login, password);
	    		store.close();
			
	    		isSuccess = true;
	    		isDone = true;
	    		
	    		if (socketType == SocketType.SSL) {
	    			this.sslConnectSuccess = true;
	    			this.sslPort = this.port;
	    		} else if (socketType == SocketType.STARTTLS){
	    			this.startTLSConnectSuccess = true;
	    			this.startTLSport = this.port;
	    		}
	    		
	    	} catch (AuthenticationFailedException e) {
	    		isSuccess = false;
	    		connectSuccess = true;
	    		if (this.socketType == SocketType.SSL) {
	    			this.sslConnectSuccess = true;
	    			this.sslPort = this.port;
	    		} else if (this.socketType == SocketType.STARTTLS) {
	    			this.startTLSConnectSuccess = true;
	    			this.startTLSport = this.port;
	    		}		
	    		
	    		errorMessage = "IMAP login failed";
	    	} catch (NoSuchProviderException e) {
	    		isSuccess = false;
	    		errorMessage = e.getMessage();
	    		logger.warning("Could not test login credentials: "+ e.getMessage());
	    	} catch (MailConnectException e) {
	    		isSuccess = false;
	    		errorMessage = "IMAP connect failed";
	    	} catch (MessagingException e) {
	    		isSuccess = false;
	    		errorMessage = "IMAP: "+e.getMessage();
	    		logger.warning("Error testing login credentials: "+ e.getMessage());
	    	}
	    	
	    	
	    } else {
	    	
	    	/* We try to guess */
	    	/* First try 993. */
	    	
	    	set("mail.store.protocol", "imaps");
	    	set("mail.imaps.host", server);
	    	set("mail.imaps.port","993");
	           
	    	Store store;
	    	try {
	    		store = session.getStore("imaps");
	    		store.connect(login, password);
	    		store.close();
			
	    		this.protocol = Protocol.IMAP;
	    		this.socketType = SocketType.SSL;
	    		this.port = 993;
	    		this.sslPort = 993;
	    		this.sslConnectSuccess = true;
	    			    		
	    		this.isSuccess = true;
	    		this.isDone = true;
	    		
	    	} catch (AuthenticationFailedException e) {
	    		
	    		this.isSuccess = false;
	    		this.connectSuccess = true;
	    		errorMessage = "IMAP login failed";
	    		this.sslPort = 993;
	    		this.sslConnectSuccess = true;
	    		
	    	} catch (NoSuchProviderException e) {
	    		isSuccess = false;
	    		errorMessage = "IMAP: "+e.getMessage();
	    		logger.warning("Could not test login credentials: "+ e.getMessage());
	    	} catch (MailConnectException e) {
	    		isSuccess = false;
	    		errorMessage = "IMAP connect failed";
	    	} catch (MessagingException e) {
	    		isSuccess = false;
	    		errorMessage = "IMAP: "+e.getMessage();
	    		logger.warning("Error testing login credentials: "+ e.getMessage());
	    	}
		
	    	if (isSuccess == false && isDone == false && sslConnectSuccess == false) {
			
	    		/* Then we try 143 w/STARTTLS */
			
	    		set("mail.store.protocol", "imap");
	    		set("mail.imap.host", server);
	    		set("mail.imap.port","143");
	    		set("mail.imap.starttls.enable", "true");
	    		set("mail.imap.starttls.required", "true");
			
	    		try {
	    			store = session.getStore("imap");
	    			store.connect(login, password);
	    			store.close();
				
	    			this.protocol = Protocol.IMAP;
	    			this.socketType = SocketType.STARTTLS;
	    			this.port = 143;
				
	    			this.isSuccess = true;
	    			this.isDone = true;
	    			this.startTLSConnectSuccess = true;
	    			this.startTLSport = 993;
	    			
	    		} catch (AuthenticationFailedException e) {
	    			
	    			this.isSuccess = false;
	    			this.connectSuccess = true;
	    			this.errorMessage = "IMAP login failed";
	    			this.port = 143;
	    			this.socketType = SocketType.STARTTLS;
	    			this.startTLSConnectSuccess = true;
	    			this.startTLSport = 993;
	    			
	    		} catch (NoSuchProviderException e) {
	    			isSuccess = false;
	    			if (this.sslConnectSuccess == false) {
	    				errorMessage = "IMAP: "+e.getMessage();
	    			}
	    			logger.warning("Could not test login credentials: "+ e.getMessage());
	    		} catch (MailConnectException e) {
	    			isSuccess = false;
	    			errorMessage = "IMAP connect failed";
	    		} catch (MessagingException e) {
	    			isSuccess = false;
	    			errorMessage = e.getMessage();
	    			logger.warning("Error testing login credentials: "+ e.getMessage());
	    		}
			
	    	}
	    }
		
	}
		
	private void set(String name, String value) {
		sessionProperties.setProperty(name, value);
	}

	public boolean isDone() {
		// TODO Auto-generated method stub
		return isDone;
	}

	public int getPort() {
		return port;
	}
	
	public SocketType getSocketType() {
		return socketType;
	}
	
	public boolean getConnectSuccess() {
		return connectSuccess;
	}

	public boolean getSSLConnectSuccess() {
		return sslConnectSuccess;
	}
	
	public boolean getStartTLSConnectSuccess() {
		return startTLSConnectSuccess;
	}
	
	public int getSSLport() {
		return sslPort;
	}

	public int getStartTLSPort() {
		return startTLSport;
	}

}
