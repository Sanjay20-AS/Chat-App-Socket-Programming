package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

/**
 * Handles communication for a single connected client in its own thread.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Set<ClientHandler> activeClients;
    private String username;

    public ClientHandler(Socket socket, Set<ClientHandler> activeClients) {
        this.socket = socket;
        this.activeClients = activeClients;
    }

    @Override
    public void run() {
        try {
            // Setup input and output streams for this socket
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Request username from the client
            out.println("SUBMITNAME");
            while (true) {
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    out.println("SUBMITNAME");
                    continue;
                }
                
                // Basic validation could be added here (e.g., checking for duplicates)
                synchronized (activeClients) {
                    boolean isUnique = true;
                    for (ClientHandler client : activeClients) {
                        if (client != this && username.equalsIgnoreCase(client.getUsername())) {
                            isUnique = false;
                            break;
                        }
                    }
                    if (isUnique) {
                        break; // Username accepted
                    } else {
                        out.println("NAMEALREADYEXISTS");
                    }
                }
            }

            out.println("NAMEACCEPTED " + username);
            System.out.println("[SERVER] " + username + " has joined the chat.");
            ChatServer.broadcastMessage("[SERVER] " + username + " has joined the chat.", this);

            // Read messages from the client in a loop
            String message;
            while ((message = in.readLine()) != null) {
                // Command parsing for private messages
                if (message.startsWith("@")) {
                    int firstSpaceIndex = message.indexOf(" ");
                    if (firstSpaceIndex != -1) {
                        String targetUser = message.substring(1, firstSpaceIndex);
                        String privateMessage = message.substring(firstSpaceIndex + 1);
                        ChatServer.sendPrivateMessage(targetUser, privateMessage, this);
                    } else {
                        out.println("[SERVER] Invalid private message format. Use: @username message");
                    }
                } else if (message.equalsIgnoreCase("/quit")) {
                    break;
                } else {
                    // Standard broadcast message
                    ChatServer.broadcastMessage("[" + username + "]: " + message, this);
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error handling client " + username + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Sends a message to the client associated with this handler.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getUsername() {
        return username;
    }

    /**
     * Cleans up resources and notifies others of disconnection.
     */
    private void cleanup() {
        if (username != null) {
            System.out.println("[SERVER] " + username + " has left the chat.");
            ChatServer.broadcastMessage("[SERVER] " + username + " has left the chat.", this);
        }
        
        activeClients.remove(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
