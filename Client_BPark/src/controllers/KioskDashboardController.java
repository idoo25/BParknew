package controllers;

import static common.Message.MessageType.ACTIVATE_RESERVATION_KIOSK;
import static common.Message.MessageType.ENTER_PARKING_KIOSK;
import static common.Message.MessageType.FORGOT_CODE_KIOSK;
import static common.Message.MessageType.RETRIEVE_CAR_KIOSK;

import java.util.Optional;

import client.BParkKioskScenes;
import common.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class KioskDashboardController {

	private static String loggedInUsername; // Added username storage
	private static int loggedInUserID;

	// UI element declarations
	@FXML
	private Label lblUserInfo; // Added label reference

	@FXML
	private Button btnEnterParking;

	@FXML
	private Button btnRetrieveCar;

	@FXML
	private Button btnForgotCode;

	@FXML
	private Button btnActivateReservation;

	@FXML
	private Button btnExit;

	@FXML
	private Button btnLogout;

	public static void setLoggedInUser(String username, int userID) {
		loggedInUsername = username; // Store username

		loggedInUserID = userID;
	}

	public static void resetLoggedInUser() {
		loggedInUsername = null; // Reset username

		loggedInUserID = 0;
	}

	@FXML
	private void handleEnterParking(ActionEvent event) {
		Message msg = new Message(ENTER_PARKING_KIOSK, loggedInUserID);
		BParkKioskScenes.sendMessage(msg);
	}

	@FXML
	public void initialize() {
		// Update user info label when the dashboard loads
		if (loggedInUsername != null) {
			lblUserInfo.setText("User: " + loggedInUsername);
		}
	}

	@FXML
	private void handleRetrieveCar(ActionEvent event) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Retrieve Car");
		dialog.setHeaderText(null);
		dialog.setContentText("Enter your parking code:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(codeStr -> {
			try {
				int parkingInfoID = Integer.parseInt(codeStr);
				Message msg = new Message(RETRIEVE_CAR_KIOSK, parkingInfoID);
				BParkKioskScenes.sendMessage(msg);
			} catch (NumberFormatException e) {
				showInfo("Invalid Input", "Parking code must be numeric.");
			}
		});
	}

	@FXML
	private void handleForgotCode(ActionEvent event) {
		Message msg = new Message(FORGOT_CODE_KIOSK, loggedInUserID);
		BParkKioskScenes.sendMessage(msg);
	}

	@FXML
	private void handleActivateReservation(ActionEvent event) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Activate Reservation");
		dialog.setHeaderText(null);
		dialog.setContentText("Enter your reservation code:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(codeStr -> {
			try {
				int parkingInfoID = Integer.parseInt(codeStr);
				Message msg = new Message(ACTIVATE_RESERVATION_KIOSK, parkingInfoID);
				BParkKioskScenes.sendMessage(msg);
			} catch (NumberFormatException e) {
				showInfo("Invalid Input", "Reservation code must be numeric.");
			}
		});
	}

	@FXML
	private void handleExit(ActionEvent event) {
		try {

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/KioskMain.fxml"));
			Parent mainRoot = loader.load();
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			Scene scene = new Scene(mainRoot);
			scene.getStylesheets().add(getClass().getResource("/css/BParkStyle.css").toExternalForm());
			stage.setScene(scene);

		} catch (Exception e) {
			e.printStackTrace();
			showInfo("Error", "Could not return to main screen.");
		}
	}

	/**
	 * + * Handle Logout - properly logs out user and notifies server +
	 */
	@FXML
	private void handleLogout(ActionEvent event) {
		try {
			// Send logout notification to server
			if (loggedInUserID != 0) {
				// Send logout message in the format used by client app
				BParkKioskScenes.sendStringMessage("LoggedOut " + loggedInUsername);
			}

			// Reset logged in user
			resetLoggedInUser();

			// Return to main kiosk screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/KioskMain.fxml"));
			Parent mainRoot = loader.load();
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			Scene scene = new Scene(mainRoot);
			scene.getStylesheets().add(getClass().getResource("/css/BParkStyle.css").toExternalForm());
			stage.setScene(scene);

			// Show logout confirmation
			showInfo("Logged Out", "You have been successfully logged out.");

		} catch (Exception e) {
			e.printStackTrace();
			showInfo("Error", "Could not complete logout process.");
		}
	}

	private void showInfo(String title, String content) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(content);
			alert.showAndWait();
		});
	}
}