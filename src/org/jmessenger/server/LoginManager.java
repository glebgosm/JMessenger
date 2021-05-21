package org.jmessenger.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

// Authentication manager based on username/password pairs
public class LoginManager {
    private static LoginManager loginManager;
    private Connection sqlConnection;
    private PreparedStatement authStatement;
    private PreparedStatement addUserStatement;

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/jm_db?user=postgres&password=123";
    private static final String DB_DRIVER = "org.postgresql.Driver";
    private static final String SQL_AUTH_REQUEST = "SELECT * FROM jm_auth_table WHERE username = ?";
    private static final String SQL_INSERT_REQUEST = "INSERT INTO jm_auth_table (username, password) VALUES (?, ?)";
    private static final String SQL_CREATE_TABLE_REQUEST =
            "CREATE TABLE IF NOT EXISTS jm_auth_table(" +
                    "user_id BIGSERIAL PRIMARY KEY NOT NULL, " +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(50))";

    // Singleton class
    private LoginManager() throws ClassNotFoundException, SQLException {
        // load SQL driver
        Class.forName(DB_DRIVER);
        // create a connection to the DB
        sqlConnection = DriverManager.getConnection(DB_URL);
        // create a table with usernames/passwords
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute(SQL_CREATE_TABLE_REQUEST);
        }
        // initialize statements
        authStatement = sqlConnection.prepareStatement(SQL_AUTH_REQUEST);
        addUserStatement = sqlConnection.prepareStatement(SQL_INSERT_REQUEST);
    }


    /**
     * Instantiate a singleton class
     * @return <code>LoginManager</code> instance
     */
    static LoginManager getInstance() throws AuthorizationException {
        if (loginManager == null) {
            try {
                loginManager = new LoginManager();
            } catch (Exception e) {
                throw new AuthorizationException(e);
            }
        }
        return loginManager;
    }


    /**
     * Check if the pair username/password is valid, i.e. is present in the database
     * @param username user name
     * @param password password (may be empty or null)
     * @return <code>LoginManager.AuthResponse.USER_OK</code> if such user exists and password is correct, <br>
     *         <code>LoginManager.AuthResponse.USER_NOT_EXIST</code> if username does not exist, <br>
     *         <code>LoginManager.AuthResponse.PASSWORD_INCORRECT</code> if username is present, but password is incorrect
     * @throws AuthorizationException if any exception occurs
     */
    AuthResponse checkCredentials(String username, String password) throws AuthorizationException {
        try {
            if (username == null || username.equals("")) return AuthResponse.USER_NOT_EXIST;
            // send SQL request: only 0 or 1 row can be returned (username is unique)
            authStatement.setString(1, username);
            ResultSet resultSet = authStatement.executeQuery();
            if (!resultSet.next()) return AuthResponse.USER_NOT_EXIST;
            String responsePassword = resultSet.getString("password");
            // compare passwords
            if (responsePassword == null)
                return password == null ? AuthResponse.USER_OK : AuthResponse.PASSWORD_INCORRECT;
            else if (password == null)
                return AuthResponse.PASSWORD_INCORRECT;
            else {
                password = getEncryptedPassword(password);
                if (password.equals(responsePassword)) return AuthResponse.USER_OK;
                else return AuthResponse.PASSWORD_INCORRECT;
            }
        } catch(Exception e) {
            throw new AuthorizationException(e);
        }
    }


    /**
     * Add a nuw username/password pair to the database.
     * @param username user name
     * @param password password, may be null or empty
     * @return true if the user was successfully added, false if the addition failed
     * @throws AuthorizationException if any exception occurs
     */
    boolean addUser(String username, String password) throws AuthorizationException {
        try {
            if (username == null || username.equals("")) return false;
            if (password != null) password = getEncryptedPassword(password);
            // send SQL request: only 0 or 1 row can be returned (username is unique)
            addUserStatement.setString(1, username);
            addUserStatement.setString(2, password);
            try {
                addUserStatement.executeUpdate();
                return true;
            } catch(SQLException e) {
                // if such username is already present in the database,
                // or any other SQL error occurs, then SQLException will be thrown
                return false;
            }
        } catch(Exception e) {
            throw new AuthorizationException(e);
        }
    }


    // encrypt the password: the DB actually stores a "SHA-256" digest of the password
    private String getEncryptedPassword(String password) throws Exception {
        if (password == null) throw new IllegalArgumentException("Cannot encrypt a null password.");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] pwdBytes = password.getBytes("UTF-8");
        pwdBytes = md.digest(pwdBytes);
        return new String(pwdBytes,"UTF-8");
    }


    /**
     * Authentication response type
     */
    enum AuthResponse {
        /**
         * Incorrect username
         */
        USER_NOT_EXIST,
        /**
         * Username/password are correct
         */
        USER_OK,
        /**
         * Username exists, but password is incorrect
         */
        PASSWORD_INCORRECT
    }


    public static void main(String[] args) throws AuthorizationException {
        LoginManager loginManager = getInstance();
        loginManager.addUser("gleb", "123");
        System.out.println(loginManager.checkCredentials("gleb", "123"));
        System.out.println(loginManager.checkCredentials("gleb", "1234"));
        System.out.println(loginManager.checkCredentials("gleb1", "123"));
    }
}


















