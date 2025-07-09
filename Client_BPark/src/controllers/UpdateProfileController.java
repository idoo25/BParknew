package controllers;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller responsible for managing the "Update Profile" screen of the
 * application.
 * <p>
 * Allows subscribers to update their email, phone number, and car number. This
 * controller handles user interactions, performs validation, and sends
 * formatted messages to the server through {@link BParkClientApp}.
 */
public class UpdateProfileController {

	/**
	 * Singleton-static instance reference to this controller.
	 * <p>
	 * Allows other classes in the application to access this controller and update
	 * the UI directly.
	 */
	public static UpdateProfileController instance;

	/**
	 * Text field where the user can enters or updates their email address.
	 */
	@FXML
	private TextField emailField;

	/**
	 * Text field where the user can enters or updates their phone number.
	 */
	@FXML
	private TextField phoneField;

	/**
	 * Text field where the user can enters or updates their car number.
	 */
	@FXML
	private TextField carNumberField;

	/**
	 * Label used to display status messages.
	 */
	@FXML
	private Label statusLabel;

	/**
	 * Initializes the controller.
	 * <p>
	 * This method is called automatically after the FXML file is loaded. It sets
	 * the static instance reference and sends a request to the server to fetch the
	 * current subscriber's data using the logged-in user ID.
	 */
	@FXML
	public void initialize() {
		instance = this;
		String userId = BParkClientScenes.getCurrentUser();
		Message requestMsg = new Message(MessageType.REQUEST_SUBSCRIBER_DATA, userId);
		BParkClientScenes.sendMessage(requestMsg);
	}

	/**
	 * Handles the "Update" button click.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Reads and trims user input from all fields.</li>
	 * <li>Uses prompt text values as fallback if fields are left empty.</li>
	 * <li>Ensures at least one field has content before sending a request.</li>
	 * <li>Sends the updated information to the server via {@link Message}.</li>
	 * <li>Displays a success or error message in {@code statusLabel}.</li>
	 * </ul>
	 */
	@FXML
	private void handleUpdate() {
		// Read only what the user typed
		String emailInput = emailField.getText().trim();
		String phoneInput = phoneField.getText().trim();
		String carInput = carNumberField.getText().trim();

		// If all fields are truly empty (user typed nothing)
		if (emailInput.isEmpty() && phoneInput.isEmpty() && carInput.isEmpty()) {
			statusLabel.setText("Please fill in at least one field before updating.");
			statusLabel.setStyle("-fx-text-fill: red;");
			return;
		}

		// If some fields are empty, fallback to promptText
		String email = emailInput.isEmpty() ? emailField.getPromptText() : emailInput;
		String phone = phoneInput.isEmpty() ? phoneField.getPromptText() : phoneInput;
		String carNumber = carInput.isEmpty() ? carNumberField.getPromptText() : carInput;

		String userId = BParkClientScenes.getCurrentUser();
		String data = userId + "," + phone + "," + email + "," + carNumber;

		Message msg = new Message(MessageType.UPDATE_SUBSCRIBER_INFO, data);
		BParkClientScenes.sendMessage(msg);

		statusLabel.setText("Profile update sent.");
		statusLabel.setStyle("-fx-text-fill: green;");
	}

	/**
	 * Sets prompt text for all input fields.
	 * <p>
	 * This is typically called by the response handler after retrieving the current
	 * subscriber details from the server.
	 *
	 * @param email  The subscriber's email to show as a placeholder.
	 * @param phone  The subscriber's phone number to show as a placeholder.
	 * @param carNum The subscriber's car number to show as a placeholder.
	 */
	public void setFieldPrompts(String email, String phone, String carNum) {
		emailField.setPromptText(email);
		phoneField.setPromptText(phone);
		carNumberField.setPromptText(carNum);
	}

	/**
	 * Sets the text of the email field.
	 * <p>
	 * Add public setters so the ClientMessageHandler can set fields
	 *
	 * @param email The email address to set in the text field.
	 */
	public void setEmail(String email) {
		emailField.setText(email);
	}

	/**
	 * Sets the text of the phone field.
	 *
	 * @param phone The phone number to set in the text field.
	 */
	public void setPhone(String phone) {
		phoneField.setText(phone);
	}

}
