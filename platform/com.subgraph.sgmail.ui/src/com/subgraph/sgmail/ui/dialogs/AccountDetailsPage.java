package com.subgraph.sgmail.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.google.common.net.InternetDomainName;
import com.subgraph.sgmail.accounts.MailAccount;
import com.subgraph.sgmail.accounts.ServerDetails;
import com.subgraph.sgmail.autoconf.ServerInformation;
import com.subgraph.sgmail.database.Model;
import com.subgraph.sgmail.database.Preferences;
import com.subgraph.sgmail.imap.IMAPAccount;
import com.subgraph.sgmail.ui.Activator;
import com.subgraph.sgmail.ui.Resources;
import com.subgraph.sgmail.autoconf.ServerInformation.SocketType;


public class AccountDetailsPage extends WizardPage {
    private final static Logger logger = Logger.getLogger(AccountDetailsPage.class.getName());
    private Composite c;
 
    private static class TextFieldWithErrorLabel {
        private final static int BASIC_TEXT_FLAGS = SWT.SINGLE | SWT.BORDER;
        private final Text textField;
        private final Label errorLabel;
        private final Color defaultLabelForeground;

        TextFieldWithErrorLabel(Composite parent, String labelText, String textMessage, boolean password, String defaultValue) {
            final Label label = new Label(parent, SWT.RIGHT);
            label.setText(labelText);
            label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

            final int flags = (password) ? (BASIC_TEXT_FLAGS | SWT.PASSWORD) : (BASIC_TEXT_FLAGS);
            textField = new Text(parent, flags);
            textField.setMessage(textMessage);
            if (defaultValue != null) {
            	textField.setText(defaultValue);
            }
            final GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
            gd.widthHint = 200;
            textField.setLayoutData(gd);

            errorLabel = new Label(parent, SWT.NONE);
            errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            defaultLabelForeground = errorLabel.getForeground();
        }

        TextFieldWithErrorLabel(Composite parent, String labelText, String textMessage, boolean password) {
        	this(parent, labelText, textMessage, password, null);
        }
        
        void addTextFocusListener(FocusListener listener) {
            textField.addFocusListener(listener);
        }

        void addTextModifyListener(ModifyListener listener) {
            textField.addModifyListener(listener);
        }

        String getText() {
            return textField.getText();
        }

        void setErrorText(String message) {
            final Color errorColor = JFaceResources.getColorRegistry().get(Resources.COLOR_ERROR_MESSAGE);
            if(errorColor != null) {
                errorLabel.setForeground(errorColor);
            }
            errorLabel.setText(message);
        }

        void setInfoText(String message) {
            errorLabel.setForeground(defaultLabelForeground);
            errorLabel.setText(message);
        }
    }

    private static class TextFieldWithLabel {
        private final static int BASIC_TEXT_FLAGS = SWT.SINGLE | SWT.BORDER;
        private final Text textField;
        
    TextFieldWithLabel(Composite parent, String labelText, String textMessage, boolean password, String defaultValue) {
        final Label label = new Label(parent, SWT.RIGHT);
        label.setText(labelText);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        final int flags = (password) ? (BASIC_TEXT_FLAGS | SWT.PASSWORD) : (BASIC_TEXT_FLAGS);
        textField = new Text(parent, flags);
        textField.setMessage(textMessage);
        if (defaultValue != null) {
        	textField.setText(defaultValue);
        }
        final GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        gd.widthHint = 200;
        textField.setLayoutData(gd);

    }

    TextFieldWithLabel(Composite parent, String labelText, String textMessage, boolean password) {
    	this(parent, labelText, textMessage, password, null);
    }
    
    void addTextFocusListener(FocusListener listener) {
        textField.addFocusListener(listener);
    }

    void addTextModifyListener(ModifyListener listener) {
        textField.addModifyListener(listener);
    }

    String getText() {
        return textField.getText();
    }

    void setText(String value) {
    	textField.setText(value);
    }

}
    
    
    private final Model model;
    
    private TextFieldWithErrorLabel realnameField;
    private TextFieldWithErrorLabel addressField;
    private TextFieldWithErrorLabel passwordField;
	private IMapServerInfoPanel serverInfoPanel;
	
	private TextFieldWithLabel imapServerField;
	private Text imapServerPort;
	private TextFieldWithLabel smtpServerField;
	private Text smtpServerPort;
	
	private Group accountDetailsGroup;
	
	private String previousAddress;
    private boolean isAddressValid;

	private TextFieldWithLabel loginField;
	private ProgressBar progress;
	private Label statusBar;
	private boolean detailsVerified = false;
	private boolean successfulLookup = false;
	private Combo imapTransportCombo;
	private Combo smtpTransportCombo;
	
	AccountDetailsPage(Model model) {
		super("details");
        setTitle("Email account details");
        setDescription("Enter information about your email account");
		setPageComplete(false);
		this.model = model;
	}
	
	void setAccountTestError(String message) {
		setErrorMessage(message);
	}

	public String getUsername() {
		return getAddressUsername(addressField.getText());
	}
	
	public String getDomain() {
		return getAddressDomain(addressField.getText());
	}
	
	public String getRealname() {
		return realnameField.getText();
	}
	
	String getPassword() {
		return passwordField.getText();
	}
	
	String getSmtpServerAddress() {
		return smtpServerField.getText();
	}
	
	String getImapServerAddress() {
		return imapServerField.getText();
	}
	
	String getSmtpServerPort() {
		if (smtpServerPort != null && !smtpServerPort.isDisposed()) {
			return smtpServerPort.getText();
		} else
			return "";
	}
	
	String getImapServerPort() {
		if (imapServerPort != null && !imapServerPort.isDisposed()) {
			return imapServerPort.getText();
		} else
			return "";
		
	}
	
	String getImapTransport() {
		if (imapTransportCombo != null && !imapTransportCombo.isDisposed()) {
			return imapTransportCombo.getText();
		} else 
			return "";
	}
	
	String getSmtpTransport() {
		if (smtpTransportCombo != null && !smtpTransportCombo.isDisposed()) {
			return smtpTransportCombo.getText();
		} else
			return "";
	}
	
    String getIncomingLogin() {
        return getUsernameByType(getIncomingServer().getUsernameType());
    }

    String getOutgoingLogin() {
        return getUsernameByType(getOutgoingServer().getUsernameType());
    }

    private String getUsernameByType(ServerInformation.UsernameType type) {
        final String email = addressField.getText();
        switch (type) {
            case USERNAME_EMAILADDRESS:
                return email;

            case USERNAME_LOCALPART:
                return getAddressUsername(email);

            case UNKNOWN:
            default:
                logger.warning("Unknown username type, returning full address");
                return email;
        }
    }

	ServerInformation getIncomingServer() {
		return serverInfoPanel.getIncomingServer();
	}
	
	ServerInformation getOutgoingServer() {
		return serverInfoPanel.getOutgoingServer();
	}

    IMAPAccount createIMAPAccount() {
    	
    	final ServerDetails smtpServer;
    	final ServerDetails imapServer;
    	final String imapProtocol;
    	
    	if (detailsVerified == true) {
    		smtpServer = createServerDetails(getOutgoingServer(), "smtps", getOutgoingLogin(), getPassword());
    		imapProtocol = getIMAPProtocol(getIncomingServer().getHostname());
    		imapServer = createServerDetails(getIncomingServer(), imapProtocol, getIncomingLogin(), getPassword());
    	} else
    	{
    		if (isOnionAddress(smtpServerField.getText()) == true) {
        		smtpServer = createServerDetailsFromFields("smtps", "", smtpServerField.getText(), Integer.parseInt(smtpServerPort.getText()), loginField.getText(), getPassword());
    			
    		} else
    			smtpServer = createServerDetailsFromFields("smtps", smtpServerField.getText(), "", Integer.parseInt(smtpServerPort.getText()), loginField.getText(), getPassword());

    		imapProtocol = getIMAPProtocol(imapServerField.getText());

    		if (isOnionAddress(imapServerField.getText()) == true) {
        		imapServer = createServerDetailsFromFields(imapProtocol, "", imapServerField.getText(), Integer.parseInt(imapServerPort.getText()), loginField.getText(), getPassword());
    			
    		} else
    			imapServer = createServerDetailsFromFields(imapProtocol, imapServerPort.getText(), "", Integer.parseInt(imapServerPort.getText()), loginField.getText(), getPassword());
    		
    	}
        final MailAccount mailAccount = Activator.getInstance().getAccountFactory().createMailAccount(addressField.getText(), addressField.getText(), getRealname(), smtpServer);
        return Activator.getInstance().getIMAPFactory().createIMAPAccount(mailAccount, imapServer);
    }

    private boolean isOnionAddress(String address) {
    		return address.toLowerCase().endsWith(".onion");
	}

	private String getIMAPProtocol(String imapServerHostname) {

        if(imapServerHostname.endsWith("gmail.com") || imapServerHostname.endsWith("googlemail.com")) {
            return "gimaps";
        } else {
            return "imaps";
        }
    }

    private ServerDetails createServerDetails(ServerInformation info, String protocol, String login, String password) {
    	return Activator.getInstance().getAccountFactory().createServerDetails(protocol, info.getHostname(), info.getOnionHostname(), info.getPort(), login, password);
    }

    private ServerDetails createServerDetailsFromFields(String protocol, String hostname, String onionHostname, int port, String login, String password) {
    	return Activator.getInstance().getAccountFactory().createServerDetails(protocol, hostname, onionHostname, port, login, password);
    }
    
	@Override
	public void createControl(Composite parent) {
		c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		createAccountDetailsGroup(c);
		setControl(c);
	}

	private Group createAccountDetailsGroup(Composite parent) {
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		g.setLayout(layout);
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        realnameField = new TextFieldWithErrorLabel(g, "Your Name:", "enter your name", false);
        realnameField.addTextModifyListener(createTextModifyListener());
        addressField = new TextFieldWithErrorLabel(g, "Address:", "enter email address", false);
        addressField.addTextFocusListener(createAddressFocusListener());
        passwordField = new TextFieldWithErrorLabel(g, "Password:", "enter password", true);
        passwordField.addTextModifyListener(createTextModifyListener());
		return g;
	}
	
	private FocusListener createAddressFocusListener() {
		return new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent event) {
				onAddressChange(addressField.getText());
                testPageComplete();
			}
		};	
	}

    private ModifyListener createTextModifyListener() {
        return new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
            	detailsVerified = false;
            	clearStatusBar();
                testPageComplete();
            }
        };
    }
    
    private VerifyListener createNumberVerifyListener() { 
 
    	return new VerifyListener() {  

			@Override
			public void verifyText(VerifyEvent e) {
	    	
	    			String currentValue = ((Text)e.widget).getText();
	    			
	    			String portString =  currentValue.substring(0, e.start) + e.text + currentValue.substring(e.end);
	    			
	    			try {  
	    				int portInt = Integer.valueOf(portString);  
	    				if(portInt < 0 || portInt > 65535)
	    					e.doit = false;  
	    				  
	    			}  
	    			catch(NumberFormatException ex){  
	    				if(!portString.equals(""))
	    					e.doit = false;  
	    			}  
	    		}
    	};
    }
	
	private void onAddressChange(String address) {
		if(address == null || address.equals(previousAddress)) {
			return;
		}
		previousAddress = address;
        isAddressValid = false;
        detailsVerified = false;
        successfulLookup = false;
        addressField.setErrorText("");
        passwordField.setErrorText("");
        
		if(isValidAddress(address)) {
            final String domain = getAddressDomain(address);
			try {
                addressField.setInfoText("Searching provider information...");
				final AccountLookupTask task = new AccountLookupTask(this, domain);
				getContainer().run(false, true, task);
				if(task.getLookupSucceeded()) {
                    addressField.setInfoText("");
                    isAddressValid = true;
                    successfulLookup = true;
				} else {
                    addressField.setErrorText("No info found for "+ domain);
                    if ((serverInfoPanel != null) && (serverInfoPanel.isDisposed() == false))
                    		serverInfoPanel.dispose();
                    if ((accountDetailsGroup != null) && (accountDetailsGroup.isDisposed() == false))
                    		accountDetailsGroup.dispose();
                    this.accountDetailsGroup = createAccountConfigGroup(c, null, null);
                    c.pack(true);
                    c.layout(true);
                    this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y + accountDetailsGroup.getSize().y); 
                    this.getShell().pack(true);
                    
                }
				
			} catch (InvocationTargetException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
            addressField.setErrorText("Invalid email address");
            if (serverInfoPanel != null) {
            	if (serverInfoPanel.isDisposed() == false) {
            		this.c.pack();  
            		this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y - serverInfoPanel.getSize().y);
            		this.getShell().pack();
            		serverInfoPanel.dispose();
            	}
            }
		//	serverInfoPanel.clearServerInfo();
		}	
	}

	private void testPageComplete() {
		
		boolean complete = false;
		
		if (successfulLookup == true) {
			complete = (isAddressValid && !getRealname().isEmpty() && !getPassword().isEmpty());
		} else	{
			
			/* We need special treatment here; blank port numbers OK when autodetect is selected */
			
			if (getImapServerPort().isEmpty() && !getImapTransport().contentEquals("Autodetect")) {
				setPageComplete(false);
				return;
			}
			
			if (getSmtpServerPort().isEmpty() && !getSmtpTransport().contentEquals("Autodetect")) {
				setPageComplete(false);
				return;
			}
			
			complete = (isAddressValid && ((!getSmtpServerAddress().isEmpty() && !getImapServerAddress().isEmpty()) &&
					    !getImapTransport().isEmpty() && !getSmtpTransport().isEmpty()));
		}
		
		setPageComplete(complete);
    }
	

	void setServerInfo(final ServerInformation incoming, final ServerInformation outgoing) {
		
		final Preferences prefs = model.getRootPreferences();
        boolean useTor = prefs.getBoolean(Preferences.TOR_ENABLED);
        
        if (accountDetailsGroup != null && accountDetailsGroup.isDisposed() == false) {
        	this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y - accountDetailsGroup.getSize().y);
        	accountDetailsGroup.dispose();
        	this.c.pack();
        	this.getShell().pack();

        }
        if (serverInfoPanel != null && serverInfoPanel.isDisposed() == false) {
        	this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y - serverInfoPanel.getSize().y);
        	serverInfoPanel.dispose();
        	this.c.pack();
        	this.getShell().pack();

        }
        
		serverInfoPanel = new IMapServerInfoPanel(c, useTor);
    	this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y + serverInfoPanel.getSize().y);
		serverInfoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));		 
		serverInfoPanel.setButtonAdapter(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				manualConfiguration(incoming, outgoing);
			}
			
		});
		
		
		c.pack();
    	this.getShell().pack();

		getControl().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				serverInfoPanel.setServerInfo(incoming, outgoing);
			}
		});
	}

	protected void manualConfiguration(ServerInformation incoming,
			ServerInformation outgoing) {
		
		addressField.setErrorText("");
		passwordField.setErrorText("");
		successfulLookup = false;
		
        this.accountDetailsGroup = createAccountConfigGroup(c, incoming, outgoing);
    	this.getShell().setSize(this.getShell().getSize().x, this.getShell().getSize().y - serverInfoPanel.getSize().y + accountDetailsGroup.getSize().y);
    	serverInfoPanel.dispose();
    	this.c.pack();
    	this.getShell().pack();		
	}

	private boolean isValidAddress(String address) {
		final String parts[] = address.split("@");
		if(parts.length != 2) {
			return false;
		}
		if(!InternetDomainName.isValid(parts[1])) {
			return false;
		}
		
		InternetDomainName idn = InternetDomainName.from(parts[1]);
		return idn.hasPublicSuffix();
	}
	
	private String getAddressUsername(String address) {
		return address.split("@")[0];
	}
	private String getAddressDomain(String address) {
		return address.split("@")[1];
	}
	
	public boolean canFlipToNextPage() {
			if (detailsVerified  == true) {
				return true;
			}
	        return isPageComplete();
	}

	
	public IWizardPage getNextPage() {
		if (detailsVerified == true) 
			return super.getNextPage();
		else if (isPageComplete()) {
			if(!verifyAccountDetails()) {
				return null;
			} else
				return super.getNextPage();
		}
		else
			return null;
    }
	

	private boolean verifyAccountDetails() {
		
		final Preferences prefs = model.getRootPreferences();
        final boolean useTor = prefs.getBoolean(Preferences.TOR_ENABLED);
        final boolean debug = prefs.getBoolean(Preferences.IMAP_DEBUG_OUTPUT);
        
		ServerInformation imap = getIncomingServer();
		ServerInformation smtp = getOutgoingServer();
		
		String smtpHostname;
		String imapHostname;
		
		if (useTor == true && imap.getOnionHostname() != null) {
			imapHostname = imap.getOnionHostname();
		} else
			imapHostname = imap.getHostname();
		
		if (useTor == true && smtp.getOnionHostname() != null) {
			smtpHostname = smtp.getOnionHostname();
		} else
			smtpHostname = smtp.getHostname();
	
		String login = getIncomingLogin();
		final String password = getPassword();
		
		if(login == null) {
			setAccountTestError("No username");
			return false;
			
		}
		if(password == null) {
			setAccountTestError("No password");
			return false;
		}
		
		SMTPServerConfigurationProbeTask smtpProbeTask = new SMTPServerConfigurationProbeTask(smtpHostname, login, password, smtp.getPort(), smtp.getSocketType(), debug);
		IMAPServerConfigurationProbeTask imapProbeTask = new IMAPServerConfigurationProbeTask(imapHostname, login, password, imap.getPort(), imap.getSocketType(), debug);
		
		if(smtpProbeTask == null || imapProbeTask == null) {
            testPageComplete();
			return false;
		}
		
		try {
            passwordField.setInfoText("Verifying login details");
			getContainer().run(false, true, smtpProbeTask);
			getContainer().run(false, true, imapProbeTask);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
			// TODO Auto-generated catch block
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
			// TODO Auto-generated catch block

		}
		if(smtpProbeTask.isSuccess() && imapProbeTask.isSuccess()) {
            passwordField.setInfoText("");
			return true;
		} else {
            passwordField.setErrorText("Login failed.");
            return false;
        }
	}
	
	private Group createAccountConfigGroup(Composite parent, ServerInformation incoming, ServerInformation outgoing) {
		
        final String connectionTypes[] = { "TLS", "STARTTLS", "Autodetect" };
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 8;
		g.setLayout(layout);
		g.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));
       
        statusBar = new Label(g, SWT.RIGHT | SWT.BORDER);
        GridData statusBarGridData = new GridData();
        statusBarGridData.horizontalSpan = 3;
        statusBarGridData.horizontalAlignment = GridData.FILL;
        statusBar.setLayoutData(statusBarGridData);
        statusBar.setVisible(true);
        
        imapServerField = new TextFieldWithLabel(g, "Incoming IMAP server address:", "Hostname", false);
        imapServerField.addTextModifyListener(createTextModifyListener());
        
        imapServerPort = new Text(g, SWT.SINGLE | SWT.BORDER);
        imapServerPort.setMessage(" Port ");
        imapServerPort.addModifyListener(createTextModifyListener());
        imapServerPort.addVerifyListener(createNumberVerifyListener());

        final Label imapTransportLabel = new Label(g, SWT.RIGHT); 
        imapTransportLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        imapTransportLabel.setText("IMAP SSL/TLS:");
        imapTransportCombo = new Combo(g, SWT.READ_ONLY);
        imapTransportCombo.setItems(connectionTypes);
        imapTransportCombo.select(2);
        imapTransportCombo.addSelectionListener(new SelectionAdapter() {
        	

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (imapTransportCombo.getText().equals("STARTTLS"))
					imapServerPort.setText("143");
				else if (imapTransportCombo.getText().equals("TLS")) 
					imapServerPort.setText("993");
				else if (imapTransportCombo.getText().equals("Autodetect"))
					imapServerPort.setText("");
				detailsVerified = false;
			}
			
		});

        new Label(g, SWT.NONE); 

        if (incoming != null) {
        	imapServerField.setText(incoming.getHostname() == null ? "" : incoming.getHostname());
        	imapServerPort.setText(Integer.toString(incoming.getPort()));
        	switch(incoming.getSocketType()) {
        		case STARTTLS:
        			imapTransportCombo.select(1);
        			break;
        		case SSL:
        			imapTransportCombo.select(0);
        			break;
			default:
				break;
        	}        	
        }
        
        smtpServerField = new TextFieldWithLabel(g, "Outoing SMTP server address:", "Address", false);
        smtpServerField.addTextFocusListener(createAddressFocusListener());
        smtpServerField.addTextModifyListener(createTextModifyListener());

        smtpServerPort = new Text(g, SWT.SINGLE | SWT.BORDER);
        smtpServerPort.setMessage(" Port ");
        
        smtpServerPort.addModifyListener(createTextModifyListener());
        smtpServerPort.addVerifyListener(createNumberVerifyListener());

        final Label smtpTransportLabel = new Label(g, SWT.RIGHT); 
        smtpTransportLabel.setText("SMTP SSL/TLS:");
        smtpTransportLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        smtpTransportCombo = new Combo(g, SWT.READ_ONLY);
        smtpTransportCombo.setItems(connectionTypes);
        smtpTransportCombo.select(2);
        smtpTransportCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (smtpTransportCombo.getText().equals("STARTTLS"))
					smtpServerPort.setText("587");
				else if (smtpTransportCombo.getText().equals("TLS")) 
					smtpServerPort.setText("465");
				else if (smtpTransportCombo.getText().equals("Autodetect"))
					smtpServerPort.setText("");
				detailsVerified = false;
			}
			
		});
        
        if (outgoing != null) {
        	smtpServerField.setText(outgoing.getHostname() == null ? "" : outgoing.getHostname());
        	smtpServerPort.setText(Integer.toString(outgoing.getPort()));
        	switch(outgoing.getSocketType()) {
        		case STARTTLS:
        			smtpTransportCombo.select(1);
        			break;
        		case SSL:
        			smtpTransportCombo.select(0);
        			break;
			default:
				break;
        	}
        	
        }

        new Label(g, SWT.NONE);  /* Empty label in third column */
       
        /* Empty row */
        
        Label emptyRow = new Label(g, SWT.RIGHT);
        GridData emptyRowGridData = new GridData();
        emptyRowGridData.horizontalSpan = 3;
        emptyRowGridData.horizontalAlignment = GridData.FILL;
        emptyRow.setLayoutData(emptyRowGridData);
        
        loginField = new TextFieldWithLabel(g, "Username:", "Login username", false, "");
        if (incoming != null) {
        	loginField.setText(getUsernameByType(incoming.getUsernameType()));
        } else
        {
        	loginField.setText(addressField.getText());
        }
        
        new Label(g, SWT.NONE); /* Fill a column with empty widget to right align the button */
        
        new Label(g, SWT.NONE); /* Fill first column in next row to center align the button */

        final Button testButton = new Button(g, SWT.PUSH | SWT.RIGHT);
		testButton.setText("Test Server Configuration");
		testButton.addSelectionListener(new SelectionAdapter() {


			@Override
			public void widgetSelected(SelectionEvent e) {
				statusBar.setText("Probing servers, please wait..");
				progress.setVisible(true);
				SMTPServerConfigurationProbeTask smtpProbeTask;
				IMAPServerConfigurationProbeTask imapProbeTask;
				
				Color red = new Color(getShell().getDisplay(),238,207, 207);
				Color green = new Color(getShell().getDisplay(),224, 248, 226);


				SocketType smtpSocketType = null;
				SocketType imapSocketType = null;
					
					if (smtpTransportCombo.getText().equals("Autodetect")) {
						smtpProbeTask = new SMTPServerConfigurationProbeTask(smtpServerField.getText(), loginField.getText(),  passwordField.getText(), true);

					} else {
						
						if (smtpTransportCombo.getText().equals("STARTTLS")) 
							smtpSocketType = SocketType.STARTTLS;
						else if (smtpTransportCombo.getText().equals("TLS")) 
							smtpSocketType = SocketType.SSL;
						smtpProbeTask = new SMTPServerConfigurationProbeTask(smtpServerField.getText(), loginField.getText(),  passwordField.getText(), Integer.parseInt(smtpServerPort.getText()), smtpSocketType, true);
					}
					try {
						getContainer().run(true, true, smtpProbeTask);
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (imapTransportCombo.getText().equals("Autodetect")) {
						imapProbeTask = new IMAPServerConfigurationProbeTask(imapServerField.getText(), loginField.getText(), passwordField.getText(), true);
					} else {
						
						if (imapTransportCombo.getText().equals("STARTTLS")) 
							imapSocketType = SocketType.STARTTLS;
						else if (imapTransportCombo.getText().equals("TLS")) 
							imapSocketType = SocketType.SSL;
						
						imapProbeTask = new IMAPServerConfigurationProbeTask(imapServerField.getText(), loginField.getText(),  passwordField.getText(), Integer.parseInt(imapServerPort.getText()), imapSocketType, true);
					}
					try {
						getContainer().run(true, true, imapProbeTask);
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
									
				if (imapProbeTask.isSuccess() == true && smtpProbeTask.isSuccess() == true) {

					smtpServerPort.setText(Integer.toString(smtpProbeTask.getPort()));
					switch(smtpProbeTask.getSocketType()) {
						case STARTTLS:
							smtpTransportCombo.select(1);
							break;
						case SSL: 
							smtpTransportCombo.select(0);
							break;
						default:
					}
					imapServerPort.setText(Integer.toString(imapProbeTask.getPort()));
					switch(imapProbeTask.getSocketType()) {
						case STARTTLS:
							imapTransportCombo.select(1);
							break;
						case SSL: 
							imapTransportCombo.select(0);
							break;
						default:
					}


					statusBar.setBackground(green);
					statusBar.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));					
					statusBar.setText("Configuration test successful.");
					
					detailsVerified = true;
										
					c.layout(true);
					c.pack(true);
				}
				else {
	
					if (smtpProbeTask.isSuccess() == true && imapProbeTask.isSuccess() == false) {
						if (smtpProbeTask.getSocketType() == SocketType.STARTTLS) {
							smtpTransportCombo.select(1);
						} else if (smtpProbeTask.getSocketType() == SocketType.SSL) {
							smtpTransportCombo.select(0);
						}
						smtpServerPort.setText(Integer.toString(smtpProbeTask.getPort()));
						
						if (imapProbeTask.getSSLConnectSuccess() == true) {
							imapServerPort.setText(Integer.toString(imapProbeTask.getSSLport()));
							imapTransportCombo.select(0);
						} else if (imapProbeTask.getStartTLSConnectSuccess() == true) {
							imapServerPort.setText(Integer.toString(imapProbeTask.getStartTLSPort()));
							imapTransportCombo.select(1);
						}
						
						
						statusBar.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));
						statusBar.setBackground(red);
					
						statusBar.setText("STMP succeeded, "+imapProbeTask.getErrorMessage()+ ".");

					} else if (smtpProbeTask.isSuccess() == false && imapProbeTask.isSuccess() == true) {
						if (imapProbeTask.getSocketType() == SocketType.STARTTLS) {
							imapTransportCombo.select(1);
						} else if (imapProbeTask.getSocketType() == SocketType.SSL) {
							imapTransportCombo.select(0);
						}
						imapServerPort.setText(Integer.toString(imapProbeTask.getPort()));
						
						statusBar.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));
						statusBar.setBackground(red);
						statusBar.setText("IMAP succeeded, "+smtpProbeTask.getErrorMessage() + ".");
						
						if (smtpProbeTask.getSSLConnectSuccess() == true) {
							smtpServerPort.setText(Integer.toString(smtpProbeTask.getSSLport()));
							smtpTransportCombo.select(0);
						} else if (smtpProbeTask.getStartTLSConnectSuccess() == true) {
							smtpServerPort.setText(Integer.toString(smtpProbeTask.getStartTLSPort()));
							smtpTransportCombo.select(1);
						}
					} else if (smtpProbeTask.isSuccess() == false && imapProbeTask.isSuccess() == false) {
						if (smtpProbeTask.getSSLConnectSuccess() == true) {
							smtpServerPort.setText(Integer.toString(smtpProbeTask.getSSLport()));
							smtpTransportCombo.select(0);
						} else if (smtpProbeTask.getStartTLSConnectSuccess() == true) {
							smtpServerPort.setText(Integer.toString(smtpProbeTask.getStartTLSPort()));
							smtpTransportCombo.select(1);
						}
						
						if (imapProbeTask.getSSLConnectSuccess() == true) {
							imapServerPort.setText(Integer.toString(imapProbeTask.getSSLport()));
							imapTransportCombo.select(0);
						} else if (imapProbeTask.getStartTLSConnectSuccess() == true) {
							imapServerPort.setText(Integer.toString(imapProbeTask.getStartTLSPort()));
							imapTransportCombo.select(1);
						}

						statusBar.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));					
						statusBar.setBackground(red);
						
						
						statusBar.setText(smtpProbeTask.getErrorMessage() + ", " + imapProbeTask.getErrorMessage()+".");
					}
				
		
				}
				progress.setVisible(false);
			}
			
		});
		
        progress = new ProgressBar(g, SWT.INDETERMINATE);
        GridData progressGridData = new GridData();
        progressGridData.horizontalSpan = 3;
        progressGridData.horizontalAlignment = GridData.FILL;
        progress.setLayoutData(progressGridData);
        progress.setVisible(false);

		return g;
	}

	private void clearStatusBar() {
		if (statusBar != null && statusBar.isDisposed() != true) {
			statusBar.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			statusBar.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));		
			statusBar.setText("");
		}
	}
	
}
