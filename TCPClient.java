/* Matthew Buchanan
 * Project2
 * CS-447
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.Base64;


/* Objects of this class are each a thread, representing a single client on a tcp connection, sending email via SMTP */
public class TCPClient extends Thread {
	
	private String[] smtpCommands;
	private DatabaseManager dbManager;
	private boolean userAuthenticated;
	private String senderName;
	private String senderAddress;
	private String[] recipients;
	private String emailBody;
	private int clientState;
	private String clientMessage;
	private String serverResponse;
	private boolean validMessage;
	private Socket client;
	private DataInputStream in;
	private DataOutputStream out;
	
	TCPClient(Socket c, DataInputStream i, DataOutputStream o) throws IOException {
		client = c;
		in = i;
		out = o;
	}
	
	@Override
	public void run() {
		try {
			connectionManager(client, in, out);
		}
		catch(IOException e) {
			System.out.println(e);
			return;
		}		
	}
	
	
	/* Manage the connection to the sender, checking for correct input and sending appropriate codes/replys */
	public void connectionManager(Socket client, DataInputStream in, DataOutputStream out) throws IOException {	
		dbManager = new DatabaseManager();
		userAuthenticated = false;
		senderAddress = client.getRemoteSocketAddress().toString();
		emailBody = "";
		senderName = "";
		clientState = 0; // Client's state increments as they progress through the smtp transfer
		
		/* Client SMTP commands */
		smtpCommands = new String[6];
		smtpCommands[0] = "HELO";
		smtpCommands[1] = "AUTH";
		smtpCommands[2] = "MAIL FROM:";
		smtpCommands[3] = "RCPT TO:";
		smtpCommands[4] = "DATA";
		smtpCommands[5] = "QUIT";
		
		out.writeUTF("220 service ready"); // notify client service is ready to use
		
		/* Loop to retrieve a single message from client and select appropriate response */
		while(true) {
			
			if(!getClientMessage(client, in)) {
				return; // client connection has reset, nothing to do but end thread
			}
			serverResponse = "";
			validMessage = false;
			validMessage = parseClientMessage();			
			
			/* START of loop to track state of client and compare to the client's confirmed valid message */
			if(validMessage && clientState != 5) {
				
				/* First user state, user just connected and sent HELO, send reply code 250 or 503 */
				if(clientMessage.substring(0, 4).equals("HELO")) {
					handleHELO();
				}
				
				/* Client is attempting to login by issuing AUTH command, check username and password */
				else if(clientMessage.substring(0, 4).equals("AUTH")) {
					handleAUTH(client, in, out);
				}
				
				/* Client issued MAIL FROM: command, check credentials against from field */
				else if(clientMessage.length() >= 10 && clientMessage.substring(0, 10).equals("MAIL FROM:")) {
					handleMAILFROM();					
				}
				
				/* Client issued RCPT TO: command, parse recipients for proper format ie: username@cs447.edu */
				else if(clientMessage.length() >= 8 && clientMessage.substring(0, 8).equals("RCPT TO:")) {
					handleRCPTTO();
				}
				
				/* Client issued DATA command, switch to email compose state */
				else if(clientMessage.substring(0, 4).equals("DATA")) {
					handleDATA();
				}
				
				/* Client issued QUIT command, disconnect client and close this connection and thread */
				else if(clientMessage.substring(0, 4).equals("QUIT")) {
					disconnect(client, in, out);
					return; // disconnect client and terminate this thread
				}
			}
			
			/* Client is sending email body */
			else if(clientState == 5 && !validMessage) {
				composeEmail();			
			}
			
			/* Client command is not recognized */
			else {
				if (!validMessage && clientState != 5){
					serverResponse = "500 bad syntax error";
				}
			}
			
			/* Finally send the message */
			if(!serverResponse.equals("")) {
				try {
					out.writeUTF(serverResponse);
				}
				catch(IOException i){
					System.out.println(i);
					return;
				}
			}
		}
	}
		
	
	/* Get client message from socket */
	public boolean getClientMessage(Socket client, DataInputStream in) {
		serverResponse = "";
		/* Retrieve client message and print it */
		try {
			clientMessage = in.readUTF();
			return true;
		} 
		catch (IOException e) {
			System.out.println("Client connection has reset");
			return false;
		}
	}
	
	
	/* Check client message against list of appropriate smtp commands */
	public boolean parseClientMessage() {
		if(!clientMessage.equals("")) {
			System.out.println(clientMessage);
			//System.out.println(clientState); // print client state
		}
		if(clientMessage.length() >= 4) {
			for(int i = 0; i < smtpCommands.length; i++) {
				if(smtpCommands[i].substring(0, 4).equals(clientMessage.substring(0, 4))) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	/* Handle HELO command from user */
	public void handleHELO() {
		if(clientMessage.substring(0, 4).equals("HELO")) {
			if(clientState == 0) {
				serverResponse = "250 Hello " + senderAddress + " pleased to meet you";
				clientState = 1;
			}
			else serverResponse = "503 bad commmand sequence";
		}
	}
	
	
	/* Handle AUTH command from user */
	public void handleAUTH(Socket client, DataInputStream in, DataOutputStream out) throws IOException {
		if(clientState == 1 && !userAuthenticated) {
			if(authenticateUser(client, in, out)) {
				serverResponse = "235 2.7.0 Authentication Succeeded";
				clientState = 2;
			}
			else {
				serverResponse = "535 5.7.8 Authentication credentitals invalid";
			}
		}
		else { 
			serverResponse = "503 bad commmand sequence";
		}
	}
	
	
	/* Handle MAIL FROM command from user */
	public void handleMAILFROM() {
		if(clientState == 2) {
			String[] tmp = clientMessage.substring(10, clientMessage.length()).split("@");
			if(senderName.equals(tmp[0])) {
				serverResponse = "250 ok sender name matches from field";
				clientState = 3;
			}
			else {
				serverResponse = "550 Sender's user id and from feild do not match!";
			}
		}
		else { 
			serverResponse = "503 bad commmand sequence";
		}
	}
	
	
	/* Handle RCPT TO command from user */
	public void handleRCPTTO() throws FileNotFoundException {
		if(clientState == 3) {
			Boolean validRecipients = true;
			clientMessage = clientMessage.replace(" ", "");
			recipients = clientMessage.substring(7, clientMessage.length()).split(",");
			for(int i = 0; i < recipients.length; i++) {
				String[] tmp = recipients[i].split("@");
				if(tmp.length < 2 || !tmp[1].equals("cs447.edu") || !dbManager.userExists(tmp[0])) {
					validRecipients = false;
					break;
				}
				recipients[i] = tmp[0];
			}
			/* Create directory for valid (registered) recipients */
			if(validRecipients) {
				for(int i = 0; i < recipients.length; i++) {
					dbManager.createUserDirectory(recipients[i]);
				}
				serverResponse = "250 recipients ok";
				clientState = 4;
			}
			else {
				serverResponse = "510 bad email address for one, or more, recipients. Make sure recipients have registered";
			}
		}
		else {
			serverResponse = "503 bad commmand sequence";
		}
	}
	
	
	/* Handle DATA command from user */
	public void handleDATA() {
		if(clientState == 4) {
			serverResponse = "354 enter email, end with ' . ' on a line by itself";
			clientState = 5;
		}
		else {
			serverResponse = "503 bad commmand sequence";
		}
	}
	
	/* Collect the email body from client */
	public void composeEmail() throws IOException {	
		if(clientMessage.equals("SEND\n")) {
			serverResponse = "250 message accepted for delivery";
			dbManager.saveEmail(senderName, recipients, emailBody);
			dbManager.logServerActivity(senderAddress, client.getLocalAddress().getHostAddress(),
					"SMTP-DATA", "250 message accepted for delivery", "Email-Recieved");
			emailBody = "";
			validMessage = true;
			clientState = 2;
			for(int i = 0; i < recipients.length; i++) {
				recipients[i] = "";
			}
		}
		else {
			emailBody += clientMessage;
		}
	}
	
	
	/* Issue closing code and close the connection*/
	public void disconnect(Socket client, DataInputStream in, DataOutputStream out) throws IOException {
		System.out.println("Client disconnecting");
		serverResponse = "221 closing service";
		out.writeUTF(serverResponse);
		try {
			Thread.sleep(1000); // give the socket time to transmit the closing code to client before closing socket
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
		out.close();
		client.close();
	}
	
	
	/* Authenticate the sender by checking their username and
	 * password against the database password file, registering them if needed */
	public boolean authenticateUser(Socket client, DataInputStream in, DataOutputStream out) throws IOException {
		String userName = "";
		String pw = "";
		out.writeUTF("334 dXNlcm5hbWU6");
		try {
			userName = in.readUTF();
			userName = new String(Base64.getDecoder().decode(userName));
		} 
		catch (IOException e) {
			System.out.println(e);
			return false;
		}
		
		userName = dbManager.parseEmailAddress(userName);
		if(userName.equals("")) {
			return false;
		}
		
		if(dbManager.userExists(userName)) {
			out.writeUTF("334 cGFzc3dvcmQ6");
			pw = in.readUTF();
			pw = new String(Base64.getDecoder().decode(pw));
			if(dbManager.authenticate(userName, pw)) {
				userAuthenticated = true;
				senderName = userName;
				return true;
			}
			else  {
				return false;
			}
		}
		else { // new user
			String temp = dbManager.registerUser(userName);
			senderName = userName;
			out.writeUTF("330 your new password is: " + temp);
		}
		return false;
	}
	
}