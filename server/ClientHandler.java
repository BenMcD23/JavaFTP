import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;


public class ClientHandler extends Thread {
	// OPTIONS
	public static final int LIST = 1;
	public static final int PUT = 2;
	// this is when put is the option selected, but the local file (client side) cant be opened
	public static final int INVALID_PUT = 3;

	public static final int NO_FILE = 4;
	public static final int FILE_EXISTS = 5;

	private String fileName;

	private Socket socket = null;
	private DataInputStream in = null;
	private DataOutputStream out = null;

	// initialise serverFiles obj
	File serverFiles = new File("serverFiles");

	// constructor
	public ClientHandler(Socket socket) {
		// set socket
		this.socket = socket;
		// try and open data streams for in and out
		try{
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputStream(socket.getOutputStream());
		}
		// catch if cant
		catch (IOException e) {
			e.printStackTrace();
		}

		try {
			// run get option, either returns LIST(1) or PUT(2)
			// (done in this way so getOption can be reused without running other funcs)
			// (and testFileExist)
			int option = getOption();

			// log the connection at this point, is a sucessfull conncection
			logConnection(option);

			// if the chosen option is LIST, then run listFiles
			if (option == LIST) {
				listFiles();
			}

			// if the option is PUT(2), then
			else if (option == PUT) {
				// run test file exists
				int fileExist = testFileExist(fileName);

				// if there is no file with the name then it returns NO_FILE(4),
				if (fileExist == NO_FILE) {
					// write this so when on client side we know to run the file upload
					out.writeUTF("noFile");
					// run so we can recieve the file
					recieveFile(fileName);
				}

				// if there is a file with the name then it returns FILE_EXISTS(5),
				else if (fileExist == FILE_EXISTS) {
					// output error message to client
					out.writeUTF("Error: Cannot upload file '" + fileName + "'; already exists on server.");
				}
			}

			else {
			// shouldnt get to this point as client side validation shouldnt allow
			// would put server side error message here but not sure if it messes up autograder
			}

			// free up input, ouput and socket
			out.close();
			in.close();
			socket.close();
		}

		catch (IOException e) {
		e.printStackTrace();
		}
	}

	// gets if the user has chose list or put
	public int getOption() throws IOException {
		// read input
		String option = in.readUTF();

		// if the input is list then return LIST(1)
		if (option.equals("list")) {
			return LIST;
		}
		// if the input is put, then
		else if (option.equals("put")) {
			// read again, this is the fileName sent from client
			fileName = in.readUTF();
			// return PUT(2)
		return PUT;
		}

		// if input is local error, then it was a put with an invalid local file
		else if (option.equals("localError")) {
			// return INVALID_PUT(3), this is so the log file still shows the connection as "put"
			return INVALID_PUT;
		}

		return 0;
	}

	// lists all the files to the client
	public void listFiles() throws IOException {
		// list all the files in the serverFiles folder
		File[] listOfFiles = serverFiles.listFiles();
		// string that is going to be written out
		String fileString = ("Listing " + listOfFiles.length + " file(s):");

		// add all the file names to string
		for (int i = 0; i < listOfFiles.length; i++) {
			fileString += "\n" + listOfFiles[i].getName();
		}

		// write out to client
		out.writeUTF(fileString);
		}

	// testing if the file exists in the server files, if it doesnt then can recieve the file
	public int testFileExist(String fileName) throws IOException {
		// get path object of given path, using client file name
		Path path = Paths.get("serverFiles/" + fileName);

		// if the file exists, then return FILE_EXISTS(5)
		if (Files.exists(path)) {
			return FILE_EXISTS;
		}

		// if the file doesnt exist, then return NO_FILE(4)
		else {
			return NO_FILE;
		}
	}

	// receives a text file from client and saves it to file in serverFiles
	public void recieveFile(String fileName) throws IOException {
		// set buffer and read in to buffer from client
		byte[] buffer = new byte[65482];
		int bytes = in.read(buffer,0,buffer.length);

		// write the buffer to a file with the given file name
		FileOutputStream fileOutputStream = new FileOutputStream("serverFiles/" + fileName);

		// done try and catch as there is an error if the text file is empty
		try {
			fileOutputStream.write(buffer,0,bytes);
		}
		catch (IndexOutOfBoundsException e) {
			// do nothing
		}
	}

	// logs each connection to log.txt
	public void logConnection(int option) throws IOException {
		// converts number options into text
		String request = null;

		if (option == LIST) {
			request = "list";	
		}
		else if (option == PUT || option == INVALID_PUT) {
			request = "put";
		}

		// create file obj
		File logFile = new File("log.txt");
		PrintWriter out = null;

		// if the log file exists, then create writer so we can append to curent file
		if (logFile.exists()) {
			out = new PrintWriter(new FileOutputStream(new File("log.txt"), true));
		}
		// otherwise just create writer
		else {
			out = new PrintWriter("log.txt");
		}

		// append data to file and close file
		out.append(LocalDate.now() + "|" + LocalTime.now() + "|" + socket.getInetAddress().getHostAddress() + "|" + request + "\n");
		out.close();
	}
}