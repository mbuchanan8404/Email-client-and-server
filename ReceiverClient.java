/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;


public class ReceiverClient extends Thread {
	
	private InetAddress address;
	private DataInputStream userInput;
	private int port;
	private DatagramPacket serverResponse;
	private DatagramSocket sock;
	private DatagramPacket packet;
	private String username;
	private String pw;
	private String count;
	private String[] emails;
	
	
	ReceiverClient(String address, int port) throws IOException, InterruptedException {
		userInput = new DataInputStream(System.in);
		username = "";
		pw = "";
		count = "";
		this.address = InetAddress.getByName(address);
		this.port = port;
		byte[] buffer = new byte[512];
		sock = new DatagramSocket();
		packet = new DatagramPacket(buffer, buffer.length, this.address, this.port);
	}
	
	@Override
	public void run() {
		try {
			login();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Boolean tmp = false;
			while(!tmp) {
				tmp = getEmail();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/* Save email(s) to user system */
	public void saveEmail(String m, int c) throws IOException {
		String path = "./" + username.split("@")[0] + "/";
		path += username.split("@")[0] + c + ".txt";
		File f = new File(path);
		f.createNewFile();
		FileWriter out = new FileWriter(f);
		out.write(m);
		out.close();
	}
	
	
	/* Retreive email(s) from server */
	public boolean getEmail() throws IOException {
		System.out.println("How many emails to download?");
		count = userInput.readLine();
		String HTTPRequest = "GET db/" + username.split("@")[0] + "/ HTTP/1.1\r\n";
		HTTPRequest += "Host: " + address.getHostAddress() + "\r\n";
		HTTPRequest += "Count: " + String.valueOf(count) + "\r\n\r\n";		
		packet.setData(HTTPRequest.getBytes());
		sock.send(packet);
		String decodedserverResponse = "";
		emails = new String[Integer.valueOf(count)];
		for(int i = 0; i < Integer.valueOf(count); i++) { 
			byte[] buffer = new byte[512];
			serverResponse = new DatagramPacket(buffer, buffer.length);
			sock.receive(serverResponse);
			decodedserverResponse = new String(serverResponse.getData());
			decodedserverResponse = decodedserverResponse.trim();
			if(decodedserverResponse.subSequence(0, 3).equals("404")) {
				System.out.println(decodedserverResponse);
				return false;
			}
			emails[i] = decodedserverResponse;
		}
		System.out.println("EMAILS:\n\n");
		for(int i = emails.length - 1; i >= 0; i--) {
			System.out.println(emails[i] + "\n\n\n");
			saveEmail(emails[i], i);
		}
		return true;
	}
	
	
	/* Login to server */
	public void login() throws IOException, InterruptedException {
		auth();
		getusername();
		String tmp = sendUserName();
		while(tmp.equals("535 5.7.8 Authentication credentitals invalid")){
			auth();
			getusername();
			tmp = sendUserName();
			System.out.println(tmp);
		}
		if(tmp.substring(0, 3).equals("330")) {
			pw = tmp.substring(26, tmp.length());
			pw = new String(Base64.getDecoder().decode(pw));
			System.out.println("\n\nYOUR PASSWORD IS: " + pw + "\n\n");
			auth();
			getusername();
			tmp = sendUserName();
		}
		if(tmp.equals("334 cGFzc3dvcmQ6")) {
			getPassWord();
			tmp = sendPassWord();
		}
		while(tmp.equals("535 5.7.8 Authentication credentitals invalid")){
			auth();
			getusername();
			tmp = sendUserName();
			if(!tmp.equals("535 5.7.8 Authentication credentitals invalid")) {
				getPassWord();
				tmp = sendPassWord();
			}
			System.out.println(tmp);
		}
		System.out.println("LoggedIn");
		
	}
	

	/* Send user pass to server */ 
	public String sendPassWord() throws IOException {
		String tmp = pw;
		tmp = new String(Base64.getEncoder().encodeToString(pw.getBytes()));
		packet.setData(tmp.getBytes());
		sock.send(packet);
		byte[] buffer = new byte[512];
		serverResponse = new DatagramPacket(buffer, buffer.length);
		String decodedserverResponse = "";
		sock.receive(serverResponse);
		decodedserverResponse = new String(serverResponse.getData());
		decodedserverResponse = decodedserverResponse.trim();
		System.out.println(decodedserverResponse);
		return decodedserverResponse;
	}
	
	
	/* Get user pass from user input */
	public void getPassWord() throws IOException {
		System.out.println("Please enter your password");
		pw = userInput.readLine();
	}
	
	
	/* Get user name from user input */
	public void getusername() throws IOException {
		System.out.println("Please enter your email address");
		username = userInput.readLine();
	}
	
	
	/* Send user name to server */
	public String sendUserName() throws IOException {
		String tmp = username;
		tmp = new String(Base64.getEncoder().encodeToString(tmp.getBytes()));
		packet.setData(tmp.getBytes());
		sock.send(packet);
		byte[] buffer = new byte[512];
		serverResponse = new DatagramPacket(buffer, buffer.length);
		String decodedserverResponse = "";
		sock.receive(serverResponse);
		decodedserverResponse = new String(serverResponse.getData());
		decodedserverResponse = decodedserverResponse.trim();
		return decodedserverResponse;
	}
	
	
	/* Send AUTH command to server */
	public void auth() throws IOException {
		packet.setData("AUTH".getBytes());
		sock.send(packet);
		byte[] buffer = new byte[512];
		serverResponse = new DatagramPacket(buffer, buffer.length);
		String decodedserverResponse = "";
		sock.receive(serverResponse);
		decodedserverResponse = new String(serverResponse.getData());
		decodedserverResponse = decodedserverResponse.trim();
		System.out.println(decodedserverResponse);
	}
}
