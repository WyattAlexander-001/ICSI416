import java.net.*;
import java.io.*;
import java.util.List;

public class Cache {
    private ServerSocket serverSocket;
    private String serverIP;
    private int serverPort;

    public Cache(int port, String serverIP, int serverPort) throws IOException {
        serverSocket = new ServerSocket(port);
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void start() {
        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                // Read command from client
                String command = in.readUTF();
                if (command.startsWith("get")) {
                    handleGetCommand(command, out);
                } else {
                    out.writeUTF("Invalid command");
                }
            } catch (IOException e) {
                System.out.println("Cache error: " + e.getMessage());
            }
        }
    }

    private void handleGetCommand(String command, DataOutputStream out) throws IOException {
        String filename = command.split(" ")[1];
        File file = new File(filename);
        if (file.exists()) {
            out.writeUTF("File delivered from cache.");
            long fileSize = file.length();
            out.writeLong(fileSize);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            fis.close();
        } else {
            out.writeUTF("File not found in cache. Fetching from server...");
            Socket serverSocket = new Socket(serverIP, serverPort);
            DataOutputStream serverOut = new DataOutputStream(serverSocket.getOutputStream());
            DataInputStream serverIn = new DataInputStream(serverSocket.getInputStream());
            serverOut.writeUTF(command);
            String serverResponse = serverIn.readUTF();
            if (serverResponse.equals("Sending file.")) {
                out.writeUTF("File delivered from server.");
                long fileSize = serverIn.readLong();
                out.writeLong(fileSize);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;
                while (totalRead < fileSize && (bytesRead = serverIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    out.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                fos.close();
            } else {
                out.writeUTF("File not found on server.");
            }
            serverIn.close();
            serverOut.close();
            serverSocket.close();
        }
    }


    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java Cache <port> <serverIP> <serverPort>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String serverIP = args[1];
        int serverPort = Integer.parseInt(args[2]);
        try {
            Cache cache = new Cache(port, serverIP, serverPort);
            cache.start();
        } catch (IOException e) {
            System.err.println("Could not start cache: " + e.getMessage());
        }
    }

}
