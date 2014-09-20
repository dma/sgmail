package com.subgraph.sgmail.ui.dialogs;

import com.subgraph.sgmail.autoconf.ServerInformation;
import com.subgraph.sgmail.autoconf.ServerInformation.SocketType;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.IProgressMonitor;

import javax.mail.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Logger;

public class SMTPServerConfigurationProbeTask implements IRunnableWithProgress {
	
	private final static Logger logger = Logger.getLogger(AccountTestLoginTask.class.getName());
	
	private final Properties sessionProperties;
	private final String serverHostname;
	private final String login;
	private final String password;
	private int port;
    private final boolean debug;
    
	private boolean isSuccess;
	private String errorMessage = "";

	private boolean autodetect = false;
	private boolean isDone;
	private boolean connectSuccess = false;

	private SocketType socketType;

	private boolean sslConnectSuccess;

	private int sslPort;

	private boolean startTLSConnectSuccess;

	private int startTLSport;
	
	SMTPServerConfigurationProbeTask(String hostname, String login, String password, boolean debug) {
		this.serverHostname = hostname;
		this.login = login;
		this.password = password;
        this.debug = debug;
        this.autodetect = true;
		this.sessionProperties = new Properties();
		
	}
	
	SMTPServerConfigurationProbeTask(String hostname, String login, String password, int port, SocketType socketType, boolean debug) {
		this.serverHostname = hostname;
		this.login = login;
		this.password = password;
        this.debug = debug;
		this.sessionProperties = new Properties();
		this.port = port;
		this.autodetect = false;
		this.socketType = socketType;
	}

	@Override
	public void run(IProgressMonitor arg0) throws InvocationTargetException,
			InterruptedException {
		
		if (autodetect == false) {
		
			set("mail.smtp.auth", "true");
			set("mail.smtp.host", this.serverHostname);
			if (socketType == SocketType.SSL) {
				set("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			} else {
		    	set("mail.smtp.starttls.enable", "true");
		    	set("mail.smtp.starttls.required", "true");
			}
			set("mail.smtp.port", Integer.toString(this.port));
			
			Session session = Session.getInstance(this.sessionProperties,
			        new javax.mail.Authenticator() {
			            protected PasswordAuthentication getPasswordAuthentication() {
			                return new PasswordAuthentication(login, password);
			            }
			        });
		    try {
				Transport t = session.getTransport("smtp");
				t.connect();
				if (t.isConnected()) {
					this.isSuccess = true;
					this.isDone = true;
					this.connectSuccess = true;
					
		    		if (socketType == SocketType.SSL) {
		    			this.sslConnectSuccess = true;
		    			this.sslPort = this.port;
		    		} else if (socketType == SocketType.STARTTLS){
		    			this.startTLSConnectSuccess = true;
		    			this.startTLSport = this.port;
		    		}
				}
				
			} catch (NoSuchProviderException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				if (e instanceof AuthenticationFailedException) {
					this.connectSuccess=true;
					this.isDone = true;
					this.isSuccess = false;
					if (this.socketType == SocketType.SSL) {
						this.sslConnectSuccess = true;
						this.sslPort = this.port;
						errorMessage = "SMTP login failed";

					} else {
						this.startTLSConnectSuccess = true;
						this.startTLSport = this.port;
						errorMessage = "SMTP login failed";
					}
					
				} else 
					e.printStackTrace();
			} 
		    
			
		} else if (autodetect == true) {
		
			
			Session session = Session.getInstance(this.sessionProperties,
					new javax.mail.Authenticator() {
		            	protected PasswordAuthentication getPasswordAuthentication() {
		            		return new PasswordAuthentication(login, password);
		            	}
		        	});
			
			/* Let's try STARTTLS on port 587 */
			
			this.sessionProperties.remove("mail.smtp.socketFactory.class");
    
			set("mail.smtp.host", this.serverHostname);
			set("mail.smtp.auth", "true");
			set("mail.smtp.starttls.enable", "true");
			set("mail.smtp.starttls.required", "true");
			set("mail.smtp.port", "587");
    	
			try {
				Transport t = session.getTransport("smtp");
				t.connect();
				if (t.isConnected()) {
					this.port = 587;
					this.socketType = SocketType.STARTTLS;
					this.isSuccess = true;
					this.isDone = true;

		    		this.startTLSConnectSuccess = true;
		    		this.startTLSport = this.port;
		    		
				}
			
			} catch (NoSuchProviderException e) {
				errorMessage = "SMTP: TLS or "+e.getMessage();
			} catch (MessagingException e) {
				if (e instanceof AuthenticationFailedException) {
					this.port = 587;
					errorMessage += "SMTP login failed";
					this.connectSuccess = true;
					this.startTLSConnectSuccess = true;
					this.startTLSport = this.port;
					
					/* If authentication fails on the submission port,
					 * we assume the credentials are bad and don't try 465.
					 */
					
				}
				else {
					errorMessage = "SMTP connect failed" + e.getMessage();
				}
			} 
		
		
			if (this.isSuccess == false && this.startTLSConnectSuccess == false) {

				/* Autodetect server settings */
				/* Test SMTPS: TLS on TCP465 */
				/* If authentication failed on TCP587, we don't bother.. */
			
		
				set("mail.smtp.auth", "true");
				set("mail.smtp.starttls.enable", "false");
				set("mail.smtp.starttls.required", "false");
				set("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				set("mail.smtp.port", "465");
		

				try {
					Transport t = session.getTransport("smtp");
					t.connect();
					if (t.isConnected()) {
						this.isSuccess = true;
						this.isDone = true;
						this.port = 465;
						this.connectSuccess = true;
						this.socketType = SocketType.SSL;

						this.sslConnectSuccess = true;
						this.sslPort = this.port;

					}
			
				} catch (NoSuchProviderException e) {
					errorMessage = "SMTP: " + e.getMessage();
				} catch (MessagingException e) {
					if (e instanceof AuthenticationFailedException) {
						errorMessage = "SMTP login failed";
						this.sslConnectSuccess = true;
						this.connectSuccess = true;
						this.sslPort = this.port;
					} else 
						errorMessage = "SMTP connect failed" + e.getMessage();
				} 
	  
			}
	    
		} 
	    
	}

	public boolean isSuccess() {
		return isSuccess;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	
	private void set(String name, String value) {
		sessionProperties.setProperty(name, value);
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
