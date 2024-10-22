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
        // No need to call connectToServer() or connectToServerAndCache()
    }

    // Removed connectToServer() method entirely

    public void start() {
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) {
                continue; // Skip empty commands
            }
            try {
                if (command.equals("quit")) {
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
        Socket serverSocket = new Socket(serverIP, serverPort);
        DataOutputStream serverOut = new DataOutputStream(serverSocket.getOutputStream());
        DataInputStream serverIn = new DataInputStream(serverSocket.getInputStream());

        serverOut.writeUTF(command);
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            System.out.println("Usage: put <filename>");
            serverSocket.close();
            return;
        }
        String filename = tokens[1];
        File file = new File(filename);
        if (file.exists()) {
            long fileSize = file.length();
            serverOut.writeLong(fileSize);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                serverOut.write(buffer, 0, bytesRead);
            }
            fis.close();
            serverOut.flush();
            String response = serverIn.readUTF();
            System.out.println("Server response: " + response);
        } else {
            System.out.println("File not found.");
        }

        serverOut.close();
        serverIn.close();
        serverSocket.close();
    }

    private void handleGetCommand(String command) throws IOException {
        Socket cacheSocket = new Socket(cacheIP, cachePort);
        DataOutputStream cacheOut = new DataOutputStream(cacheSocket.getOutputStream());
        DataInputStream cacheIn = new DataInputStream(cacheSocket.getInputStream());

        cacheOut.writeUTF(command);
        String[] tokens = command.split(" ");
        if (tokens.length < 2) {
            System.out.println("Usage: get <filename>");
            cacheSocket.close();
            return;
        }
        String filename = tokens[1];
        String response = cacheIn.readUTF();
        if (response.equals("File delivered from cache.") || response.equals("File delivered from server.")) {
            long fileSize = cacheIn.readLong();
            FileOutputStream fos = new FileOutputStream(filename);
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead;
            while (totalRead < fileSize && (bytesRead = cacheIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fos.close();
            System.out.println("File received and saved as " + filename);
            System.out.println("Server response: " + response);
        } else {
            System.out.println("Cache response: " + response);
        }

        cacheOut.close();
        cacheIn.close();
        cacheSocket.close();
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
