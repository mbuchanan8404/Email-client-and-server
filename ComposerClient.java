/* Matthew Buchanan
 * Project2
 * CS-447
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;


public class ComposerClient extends Thread {
	
	private Socket sock;
	private DataInputStream userInput;
	private DataInputStream in;
	private DataOutputStream out;
	private String userName;
	private String pw;
	private InetAddress address;
	private int port;
	
	ComposerClient(String a, int p) throws IOException {
		userInput = new DataInputStream(System.in);
		address = InetAddress.getByName(a);
		System.out.println(address);
		port = p;
		System.out.println(port);
	}
	
	@Override
	public void run() {
		try {
			connectToServer(address, port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//userAddress = sock.getLocalSocketAddress().toString();
		try {
			compose(address, port);
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/* Perform connection steps and call functions to Compose Email */
	public void compose(InetAddress serverAddress, int serverPort) throws IOException, InterruptedException {
		login(serverAddress, serverPort);	
		initiateMailTransfer();
		getEmailHeaderFromUser();
		getEmailBodyFromUser();
		disconnectFromServer();
	}
	
	
	/* Perform login steps */
	public void login(InetAddress serverAddress, int serverPort) throws IOException, InterruptedException {
		String serverResponse = "";
		userName = getUserName();
		serverResponse = authenticateAtServer();
		while(serverResponse.equals("535 5.7.8 Authentication credentitals invalid")) {
			System.out.println("Email address invalid. Make sure your email is your username@****.edu\n");
			userName = getUserName();
			serverResponse = authenticateAtServer();
		}
		
		/* New user, password is displayed for user and connection reset */
		if (serverResponse.substring(0, 3).equals("330")) {
			pw = (String) serverResponse.subSequence(26, serverResponse.length());
			pw = new String(Base64.getDecoder().decode(pw));
			System.out.println("\n\nYOUR PASSWORD IS: " + pw + "\n\n");
			String tm = in.readUTF();
			disconnectFromServer();
			Thread.sleep(5000);
			connectToServer(serverAddress, serverPort);
			serverResponse = authenticateAtServer();

		}
		
		/* User is registered and ready to input password */
		if (serverResponse.equals("334 cGFzc3dvcmQ6")){
			Boolean loggedIn = false;
			while(!loggedIn) {
				String temp = getPasswordFromUser();
				temp = Base64.getEncoder().encodeToString(temp.getBytes());
				out.writeUTF(temp);
				serverResponse = in.readUTF();
				System.out.println(serverResponse);
				if(serverResponse.equals("235 2.7.0 Authentication Succeeded")) {
					loggedIn = true;
				}
				else {
					System.out.println("Please try again");
					serverResponse = authenticateAtServer();
				}
			}
		}
	}
	
	
	/* Tell server to ready for mail from this user */
	public void initiateMailTransfer() throws IOException {
		String temp = "MAIL FROM:" + userName;
		out.writeUTF(temp);
		String sResponse = "";
		sResponse = in.readUTF();
		System.out.println(sResponse);
	}
	
	
	/* Get the recipients from the user */
	public void getEmailHeaderFromUser() throws IOException {
		String temp = "RCPT TO:";
		System.out.println("Enter your recipients names, seperated by a comma.");
		temp += userInput.readLine();
		out.writeUTF(temp);
		String sResponse = in.readUTF();
		while(sResponse.equals("510 bad email address for one, or more, recipients. Make sure recipients have registered")) {
			System.out.println(sResponse);
			temp = "RCPT TO:";
			System.out.println("Enter your recipients names, seperated by a comma.");
			temp += userInput.readLine();
			out.writeUTF(temp);
			sResponse = in.readUTF();
		}
		System.out.println(sResponse);
	}
	
	
	/* Gets the body of the mail from user input, add final period when user enters 'SEND' */
	public void getEmailBodyFromUser() throws IOException {
		String temp = "";
		String endOfEmailToken = "\r\n.\r\n";
		Boolean sendEmail = false;
		
		out.writeUTF("DATA:");
		in.readUTF();
		
		System.out.println("SUBJECT: ");
		String tmp = "";
		tmp = userInput.readLine();
		if(!tmp.equals("")) {
			out.writeUTF("\nSubject: " + tmp);
		}
		out.writeUTF("\n\n");
		System.out.println("Enter your email one line at a time. Enter SEND on its own line to send the email");
		while (!sendEmail) {
			temp = userInput.readLine() + "\n";

			if(temp.equals("SEND\n")) {
				sendEmail = true;
			}
			out.writeUTF(temp);
		}
		String t = in.readUTF();
		System.out.println(t);
	}
	
	
	/* Get the user's password */
	public String getPasswordFromUser() throws IOException {
		System.out.println("Please enter your password:");
		String temp = "";
		while(temp.equals("")) {
			temp = userInput.readLine();
		}
		return temp;
	}
	
	
	/* Disconnect from the server */
	public void disconnectFromServer() throws IOException {
		out.writeUTF("QUIT");
		System.out.println(in.readUTF());
		in.close();
		out.close();
		sock.close();
	}
	
	
	/* Ready the server to accept user's password */
	public String authenticateAtServer() throws IOException {
		String temp = "";
		out.writeUTF("AUTH");
		System.out.println(in.readUTF());
		String tmp = userName;
		tmp = Base64.getEncoder().encodeToString(tmp.getBytes());
		out.writeUTF(tmp);
		temp = in.readUTF();
		System.out.println(temp);
		return temp;
	}
	
	
	/* Establish a connection to the email server's socket */
	public void connectToServer(InetAddress a, int p) throws UnknownHostException, IOException {
		sock = new Socket(a, p); 
	    System.out.println("Connected");
	    in = new DataInputStream(sock.getInputStream());
	    out = new DataOutputStream(sock.getOutputStream()); 
		String temp = "";
		temp = in.readUTF();
		System.out.println(temp);
		out.writeUTF("HELO");
		temp = in.readUTF();
		System.out.println(temp);
	}
	
	
	/* Get username from user */
	public String getUserName() throws IOException {
		String temp = "";
        System.out.println("Please enter your email Address");
        temp = userInput.readLine();
        return temp;
	}
}
