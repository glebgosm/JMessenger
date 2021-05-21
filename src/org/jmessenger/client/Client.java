package org.jmessenger.client;

import org.jmessenger.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

/**
 * Entrypoint class for JMessenger users.
 */
class Client {
    private Connection connection;
    private View view;
    private String userName;
    private String password;

    public static void main(String[] args) {
        // load server configuration from file
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(new File("server.properties")));
        } catch (IOException e) {
            System.out.println("Failed to read settings from file \"server.properties\"");
            e.printStackTrace();
            return;
        }
        // create a client
        Client client = new Client();
        // open GUI
        client.view = View.getInstance("JMessenger",client);
        if (client.view == null) {
            System.out.println("Failed to create a GUI form.");
            return;
        }
        // Establish connection with the server and authenticate user
        while (true) {
            // ask user name
            client.userName = client.view.getUserInput("Enter your username");
            if (client.userName == null) {
                System.out.println("Failed to request user name.");
                client.view.dispose();
                return;
            }
            // ask user password
            client.password = client.view.getUserInput("Enter password");
            if (client.password == null) {
                System.out.println("Failed to request user password.");
                client.view.dispose();
                return;
            }
            // initialize connection and authenticate
            try {
                String port = properties.getProperty("SERVER_PORT");
                boolean connectionOK = client.connectServer(
                        properties.getProperty("SERVER_ADDRESS"),
                        Integer.parseInt(port)
                );
                if (connectionOK) break;
            } catch (Exception e) {
                client.view.popupError("Failed to connect to the server.");
                client.view.dispose();
                return;
            }
            client.view.popupError("Password incorrect. Try again.");
        }
        // listen to incoming messages
        while (true) {
            try {
                Message message = client.connection.receiveMessage();
                if (message.getType() == MessageType.TEXT) {
                    client.view.displayMessage(message.getText());
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Establish connection with the server.
     * @throws IOException if connection fails
     */
    private boolean connectServer(String serverAddress, int serverPort) throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        connection = new Connection(socket);
        // send username
        while (true) {
            Message message = connection.receiveMessage();
            if (message.getType() != MessageType.NAME_REQUEST) continue;
            connection.sendMessage(new Message(MessageType.TEXT, userName));
            break;
        }
        // send password
        while (true) {
            if (connection.receiveMessage().getType() != MessageType.PASSWORD_REQUEST) continue;
            connection.sendMessage(new Message(MessageType.TEXT, password));
            break;
        }
        return connection.receiveMessage().getType() == MessageType.LOGIN_OK;
    }

    /**
     * Send a message to the server.
     * @param text text to be sent
     */
    public void sendMessage(String text) {
        try {
            connection.sendMessage(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            e.printStackTrace( );
            view.displayMessage("\nError occurred while sending the message. Try again.\n");
        }
    }

    /**
     * Disconnect from server.
     */
    public void disconnect() {
        if (connection == null) return;
        try {
            connection.sendMessage(new Message(MessageType.DISCONNECT));
        } catch (IOException e) {
            e.printStackTrace( );
        }
        connection = null;
    }
}

































