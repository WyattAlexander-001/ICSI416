import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server is running on port " + port);
    }

    public void start() {
        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                String command = in.readUTF();
                if (command.startsWith("put")) {
                    handlePutCommand(command, in, out);
                } else if (command.startsWith("get")) {
                    handleGetCommand(command, out);
                } else {
                    out.writeUTF("Invalid command");
                }
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    private void handlePutCommand(String command, DataInputStream in, DataOutputStream out) throws IOException {
        String filename = command.split(" ")[1];
        long fileSize = in.readLong();
        FileOutputStream fos = new FileOutputStream(filename);
        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int bytesRead;
        while (totalRead < fileSize && (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
            fos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        fos.close();
        out.writeUTF("File successfully uploaded.");
    }


    private void handleGetCommand(String command, DataOutputStream out) throws IOException {
        String filename = command.split(" ")[1];
        File file = new File(filename);
        if (file.exists()) {
            out.writeUTF("Sending file.");
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
            out.writeUTF("File not found.");
        }
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Server <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        try {
            Server server = new Server(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

}
