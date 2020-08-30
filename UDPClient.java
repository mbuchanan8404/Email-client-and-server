/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;


/* Objects of this class represent a client trying to receive email */
public class UDPClient {
	
	private int state; // 0 == unknown to server, 1 == AUTH received, 2 == logging in, 3 == logged in, 4 == finished downloading
	private DatagramPacket clientMessage;
	private String username;
	private String pw;
	private String decodedClientMessage;	
	private InetAddress clientAddress;
	private int clientPort;
	private DatagramSocket socket;
	
	
	/* Constructor*/
	UDPClient(DatagramPacket newPacket, DatagramSocket s) throws IOException, InterruptedException {
		socket = s;
		state = 0;
		messageHandler(newPacket);
	}
	
	
	/* Create a new thread to handle client message processing and return to listen loop*/
	public void messageHandler(DatagramPacket newPacket) throws IOException, InterruptedException {
		clientMessage = newPacket;
		clientAddress = newPacket.getAddress();
		clientPort = newPacket.getPort();
		decodedClientMessage = new String(newPacket.getData());
		new UDPRequestHandler(this).start();
	}
	
	
	/* Get socket */
	public DatagramSocket getSocket() {
		return this.socket;
	}
	
	
	/* Set password */
	public void setPassword(String s) {
		pw = s;
	}
	
	
	/* Get password */
	public String getPassword() {
		return pw;
	}
	
	
	/* Set client port */
	public void setClientPort(int p) {
		clientPort = p;
	}
	
	
	/* Get client port */
	public int getClientPort() {
		return clientPort;
	}
	
	
	/* Set client data */
	public void setClientData(String d) {
		clientMessage.setData(d.getBytes());
	}
	
	
	/* Get client data */
	public byte[] getClientData() {
		return clientMessage.getData();
	}
	
	
	/*  Get client user name */
	public String getClientUserName(){
		return username;
	}
	
	
	/* Set client user name */
	public void setClientUsername(String u) {
		username = u;
	}
	
	
	/* Get client state */
	public int getClientState() {
		return state;
	}
	
	
	/* Set client state */
	public void setClientState(int newState) {
		state = newState;
	}
	
	
	/* Set client message */
	public void setClientMessage(String s) {
		clientMessage.setData(s.getBytes());
	}
	
	
	/* Get raw client message */
	public DatagramPacket getRawClientMessage() {
		return clientMessage;
	}
	
	
	/* Get decodedClientMessage */
	public String getDecodedClientMessage() {
		return decodedClientMessage.trim();
	}


	public InetAddress getClientAddress() {
		return clientAddress;
	}


	public void setClientAddress(InetAddress clientAddress) {
		this.clientAddress = clientAddress;
	}
}
