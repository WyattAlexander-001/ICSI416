import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MyFile {
    private static final int CHUNK_SIZE = 1000;

    // Reads data from a file and divides it into 1000-byte chunks
    public static List<byte[]> readFileInChunks(File file) throws IOException {
        List<byte[]> fileChunks = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                fileChunks.add(chunk);
            }
        }
        return fileChunks;
    }

    // Writes those chunks into a new file
    public static void writeFileFromChunks(List<byte[]> chunks, File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            for (byte[] chunk : chunks) {
                bos.write(chunk);
            }
        }
    }
}
