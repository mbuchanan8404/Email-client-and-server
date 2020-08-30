/* Matthew Buchanan
 * Project2
 * CS-447
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.SocketException; 


public class UDPServer extends Thread{
	private static final int BUFFER_SIZE = 512;
    private ArrayList<UDPClient> clientList = new ArrayList<UDPClient>();
	private DatagramSocket socket; 
	private int port;
    
    public UDPServer(int p) throws IOException, InterruptedException {
    	port = p;
    }
    
    @Override
    public void run() {
    	try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			listenLoop();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    
    
    /* Listen for incoming datagrams and start a thread to generate the appropriate client response */
    public void listenLoop() throws IOException, InterruptedException {
    	while(true) {
    	    byte[] inputBuffer = new byte[BUFFER_SIZE];
    		DatagramPacket newPacket = new DatagramPacket(inputBuffer, BUFFER_SIZE);
    		socket.receive(newPacket);
    		
    		String o = new String(newPacket.getData());
    		System.out.println("Received: " + o.trim());
    		
    		int userIndex = -1;
    		userIndex = checkClientListForClient(newPacket.getPort(), newPacket.getAddress());
    		if(userIndex >= 0) {
    			clientList.get(userIndex).messageHandler(newPacket); // Pass the message to the appropriate client
    		}
    		else { 
    			UDPClient newClient = new UDPClient(newPacket, socket); // Create new client and pass it the message
    			clientList.add(newClient);
    		}
    		
    	}
    }
    
    
    /* Check if the client is already in the list of active clients and purge finished clients */
    public int checkClientListForClient(int port, InetAddress addy) {
    	for(int i = 0; i < clientList.size(); i++) {
    		if(clientList.get(i).getClientState() == 4) { // Search list of active clients
    			clientList.remove(i);
    		}
    	}
    	for(int i = 0; i < clientList.size(); i++) {
    		if(clientList.get(i).getClientAddress().equals(addy)) { // Search list of active clients
    			return i;
    		}
    	}
    	return -1; // -1 signifies the client does not exist yet
    }
}
