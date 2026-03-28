package com.mygdx.hillclimbracing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GestureControlServer - TCP server to receive gesture commands from Python client
 * and forward them to the Hill Climb Racing game.
 */
public class GestureControlServer implements Runnable {
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> currentCommand = new AtomicReference<>("NONE");
    private final HillClimbGame game;

    /**
     * Constructor for the gesture control server
     * @param game The Hill Climb Racing game instance
     * @param port The port to listen on
     */
    public GestureControlServer(HillClimbGame game, int port) {
        this.game = game;
        this.port = port;
    }

    /**
     * Start the server in a separate thread
     */
    public void start() {
        Thread serverThread = new Thread(this);
        serverThread.setDaemon(true); // Make it a daemon thread so it stops when the game exits
        serverThread.start();
        System.out.println("Gesture Control Server started on port " + port);
    }

    /**
     * Stop the server
     */
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    /**
     * Process gesture commands and control the game
     * @param command The command to process
     */
    private void processCommand(String command) {
        currentCommand.set(command);

        // Apply the command to the game controls
        switch (command) {
            case "ACCELERATE":
                game.setGasPressed(true);
                game.setBrakePressed(false);
                break;
            case "BRAKE":
                game.setGasPressed(false);
                game.setBrakePressed(true);
                break;
            case "RESPAWN":
                game.respawnVehicle();
                break;
            case "NONE":
                // Release all controls
                game.setGasPressed(false);
                game.setBrakePressed(false);
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    /**
     * Get the current command being executed
     * @return The current command
     */
    public String getCurrentCommand() {
        return currentCommand.get();
    }

    /**
     * Main server thread run method
     */
    @Override
    public void run() {
        running.set(true);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (running.get()) {
                try {
                    // Accept client connections
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Handle client in a new thread
                    Thread clientThread = new Thread(() -> handleClient(clientSocket));
                    clientThread.setDaemon(true);
                    clientThread.start();

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Handle an individual client connection
     * @param clientSocket The client socket
     */
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("GESTURE:")) {
                    String command = line.substring(8).trim();
                    System.out.println("Received gesture command: " + command);
                    processCommand(command);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
