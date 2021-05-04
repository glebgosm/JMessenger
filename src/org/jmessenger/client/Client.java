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
        // ask user name
        client.userName = client.view.getUserName();
        if (client.userName == null) {
            System.out.println("Failed to request user name.");
            client.view.dispose();
            return;
        }
        // initialize connection
        try {
            String port = properties.getProperty("SERVER_PORT");
            client.connectServer(properties.getProperty("SERVER_ADDRESS"), Integer.parseInt(port));
        } catch(Exception e) {
            client.view.popupError("Failed to connect to the server.");
            client.view.dispose();
            return;
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
    private void connectServer(String serverAddress, int serverPort) throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        connection = new Connection(socket);
        while (true) {
            Message message = connection.receiveMessage( );
            if (message.getType() != MessageType.NAME_REQUEST) continue;
            connection.sendMessage(new Message(MessageType.TEXT, userName));
            return;
        }
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

































