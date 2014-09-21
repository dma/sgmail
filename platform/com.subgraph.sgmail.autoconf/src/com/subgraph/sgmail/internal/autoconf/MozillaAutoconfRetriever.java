package com.subgraph.sgmail.internal.autoconf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class MozillaAutoconfRetriever implements AutoconfRetriever {
	
    private final static Logger logger = Logger.getLogger(MozillaAutoconfRetriever.class.getName());    
    
    private final static String MOZILLA_AUTOCONF_LOCAL_PATH = "/xml/ispdb/";
    private final static String MOZILLA_MX_URL = "https://mx.thunderbird.net/dns/mx/";
    
    @Override
    public InputStream lookupDomain(String domain) {
    	
    	final String path = getTargetPath(domain);
    	return this.getClass().getResourceAsStream(path);
	
    }

    
    private String getTargetPath(String domain){
        return new String(MOZILLA_AUTOCONF_LOCAL_PATH + domain);
    }


	@Override
	public InputStream lookupMX(String domain) {
		try {
			final URL url = getTargetMXURL(domain);
			return url.openStream();
		} catch (MalformedURLException e) {
			return null;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}


	private URL getTargetMXURL(String domain) throws MalformedURLException {
		return new URL(MOZILLA_MX_URL + domain);
	}
}
