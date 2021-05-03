package org.jmessenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A class responsible for sending and receiving messages,
 * using a <code>Socket</code> instance
 */
public class Connection {
    private final Socket socket;
    private final ObjectInputStream objectInputStream;
    private final ObjectOutputStream objectOutputStream;

    /**
     * Creates a connection associated with the given socket.
     * @param socket a <code>Socket</code> instance which will be used to send and receive messages
     * @throws IOException if connection fails or the socket is not connected
     */
    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Send a message via the socket, associated with this connection.
     * @param message the <code>Message</code> object to be sent
     * @throws IOException if connection fails
     */
    public void sendMessage(Message message) throws IOException {
        if (message==null) return;
        // here we synchronize the stream rather than this connection to avoid deadlock
        synchronized (objectOutputStream) {
            objectOutputStream.writeObject(message);
        }
    }

    /**
     * Read a message from the socket, associated with this connection.
     * This method blocks until a message arrives at the socket.
     * @return the <code>Message</code> object retrieved from the socket
     * @throws IOException if connection fails
     */
    public Message receiveMessage() throws IOException {
        // here we synchronize the stream rather than this connection to avoid deadlock
        synchronized (objectInputStream) {
            // loop until a Message instance arrives
            while (true) {
                try {
                    Object o = objectInputStream.readObject();
                    if (o instanceof Message)
                        return (Message) o;
                } catch (ClassNotFoundException e) {
                    // try again - go to next loop iteration
                }
            }
        }
    }

    /**
     * Close the connection: close the associated socket.
     * @throws IOException if connection fails
     */
    public void close() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        socket.close();
    }
}
