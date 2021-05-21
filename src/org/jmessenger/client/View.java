package org.jmessenger.client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class responsible for GUI of the client side.
 */
public class View extends JFrame {
    private static final int FRAME_WIDTH = 500;
    private static final int FRAME_HEIGHT = 300;
    private JTextArea textArea = new JTextArea(  );
    private JTextField textField = new JTextField(  );
    private JButton sendButton = new JButton("Send");
    private Client client;

    /**
     * Create a new <code>View</code> instance, associated with the given client.
     * This is the only correct way to instantiate this class from outside.
     * @param title window title
     * @param client <code>Client</code> instance associated with this view
     * @return a <code>View</code> instance or null if an error occurs.
     */
    public static View getInstance(String title, Client client) {
        AtomicReference<View> view = new AtomicReference<>();
        if (!runOnEDT(() -> view.set(new View(title,client))))
            return null;
        return view.get();
    }

    /**
     * All Swing operations must be run on the EDT thread,
     * so this constructor must be called only from <code>getInstance</code> method.
     * @param title window title
     * @param client <code>Client</code> instance associated with the view created
     */
    private View(String title, Client client) {
        super(title);
        this.client = client;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter( ) {
            @Override
            public void windowClosing(WindowEvent e) {
                SwingWorker<Void,Void> worker = new SwingWorker<>( ) {
                    @Override
                    protected Void doInBackground() {
                        client.disconnect( );
                        return null;
                    }
                };
                worker.execute();
            }
        });
        setLayout(null);
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        addMessagesArea( );
        addInputTextField( );
        addSendButton( );
        setVisible(true);
        textField.grabFocus();
    }

    /**
     * Create messages area.
     */
    private void addMessagesArea() {
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBounds(5,5,FRAME_WIDTH - 10,FRAME_HEIGHT - 70);
        add(scrollPane);
    }

    /**
     * Create "send" button.
     */
    private void addSendButton() {
        sendButton.setBounds(FRAME_WIDTH - 100,FRAME_HEIGHT - 60,95, 29);
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> {
            String text = textField.getText() + "\n";
            sendMessage(text);
            textField.setText("");
            textField.grabFocus();
        });
        add(sendButton);
    }

    /**
     * Setup an input text box.
     */
    private void addInputTextField() {
        textField.setBounds(5,FRAME_HEIGHT - 60,FRAME_WIDTH - 105,30);
        textField.addKeyListener(new KeyAdapter( ) {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    sendButton.doClick();
            }
        });
        textField.getDocument().addDocumentListener(new DocumentListener( ) {
            @Override
            public void insertUpdate(DocumentEvent e) {
                sendButton.setEnabled(!textField.getText().equals(""));
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                sendButton.setEnabled(!textField.getText().equals(""));
            }
            @Override
            public void changedUpdate(DocumentEvent e) { }
        });
        add(textField);
    }

    /**
     * Induce the associated client to send a message.
     * This must be done on a thread other than the EDT, use one of the Swing Worker threads.
     * @param text text to be sent
     */
    private void sendMessage(String text) {
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                client.sendMessage(text);
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Request user name from the user.
     * @return user name entered
     */
    public String getUserInput(String requestText) {
        AtomicReference<String> name = new AtomicReference<>();
        boolean ok = runOnEDT(() ->
                name.set(JOptionPane.showInputDialog(
                        this,
                        requestText,
                        "Client configuration",
                        JOptionPane.QUESTION_MESSAGE)
                )
        );
        if (!ok) return null;
        return name.get();
    }

    /**
     * Pop up a dialog to inform user about an internal error.
     * @param errorMessage error message
     */
    public void popupError(String errorMessage) {
        runOnEDT(() ->
                JOptionPane.showMessageDialog(
                this,
                errorMessage,
                "Ошибка",
                JOptionPane.ERROR_MESSAGE)
        );
    }

    /**
     * Display the incoming message.
     * @param text message text to be displayed
     */
    public void displayMessage(String text) {
        runOnEDT(() -> textArea.append(text));
    }

    /**
     * Execute a runnable task on the Event Dispatching Thread.
     * All Swing operations must work on the EDT.
     * This method blocks until the operation is complete.
     * @return <code>true</code> if execution was successful and <code>false</code> otherwise.
     */
    private static boolean runOnEDT(Runnable task) {
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

}
