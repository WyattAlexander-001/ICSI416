import java.net.*;
import java.io.*;

public class TCP_Transport {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public TCP_Transport(Socket socket) throws IOException {
        this.socket = socket;
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] buffer = new byte[4096];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        long fileSize = file.length();
        out.writeLong(fileSize);  // Send file size

        int bytesRead;
        while ((bytesRead = bis.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        bis.close();
        out.flush();
    }

    public void receiveFile(String savePath) throws IOException {
        long fileSize = in.readLong();  // Read file size
        File file = new File(savePath);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalRead = 0;
        while (totalRead < fileSize && (bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
            bos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }

        bos.close();
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public String receiveMessage() throws IOException {
        return in.readUTF();
    }

    public void close() throws IOException {
        socket.close();
        out.close();
        in.close();
    }
    public Socket getSocket() {
        return socket;
    }
}
