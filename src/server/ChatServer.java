package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main server class for the Chat Application.
 * Listens for incoming client connections and manages the set of active clients.
 */
public class ChatServer {
    // Port on which the server will listen
    private static final int PORT = 8080;
    
    // Thread-safe set to keep track of all connected client handlers
    private static Set<ClientHandler> activeClients = Collections.synchronizedSet(new HashSet<>());
    
    // Thread pool for managing client handler threads efficiently
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("[SERVER] Starting the chat server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Server is listening and waiting for connections...");
            
            while (true) {
                // Wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New connection accepted from " + clientSocket.getInetAddress());
                
                // Create a new handler for this client
                ClientHandler clientThread = new ClientHandler(clientSocket, activeClients);
                activeClients.add(clientThread);
                
                // Execute the handler in the thread pool
                pool.execute(clientThread);
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all active clients except the sender.
     */
    public static void broadcastMessage(String message, ClientHandler excludeUser) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                if (client != excludeUser) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Sends a private message to a specific user.
     */
    public static void sendPrivateMessage(String targetUsername, String message, ClientHandler sender) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                if (client.getUsername() != null && client.getUsername().equalsIgnoreCase(targetUsername)) {
                    client.sendMessage("[Private from " + sender.getUsername() + "]: " + message);
                    sender.sendMessage("[Private to " + targetUsername + "]: " + message);
                    return;
                }
            }
            sender.sendMessage("[SERVER] User '" + targetUsername + "' not found.");
        }
    }
}
