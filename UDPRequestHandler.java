/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Base64;


public class UDPRequestHandler extends Thread {
	
	private DatabaseManager db;
	private UDPClient client;
	private DatagramPacket response;
	private byte[] outputBuffer;
	
	
	UDPRequestHandler(UDPClient c) throws IOException, InterruptedException {
		client = c;
	}
	
	
	@Override
	public void run() {
		try {
			handleRequest();
		} catch (IOException | InterruptedException e) {
			System.out.println(e);
		}
		return;
	}
	
	
	/* Parse a logged in client's messages and call appropriate handlers */
	public void handleRequest() throws IOException, InterruptedException {
		db = new DatabaseManager();
		constructDatagramPacket();
		if(client.getClientState() == 0 || client.getClientState() == 1 || client.getClientState() == 2) {
			clientLogin();
		}
		else if (client.getClientState() == 3){
			int numberOfEmails = parseGETRequest();
			String httpResponse;
			if(numberOfEmails > db.getUserNumberOfEmails(client.getClientUserName())) { // Client asked for more emails than they have in the db
				httpResponse = "404 Not Found: You have " + String.valueOf(db.getUserNumberOfEmails(client.getClientUserName()) + " emails\r\n");
				response.setData(httpResponse.getBytes());
				sendResponse();
			}
			else if(numberOfEmails >= 0) { // Client asked for appropriate number of emails 
				for(int i = 0; i < numberOfEmails; i++) {
					String r = createHTTPResponse(numberOfEmails, i);
					response.setData(r.getBytes());
					sendResponse();
					db.logServerActivity(client.getSocket().getLocalAddress().getHostAddress(),
							client.getClientAddress().getHostAddress(), "HTTP-GET", "HTTP/1.1 OK", "Email Sent");
				}
				client.setClientState(4); // Done with email transfer;
				return;
			}
			else if (numberOfEmails < 0){ // Error in the format of the HTTP GET request itself
				httpResponse = "400 bad request: HTTP request has invalid format";
				response.setData(httpResponse.getBytes());
				sendResponse();
			}
		}
	}
	
	
	/* Construct datagram packet */
	public void constructDatagramPacket() {
		outputBuffer = new byte[512];
		response = new DatagramPacket(outputBuffer, outputBuffer.length, client.getClientAddress(), client.getClientPort());
	}
	
	
	/* Perform login for client */
	public void clientLogin() throws IOException {
		/* Client issued AUTH */
		if(client.getDecodedClientMessage().equals("AUTH") && client.getClientState() == 0) {
			handleAuth();
		}
		
		/* Retrieve and validate client username, or register the new client */
		else if(client.getClientState() == 1) {
			String tmp = client.getDecodedClientMessage();
			tmp = new String(Base64.getDecoder().decode(tmp));
			String un = db.parseEmailAddress(tmp);
			if(un.equals("")) { // Invalid email address format
				response.setData("535 5.7.8 Authentication credentitals invalid".getBytes());
				sendResponse();
				client.setClientState(0);
			}
			else {
				client.setClientUsername(un);
				client.setClientState(2);
				/* Register a new client */
				if(!db.userExists(client.getClientUserName())) {
					String newpw = db.registerUser(client.getClientUserName());
					response.setData(("330 your new password is: " + newpw).getBytes());
					client.setClientState(0);
				}
				else {
					response.setData("334 cGFzc3dvcmQ6".getBytes());
				}
				sendResponse();
			}
		}
		
		/* Retrieve and validate client password */
		else if(client.getClientState() == 2 && db.userExists(client.getClientUserName())) {
			String temp = client.getDecodedClientMessage();
			temp = new String(Base64.getDecoder().decode(temp));
			if(db.authenticate(client.getClientUserName(), temp)) {
				client.setClientState(3); // client logged into server
				client.setPassword(temp);
				response.setData("235 2.7.0 Authentication Succeeded".getBytes());
			}
			else {
				response.setData("535 5.7.8 Authentication credentitals invalid".getBytes());
				client.setClientState(0); // client must re-enter credentials
			}
			sendResponse();
		}
	}
	
	
	/* Handle AUTH command from user */
	public void handleAuth() throws IOException {
		response.setData("334 dXNlcm5hbWU6".getBytes());
		sendResponse();
		client.setClientState(1);
	}
	
	
	/* Parse HTTP request from user and return informative values about errors in the request */
	public int parseGETRequest() {
		String[] headers = client.getDecodedClientMessage().split("\r\n");
		if(headers.length < 3) {
			return -1; // Not the right number of header lines in the header portion
		}
		
		String[] firstHeader = headers[0].split(" ");
		if(firstHeader.length != 3 || !firstHeader[0].equals("GET") || !firstHeader[2].equals("HTTP/1.1")) {
			return -2; // First header line does not have proper format
		}
		
		String[] secondHeader = headers[1].split(" ");
		if(secondHeader.length != 2 || !secondHeader[0].equals("Host:")) {
			return -3; // Second header line does not have proper format
		}
		
		String[] thirdHeader = headers[2].split(" ");
		if(thirdHeader.length != 2 || !thirdHeader[0].equals("Count:")) {
			return -4; // Third header line does not have proper format
		}
		
		int count = Integer.valueOf(thirdHeader[1]);
		if(count >= 0) {
			return count; // count is a valid integer
		}
		else {
			return -5; // invalid or corrupt email count number
		}
	}
	
	
	/* Construct HTTP response */
	public String createHTTPResponse(int count, int mn) throws FileNotFoundException {
		String httpResponse;
		httpResponse = "HTTP/1.1 200 OK\r\n";
		httpResponse += "Server: " + response.getAddress().getHostAddress() + "\r\n";
		httpResponse += "Last-Modified: " + db.getTimeStamp() + "\r\n";
		httpResponse += "Count: " + count + "\r\n";
		httpResponse += "Content-Type: text/plain\r\n";
		httpResponse += "Message: " + mn + "\r\n\r\n";
		httpResponse += db.retrieveEmail(client.getClientUserName(), mn);
		return httpResponse;
	}	
	
	
	/* Send the Server's response to the client */
	public void sendResponse() throws IOException {
		client.getSocket().send(response);
		String tmp = new String(response.getData());
		System.out.println("Sent: " + tmp + "  to: " + response.getAddress().getHostAddress() + " " + response.getPort());
	}
	
}