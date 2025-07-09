package controllers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import common.ParkingOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller class for managing subscriber-related functionality in the BPark
 * client application.
 * 
 * This class manages UI interactions such as checking availability, making and
 * canceling reservations, updating profiles, extend parking and viewing
 * history. It communicates with the server via the BParkClientApp and updates
 * the UI accordingly.
 */

public class SubscriberController implements Initializable {

	/** Buttons for navigation and main actions. */
	@FXML
	private Button btnCheckAvailability;
	@FXML
	private Button btnMakeReservation;
	@FXML
	private Button btnViewHistory;
	@FXML
	private Button btnUpdateProfile;
	@FXML
	private Button btnLogout;

	/** Fields for parking and reservation input. */
	@FXML
	private TextField txtParkingCode;
	@FXML
	private TextField txtCancelCode;

	/** Label to show available parking spots and current user. */
	@FXML
	private Label lblAvailableSpots;
	@FXML
	private Label lblUserInfo;

	/** Reservation form controls. */
	@FXML
	private DatePicker datePickerReservation;
	@FXML
	private ComboBox<String> comboTimeSlot;
	@FXML
	private Label lblReservationStatus;

	/** Table to show past parking history. */
	@FXML
	private TableView<ParkingOrder> tableParkingHistory;
	@FXML
	private TableColumn<ParkingOrder, String> colDate;
	@FXML
	private TableColumn<ParkingOrder, String> colEntry;
	@FXML
	private TableColumn<ParkingOrder, String> colExit;
	@FXML
	private TableColumn<ParkingOrder, String> colSpot;
	@FXML
	private TableColumn<ParkingOrder, String> colStatus;

	/** Main content container to dynamically load views. */
	@FXML
	private VBox mainContent;

	@FXML
	private Button btnExit;

	/** Observable list for storing and displaying parking history. */
	private ObservableList<ParkingOrder> parkingHistory = FXCollections.observableArrayList();

	/** Called when the controller is loaded. Sets up the UI. */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupUIreservation();
	}

	/** Sets the label with the current user's name. */
	public void setUserName(String userName) {
		lblUserInfo.setText("User: " + userName);
	}

	/** Loads the home view UI into the main content area. */
	public void loadHomeView() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/HomeView.fxml"));
			Parent homeView = loader.load();
			mainContent.getChildren().clear();
			mainContent.getChildren().add(homeView);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Loads the Reservation view FXML. */
	@FXML
	private void showReservationView() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ReservationView.fxml"));
			Parent reservationView = loader.load();

			mainContent.getChildren().clear();
			mainContent.getChildren().add(reservationView);

			setupUIreservation();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Loads the profile view for updating user details. */
	@FXML
	private void showProfileView() {
		try {
			// Load the FXML file for the profile update screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/UpdateProfileView.fxml"));
			// Load the actual UI components from the FXML file into a Node object
			Node profileView = loader.load();
			mainContent.getChildren().setAll(profileView); // This replaces the center of the UI
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ===== Action Handlers =====
	/**
	 * Handles the action of returning to the home screen.
	 * <p>
	 * This method is typically triggered when the user clicks a "Home" button. It
	 * calls {@code loadHomeView()} to load and display the main home interface in
	 * the central content area.
	 */
	@FXML
	private void handleGoHome() {
		loadHomeView();
	}

	/**
	 * Sends a parking availability request to the server, triggered manually by the
	 * user.
	 * <p>
	 * Marks that the availability check was explicitly requested by the user (not
	 * automatic), and then sends a message to the server to retrieve the current
	 * number of available spots. This method is typically used when the user clicks
	 * a "Check Availability" button.
	 */
	@FXML
	private void handleShowAvailableSpots() {
		Message checkMsg = new Message(Message.MessageType.CHECK_PARKING_AVAILABILITY, null);
		BParkClientScenes.sendMessage(checkMsg);

	}

	/**
	 * Sends a request to the server to retrieve current parking availability.
	 * <p>
	 * This method can be triggered from different parts of the application to check
	 * how many parking spots are currently available.
	 */
	@FXML
	private void checkParkingAvailability() {
		Message msg = new Message(MessageType.CHECK_PARKING_AVAILABILITY, null);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Handles the process of making a new parking reservation.
	 * <p>
	 * This method checks that both a date and time have been selected by the user.
	 * If valid, it formats the reservation data and sends it to the server in order
	 * to create a new reservation request.
	 */
	@FXML
	private void handleMakeReservation() {
		LocalDate selectedDate = datePickerReservation.getValue();
		String selectedTime = comboTimeSlot.getValue();

		if (selectedDate == null || selectedTime == null) {
			showAlert("Error", "Please select both date and time");
			return;
		}

		// Format: "YYYY-MM-DD HH:MM"
		String dateTimeStr = selectedDate.toString() + " " + selectedTime;
		String reservationData = BParkClientScenes.getCurrentUser() + "," + dateTimeStr;

		Message msg = new Message(MessageType.RESERVE_PARKING, reservationData);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Handles reservation cancellation based on the code entered in the text field.
	 * <p>
	 * This method retrieves the reservation code entered by the user, asks for
	 * confirmation, and if confirmed, sends a cancellation request to the server.
	 */
	@FXML
	private void handleCancelReservationFromPage() {
		String code = txtCancelCode.getText();

		if (code != null && !code.trim().isEmpty()) {
			// Step 1: Show confirmation alert
			Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
			confirmAlert.setTitle("Confirm Cancellation");
			confirmAlert.setHeaderText("Are you sure you want to cancel your reservation?");
			confirmAlert.setContentText("Reservation code: " + code);

			// Step 2: Wait for user response
			Optional<ButtonType> result = confirmAlert.showAndWait();

			// Step 3: If user confirms, proceed with cancellation
			if (result.isPresent() && result.get() == ButtonType.OK) {
				String cancellationData = BParkClientScenes.getCurrentUser() + "," + code;
				Message msg = new Message(MessageType.CANCEL_RESERVATION, cancellationData);
				BParkClientScenes.sendMessage(msg);
				txtCancelCode.clear();
			}

		} else {
			// Show error if field is empty
			showError("Missing Code", "Please enter a reservation code before clicking Cancel.");
		}
	}

	/** Requests parking history data from the server. */
	@FXML
	private void handleViewHistory() {
		Message msg = new Message(MessageType.GET_PARKING_HISTORY, BParkClientScenes.getCurrentUser());
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Sets up reservation UI elements including time slot combo box and date
	 * picker.
	 * 
	 * <p>
	 * The combo box is populated with time slots in 15-minute intervals from 06:00
	 * to 22:45. The date picker is restricted to only allow dates between 1 to 7
	 * days from today.
	 * </p>
	 */
	private void setupUIreservation() {
		// Initialize time slots for reservation (15-minute intervals)
		ObservableList<String> timeSlots = FXCollections.observableArrayList();
		for (int hour = 6; hour <= 22; hour++) {
			for (int minute = 0; minute < 60; minute += 15) {
				timeSlots.add(String.format("%02d:%02d", hour, minute));
			}
		}
		if (comboTimeSlot != null) {
			comboTimeSlot.setItems(timeSlots);
		}

		// Set date picker constraints (1-7 days from today)
		if (datePickerReservation != null) {
			datePickerReservation.setDayCellFactory(picker -> new DateCell() {
				@Override
				public void updateItem(LocalDate date, boolean empty) {
					super.updateItem(date, empty);
					LocalDate today = LocalDate.now();
					setDisable(empty || date.isBefore(today.plusDays(1)) || date.isAfter(today.plusDays(7)));
				}
			});
		}

		// Setup parking history table
		if (tableParkingHistory != null) {
			tableParkingHistory.setItems(parkingHistory);
			// Setup column cell value factories here
		}
	}

	/**
	 * Loads the Extend Parking screen and displays it in the main content area.
	 * <p>
	 * This method replaces the current view with the Extend Parking UI, which
	 * allows the user to extend an active parking session.
	 */
	@FXML
	private void handleExtendParking() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ExtendParkingView.fxml"));
			Node extendParkingView = loader.load();
			mainContent.getChildren().setAll(extendParkingView);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs the user out and closes the current window.
	 * <p>
	 * This method sends a logout message to the server, closes the current UI
	 * window, and returns control to the login screen.
	 */
	@FXML
	private void handleLogout() {
		// Return to login screen instead of closing
		BParkClientScenes.returnToLogin();
	}

	@FXML
	private void handleExit() {
		// This maintains the old logout behavior (exit application)
		BParkClientScenes.exitApplication();
	}

	// ===== UI Update Methods =====
	/** Updates the UI to reflect number of available spots. */
	public void updateAvailableSpots(int spots) {
		if (lblAvailableSpots != null) {
			lblAvailableSpots.setText("Available Spots: " + spots);

			// Update UI based on availability
			boolean canReserve = spots >= (10 * 0.4); // 40% rule
			if (btnMakeReservation != null) {
				btnMakeReservation.setDisable(!canReserve);
			}
			if (lblReservationStatus != null && !canReserve) {
				lblReservationStatus.setText("Reservations unavailable (less than 40% spots free)");
			}
		}
	}

	/** Replaces parking history with a new list of records. */
	public void updateParkingHistory(ObservableList<ParkingOrder> history) {
		this.parkingHistory.clear();
		this.parkingHistory.addAll(history);
	}

	// ===== Utility Methods =====
	/** Displays an information alert with given title and content. */
	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/** Displays an error alert with given title and content. */
	private void showError(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}