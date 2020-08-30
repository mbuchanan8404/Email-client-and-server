/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.io.IOException;

public class Project2 {
	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
		if(args.length != 2) {
			System.out.println("Wrong number of command line arguments");
			return;
		}
		int tcpPort = Integer.valueOf(args[0]);
		int udpPort = Integer.valueOf(args[1]);
		TCPServer t = new TCPServer(tcpPort);
		t.start();
		UDPServer u = new UDPServer(udpPort);
		u.start();
		t.join();
		u.join();
		System.out.println("Exiting");
	}

}
