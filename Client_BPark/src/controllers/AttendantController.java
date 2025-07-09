package controllers;

import java.net.URL;
import java.util.ResourceBundle;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import common.ParkingOrder;
import common.ParkingSubscriber;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * ||in CLIENT||
 *
 * Controller class for the Attendant Dashboard in the BPARK client application.
 * Handles active parking management, subscriber registration, and real-time
 * updates. Communicates with the server via BParkClientScenes messaging.
 */
public class AttendantController implements Initializable {

	// ====== Registration Fields ======
	@FXML
	private TextField UserID;
	@FXML
	private TextField txtName;
	@FXML
	private TextField txtPhone;
	@FXML
	private TextField txtEmail;
	@FXML
	private TextField txtCarNumber;
	@FXML
	private TextField txtUsername;
	@FXML
	private TextField subscriberIdField;
	@FXML
	private Label lblRegistrationStatus;
	@FXML
	private Button btnLogout;

	// ====== Active Parking Table ======
	@FXML
	private TableView<ParkingOrder> tableActiveParkings;
	@FXML
	private TableColumn<ParkingOrder, String> colParkingCode;
	@FXML
	private TableColumn<ParkingOrder, String> colSubscriberName;
	@FXML
	private TableColumn<ParkingOrder, String> colSpot;
	@FXML
	private TableColumn<ParkingOrder, String> colEntryTime;
	@FXML
	private TableColumn<ParkingOrder, String> colExpectedExit;
	@FXML
	private TableColumn<ParkingOrder, String> colType;
	@FXML
	private TableColumn<ParkingOrder, String> colCode;

	// ====== Subscribers Table ======
	@FXML
	private TableView<ParkingSubscriber> tableSubscribers;
	@FXML
	private TableColumn<ParkingSubscriber, String> colUserID;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubName;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubPhone;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubEmail;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubCar;
	@FXML
	private TableColumn<ParkingSubscriber, String> colSubUsername;

	// ====== System Status ======
	@FXML
	private Label lblParkingStatus;
	@FXML
	private Label lblAttendantInfo;

	// Quick Assist Controls
	@FXML
	private TextField txtAssistCode;
	@FXML
	private ComboBox<String> comboAssistAction;

	@FXML
	private Button btnExit;

	private ObservableList<ParkingOrder> activeParkings = FXCollections.observableArrayList();

	/**
	 * Updates the UI to display the logged-in attendant's name.
	 *
	 * @param userName The name of the logged-in user.
	 */
	public void setUserName(String userName) {
		if (lblAttendantInfo != null) {
			lblAttendantInfo.setText("Attendant: " + userName);
		}
	}

	/**
	 * Initializes the controller after its root element has been completely
	 * processed. Sets up UI bindings and triggers initial data loading.
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		BParkClientScenes.setAttendantController(this);
		setupUI();
		loadActiveParkings();
		loadSubscribers();
	}

	/**
	 * Configures UI bindings, table column factories, and starts auto-refresh for
	 * active parking data.
	 */
	private void setupUI() {
		if (tableActiveParkings != null) {
			tableActiveParkings.setItems(activeParkings);
			setupTableColumns();
		}

		if (tableSubscribers != null) {
			colUserID.setCellValueFactory(
					cellData -> new SimpleStringProperty(cellData.getValue().getSubscriberID() + ""));
			colSubName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFirstName()));
			colSubPhone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhoneNumber()));
			colSubEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
			colSubCar.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCarNumber()));
			colSubUsername
					.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSubscriberCode()));
		}

		startAutoRefresh();
	}

	/**
	 * Sets up the cell value factories for the active parkings table columns. Maps
	 * data fields from ParkingOrder entity to respective table columns.
	 */
	private void setupTableColumns() {
		colParkingCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getParkingCode()));
		colSubscriberName
				.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSubscriberName()));
		colSpot.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSpotNumber()));
		colEntryTime
				.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedEntryTime()));
		colExpectedExit.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getFormattedExpectedExitTime()));
		colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderType()));
		colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getParkingCode()));
	}

	/**
	 * Starts a timeline that refreshes the active parking list every 30 seconds.
	 */
	private void startAutoRefresh() {
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> loadActiveParkings()));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
	}

	/**
	 * Updates the list of currently active parkings and refreshes the parking
	 * status label.
	 *
	 * @param parkings An observable list of ParkingOrder objects to populate the
	 *                 table.
	 */
	public void updateActiveParkings(ObservableList<ParkingOrder> parkings) {
		this.activeParkings.clear();
		this.activeParkings.addAll(parkings);

		Platform.runLater(() -> {
			if (lblParkingStatus != null) {
				lblParkingStatus.setText(String.format("Active Parking Spots: %d", parkings.size()));
			}
		});
	}

	/**
	 * Updates the subscriber table with the given list of subscribers.
	 *
	 * @param subscribers List of ParkingSubscriber objects.
	 */
	public void updateSubscriberTable(java.util.List<ParkingSubscriber> subscribers) {
		Platform.runLater(() -> {
			ObservableList<ParkingSubscriber> list = FXCollections.observableArrayList(subscribers);
			tableSubscribers.setItems(list);
		});
	}

	/**
	 * Validates the registration form fields.
	 *
	 * @return true if all required fields are valid, false otherwise.
	 */
	private boolean validateRegistrationForm() {
		if (txtName.getText().trim().isEmpty()) {
			showError("Validation Error", "Name is required");
			return false;
		}
		if (txtPhone.getText().trim().isEmpty()) {
			showError("Validation Error", "Phone number is required");
			return false;
		}
		if (txtEmail.getText().trim().isEmpty()) {
			showError("Validation Error", "Email is required");
			return false;
		}
		if (txtUsername.getText().trim().isEmpty()) {
			showError("Validation Error", "Username is required");
			return false;
		}
		if (!txtEmail.getText().matches(".+@.+\\..+")) {
			showError("Validation Error", "Invalid email format");
			return false;
		}
		if (!txtPhone.getText().matches("0\\d{9}|\\+972\\d{9}")) {
			showError("Validation Error", "Invalid phone format (use 0XXXXXXXXX or +972XXXXXXXXX)");
			return false;
		}
		return true;
	}

	/**
	 * Displays a detailed alert with subscriber info.
	 *
	 * @param parkingSubscriber The subscriber to show.
	 */
	public void showSubscriberDetails(ParkingSubscriber parkingSubscriber) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Subscriber Details");
			alert.setHeaderText("Details for " + parkingSubscriber.getFirstName());

			String details = String.format("User ID: %s\nName: %s\nPhone: %s\nEmail: %s\nCar num: %s\ntype: %s",
					parkingSubscriber.getSubscriberID(), parkingSubscriber.getFirstName(),
					parkingSubscriber.getPhoneNumber(), parkingSubscriber.getEmail(), parkingSubscriber.getCarNumber(),
					parkingSubscriber.getUserType());

			alert.setContentText(details);
			alert.showAndWait();
		});
	}

	/**
	 * Displays a registration success message.
	 *
	 * @param message Message to show.
	 */
	public void showRegistrationSuccess(String message) {
		Platform.runLater(() -> {
			lblRegistrationStatus.setText("✓ " + message);
			lblRegistrationStatus.setStyle("-fx-text-fill: green;");
			clearRegistrationForm();

			Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> lblRegistrationStatus.setText("")));
			timeline.play();
		});
	}

	/**
	 * Displays a registration error message.
	 *
	 * @param message Message to show.
	 */
	public void showRegistrationError(String message) {
		Platform.runLater(() -> {
			lblRegistrationStatus.setText("✗ " + message);
			lblRegistrationStatus.setStyle("-fx-text-fill: red;");
		});
	}

	/**
	 * Shows a generic info alert.
	 *
	 * @param title   Alert title.
	 * @param content Alert content.
	 */
	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Shows an error alert.
	 *
	 * @param title   Alert title.
	 * @param content Alert content.
	 */
	private void showError(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	// ====== Action Handlers ======

	/**
	 * Sends a REGISTER_SUBSCRIBER message with form data.
	 */
	@FXML
	private void handleRegisterSubscriber() {
		if (!validateRegistrationForm()) {
			return;
		}

		String registrationData = String.format("%s,%s,%s,%s,%s,%s", BParkClientScenes.getCurrentUser(),
				txtName.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(),
				txtCarNumber.getText().trim(), txtUsername.getText().trim());

		Message msg = new Message(MessageType.REGISTER_SUBSCRIBER, registrationData);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Generates a username suggestion from the subscriber's name.
	 */
	@FXML
	private void handleGenerateUsername() {
		String baseName = txtName.getText().trim();
		if (baseName.isEmpty()) {
			showAlert("Error", "Please enter subscriber name first");
			return;
		}

		String suggestion = baseName.toLowerCase().replaceAll("[^a-z0-9]", "");
		txtUsername.setText(suggestion);
	}

	/**
	 * Sends GET_ACTIVE_PARKINGS message to server.
	 */
	@FXML
	private void loadActiveParkings() {
		Message msg = new Message(MessageType.GET_ACTIVE_PARKINGS, null);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Sends GET_ALL_SUBSCRIBERS message to server.
	 */
	@FXML
	private void loadSubscribers() {
		Message msg = new Message(MessageType.GET_ALL_SUBSCRIBERS, null);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Clears the registration form fields.
	 */
	@FXML
	private void clearRegistrationForm() {
		txtName.clear();
		txtPhone.clear();
		txtEmail.clear();
		txtCarNumber.clear();
		txtUsername.clear();
		lblRegistrationStatus.setText("");
	}

	/**
	 * Disconnects and exits the application.
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

	/**
	 * Open subscriber History
	 */
	@FXML
	private void handleSubscriberIdEnter() {
		String subscriberId = subscriberIdField.getText();
		Message msg = new Message(MessageType.GET_PARKING_HISTORY, subscriberId);
		BParkClientScenes.sendMessage(msg);
	}
}
