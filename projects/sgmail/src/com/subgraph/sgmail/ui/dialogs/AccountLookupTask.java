package com.subgraph.sgmail.ui.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.subgraph.sgmail.servers.ServerInformation;
import com.subgraph.sgmail.servers.ServerInformation.Protocol;
import com.subgraph.sgmail.servers.MozillaAutoconfiguration;

public class AccountLookupTask implements IRunnableWithProgress {

	private final AccountDetailsPage page;
	private final NewAccountDialog dialog;
	private final String domain;
	
	public AccountLookupTask(AccountDetailsPage page, String domain) {
		this.page = page; this.dialog = null;
		this.domain = domain;
	}
	public AccountLookupTask(NewAccountDialog dialog, String domain) {
		this.dialog = dialog; page = null;
		this.domain = domain;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		MozillaAutoconfiguration autoconf = new MozillaAutoconfiguration(domain);
		monitor.beginTask("Looking up server information", 1);
		if(autoconf.performLookup()) {
			final ServerInformation incoming = getIMAPServer(autoconf);
			final ServerInformation outgoing = getSMTPServer(autoconf);
			if(dialog != null) dialog.setServerInfo(incoming, outgoing);
			if(page != null) page.setServerInfo(incoming, outgoing);
		} else {
			// XXX notify user that autoconf did not complete successfully
		}
		monitor.done();
	}
	
	private ServerInformation getIMAPServer(MozillaAutoconfiguration autoconf) {
		for(ServerInformation info: autoconf.getIncomingServers()) {
			if(info.getProtocol() == Protocol.IMAP) {
				return info;
			}
		}
		return null;
	}
	
	private ServerInformation getSMTPServer(MozillaAutoconfiguration autoconf) {
		for(ServerInformation info: autoconf.getOutgoingServers()) {
			return info;
		}
		return null;
	}




		


}
