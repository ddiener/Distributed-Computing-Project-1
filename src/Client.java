import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    private Socket client;
    private PrintWriter w;
    private BufferedReader r;

    /**
     * Connect to the server, then create input and output buffers
     */
    public Client(String machineName, int portNumber) {
        try {
            client = new Socket(machineName, portNumber);
            w = new PrintWriter(client.getOutputStream(), true);
            r = new BufferedReader(new InputStreamReader(client.getInputStream()));
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Read input from the user, send it to the server and display the response
     */
    public void run() throws IOException {
        String input;
        Scanner stdIn = new Scanner(System.in);

        while (true) {
            System.out.print("myftp> ");
            input = stdIn.nextLine();
            w.println(input);
            

            // TODO might be better way to emphasize quit condition
            String[] splitCommand = input.split(" ");
            if (input.equals("quit"))
                break;
            else if (splitCommand[0].equals("put")) {
                if( splitCommand.length != 2 ) {
                    System.out.println( "Incorrect usage. Correct usage: myftp> put <local filename>");
                    continue;
                }


                try {
                    put(splitCommand[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }
            else if (splitCommand[0].equals("get")) {
                if( splitCommand.length != 2 ) {
                    System.out.println( "Incorrect usage. Correct usage: myftp> get <remote filename>");
                    continue;
                }

                PrintWriter file = new PrintWriter(splitCommand[1]);
                String line;

                while (!(line = r.readLine()).equals("STOP")) {
                    file.println(line);
                }
                file.close();
                continue;
            }

            try {
                String response = "";
                int numLines = Integer.parseInt(r.readLine());
                for (int i = 0; i < numLines; i++) {
                    if (r.ready()) {
                        response = r.readLine();
                    }
                    System.out.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        quit();
    }

    /**
     * Places a local file on a remote server
     */
    public void put(String localFilename) throws IOException {
        w.println(localFilename);
        File file = new File(localFilename);

        if( !file.exists() ) {
            System.out.println(localFilename + ": file does not exist");
            w.flush();
            return;
        }

        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        OutputStream os = client.getOutputStream();

        byte[] contents;
        long fileLength = file.length();
        long current = 0;

        while (current != fileLength) {
            int size = 10000;
            if (fileLength - current >= size)
                current += size;
            else {
                size = (int) (fileLength - current);
                current = fileLength;
            }
            contents = new byte[size];
            bis.read(contents, 0, size);
            os.write(contents);

        }
        os.flush();
        fis.close();
        bis.close();
        // TODO probably better way to terminate the data
        w.print("STOP\n");
    }

    /**
     * Close the buffers, close the socket
     */
    private void quit() {
        try {
            w.close();
            r.close();
            client.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Start a client, take in the args and run it
     */
    public static void main(String [] args) throws IOException {
        String machineName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        Client client = new Client(machineName, portNumber);
        client.run();
    }
}