import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class Server  {

    private ServerSocket server;
	private BufferedReader r;
	private PrintWriter w;
    private String serverDir;

	/**
     * Default constructor
     */
    public Server( int portNumber ) {
        try {
            server = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Save the starting directory of the server
        serverDir = System.getProperty("user.dir");
    }

    /**
     * Main running method. Prints prompt, takes in commands, sorts out execution accordingly
     */
    public void run() throws IOException {
        while (true) {
            Socket client = null;
            try {
                client = server.accept();
                r = new BufferedReader(new InputStreamReader(client.getInputStream()));
                w = new PrintWriter(client.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String command = "";
            loop:
            while (true) {
                try {
                    command = r.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(command);
                // TODO need to handle for bad commands like empty command
                String[] splitCommand = command.split(" ");
                switch (splitCommand[0]) {
                    case "get":
                        FileInputStream fstream = new FileInputStream(splitCommand[1]);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                        String strLine;
                        while ((strLine = br.readLine()) != null)   {
                            w.println(strLine);
                        }

                        br.close();
                        w.println("STOP");
                        w.flush();
                        break;
                    case "put":
                        if( splitCommand.length != 2 ) {
                            w.write( 1 + "\n" + command + " usage : put <local file>\n");
                            w.flush();
                            break;
                        }
                        // File tester = new File(splitCommand[1]);
                        PrintWriter file = new PrintWriter(splitCommand[1]);

                        // TODO probably better name for this variable
                        String line_1;
                        r.readLine();
                        while (!(line_1 = r.readLine()).equals("STOP")) {
                            file.println(line_1);
                        }
                        file.close();
                        break;
                    case "delete":
                        if( splitCommand.length == 1 ) {
                            w.print(1 + "\n" + command + " not enough arguments.\n");
                            w.flush();
                            break;
                        }

                        if (splitCommand.length > 2) {
                            w.print(1 + "\n" + command + ": too many arguments\n");
                            w.flush();
                            break;
                        }

                        File _temp = new File( splitCommand[1] );
                        if( !_temp.exists() ) {
                            w.print(1 + "\n" + splitCommand[1] + ": file does not exist\n");
                            w.flush();
                            break;
                        }

                        String alteredCommand = "rm -r " + System.getProperty("user.dir") + "/" + splitCommand[1];
                        executeBuilt(alteredCommand);
                        break;
                    case "pwd":
                        if( splitCommand.length != 1 ) {
                            w.print(1 + "\nUsage: myftp> pwd.\n" );
                            w.flush();
                            break;
                        }
                        w.print(1 + "\n" + System.getProperty("user.dir") + "\n");
                        w.flush();
                        break;
                    case "cd":
                        // Check the right amount of arguments are supplied
                        if (splitCommand.length > 2) {
                            w.print(1 + "\n" + command + ": too many arguments\n");
                            w.flush();
                            break;
                        }

                        String newPath;

                        if (splitCommand[1].equals("..") ) {
                            File temp = new File( System.getProperty("user.dir"));
                            if( temp.getParentFile() == null ) {
                                w.print( 0 + "\n");
                                w.flush();
                                break;
                            }
                            String parentPath = temp.getParentFile().getAbsolutePath();
                            System.setProperty( "user.dir", parentPath );
                            w.print(0 + "\n");
                            w.flush();
                            break;
                        }
                        File file_1 = new File(System.getProperty("user.dir") + "/" + splitCommand[1]);
                        if (file_1.exists() && file_1.isDirectory()) {
                            newPath = file_1.getAbsolutePath();
                            System.setProperty("user.dir", newPath);
                            w.print(0 + "\n");
                            w.flush();
                        }
                        else {
                            w.print(1 + "\n" + splitCommand[1] + ": no such file or directory\n");
                            w.flush();
                        }

                        break;
                    case "quit":
                        if( splitCommand.length != 1 ) {
                            w.print(1 + "\nUsage: myftp> quit\n" );
                            w.flush();
                            break;
                        }
                        // TODO probably better to make this a method call
                        try {
                            r.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        w.close();
                        try {
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();

                        }

                        // Restore the original server directory
                        System.setProperty("user.dir", serverDir);

                        break loop;
                    default:
                        command = splitCommand[0] + " " + System.getProperty("user.dir");
                        if (splitCommand.length > 1)
                            command += "/" + splitCommand[1];
                        System.out.println(command);

                        // error handling for mkdir
                        if( splitCommand[0].equals( "mkdir") && splitCommand.length != 2 ) {
                            w.print( 1 + "\nUsage: myftp> mkdir <new directory>\n" );
                            w.flush();
                            break;
                        }

                        if( splitCommand[0].equals("mkdir") ) {
                            File testing = new File( splitCommand[1] );
                            if( testing.exists() ) {
                                w.print( 1 + "\nTarget directory " + splitCommand[1] + " already exists\n");
                                w.flush();
                                break;
                            }
                        }



                        // error handling for ls
                        if( splitCommand[0].equals( "ls") && splitCommand.length != 1) {
                            w.print(1 + "\nUsage: myftp> ls\n" );
                            w.flush();
                            break;
                        }

                        if( !splitCommand[0].equals("ls") && !splitCommand[0].equals("mkdir") ) {
                            w.print( 1 + "\n" + splitCommand[0] + " is not supported by myftp\n");
                            w.flush();
                            break;
                        }

                        executeBuilt(command);


                }
            }
        }
    }

    /**
     * This is a function that executes built in functions with an exec system call
     */
    private void executeBuilt(String command) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader pOutput = new BufferedReader(new InputStreamReader(
                p.getInputStream()
        ));
        String line;
        String lines = "";
        int numLines = 0;
        try {
            while ((line = pOutput.readLine()) != null) {
                lines = lines + line + "\n";
                numLines++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        lines = numLines + "\n" + lines;
        System.out.print(lines);
        w.print(lines);
        w.flush();
    }

    /**
     * The main running method. Invokes run() and upon completion, closes all streams and other resources.
     */
    public static void main(String[] args) throws Exception {
        int portNumber = Integer.parseInt(args[0]);
        Server serv = new Server(portNumber);
        serv.run();
    }
}