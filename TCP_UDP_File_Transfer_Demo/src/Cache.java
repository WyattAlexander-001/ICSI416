import java.net.*;
import java.io.*;

public class Cache {
    private int port;
    private String serverIP;
    private int serverPort;
    private String protocol;

    public Cache(int port, String serverIP, int serverPort, String protocol) throws IOException {
        this.port = port;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.protocol = protocol;
    }

    public void start() throws IOException {
        if (protocol.equalsIgnoreCase("tcp")) {
            startTCPCache();
        } else if (protocol.equalsIgnoreCase("snw")) {
            startSNWCache();
        } else {
            System.out.println("Unsupported protocol.");
        }
    }

    private void startTCPCache() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                TCP_Transport tcpTransport = new TCP_Transport(clientSocket);
                String command = tcpTransport.receiveMessage();
                if (command.startsWith("get")) {
                    handleGetCommand(command, tcpTransport);
                } else {
                    tcpTransport.sendMessage("Invalid command");
                }
                tcpTransport.close();
            } catch (IOException e) {
                System.out.println("Cache error: " + e.getMessage());
            }
        }
    }

    private void startSNWCache() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                TCP_Transport tcpTransport = new TCP_Transport(clientSocket);
                String command = tcpTransport.receiveMessage();
                if (command.startsWith("get")) {
                    handleSNWGetCommand(command, tcpTransport);
                } else {
                    tcpTransport.sendMessage("Invalid command");
                }
                tcpTransport.close();
            } catch (IOException e) {
                System.out.println("Cache error: " + e.getMessage());
            }
        }
    }

    private void handleSNWGetCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        String filename = tokens[1];

        // Receive the client's SNW port number
        int clientSNWPort = Integer.parseInt(tcpTransport.receiveMessage());

        File file = new File(filename);
        if (file.exists()) {
            tcpTransport.sendMessage("File delivered from cache.");

            // Send file via SNW to client's SNW port
            int cacheToClientSNWPort = 30002; // Ensure this port is available
            SNWTransport snwTransport = new SNWTransport(
                    tcpTransport.getSocket().getInetAddress().getHostAddress(),
                    clientSNWPort,
                    cacheToClientSNWPort
            );
            snwTransport.sendFile(file);
            snwTransport.close();
        } else {
            System.out.println("Cache: File not found in cache. Fetching from server...");

            // Set up SNWTransport receiver BEFORE sending the SNW port to the server
            int cacheSNWPort = 30001; // Ensure this port is available
            SNWTransport snwTransport = new SNWTransport(cacheSNWPort); // Receiver

            // Fetch the file from the server
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport serverTransport = new TCP_Transport(serverSocket);

            // Send the command to the server
            serverTransport.sendMessage(command);

            // Inform server of cache's SNW port
            serverTransport.sendMessage(String.valueOf(cacheSNWPort));

            String serverResponse = serverTransport.receiveMessage();
            if (serverResponse.equals("Sending file.")) {
                // Receive the file from the server via SNW
                snwTransport.receiveFile(new File(filename));
                snwTransport.close();

                serverTransport.close();
                serverSocket.close();

                // Ensure the file is fully written
                File receivedFile = new File(filename);
                long expectedFileSize = receivedFile.length();
                System.out.println("Cache: Received file size from server: " + expectedFileSize);

                // Now send the file to the client
                tcpTransport.sendMessage("File delivered from server.");
                int cacheToClientSNWPort = 30002; // Ensure this port is available
                SNWTransport snwToClient = new SNWTransport(
                        tcpTransport.getSocket().getInetAddress().getHostAddress(),
                        clientSNWPort,
                        cacheToClientSNWPort
                );
                snwToClient.sendFile(receivedFile);
                snwToClient.close();
            } else {
                tcpTransport.sendMessage("File not found on server.");
                serverTransport.close();
                serverSocket.close();
                snwTransport.close(); // Close the receiver if no file is sent
            }
        }
    }



    private void handleGetCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        String filename = tokens[1];
        File file = new File(filename);
        if (file.exists()) {
            tcpTransport.sendMessage("File delivered from cache.");
            tcpTransport.sendFile(filename);
        } else {
            // Do not send a message to the client here
            System.out.println("Cache: File not found in cache. Fetching from server...");
            // Fetch the file from the server
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport serverTransport = new TCP_Transport(serverSocket);
            serverTransport.sendMessage(command);
            String serverResponse = serverTransport.receiveMessage();
            if (serverResponse.equals("Sending file.")) {
                // Receive the file from the server
                serverTransport.receiveFile(filename);
                // Close the connection to the server
                serverTransport.close();
                serverSocket.close();
                // Now send the file to the client
                tcpTransport.sendMessage("File delivered from server.");
                tcpTransport.sendFile(filename);
            } else {
                tcpTransport.sendMessage("File not found on server.");
                serverTransport.close();
                serverSocket.close();
            }
        }
    }


    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java Cache <port> <serverIP> <serverPort> <protocol>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String serverIP = args[1];
        int serverPort = Integer.parseInt(args[2]);
        String protocol = args[3];
        try {
            Cache cache = new Cache(port, serverIP, serverPort, protocol);
            cache.start();
        } catch (IOException e) {
            System.err.println("Could not start cache: " + e.getMessage());
        }
    }
}
