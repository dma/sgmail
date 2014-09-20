package com.subgraph.sgmail.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class ServerDetailsEntryPanel extends Composite {

	public ServerDetailsEntryPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		final Group g = new Group(this, SWT.NONE);
		g.setText("Server Information");
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 20;
		g.setLayout(layout);
		create(g);
	}

	private void create(Composite parent) {
		/*incomingProtocol = createLabelPair(parent, "Incoming protocol:");
		incomingHost = createLabelPair(parent, "Incoming server:");
		smtpHost = createLabelPair(parent, "SMTP server:");
		clearServerInfo();*/
	}
}
