package controllers;

import static common.Message.MessageType.KIOSK_ID_LOGIN;
import static common.Message.MessageType.KIOSK_RF_LOGIN;

import java.util.Optional;

import client.BParkKioskScenes;
import common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

public class KioskController {

	@FXML
	private Button btnLoginByID;

	@FXML
	private Button btnLoginByRF;

	@FXML
	private Button btnCheckAvailability;

	@FXML
	private Button btnExitKiosk;
	private static Stage mainStage;

	public static void setMainStage(Stage stage) {
		mainStage = stage;
	}

	@FXML
	private void handleCheckAvailability() {
		// Send a request to the server
		Message checkMsg = new Message(Message.MessageType.CHECK_PARKING_AVAILABILITY, null);
		BParkKioskScenes.sendMessage(checkMsg);

	}

	@FXML
	private void handleExitKiosk() {
		// Show confirmation dialog
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Exit Kiosk");
		alert.setHeaderText(null);
		alert.setContentText("Are you sure you want to exit the kiosk application?");

		// Custom buttons
		ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(exitButton, cancelButton);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == exitButton) {
			try {
				// Disconnect from server if connected
				if (BParkKioskScenes.isConnected()) {
					BParkKioskScenes.sendStringMessage("KioskTerminating");
					BParkKioskScenes.disconnect();
				}

				// Close the application
				Platform.exit();
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void handleLoginByID() {
		Platform.runLater(() -> {
			Dialog<Pair<String, String>> dialog = new Dialog<>();
			dialog.setTitle("Login");
			dialog.setHeaderText("Please enter your Username and User ID");

			ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
			dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(20, 150, 10, 10));

			TextField usernameField = new TextField();
			usernameField.setPromptText("Username");
			TextField userIDField = new TextField();
			userIDField.setPromptText("User ID");

			grid.add(new Label("Username:"), 0, 0);
			grid.add(usernameField, 1, 0);
			grid.add(new Label("User ID:"), 0, 1);
			grid.add(userIDField, 1, 1);

			dialog.getDialogPane().setContent(grid);

			var loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
			loginButton.setDisable(true);

			usernameField.textProperty().addListener((obs, oldVal, newVal) -> loginButton
					.setDisable(newVal.trim().isEmpty() || userIDField.getText().trim().isEmpty()));
			userIDField.textProperty().addListener((obs, oldVal, newVal) -> loginButton
					.setDisable(newVal.trim().isEmpty() || usernameField.getText().trim().isEmpty()));

			dialog.setResultConverter(dialogButton -> {
				if (dialogButton == loginButtonType) {
					return new Pair<>(usernameField.getText(), userIDField.getText());
				}
				return null;
			});

			Optional<Pair<String, String>> result = dialog.showAndWait();

			result.ifPresent(pair -> {
				String username = pair.getKey();
				String userIDStr = pair.getValue();
				try {
					int userID = Integer.parseInt(userIDStr);
					Message msg = new Message(KIOSK_ID_LOGIN, username + "," + userID);
					BParkKioskScenes.sendMessage(msg);
				} catch (NumberFormatException e) {
					showAlert("Invalid Input", "User ID must be numeric.");
				}
			});
		});
	}

	@FXML
	private void handleLoginByRF() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Login by RF");
		dialog.setHeaderText(null);
		dialog.setContentText("Enter your User ID:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(idStr -> {
			try {
				int userID = Integer.parseInt(idStr);
				Message msg = new Message(KIOSK_RF_LOGIN, userID);
				BParkKioskScenes.sendMessage(msg);
			} catch (NumberFormatException e) {
				showAlert("Invalid Input", "Please enter a valid numeric User ID.");
			}
		});
	}

	public static void handleKioskLoginResult(Object content) {
		if (content instanceof String response) {
			if (!response.isEmpty()) {
				// Split "John Doe,4"
				String[] parts = response.split(",");
				if (parts.length == 2) {
					String name = parts[0].trim();
					int userID = Integer.parseInt(parts[1].trim());

					// Store for future operations
					KioskDashboardController.setLoggedInUser(name, userID);

					// Welcome message and load dashboard
					showWelcomeAndLoadDashboard(name);
				} else {
					showAlertStatic("Login Failed", "Invalid login data received from server.");
				}
			} else {
				showAlertStatic("Login Failed", "Invalid credentials or user not found.");
			}
		}
	}

	private void showAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private static void showAlertStatic(String title, String message) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(message);
			alert.showAndWait();
		});
	}

	private static void showWelcomeAndLoadDashboard(String name) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Login Success");
			alert.setHeaderText(null);
			alert.setContentText("Welcome " + name + "!");
			alert.showAndWait();

			try {
				FXMLLoader loader = new FXMLLoader(KioskController.class.getResource("/client/KioskDashboard.fxml"));
				Parent dashboardRoot = loader.load();
				Scene dashboardScene = new Scene(dashboardRoot);
				dashboardScene.getStylesheets()
						.add(KioskController.class.getResource("/css/BParkStyle.css").toExternalForm());
				mainStage.setScene(dashboardScene);
			} catch (Exception e) {
				e.printStackTrace();
				showAlertStatic("Error", "Failed to load dashboard screen.");
			}
		});
	}
}