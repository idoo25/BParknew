package controllers;

import java.net.URL;
import java.util.ResourceBundle;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Controller for managing the login process in the BPark client application.
 * <p>
 * Handles input validation, server connection, and communication for subscriber
 * login. It also responds to login success/failure and configures keyboard
 * behavior.
 * <p>
 * Implements {@link Initializable} to set up the login form after loading the
 * FXML.
 */
public class LoginController implements Initializable {

	/** Text field for entering the subscriber's user name. */
	@FXML
	private TextField txtUsername;

	/** Text field for entering the subscriber's user code */
	@FXML
	private TextField txtUsercode;

	/** Text field for entering the server's IP address. */
	@FXML
	private TextField txtServerIP;

	/** Button that triggers the login process. */
	@FXML
	private Button btnLogin;

	/** Label used to display status message. */
	@FXML
	private Label lblStatus;

	/** Flag to indicate whether a connection attempt is in progress. */
	private boolean isConnecting = false;
	/** Static reference to the controller instance for global access. */
	private static LoginController instance;

	/**
	 * Constructor that assigns this object to the static {@code instance} field.
	 * Useful for accessing the controller from other parts of the application.
	 */
	public LoginController() {
		instance = this;
	}

	/**
	 * Gets the current instance of the LoginController.
	 *
	 * @return the singleton- instance of this controller.
	 */
	public static LoginController getInstance() {
		return instance;
	}

	/**
	 * Initializes the login screen UI elements and attempts server connection.
	 * <p>
	 * Sets default values, adds keyboard shortcuts, and focuses the username field.
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object, or {@code null} if unknown.
	 * @param resources The resources used to localize the root object, or
	 *                  {@code null} if none.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// If the client is a user - connect to the server
		if (!BParkClientScenes.isConnected()) {
			System.out.println("Connecting to server...");
			BParkClientScenes.connectToServer();
		}
		// Set default server IP
		txtServerIP.setText("localhost");

		// Enable Enter key to login
		txtUsername.setOnKeyPressed(this::handleEnterKey);
		txtUsercode.setOnKeyPressed(this::handleEnterKey);

		// Focus on username field
		Platform.runLater(() -> txtUsername.requestFocus());
	}

	/**
	 * Sends a request to the server to retrieve current parking availability.
	 * <p>
	 * This method is typically triggered from the login screen to provide users who
	 * are not yet register about available parking spots before logging in.
	 */
	@FXML
	private void handleCheckAvailability() {
		// Send a request to the server
		Message checkMsg = new Message(Message.MessageType.CHECK_PARKING_AVAILABILITY, null);
		BParkClientScenes.sendMessage(checkMsg);

	}

	/**
	 * Handles the logic for logging into the system.
	 * <p>
	 * Validates user inputs, attempts server connection, and sends a login request.
	 * Prevents multiple simultaneous login attempts using the {@code isConnecting}
	 * flag.
	 */
	@FXML
	private void handleLogin() {
		if (isConnecting) {
			return; // Prevent multiple connection attempts
		}

		String username = txtUsername.getText().trim();
		String usercode = txtUsercode.getText().trim();
		String serverIP = txtServerIP.getText().trim();

		// Validate input
		if (username.isEmpty()) {
			showError("Please enter your username");
			txtUsername.requestFocus();
			return;
		}

		if (usercode.isEmpty()) {
			showError("Please enter your userCode");
			txtUsercode.requestFocus();
			return;
		}

		if (serverIP.isEmpty()) {
			showError("Please enter server IP address");
			txtServerIP.requestFocus();
			return;
		}

		// Update UI for connection attempt
		isConnecting = true;
		btnLogin.setDisable(true);
		btnLogin.setText("Connecting...");
		lblStatus.setText("Connecting to server...");
		lblStatus.setStyle("-fx-text-fill: #3498DB;");

		// Connect to server in background thread
		new Thread(() -> {
			try {
				BParkClientScenes.connectToServer();

				// Wait a bit for connection to establish
				Thread.sleep(500);

				// Send login request
				Platform.runLater(() -> {
					BParkClientScenes.setCurrentUser(username);

					// Send login message
					Message loginMsg = new Message(MessageType.SUBSCRIBER_LOGIN, username + "," + usercode);
					BParkClientScenes.sendMessage(loginMsg);

					lblStatus.setText("Authenticating...");
				});

			} catch (Exception e) {
				Platform.runLater(() -> {
					showError("Failed to connect to server: " + e.getMessage());
					resetLoginButton();
				});
			}
		}).start();
	}

	/**
	 * Triggered when the Enter key is pressed in the user name or user code fields.
	 * <p>
	 * Allows quick login by pressing Enter instead of clicking the login button.
	 *
	 * @param event The key event triggered by the user.
	 */
	private void handleEnterKey(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			handleLogin();
		}
	}

	/**
	 * Handles server response when login fails.
	 * <p>
	 * Displays an error message and resets the login button and focus state.
	 *
	 * @param reason A message explaining the reason for login failure.
	 */
	public void handleLoginFailed(String reason) {
		Platform.runLater(() -> {
			showError(reason != null ? reason : "Login failed. Please check your credentials.");
			resetLoginButton();
			txtUsername.requestFocus();
			txtUsername.selectAll();
		});
	}

	/**
	 * Handles server response when login is successful.
	 * <p>
	 * Displays a success message and closes the login window.
	 *
	 * @param userType The type of user logged in (subscriber, attendant , manager).
	 */
	public void handleLoginSuccess(String userType) {
		Platform.runLater(() -> {
			lblStatus.setText("Login successful! Loading interface...");
			lblStatus.setStyle("-fx-text-fill: #27AE60;");

			// Close login window - with null safety check
			if (btnLogin != null && btnLogin.getScene() != null && btnLogin.getScene().getWindow() != null) {
				btnLogin.getScene().getWindow().hide();
			}
		});
	}

	/**
	 * Resets the login button state after a failed login attempt or cancellation.
	 */
	private void resetLoginButton() {
		isConnecting = false;
		btnLogin.setDisable(false);
		btnLogin.setText("Login");
	}

	/**
	 * Displays an error message in the status label with red text styling.
	 *
	 * @param message The message to display to the user.
	 */
	private void showError(String message) {
		lblStatus.setText(message);
		lblStatus.setStyle("-fx-text-fill: #E74C3C;");
	}

}