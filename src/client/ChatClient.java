package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Console-based Chat Client.
 * Connects to the ChatServer and handles user input and incoming messages.
 */
public class ChatClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private Socket socket;

    public ChatClient() {
        scanner = new Scanner(System.in);
    }

    public void startClient() {
        try {
            // Connect to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to the chat server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start a new thread to listen for messages from the server
            Thread listenerThread = new Thread(new IncomingMessageHandler());
            listenerThread.start();

            // Main thread handles user input
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("/quit")) {
                    out.println("/quit");
                    break; // Exit the loop to trigger cleanup
                }
                out.println(input);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Cleans up resources when disconnecting.
     */
    private void cleanup() {
        System.out.println("Disconnecting from server...");
        try {
            if (scanner != null) scanner.close();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * Inner class to handle messages arriving from the server concurrently.
     */
    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("SUBMITNAME")) {
                        System.out.print("Enter your username: ");
                    } else if (serverMessage.startsWith("NAMEACCEPTED")) {
                        System.out.println("Username accepted! You are now in the chat.");
                        System.out.println("Type your messages below. Use '@username message' for private messages or '/quit' to exit.");
                    } else if (serverMessage.startsWith("NAMEALREADYEXISTS")) {
                        System.out.println("Username is already taken.");
                        System.out.print("Enter a different username: ");
                    } else {
                        System.out.println(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.startClient();
    }
}
