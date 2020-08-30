/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;


public class TCPServer extends Thread{
	
	private ServerSocket tcpListenServer;
	
	TCPServer(int tcpPort) throws IOException {
		tcpListenServer = new ServerSocket(tcpPort);
	}
	
	
	@Override
	public void run() {
		while(true) {
			Socket newSocket = null;
			try {
				newSocket = tcpListenServer.accept();
			} catch (IOException e) {
				System.out.println("Failure at tcp socket creation");
				e.printStackTrace();
			}
			DataInputStream in = null;
			try {
				in = new DataInputStream(newSocket.getInputStream());
			} catch (IOException e) {
				System.out.println("Failure at initializing socket input stream");
				e.printStackTrace();
			} 
            DataOutputStream out = null;
			try {
				out = new DataOutputStream(newSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("Failure at initializing socket output stream");
				e.printStackTrace();
			}
            try {
				new TCPClient(newSocket, in, out).start();
			} catch (IOException e) {
				System.out.println("Failure at new TCPClient() call");
				e.printStackTrace();
			} 
		}
	}
}
