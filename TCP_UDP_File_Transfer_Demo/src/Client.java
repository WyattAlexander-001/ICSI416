import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String serverIP;
    private int serverPort;
    private String cacheIP;
    private int cachePort;
    private String protocol;
    private Scanner scanner = new Scanner(System.in);

    public Client(String serverIP, int serverPort, String cacheIP, int cachePort, String protocol) throws IOException {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.cacheIP = cacheIP;
        this.cachePort = cachePort;
        this.protocol = protocol;
        System.out.println("Client Side Starting on port: " + this.serverPort);
        connectToServer();
    }

    private void connectToServer() throws IOException {
        if (protocol.equals("tcp")) {
            socket = new Socket(cacheIP, cachePort);  // Connects to the cache
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }
    }

    public void start() {
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine();
            try {
                if (command.equals("quit")) {
                    out.writeUTF(command);
                    break;
                } else if (command.startsWith("put") || command.startsWith("get")) {
                    out.writeUTF(command);
                    String response = in.readUTF();
                    System.out.println("Response: " + response);
                } else {
                    System.out.println("Unsupported command.");
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
        close();
    }

    private void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing client resources: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Error starting client: " + e.getMessage());
        }
    }
}
