package org.jmessenger.server;

import org.jmessenger.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class Server {
    private final ServerSocket serverSocket;
    private final LoginManager loginManager;
    // One <code>Connection</code> object per client
    private Map<String, Connection> connectionMap = new HashMap<>();

    public static void main(String[] args) {
        // load server configuration from file
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(new File("server.properties")));
        } catch (IOException e) {
            System.out.println("Server: Failed to read settings from file \"server.properties\"");
            return;
        }
        // create a server and start it
        String port = properties.getProperty("SERVER_PORT");
        Server server = null;
        try {
            server = new Server(Integer.parseInt(port));
        } catch (Exception e) {
            System.out.println("Failed to start a server.");
            return;
        }
        System.out.println("Server started");
        server.start();
    }

    /**
     * Creates a <code>ServerSocket</code> on the given port
     * @param serverPort port to listen to
     * @throws IOException if I/O error occurs while opening the socket
     */
    private Server(int serverPort) throws Exception {
        // create a server socket
        try {
            serverSocket = new ServerSocket(serverPort);
            loginManager = LoginManager.getInstance();
        } catch (IOException e) {
            System.out.println("Server: Failed to connect to the port " + serverPort);
            throw e;
        } catch (AuthorizationException e) {
            System.out.println("Server: Failed to connect to database.");
            throw e;
        }
    }

    /**
     * Start listening a server socket, wait for incoming connections.
     * For each new established connection start a new thread to listen to it.
     */
    private void start() {
        while (true) {
            Connection connection;
            try {
                Socket socket = serverSocket.accept( );
                connection = new Connection(socket);
            } catch (Exception e) {
                continue;
            }
            ConnectionHandler connectionHandler = new ConnectionHandler(connection);
            connectionHandler.setDaemon(true);
            connectionHandler.start();
        }
    }

    /**
     * A thread communicating with a particular connection.
     */
    private class ConnectionHandler extends Thread {
        private final Connection connection;
        private String userName = null;
        public ConnectionHandler(Connection connection) {
            this.connection = connection;
        }
        @Override
        public void run() {
            try {
                // request client name
                while (userName == null || userName.equals("") || connectionMap.containsKey(userName)) {
                    connection.sendMessage(new Message(MessageType.NAME_REQUEST));
                    userName = connection.receiveMessage( ).getText( );
                }
                // request client password
                connection.sendMessage(new Message(MessageType.PASSWORD_REQUEST));
                String password = connection.receiveMessage().getText();
                // authenticate user
                LoginManager.AuthResponse authResponse = loginManager.checkCredentials(userName,password);
                if (authResponse == LoginManager.AuthResponse.USER_NOT_EXIST) {
                    loginManager.addUser(userName,password);
                } else if (authResponse == LoginManager.AuthResponse.PASSWORD_INCORRECT) {
                    connection.sendMessage(new Message(MessageType.PASSWORD_INCORRECT));
                    return;
                }
                connection.sendMessage(new Message(MessageType.LOGIN_OK));
            } catch (Exception e) {
                // drop connection
                return;
            }
            // authentication ok, process the user
            connectionMap.put(userName, connection);
            System.out.println("Connected a new user: " + userName);
            // start listening to the client and broadcasting its messages
            while(true) {
                Message message;
                try {
                    message = connection.receiveMessage( );
                } catch (Exception e) {
                    continue;
                }
                if (message.getType() == MessageType.DISCONNECT) {
                    connectionMap.remove(userName);
                    System.out.println("Disconnected user " + userName);
                    return;
                }
                message.setText(userName + ": " + message.getText());
                // broadcast the message
                for (String name : connectionMap.keySet( )) {
                    Connection c = connectionMap.get(name);
                    try {
                        c.sendMessage(message);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
    }

}






















