import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class client {
    public static void main(String[] args) {
        final File[] fileToSend = new File[1];
        JFrame jFrame = new JFrame("Client Side");
        jFrame.setSize(640,480);
        jFrame.setLayout(new BoxLayout(jFrame.getContentPane(),BoxLayout.Y_AXIS));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JLabel jTTitle = new JLabel("File Sending Program ICSI-416");
        jTTitle.setFont(new Font("Garamond", Font.BOLD, 64));
        jTTitle.setBorder(new EmptyBorder(20,0,10,0));
        jTTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jLFileName = new JLabel("Simple app to send file");
        jLFileName.setFont(new Font("Arial", Font.BOLD, 32));
        jLFileName.setBorder(new EmptyBorder(50,0,0,0));
        jLFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jPButton = new JPanel();
        jPButton.setBorder(new EmptyBorder(75,0,0,0));

        JButton jBAttachFile = new JButton("Attach file");
        jBAttachFile.setPreferredSize(new Dimension(150,75));
        jBAttachFile.setFont(new Font("Arial", Font.BOLD, 20));

        jBAttachFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setDialogTitle("Select your file to send");

                if(jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                    fileToSend[0] = jFileChooser.getSelectedFile();
                    jLFileName.setText("File you want to send is: " + fileToSend[0].getName());
                }
            }
        });

        jPButton.add(jBAttachFile);

        JButton jBSendFile = new JButton("Send file");
        jBSendFile.setPreferredSize(new Dimension(150,75));
        jBSendFile.setFont(new Font("Arial", Font.BOLD, 20));
        jPButton.add(jBSendFile);

        jBSendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(fileToSend[0] == null){
                    jLFileName.setText("You need to pick a file to send a file");
                }else{
                    try {
                        FileInputStream fileInputStream = new FileInputStream(fileToSend[0].getAbsolutePath());
                        Socket socket = new Socket("localhost", 1234); // Form connection with server
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream()); // Allows us to write to server
                        String fileName = fileToSend[0].getName();
                        byte[] fileNameBytes = fileName.getBytes();

                        byte[] fileContentBytes = new byte[(int) fileToSend[0].length()];
                        fileInputStream.read(fileContentBytes);

                        dataOutputStream.writeInt(fileNameBytes.length);
                        dataOutputStream.write(fileNameBytes);

                        dataOutputStream.writeInt(fileContentBytes.length);
                        dataOutputStream.write(fileContentBytes);
                    }catch(IOException error){
                        error.printStackTrace();
                    }
                }
            }
        });

        jFrame.add(jTTitle);
        jFrame.add(jLFileName);
        jFrame.add(jPButton);
        jFrame.setVisible(true);
    }
}
