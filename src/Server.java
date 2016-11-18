import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Sven & Jeroen
 * Date created: 17-11-2016
 */
public class Server {

    private static final int SERVER_PORT = 1234;

    private Map<User, ClientThread> clients;

    // initialize the client map
    private Server() {
        clients = new HashMap<>();
    }

    // start the server when the program starts
    public static void main(String Args[]) {
        new Server().run();
    }

    private void run() {
        try {
            // open the socket on the port
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            // success!
            System.out.println("Server started on port: " + SERVER_PORT);

            //noinspection InfiniteLoopStatement
            while (true) {
                // wait for a client to connect
                final Socket clientSocket = serverSocket.accept();
                // create a new client thread and start it
                new ClientThread(clientSocket, this).start();
                System.out.println("Client connected.");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Forward a message to all other clients
     *
     * @param from    the User that send the message
     * @param message the message that was send
     */
    void forward(User from, String message) {

        // iterate through all users
        // if the user is not the one that send the message
        clients.values().stream().filter(ct -> ct.getUser() != from).forEach(ct -> {
            final Socket clientSocket = ct.getSocket();
            try {
                final OutputStream os = clientSocket.getOutputStream();
                final PrintWriter writer = new PrintWriter(os);

                final JSONObject json = new JSONObject();
                json.put("message", message);
                json.put("username", from.getUsername());
                json.put("colour", from.getColour());

                writer.println(json.toString());
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Whispers something to a given user.
     * if user not found, send it back.
     *
     * @param message  is the message
     * @param username is the username
     * @param socket   the socket
     */
    void whisper(String username, String message, Socket socket, User user) {
        //loop a Map
        boolean isFound = false;
        for (ClientThread ct : clients.values()) {
            if (ct.getUser().getUsername().equalsIgnoreCase(username)) {
                try {
                    final OutputStream os = ct.getSocket().getOutputStream();
                    final PrintWriter writer = new PrintWriter(os);

                    final JSONObject json = new JSONObject();
                    json.put("whisper", message);
                    json.put("from", user.getUsername());
                    json.put("colour", user.getColour());

                    writer.println(json.toString());
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isFound = true;
                break;
            }
        }

        if (!isFound) {
            try {
                final OutputStream os = socket.getOutputStream();
                final PrintWriter writer = new PrintWriter(os);

                final JSONObject json = new JSONObject();
                json.put("message", "Couldn't find the user: " + username);
                json.put("username", "server");
                json.put("colour", "RED");

                writer.println(json.toString());
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a doing message
     *
     * @param action the action
     * @param from   the user
     */
    void me(String action, User from) {
        final JSONObject json = new JSONObject();

        json.put("me", from.getUsername() + " " + action);
        json.put("colour", from.getColour());
        sendToAll(json);
    }

    /**
     * Send a message that a certain user has changed their colour
     *
     * @param user      the user that changed their colour
     * @param oldColour the old colour
     */
    void changedColour(User user, String oldColour) {
        final JSONObject json = new JSONObject();

        json.put("me", "[SERVER]: " + user.getUsername() + " changed their colour to " + user.getColour().toUpperCase());
        json.put("colour", oldColour);
        sendToAll(json);
    }

    /**
     * Send a message that a certain user has changed their username
     *
     * @param user        the user that changed their colour
     * @param oldUsername the old username
     */
    void changedUsername(User user, String oldUsername) {
        final JSONObject json = new JSONObject();

        json.put("me", "[SERVER]: " + oldUsername + " changed their username to " + user.getUsername());
        json.put("colour", user.getColour());
        sendToAll(json);
    }

    /**
     * Save a client connection by a User
     *
     * @param user   the user that is connected on that thread
     * @param thread the thread the user is connected on
     */
    void connect(User user, ClientThread thread) {
        if (!user.getUsername().isEmpty()) clients.put(user, thread);
    }

    /**
     * Check if a user already exists
     *
     * @param username the username to check
     */
    boolean checkUsernameExist(final String username) {

        // iterate through all clients
        for (ClientThread ct : clients.values()) {
            if (ct.getUser().getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a client from the map
     *
     * @param user the user to remove
     */
    void removeClient(User user) throws IOException {
        if (user != null) System.err.println("User with username '" + user.getUsername() + "' disconnected.");
        clients.get(user).getSocket().close();
        clients.remove(user);
    }

    /**
     * Send a certain json message to all users
     *
     * @param json the message to send
     */
    private void sendToAll(JSONObject json) {
        // iterate through all users
        clients.values().forEach(ct -> {
            final Socket clientSocket = ct.getSocket();
            try {
                final OutputStream os = clientSocket.getOutputStream();
                final PrintWriter writer = new PrintWriter(os);

                writer.println(json.toString());
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}