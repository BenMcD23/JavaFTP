import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class Server {
	private ServerSocket server = null;
	private ExecutorService executor = null;

	// constructor
	public Server() throws IOException {
		// try and open port
		try {
			server = new ServerSocket(9654);
		} 
		// catch if cant open port
		catch (IOException e) {
			System.err.println("Could not listen on port: 9654.");
			System.exit(1);
		}

		// initialise the executor.
		executor = Executors.newFixedThreadPool(20);

		// loop indefinitely wating for a client to connect 
		while( true ) {
			// when someone connects, accept
			Socket client = server.accept();
			ClientHandler clientHandler = new ClientHandler(client);
			// submit thread and runs ClientHandler constructor
			executor.submit(clientHandler);
			// ClientHandler constructor then deals with the inputted instructions
		}
	}

	public static void main(String[] args) throws IOException {
		// run server
		new Server();
	}
}