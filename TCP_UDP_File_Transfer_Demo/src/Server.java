import java.net.*;
import java.io.*;

public class Server {
    private int port;
    private String protocol;

    public Server(int port, String protocol) {
        this.port = port;
        this.protocol = protocol;
        System.out.println("Server is running on port " + port + " with protocol " + protocol);
    }

    public void start() throws IOException {
        if (protocol.equalsIgnoreCase("tcp")) {
            startTCPServer();
        } else if (protocol.equalsIgnoreCase("snw")) {
            startSNWServer();
        } else {
            System.out.println("Unsupported protocol.");
        }
    }

    private void startTCPServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                TCP_Transport tcpTransport = new TCP_Transport(clientSocket);
                String command = tcpTransport.receiveMessage();
                if (command.startsWith("put")) {
                    handlePutCommand(command, tcpTransport);
                } else if (command.startsWith("get")) {
                    handleGetCommand(command, tcpTransport);
                } else {
                    tcpTransport.sendMessage("Invalid command");
                }
                tcpTransport.close();
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    private void startSNWServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                TCP_Transport tcpTransport = new TCP_Transport(clientSocket);
                String command = tcpTransport.receiveMessage();
                if (command.startsWith("put")) {
                    handleSNWPutCommand(command, tcpTransport);
                } else if (command.startsWith("get")) {
                    handleSNWGetCommand(command, tcpTransport);
                } else {
                    tcpTransport.sendMessage("Invalid command");
                }
                tcpTransport.close();
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    private void handleSNWPutCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            tcpTransport.sendMessage("Usage: put <filename>");
            return;
        }
        String filename = tokens[1];

        // Receive file via SNW
        SNWTransport snwTransport = new SNWTransport(port);
        File file = new File(filename);
        snwTransport.receiveFile(file);
        snwTransport.close();

        tcpTransport.sendMessage("File successfully uploaded.");
    }

    private void handleSNWGetCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            tcpTransport.sendMessage("Usage: get <filename>");
            return;
        }
        String filename = tokens[1];

        // Receive cache's SNW port
        int cacheSNWPort = Integer.parseInt(tcpTransport.receiveMessage());

        File file = new File(filename);
        if (file.exists()) {
            tcpTransport.sendMessage("Sending file.");

            int serverToCacheSNWPort = 30003; // Ensure this port is available
            SNWTransport snwTransport = new SNWTransport(
                    tcpTransport.getSocket().getInetAddress().getHostAddress(),
                    cacheSNWPort,
                    serverToCacheSNWPort
            );
            snwTransport.sendFile(file);
            snwTransport.close();
        } else {
            tcpTransport.sendMessage("File not found.");
        }
    }

    private void handlePutCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            tcpTransport.sendMessage("Usage: put <filename>");
            return;
        }
        String filename = tokens[1];
        tcpTransport.receiveFile(filename);
        tcpTransport.sendMessage("File successfully uploaded.");
    }

    private void handleGetCommand(String command, TCP_Transport tcpTransport) throws IOException {
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            tcpTransport.sendMessage("Usage: get <filename>");
            return;
        }
        String filename = tokens[1];
        File file = new File(filename);
        if (file.exists()) {
            tcpTransport.sendMessage("Sending file.");
            tcpTransport.sendFile(filename);
        } else {
            tcpTransport.sendMessage("File not found.");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Server <port> <protocol>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String protocol = args[1];
        try {
            Server server = new Server(port, protocol);
            server.start();
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}
