package com.subgraph.sgmail.ui.dialogs;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.subgraph.sgmail.database.Model;

public class ServerDetailsPage extends WizardPage {

	private Text imapServerHostname;
	private Text imapServerPort;
	private Text smtpServerHostname;
	private Text smtpServerPort;
	private Model model;
	
	protected ServerDetailsPage(Model model) {
		super("serverdetails");
        setTitle("Email account details");
        setDescription("Enter information about your email account");
		setPageComplete(false);
		this.model = model;
	}

	@Override
	public void createControl(Composite parent) {
		
		final Composite c = new Composite(parent, SWT.NONE);
	    GridLayout layout = new GridLayout();
	    c.setLayout(layout);
	    layout.numColumns = 4;
	    Label imapServerLabel = new Label(c, SWT.NONE);
	    imapServerLabel.setText("Incoming (IMAPS):");
	    imapServerHostname = new Text(c, SWT.BORDER | SWT.SINGLE);
	    imapServerHostname.setText("");

	    imapServerHostname.addKeyListener(new KeyListener() {
	      @Override
	      public void keyPressed(KeyEvent e) {
	        // TODO Auto-generated method stub
	      }

	      @Override
	      public void keyReleased(KeyEvent e) {
	        if (!imapServerLabel.getText().isEmpty()) {
	          setPageComplete(true);
	        }

	      }


	    });
	    
	    Label imapServerPortLabel = new Label(c, SWT.NONE);
	    imapServerPortLabel.setText("Port:");
	    imapServerPort = new Text(c, SWT.BORDER | SWT.SINGLE);
	    imapServerPort.setText("");

	    imapServerPort.addKeyListener(new KeyListener() {
	      @Override
	      public void keyPressed(KeyEvent e) {
	        // TODO Auto-generated method stub
	      }

	      @Override
	      public void keyReleased(KeyEvent e) {
	        if (!imapServerPort.getText().isEmpty()) {
	          setPageComplete(true);
	        }

	      }


	    });
	    
	    
	    Label smtpServerLabel = new Label(c, SWT.NONE);
	    smtpServerLabel.setText("Outgoing SMTP:");
	    smtpServerHostname = new Text(c, SWT.BORDER | SWT.SINGLE);
	    smtpServerHostname.setText("");

	    imapServerHostname.addKeyListener(new KeyListener() {
	      @Override
	      public void keyPressed(KeyEvent e) {
	        // TODO Auto-generated method stub
	      }

	      @Override
	      public void keyReleased(KeyEvent e) {
	        if (!smtpServerPort.getText().isEmpty()) {
	          setPageComplete(true);
	        }

	      }


	    });
	    
	    Label smtpServerPortLabel = new Label(c, SWT.NONE);
	    smtpServerPortLabel.setText("Port:");
	    imapServerPort = new Text(c, SWT.BORDER | SWT.SINGLE);
	    imapServerPort.setText("");

	    imapServerPort.addKeyListener(new KeyListener() {
	      @Override
	      public void keyPressed(KeyEvent e) {
	        // TODO Auto-generated method stub
	      }

	      @Override
	      public void keyReleased(KeyEvent e) {
	        if (!imapServerPort.getText().isEmpty()) {
	          setPageComplete(true);
	        }

	      }


	    });
	    
	    setControl(c);
	    setPageComplete(false);

	    

		
	}

}
