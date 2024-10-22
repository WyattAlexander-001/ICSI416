import java.net.*;
import java.io.*;
import java.util.List;

public class Cache {
    private ServerSocket serverSocket;

    public Cache(int port) throws IOException {
        serverSocket = new ServerSocket(port);
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
            List<byte[]> fileChunks = MyFile.readFileInChunks(file);
            for (byte[] chunk : fileChunks) {
                out.write(chunk, 0, chunk.length);
            }
            out.writeUTF("File delivered from cache.");
        } else {
            out.writeUTF("File not found, fetching from server...");
            // Implement fetching from server and saving to cache
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            Cache cache = new Cache(port);
            cache.start();
        } catch (IOException e) {
            System.err.println("Could not start cache: " + e.getMessage());
        }
    }
}
