/* Matthew Buchanan
 * Project2
 * CS-447
 */


import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;


/* Objects of this class are used to read/write emails and access/update the password file
 * User names and passwords are processed un-encoded internally and saved to file/transmitted with base64 encoding */
public class DatabaseManager {
	
	private File database;
	private File passwords;
	private File logFile;
	
	DatabaseManager() throws IOException{
		 database = new File("db");
		 database.mkdir();
		 passwords = new File("./db/.user_pass.txt");
		 if (!passwords.exists()) {
			 passwords.createNewFile();
		 }
	}
	
	
	/* Create a directory for a recipient */
	public boolean createUserDirectory(String user) {
		String fileName = user;
		String path = "./db/" + fileName;
		File userFile = new File(path);
		return userFile.mkdir();
	}
	
	
	/* Log server Activities */
	public void logServerActivity(String sourceIP, String destinationIP, String command, String message, String description) throws IOException {
		String logMessage = "";
		logMessage += getTimeStamp() + "  " + "from:" + sourceIP + "-to-" + destinationIP
				+ "  " + command + "  " + message + "  " + description + "\n";
		
		logFile = new File("./db/server_log.txt");
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
        bw.append(logMessage);
        bw.close();
	}
	
	
	/* Save email to the database folder in each recipient's sub-folder */
	public void saveEmail(String sender, String[] recipients, String body) throws IOException {
		for(int i = 0; i < recipients.length; i++) {
			String email = "";
			email += "Date:     " + getTimeStamp() + "\n";
			email += "From:     " + sender + "@cs447.edu\n";
			email += "To:       ";
			email += recipients[i] +"@cs447.edu";
			email += body + "\r\n.\r\n";
			String path = "./db/" + recipients[i];
			int count = new File(path).listFiles().length;
			path += "/" + recipients[i] + count + ".email";
			File f = new File(path);
			f.createNewFile();
			FileWriter out = new FileWriter(f);
			out.write(email);
			out.close();
		}
	}
	
	
	/* Retrieve email for client */
	public String retrieveEmail(String un, int num) throws FileNotFoundException {
		String path = "./db/" + un + "/" + un + String.valueOf(num) + ".email";
		File f = new File(path);
		Scanner in = new Scanner(f);
		String tmp = "";
		while(in.hasNextLine()) {
			tmp += in.nextLine() + "\n";
		}
		in.close();
		return tmp;
	}
	
	
	/* Get number of emails for user */
	public int getUserNumberOfEmails(String s) {
		int count = 0;
		String path = "./db/" + s + "/";
		File newfile = new File(path);
		if(newfile.exists()) {
			count = newfile.listFiles().length;
		}
		return count;
	}
	
	
	/* Get a time stamp for the email */ 
	public String getTimeStamp() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
		Date date = new Date();  
		return formatter.format(date);
	}
	
	
	/* Add the sender to the database in the password file */
	public boolean addUser(String un, String pw) {
		try {
	           BufferedWriter bw = new BufferedWriter(new FileWriter(passwords, true));
	           bw.append(un);
	           bw.newLine();
	           bw.append(pw);
	           bw.newLine();
	           bw.close();
	           return true;
	       } catch (IOException e) {
	           System.out.println(e.getMessage());
	    }
		return false;
	}
	
	
	/* Check if the sender is present in the database */
	public boolean userExists(String sender) throws FileNotFoundException {
		Scanner myScanner = new Scanner(new File("./db/.user_pass.txt"));
		String temp;
		while(myScanner.hasNextLine()) {
			temp = myScanner.nextLine();
			temp = new String(Base64.getDecoder().decode(temp));
			if (temp.equals(sender)) {
				System.out.println("User DOES Exist");
				myScanner.close();
				return true;
			}
			if(myScanner.hasNextLine()) {
				myScanner.nextLine();
			}
		}
		myScanner.close();
		return false;
	}
	
	
	/* Authenticate the user in the database checking the password file */
	public boolean authenticate(String un, String pw) throws FileNotFoundException {
		Scanner myScanner = new Scanner(new File("./db/.user_pass.txt"));
		String temp;
		while(myScanner.hasNextLine()) {
			temp = myScanner.nextLine();
			temp = new String(Base64.getDecoder().decode(temp));
			if (temp.equals(un)) {
				if(myScanner.hasNextLine()) {
					temp = myScanner.nextLine();
					temp = new String(Base64.getDecoder().decode(temp));
					int i = Integer.valueOf(temp);
					i -= 447;
					temp = String.valueOf(i);
				}
				else {
					myScanner.close();
					return false;
				}
				if(temp.equals(pw)) {
					myScanner.close();
					System.out.println("Sender Authenticated in database");
					return true;
				}
			}
		}
		myScanner.close();
		return false;
	}
	
	
	/* Add a new user to the database with a randomly generated password */
	public String registerUser(String un) {
		String password = "";
		String pw64 = "";
		Random rn = new Random();
		int tempPW = rn.nextInt(99552);
		password = String.valueOf(tempPW);
		password = new String(Base64.getEncoder().encodeToString(password.getBytes()));
		tempPW += 447;
		pw64 = String.valueOf(tempPW);
		pw64 = new String(Base64.getEncoder().encodeToString(pw64.getBytes()));
		String u = new String(Base64.getEncoder().encodeToString(un.getBytes()));
		addUser(u, pw64);
		return password;
	}
	
	
	/* Parse email address for white space and proper format, returning empty string means bad format */
	public String parseEmailAddress(String add) {
		String removeWhiteSpace = add.replaceAll(" ","");
		removeWhiteSpace = removeWhiteSpace.trim();
		String[] tmp = removeWhiteSpace.split("@");
		if(tmp.length != 2) {
			return "";
		}
		if(!tmp[1].equals("cs447.edu")) {
			return "";
		}
		return tmp[0];
	}
}
