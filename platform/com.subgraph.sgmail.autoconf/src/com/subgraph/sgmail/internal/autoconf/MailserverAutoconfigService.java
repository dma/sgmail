package com.subgraph.sgmail.internal.autoconf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.subgraph.sgmail.autoconf.AutoconfigResult;
import com.subgraph.sgmail.autoconf.MailserverAutoconfig;
import com.subgraph.sgmail.autoconf.ServerInformation;
import com.subgraph.sgmail.database.Model;
import com.subgraph.sgmail.database.Preferences;
import com.google.common.net.InternetDomainName;

public class MailserverAutoconfigService implements MailserverAutoconfig {

    private static class DomainEntry {
        private final ServerInformationImpl incomingServer;
        private final ServerInformationImpl outgoingServer;
        
        DomainEntry(ServerInformationImpl incomingServer, ServerInformationImpl outgoingServer) {
            this.incomingServer = incomingServer;
            this.outgoingServer = outgoingServer;
        }
        ServerInformationImpl getIncomingServer() {
            return incomingServer;
        }

        ServerInformationImpl getOutgoingServer() {
            return outgoingServer;
        }
    }
    private final static Map<String, DomainEntry> domainEntries = new HashMap<>();
    private Model model;

    static {
        addDomainEntry("riseup.net", "imap.riseup.net", "zsolxunfmbfuq7wf.onion", "smtp.riseup.net", "zsolxunfmbfuq7wf.onion");
    }

    private static void addDomainEntry(String domain, String imapHostname, String imapOnionname, String smtpHostname, String smtpOnionname) {
        final ServerInformationImpl incomingServer = createImapServer(imapHostname, imapOnionname);
        final ServerInformationImpl outgoingServer = createSMTPServer(smtpHostname, smtpOnionname);
        addDomainEntry(domain, incomingServer, outgoingServer);
    }

    private static void addDomainEntry(String domain, String imapHostname, String smtpHostname) {
        final ServerInformationImpl incomingServer = createImapServer(imapHostname);
        final ServerInformationImpl outgoingServer = createSMTPServer(smtpHostname);
        addDomainEntry(domain, incomingServer, outgoingServer);
    }

    private static void addDomainEntry(String domain, ServerInformationImpl incomingServer, ServerInformationImpl outgoingServer) {
        final DomainEntry entry = new DomainEntry(incomingServer, outgoingServer);
        domainEntries.put(domain, entry);
    }

    private static ServerInformationImpl createImapServer(String hostname, String onion) {
        return createIMAPBuilder().hostname(hostname).onion(onion).build();
    }
    private static ServerInformationImpl createImapServer(String hostname) {
        return createIMAPBuilder().hostname(hostname).build();
    }

    private static ServerInformationImpl createSMTPServer(String hostname, String onion) {
        return createSMTPBuilder().hostname(hostname).onion(onion).build();
    }

    private static ServerInformationImpl createSMTPServer(String hostname) {
        return createSMTPBuilder().hostname(hostname).build();
    }

    private static ServerInformationImpl.Builder createIMAPBuilder() {
        return new ServerInformationImpl.Builder()
                .protocol(ServerInformationImpl.Protocol.IMAP)
                .socketType(ServerInformationImpl.SocketType.SSL)
                .port(993)
                .authenticationType(ServerInformationImpl.AuthenticationType.PASSWORD_CLEAR)
                .usernameType(ServerInformationImpl.UsernameType.USERNAME_EMAILADDRESS);
    }

    private static ServerInformationImpl.Builder createSMTPBuilder() {
        return new ServerInformationImpl.Builder()
                .protocol(ServerInformationImpl.Protocol.SMTP)
                .socketType(ServerInformationImpl.SocketType.SSL)
                .port(465)
                .authenticationType(ServerInformationImpl.AuthenticationType.PASSWORD_CLEAR)
                .usernameType(ServerInformationImpl.UsernameType.USERNAME_EMAILADDRESS);
    }

	@Override
	public AutoconfigResult resolveDomain(String domainName) {
		final List<ServerInformation> incomingServers = new ArrayList<>();
		final List<ServerInformation> outgoingServers = new ArrayList<>();
		final String dn = domainName.toLowerCase();
		final String torEnabled = model.getRootPreferences().getPreference(Preferences.TOR_ENABLED);
		
		if(domainEntries.containsKey(dn)) {
			final DomainEntry entry = domainEntries.get(dn);
			incomingServers.add(entry.getIncomingServer());
			outgoingServers.add(entry.getOutgoingServer());
			return new AutoconfigResultImpl(incomingServers, outgoingServers);
		}
		
		final MozillaAutoconfiguration mozillaAutoconf = new MozillaAutoconfiguration(dn);
		if(mozillaAutoconf.performLookup()) {
			incomingServers.addAll(mozillaAutoconf.getIncomingServers());
			outgoingServers.addAll(mozillaAutoconf.getOutgoingServers());
			return new AutoconfigResultImpl(incomingServers, outgoingServers);
		} else 
		{	
			if (torEnabled.equals("false")) 
			{
				/* Use local resolver, attempt to look up the MX record, pick the one with the lowest priority */
				
				String mx = GetMX.getMX(dn);
				if (mx != null)
				{
					InternetDomainName d = InternetDomainName.from(mx);
					if (d != null) {
						final MozillaAutoconfiguration mozillaAutoconfMX = new MozillaAutoconfiguration(d.topPrivateDomain().toString());
						if (mozillaAutoconfMX.performLookup()) {
							incomingServers.addAll(mozillaAutoconfMX.getIncomingServers());
							outgoingServers.addAll(mozillaAutoconfMX.getOutgoingServers());
							return new AutoconfigResultImpl(incomingServers, outgoingServers);
						}
					}
									
				}
				/* E-mail domain is bad, autoconfig fails */

			} else
				
				/* Due to limitations in Tor we will need to use the Mozilla MX lookup web service */ 
				
				{
					MozillaAutoconfRetriever retriever = new MozillaAutoconfRetriever();
					InputStream is = retriever.lookupMX(dn);
					if (is != null)
					{
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						String mx = null;
					
						try {
							String data = br.readLine();
							System.out.println(data);	
							if (data.startsWith("No MX")) {
								System.out.println("No MX for "+dn);
							}
							else {
								mx = data; 
							}
							br.close();
							if (mx != null) {
								InternetDomainName d = InternetDomainName.from(mx);
								String topPrivateDomain = d.topPrivateDomain().toString();
								System.out.println("Looking up autoconfig for: "+topPrivateDomain);
								final MozillaAutoconfiguration mozillaAutoconfMX = new MozillaAutoconfiguration(topPrivateDomain);
								if (mozillaAutoconfMX.performLookup()) {
									incomingServers.addAll(mozillaAutoconfMX.getIncomingServers());
									outgoingServers.addAll(mozillaAutoconfMX.getOutgoingServers());
									return new AutoconfigResultImpl(incomingServers, outgoingServers);
								}
							}
						} catch (IOException e) {
					
						}
					}
				}		
		}
		
		
		
		
		
		return null;
	}
	
	void setModel(Model model) {
		this.model = model;
	}
}
