import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MyFile {
    private static final int CHUNK_SIZE = 1000;

    //Reads data from a file, divides into 1000 byte CHUNKS,
    public static List<byte[]> readFileInChunks(File file) throws IOException {
        List<byte[]> fileChunks = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                fileChunks.add(buffer.clone());  // Important to clone the buffer
                buffer = new byte[CHUNK_SIZE];  // Reset buffer
            }
        }
        return fileChunks;
    }

    //Writes those chunks into a new file
    public static void writeFileFromChunks(List<byte[]> chunks, File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            for (byte[] chunk : chunks) {
                bos.write(chunk);
            }
        }
    }

//    public static void main(String[] args) throws IOException {
//        System.out.println("Testing MyFile Class:");
//        File testFile = new File("test.txt");
//        List<byte[]> chunks = MyFile.readFileInChunks(testFile);
//        System.out.println("Number of chunks: " + chunks.size());
//        for (byte[] chunk : chunks) {
//            System.out.println(new String(chunk));
//        }
//    }
}
