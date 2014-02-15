package com.subgraph.sgmail.identity.server;

import com.subgraph.sgmail.identity.protocol.KeyLookupRequest;
import com.subgraph.sgmail.identity.protocol.KeyRegistrationFinalizeRequest;
import com.subgraph.sgmail.identity.protocol.KeyRegistrationRequest;
import org.bouncycastle.openpgp.PGPPublicKeyRing;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {
	private final static Logger logger = Logger.getLogger(Server.class.getName());
	
	private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Properties properties;

    private final MessageDispatcher dispatcher;
    private final Object keyRegistrationLock = new Object();
    private final Map<String, KeyRegistrationState> registrationByEmail = new HashMap<>();
    private final Map<Long, KeyRegistrationState> registrationByRequestId = new HashMap<>();
    private final Map<String, PublicKeyRecord> keysByEmail = new HashMap<>();

    private final Random random = new SecureRandom();

    private KeyRegistrationMailer registrationMailer;
	
	public Server(Properties properties) {
		this.properties = properties;
        this.dispatcher = createMessageDispatcher();
	}

    private MessageDispatcher createMessageDispatcher() {
        final MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.addHandler(KeyLookupRequest.class, new KeyLookupHandler(this));
        dispatcher.addHandler(KeyRegistrationRequest.class, new KeyRegistrationHandler(this));
        dispatcher.addHandler(KeyRegistrationFinalizeRequest.class, new KeyRegistrationFinalizeHandler(this));
        return dispatcher;
    }

    public MessageDispatcher getMessageDispatcher() {
        return dispatcher;
    }

    public synchronized KeyRegistrationMailer getRegistrationMailer() {
        if(registrationMailer == null) {
            registrationMailer = KeyRegistrationMailer.create(properties);
        }
        return registrationMailer;
    }

    public KeyRegistrationState getRegistrationStateByEmail(String emailAddress) {
        synchronized (keyRegistrationLock) {
            return registrationByEmail.get(emailAddress);
        }
    }

    public KeyRegistrationState getRegistrationStateByRequestId(long requestId) {
        synchronized (keyRegistrationLock) {
            return registrationByRequestId.get(requestId);
        }
    }

    public KeyRegistrationState createRegistrationState(String emailAddress, PGPPublicKeyRing publicKeyRing) {
        synchronized (keyRegistrationLock) {
            if(registrationByEmail.containsKey(emailAddress)) {
                return registrationByEmail.get(emailAddress);
            }
            final KeyRegistrationState krs = new KeyRegistrationState(emailAddress, publicKeyRing, random.nextLong(), random.nextLong(), new Date());
            registrationByEmail.put(emailAddress, krs);
            registrationByRequestId.put(krs.getRequestId(), krs);
            return krs;
        }
    }

    public void registerPublicKey(KeyRegistrationState krs) {
        synchronized (keyRegistrationLock) {
            System.out.println("Registering key for "+ krs.getEmailAddress());
        final PGPPublicKeyRing pkr = krs.getPublicKeyRing();
            try {
                registrationByEmail.remove(krs.getEmailAddress());
                registrationByRequestId.remove(krs.getRequestId());
                keysByEmail.put(krs.getEmailAddress(), new PublicKeyRecord(pkr.getEncoded()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public PublicKeyRecord lookupRecordByEmail(String email) {
        synchronized (keyRegistrationLock) {
            return keysByEmail.get(email);
        }
    }

	public void start() throws IOException {

		try(ServerSocket listeningSocket = new ServerSocket(getListeningPort())) {
			while(true) {
				Socket newSocket = listeningSocket.accept();
				launchConnectionTask(newSocket);
			}
		} 
	}

    private int getListeningPort() {
        final String portValue = properties.getProperty("com.subgraph.identity.server.port", "12345");
        try {
            return Integer.parseInt(portValue);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Could not parse port value: "+ portValue);
        }
    }
	
	private void launchConnectionTask(Socket s) {
		try {
			final InputStream in = s.getInputStream();
			final OutputStream out = s.getOutputStream();
			executor.execute(new ConnectionTask(this, in, out));
		} catch (IOException e) {
			logger.warning("IOException getting streams from socket "+ e);
			quietlyClose(s);
		}
	}
	
	private void quietlyClose(Closeable c) {
		try {
			c.close();
		} catch (IOException e) { }
	}
	
	public static void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Need property file argument");
        } else {
            try {
                final Server server = new Server(loadProperties(args[0]));
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

    private static Properties loadProperties(String path) throws IOException {
        final File propertiesFile = new File(path);
        try(Reader reader = new FileReader(propertiesFile)) {
            final Properties p = new Properties();
            p.load(reader);
            return p;
        }
    }
}
