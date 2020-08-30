/* Matthew Buchanan
 * Project2
 * CS-447
 */

import java.io.DataInputStream;
import java.io.IOException;

public class Project2Client {

	public static void main(String[] args) throws IOException, InterruptedException {
		if(args.length != 3) {
			System.out.println("Wrong number of command line arguments");
			return;
		}
		String tcpServerAddress = args[0];
		int tcpServerPort = Integer.valueOf(args[1]);
		String udpServerAddress = args[0];
		int udpServerPort = Integer.valueOf(args[2]);
		
		DataInputStream userInput = new DataInputStream(System.in);
		System.out.println("Compose, Receive, or Quit?[C/R/Q]");
		String userChoice = userInput.readLine();
		while(!userChoice.equals("Q")) {
			while(!userChoice.equals("C") && !userChoice.equals("R") && !userChoice.equals("Q")) {
				System.out.println("Compose, Receive, or Quit?[C/R/Q]");
				userChoice = userInput.readLine();
			}
			if(userChoice.equals("C")) {
				ComposerClient cc = new ComposerClient(tcpServerAddress, tcpServerPort);
				cc.start();
				cc.join();
			}
			else if (userChoice.equals("R")) {
				ReceiverClient rc = new ReceiverClient(udpServerAddress, udpServerPort);
				rc.start();
				rc.join();
			}
			System.out.println("Compose, Receive, or Quit?[C/R/Q]");
			userChoice = userInput.readLine();
		}
		System.out.println("EXITING");
	}

}
