package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Optional Swing GUI Client for the Chat Application.
 * Connects to the server and provides a graphical interface for chatting.
 */
public class ChatClientGUI {
    private String serverAddress = "127.0.0.1";
    private int serverPort = 8080;
    
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;
    
    private JFrame frame = new JFrame("Java Chat Client");
    private JTextField messageField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(16, 40);

    public ChatClientGUI() {
        // Setup GUI components
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(messageField);
        JButton sendButton = new JButton("Send");
        bottomPanel.add(sendButton);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        
        // Add event listeners
        ActionListener sendListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    out.println(messageField.getText());
                    messageField.setText("");
                }
            }
        };
        messageField.addActionListener(sendListener);
        sendButton.addActionListener(sendListener);
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
    /**
     * Prompts the user for a username.
     */
    private String getUsername() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a username:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    /**
     * Connects to the server and enters the processing loop.
     */
    private void runClient() {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Process messages from server
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                
                if (line.startsWith("SUBMITNAME")) {
                    String username = getUsername();
                    if (username != null) {
                        out.println(username);
                    } else {
                        System.exit(0);
                    }
                } else if (line.startsWith("NAMEACCEPTED")) {
                    messageField.setEditable(true);
                    frame.setTitle("Chat Room - " + line.substring(13));
                } else if (line.startsWith("NAMEALREADYEXISTS")) {
                    JOptionPane.showMessageDialog(frame, "Username already taken, please choose another.");
                } else {
                    messageArea.append(line + "\n");
                    // Auto-scroll to bottom
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error connecting to server: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI client = new ChatClientGUI();
            // Start the network communication in a separate thread to keep GUI responsive
            new Thread(client::runClient).start();
        });
    }
}
