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
        File file = new File(filename);
        List<byte[]> fileChunks = new ArrayList<>();
        while (in.available() > 0) {
            byte[] buffer = new byte[1000];
            int bytesRead = in.read(buffer);
            fileChunks.add(Arrays.copyOf(buffer, bytesRead));
        }
        MyFile.writeFileFromChunks(fileChunks, file);
        out.writeUTF("File successfully uploaded.");
    }

    private void handleGetCommand(String command, DataOutputStream out) throws IOException {
        String filename = command.split(" ")[1];
        File file = new File(filename);
        if (file.exists()) {
            List<byte[]> fileChunks = MyFile.readFileInChunks(file);
            for (byte[] chunk : fileChunks) {
                out.write(chunk, 0, chunk.length);
            }
            out.writeUTF("File successfully downloaded.");
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
