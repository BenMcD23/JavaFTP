import java.io.*;
import java.net.*;
import java.nio.file.*;


public class Client {
    // this is if the local file (the one trying to upload) cant be opened 
    // (may not exist)
    public static final int NO_LOCAL_FILE = 1;
    // this is if the given file name isnt on the server
    // this means we can upload our file
    public static final int NO_SERVER_FILE = 2;
    // this is if the given file name is on the server
    // we cant upload
    public static final int SERVER_FILE = 3;

    private String fileName = null;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    // constructor
    public Client(String[] args) {
        // try and run the related function depending on input
        try {
            // can only use args[index] if its larger than 0
            if (args.length > 0) {
                // if input is list and only 1 argument, then can run
                if ((args[0].equals("list")) && (args.length == 1)) {
                    // connect after valid input
                    createConnection();
                    listFiles();

                    // free up input, ouput and socket
                    freeConnection();
                }

                // if input is list but more than 1 argument, then print help
                else if ((args[0].equals("list")) && (args.length > 1)) {
                    System.out.println("list usage: 'java Client list'.");
                    System.exit(1);
                }

                // if input is put and correct amount of args, then can run
                else if ((args[0].equals("put")) && (args.length == 2)) {
                    fileName = args[1];

                    // run testFileExistLocal, returns if the local file exists or not
                    int fileExistLocal = testFileExistLocal(fileName);

                    // if the file doesnt exist locally, then show error message
                    if (fileExistLocal == NO_LOCAL_FILE) {
                        System.out.println("Error: Cannot open local file '" + fileName + "' for reading.");
                    }

                    // the file exists locally, now check if it exists on server
                    else {
                        // connect after valid input
                        createConnection();
                        
                        // test if it exists on server
                        int fileExistServer = testFileExistServer(fileName);
                        
                        // if it doesnt exist, can upload
                        if (fileExistServer == NO_SERVER_FILE) {
                            uploadFile(fileName);
                            // print message that its uploaded
                            System.out.println("Uploaded file '" + fileName + "'.");

                            // free up input, ouput and socket
                            freeConnection();
                        }
                    }
                }

                // if input is put but not correct number of args, then print help
                else if ((args[0].equals("put")) && (args.length != 2)) {
                    System.out.println("put usage: 'java Client put exampleFileName'.");
                    System.exit(1);
                }
            }

            // invalid input (args is >= 0)
            else {
                System.out.println("Available Commands:\nput usage: 'java Client put exampleFileName'.\nlist usage: 'java Client list'.");
                System.exit(1);
            }
        }

        // catch if we cant use any of the IO for any reason
        catch (IOException e) {
            System.err.println("Couldn't use the I/O connection.");
            System.exit(1);
        }
    }

    // opens connection to server
    private void createConnection() {
        // try and open socket and set input and output streams
        try{
            socket = new Socket("localhost", 9654);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        }

        // catch errors
        catch (UnknownHostException e) {
            System.err.println("Can't connect to that host.");
            System.exit(1);
        }
        catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to host.");
            System.exit(1);
        }
    }

    // lists all the files
    private void listFiles() throws IOException {
        // write list to server
        out.writeUTF("list");

        // get response from server, which is the list of files and print
        String option = in.readUTF();
        System.out.println(option);
    }

    // test if the file exists locally
    private int testFileExistLocal(String fileName) {
        // testing if local file exists
        Path path = Paths.get(fileName);

        // if the file doesnt exist, return NO_LOCAL_FILE(1)
        if (!Files.exists(path)) {
            return NO_LOCAL_FILE;
        }

        return 0;
    }

    // tests if the file exists on the server
    private int testFileExistServer(String fileName) throws IOException {
        // write to the server that we're "putting" and the name of the file
        // the put is so it can be logged
        out.writeUTF("put");
        out.writeUTF(fileName);

        // read from the server if the file exists or not on the server
        String message = in.readUTF();

        // if the message is noFile then it doesnt exist on the server and we can upload
        // return NO_SERVER_FILE(2)
        if (message.equals("noFile")) {
            return NO_SERVER_FILE;
        }

        // if it exists on server then print a message from server saying it exists
        // (doesnt necessarily need to be a message from server, could be a message from server side)
        else {
            System.out.println(message);
            // need to return, but doesnt matter what
            return SERVER_FILE;
        }
    }
    

    // uploads given file to the server
    private void uploadFile(String fileName) throws IOException {
        // set buffer and read file into buffer
        byte[] buffer = new byte[65482];
        FileInputStream fileInputStream = new FileInputStream(fileName);

        // try and catch as if file is empty causes error.
        // allows an empty file to be uploaded
        try {
            // write buffer to server so can be saved to file there
            int bytes = fileInputStream.read(buffer,0,buffer.length);
            out.write(buffer,0,bytes);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // do nothing
        }
    }

    // free's up all connection stuff
    private void freeConnection() {
        try {        
            out.close();
            in.close();
            socket.close();
        }
        catch (IOException e) {
            System.err.println("Couldn't close connection.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // run constructor
        new Client(args);
    }
}