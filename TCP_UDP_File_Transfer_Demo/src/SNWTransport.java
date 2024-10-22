import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class SNWTransport {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    // For sender with specified local port
    public SNWTransport(String ip, int port, int localPort) throws IOException {
        if (localPort > 0) {
            this.socket = new DatagramSocket(localPort);
        } else {
            this.socket = new DatagramSocket();
        }
        this.address = InetAddress.getByName(ip);
        this.port = port;
        socket.setSoTimeout(5000); // Set timeout to 1 second
    }

    // For sender
    public SNWTransport(String ip, int port) throws IOException {
        this(ip, port, 0); // 0 means any available port
    }


    // For receiver
    public SNWTransport(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        socket.setSoTimeout(5000); // Set timeout to 5 seconds
    }

    public void sendFile(File file) throws IOException {
        long fileSize = file.length();
        String lenMessage = "LEN:" + fileSize;
        byte[] lenBytes = lenMessage.getBytes();
        DatagramPacket lenPacket = new DatagramPacket(lenBytes, lenBytes.length, address, port);
        System.out.println("Sending LEN message to " + address + ":" + port);
        socket.send(lenPacket);

        if (fileSize == 0) {
            // Handle zero-length file
            // Wait for FIN
            byte[] finBuffer = new byte[1024];
            DatagramPacket finPacket = new DatagramPacket(finBuffer, finBuffer.length);
            try {
                socket.receive(finPacket);
                String finStr = new String(finPacket.getData(), 0, finPacket.getLength());
                if (!finStr.equals("FIN")) {
                    System.out.println("Failed to receive FIN, terminating.");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Did not receive FIN. Terminating.");
            }
            return;
        }

        List<byte[]> chunks = MyFile.readFileInChunks(file);
        for (byte[] chunk : chunks) {
            System.out.println("Sending data packet to " + address + ":" + port);
            DatagramPacket dataPacket = new DatagramPacket(chunk, chunk.length, address, port);
            socket.send(dataPacket);

            // Wait for ACK
            System.out.println("Waiting for ACK...");
            byte[] ackBuffer = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            try {
                socket.receive(ackPacket);
                String ackStr = new String(ackPacket.getData(), 0, ackPacket.getLength());
                System.out.println("Received ACK from " + ackPacket.getAddress() + ":" + ackPacket.getPort());
                if (!ackStr.equals("ACK")) {
                    System.out.println("Failed to receive ACK, terminating.");
                    return;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Did not receive ACK. Terminating.");
                return;
            }
        }
        // Wait for FIN
        byte[] finBuffer = new byte[1024];
        DatagramPacket finPacket = new DatagramPacket(finBuffer, finBuffer.length);
        try {
            socket.receive(finPacket);
            String finStr = new String(finPacket.getData(), 0, finPacket.getLength());
            if (!finStr.equals("FIN")) {
                System.out.println("Failed to receive FIN, terminating.");
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Did not receive FIN. Terminating.");
        }
    }


    public void receiveFile(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        long expectedBytes = 0;
        long totalReceived = 0;

        // Receive LEN message
        System.out.println("Waiting to receive LEN message on port " + socket.getLocalPort());
        byte[] lenBuffer = new byte[1024];
        DatagramPacket lenPacket = new DatagramPacket(lenBuffer, lenBuffer.length);
        try {
            socket.receive(lenPacket);
            // Set the sender's address and port
            address = lenPacket.getAddress();
            port = lenPacket.getPort();
            String lenStr = new String(lenPacket.getData(), 0, lenPacket.getLength());
            System.out.println("Received LEN message from " + address + ":" + port);
            if (lenStr.startsWith("LEN:")) {
                expectedBytes = Long.parseLong(lenStr.substring(4));
            } else {
                System.out.println("Invalid LEN message. Terminating.");
                fos.close();
                return;
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Did not receive data. Terminating.");
            fos.close();
            return;
        }

        if (expectedBytes == 0) {
            System.out.println("Received zero-length file.");
        } else {
            while (totalReceived < expectedBytes) {
                byte[] dataBuffer = new byte[1000];
                DatagramPacket dataPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
                try {
                    socket.receive(dataPacket);
                    System.out.println("Received data packet from " + dataPacket.getAddress() + ":" + dataPacket.getPort());
                    fos.write(dataPacket.getData(), 0, dataPacket.getLength());
                    totalReceived += dataPacket.getLength();

                    // Send ACK
                    System.out.println("Sending ACK to " + dataPacket.getAddress() + ":" + dataPacket.getPort());
                    byte[] ackBytes = "ACK".getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, dataPacket.getAddress(), dataPacket.getPort());
                    socket.send(ackPacket);
                } catch (SocketTimeoutException e) {
                    System.out.println("Data transmission terminated prematurely.");
                    fos.close();
                    return;
                }
            }
        }
        fos.close();

        // Send FIN
        System.out.println("Sending FIN to " + address + ":" + port);
        byte[] finBytes = "FIN".getBytes();
        DatagramPacket finPacket = new DatagramPacket(finBytes, finBytes.length, address, port);
        socket.send(finPacket);
    }

    public void close() {
        socket.close();
    }
}
