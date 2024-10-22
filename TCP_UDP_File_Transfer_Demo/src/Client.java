import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private String serverIP;
    private int serverPort;
    private String cacheIP;
    private int cachePort;
    private String protocol;
    private Scanner scanner = new Scanner(System.in);

    public Client(String serverIP, int serverPort, String cacheIP, int cachePort, String protocol) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.cacheIP = cacheIP;
        this.cachePort = cachePort;
        this.protocol = protocol;
        System.out.println("Client Side Starting.");
    }

    public void start() {
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) {
                continue; // Skip empty commands
            }
            try {
                if (command.equals("quit")) {
                    System.out.println("Exiting program!");
                    break;
                } else if (command.startsWith("put")) {
                    handlePutCommand(command);
                } else if (command.startsWith("get")) {
                    handleGetCommand(command);
                } else {
                    System.out.println("Unsupported command.");
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
        // No need to call close() since we're closing sockets in handlers
    }

    private void handlePutCommand(String command) throws IOException {
        System.out.println("Awaiting server response.");
        if (protocol.equalsIgnoreCase("tcp")) {
            // Use TCP transport
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport tcpTransport = new TCP_Transport(serverSocket);

            tcpTransport.sendMessage(command);
            String[] tokens = command.split(" ");
            if (tokens.length < 2) {
                System.out.println("Usage: put <filename>");
                tcpTransport.close();
                return;
            }
            String filename = tokens[1];
            File file = new File(filename);
            if (file.exists()) {
                tcpTransport.sendFile(filename);
                String response = tcpTransport.receiveMessage();
                System.out.println("Server response: " + response);
            } else {
                System.out.println("File not found.");
            }

            tcpTransport.close();
        } else if (protocol.equalsIgnoreCase("snw")) {
            // Use SNW transport
            // Send command via TCP
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport tcpTransport = new TCP_Transport(serverSocket);

            tcpTransport.sendMessage(command);
            String[] tokens = command.split(" ");
            if (tokens.length < 2) {
                System.out.println("Usage: put <filename>");
                tcpTransport.close();
                return;
            }
            String filename = tokens[1];
            File file = new File(filename);
            if (file.exists()) {
                // Send file via SNW
                SNWTransport snwTransport = new SNWTransport(serverIP, serverPort);
                snwTransport.sendFile(file);
                snwTransport.close();

                String response = tcpTransport.receiveMessage();
                System.out.println("Server response: " + response);
            } else {
                System.out.println("File not found.");
            }

            tcpTransport.close();
        } else {
            System.out.println("Unsupported protocol.");
        }
    }

    private void handleGetCommand(String command) throws IOException {
        if (protocol.equalsIgnoreCase("tcp")) {
            // Use TCP transport
            Socket cacheSocket = new Socket(cacheIP, cachePort);
            TCP_Transport tcpTransport = new TCP_Transport(cacheSocket);

            tcpTransport.sendMessage(command);
            String[] tokens = command.split(" ");
            if (tokens.length < 2) {
                System.out.println("Usage: get <filename>");
                tcpTransport.close();
                return;
            }
            String filename = tokens[1];
            String response = tcpTransport.receiveMessage();
            System.out.println("Server response: " + response);
            if (response.equals("File delivered from cache.") || response.equals("File delivered from server.")) {
                tcpTransport.receiveFile(filename);
                System.out.println("File received and saved as " + filename);
            } else {
                System.out.println("Cache response: " + response);
            }

            tcpTransport.close();
        } else if (protocol.equalsIgnoreCase("snw")) {
            // SNW code
            // Send command via TCP
            Socket cacheSocket = new Socket(cacheIP, cachePort);
            TCP_Transport tcpTransport = new TCP_Transport(cacheSocket);

            String[] tokens = command.split(" ");
            if (tokens.length < 2) {
                System.out.println("Usage: get <filename>");
                tcpTransport.close();
                return;
            }
            String filename = tokens[1];

            // Set up SNWTransport receiver before sending the SNW port
            int snwPort = 30000; // Ensure this port is available
            SNWTransport snwTransport = new SNWTransport(snwPort); // Receiver

            tcpTransport.sendMessage(command);
            // Send the SNW port to the cache
            tcpTransport.sendMessage(String.valueOf(snwPort));

            String response = tcpTransport.receiveMessage();
            System.out.println("Server response: " + response);

            if (response.equals("File delivered from cache.") || response.equals("File delivered from server.")) {
                // Receive file via SNW
                snwTransport.receiveFile(new File(filename));
                snwTransport.close();

                System.out.println("File received and saved as " + filename);
            } else {
                System.out.println("Cache response: " + response);
                snwTransport.close();
            }

            tcpTransport.close();
        } else {
            System.out.println("Unsupported protocol.");
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java Client <serverIP> <serverPort> <cacheIP> <cachePort> <protocol>");
            return;
        }
        try {
            Client client = new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4]);
            client.start();
        } catch (Exception e) {
            System.err.println("Error starting client: " + e.getMessage());
        }
    }
}
