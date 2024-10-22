import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SNWTransport {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public SNWTransport(String ip, int port) throws IOException {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(ip);
        this.port = port;
    }

    public void sendFile(File file) throws IOException {
        List<byte[]> chunks = MyFile.readFileInChunks(file);
        for (byte[] chunk : chunks) {
            DatagramPacket packet = new DatagramPacket(chunk, chunk.length, address, port);
            socket.send(packet);
            // Wait for ACK
            byte[] ack = new byte[100];
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
            socket.receive(ackPacket);
            String ackStr = new String(ack, 0, ackPacket.getLength());
            if (!ackStr.equals("ACK")) {
                System.out.println("Failed to receive ACK, terminating.");
                return;
            }
        }
        // Send FIN message
        byte[] fin = "FIN".getBytes();
        DatagramPacket finPacket = new DatagramPacket(fin, fin.length, address, port);
        socket.send(finPacket);
    }

    public void receiveFile(File file) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String message = new String(buffer, 0, packet.getLength());
            if (message.equals("FIN")) {
                break;
            } else {
                chunks.add(Arrays.copyOf(buffer, packet.getLength()));
                byte[] ack = "ACK".getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                socket.send(ackPacket);
            }
        }
        MyFile.writeFileFromChunks(chunks, file);
    }
}
