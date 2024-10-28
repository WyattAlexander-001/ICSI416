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

        int clientSNWPort = Integer.parseInt(tcpTransport.receiveMessage());

        File file = new File(filename);
        if (file.exists()) {
            tcpTransport.sendMessage("File delivered from cache.");
            int cacheToClientSNWPort = 20030;
            SNWTransport snwTransport = new SNWTransport(
                    tcpTransport.getSocket().getInetAddress().getHostAddress(),
                    clientSNWPort,
                    cacheToClientSNWPort
            );
            snwTransport.sendFile(file);
            snwTransport.close();
        } else {
            System.out.println("Cache: File not found in cache. Fetching from server...");
            int cacheSNWPort = 20020;
            SNWTransport snwTransport = new SNWTransport(cacheSNWPort);
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport serverTransport = new TCP_Transport(serverSocket);

            serverTransport.sendMessage(command);
            serverTransport.sendMessage(String.valueOf(cacheSNWPort));

            String serverResponse = serverTransport.receiveMessage();
            if (serverResponse.equals("Sending file.")) {
                snwTransport.receiveFile(new File(filename));
                snwTransport.close();

                serverTransport.close();
                serverSocket.close();

                File receivedFile = new File(filename);
                long expectedFileSize = receivedFile.length();
                System.out.println("Cache: Received file size from server: " + expectedFileSize);

                tcpTransport.sendMessage("File delivered from server.");
                int cacheToClientSNWPort = 20030; // Ensure this port is available
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
                snwTransport.close();
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
            System.out.println("Cache: File not found in cache. Fetching from server...");
            Socket serverSocket = new Socket(serverIP, serverPort);
            TCP_Transport serverTransport = new TCP_Transport(serverSocket);
            serverTransport.sendMessage(command);
            String serverResponse = serverTransport.receiveMessage();
            if (serverResponse.equals("Sending file.")) {
                serverTransport.receiveFile(filename);
                serverTransport.close();
                serverSocket.close();
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
