package controllers;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import common.ParkingOrder;
import common.ParkingReport;
import common.ParkingSubscriber;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * ||in CLIENT||
 * 
 * ManagerController handles the Manager dashboard UI logic and communicates
 * with the server via the BParkClientApp to request and update parking data. It
 * manages reports, active parkings, and subscriber data visualization.
 */
public class ManagerController implements Initializable {

	// === Dashboard Labels ===
	@FXML
	private Label lblTotalSpots;
	@FXML
	private Label lblOccupied;
	@FXML
	private Label lblAvailable;
	@FXML
	private Label lblReservations;
	@FXML
	private Label lblSystemStatus;
	@FXML
	private Label lblManagerInfo;
	@FXML
	private Label lblLastUpdate;

	@FXML
	private TextField UserID;
	@FXML
	private TextField subscriberIdField;

	// === Subscribers Table ===
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

	// === Active Parking Table ===
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

	// === Charts ===
	@FXML
	private LineChart<String, Number> occupancyChart;
	@FXML
	private PieChart parkingTypesChart;
	@FXML
	private BarChart<String, Number> parkingTimeChart;
	@FXML
	private AreaChart<String, Number> subscriberActivityChart;

	// === Parking Report Charts and Labels ===
	@FXML
	private BarChart<String, Number> chartTotalParkingTimePerDay;
	@FXML
	private BarChart<String, Number> chartHourlyDistribution;
	@FXML
	private PieChart chartExtensionsPercentage;
	@FXML
	private BarChart<String, Number> chartLateExitsByHour;
	@FXML
	private PieChart chartLateSubscribersRate;
	@FXML
	private Label lblTotalExtensions;
	@FXML
	private Label lblTotalLateExits;
	@FXML
	private Label lblTotalMonthHours;
	@FXML
	private Label lblExtensionsPercent;
	@FXML
	private Label lblLateSubscriberPercent;

	// === Subscribers Report ===
	@FXML
	private BarChart<String, Number> chartSubscribersPerDay;
	@FXML
	private PieChart chartReservationUsage;
	@FXML
	private Label lblTotalReservations;
	@FXML
	private Label lblCancelledReservations;
	@FXML
	private Label lblPreOrderReservations;
	@FXML
	private Label lblUsedReservations;

	// === Report Summary Labels ===
	@FXML
	private Label lblTotalParkings;
	@FXML
	private Label lblLateExits;
	@FXML
	private Label lblExtensions;
	@FXML
	private Label lblActiveSubscribers;
	@FXML
	private Label lblTotalOrders;
	@FXML
	private Label lblReservationCount;
	@FXML
	private Label lblCancelled;

	// @FXML private Button btnExit;

	private Timeline refreshTimeline;
	private ObservableList<ParkingReport> currentReports = FXCollections.observableArrayList();

	/**
	 * Initializes the Manager dashboard with UI setup and data loading.
	 * 
	 * @param location  Not used
	 * @param resources Not used
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		BParkClientScenes.setManagerController(this);
		setupUI();
		loadInitialData();
		startAutoRefresh();
		loadSubscribers();

	}

	/**
	 * Sets up the initial UI elements such as table column bindings and labels.
	 */
	private void setupUI() {
		setupTableColumns();

		// Set manager info
		if (lblManagerInfo != null) {
			lblManagerInfo.setText("Manager: " + BParkClientScenes.getCurrentUser());
		}

		if (tableSubscribers != null) {
			colUserID.setCellValueFactory(
					cellData -> new SimpleStringProperty(cellData.getValue().getSubscriberID() + ""));
			colSubName.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName()));
			colSubPhone.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPhoneNumber()));
			colSubEmail.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
			colSubCar.setCellValueFactory(
					cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCarNumber()));
			colSubUsername.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
					cellData.getValue().getSubscriberCode()));
		}

	}

	/**
	 * Loads initial parking and report data from the server.
	 */
	private void loadInitialData() {
		// Load parking availability
		checkParkingStatus();

		// Load initial reports
		loadReports("ALL");

		// Update timestamp
		updateLastRefreshTime();
	}

	/**
	 * Starts the auto-refresh mechanism that periodically updates the dashboard.
	 */
	private void startAutoRefresh() {
		// Refresh dashboard every 30 seconds
		refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
			checkParkingStatus();
			loadReports("ALL");
			updateLastRefreshTime();
		}));
		refreshTimeline.setCycleCount(Timeline.INDEFINITE);
		refreshTimeline.play();
	}

	// ===== Action Handlers =====

	/**
	 * Requests and loads all available reports from the server.
	 */
	@FXML
	private void handleGenerateReports() {
		loadReports("ALL");
	}

	/**
	 * Sends a request to generate monthly reports.
	 */
	@FXML
	private void handleGenerateMonthlyReports() {
		Message msg = new Message(MessageType.GENERATE_MONTHLY_REPORTS, 10);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Sends a message to check parking availability and load active parkings.
	 */
	@FXML
	private void checkParkingStatus() {
//		Message msg = new Message(MessageType.CHECK_PARKING_AVAILABILITY, null);
//		BParkClientApp.sendMessage(msg);

		// Also get active parkings for statistics
		Message activeMsg = new Message(MessageType.GET_ACTIVE_PARKINGS, null);
		BParkClientScenes.sendMessage(activeMsg);
	}

	/**
	 * Sends a request to load specific type of reports.
	 * 
	 * @param type The report type to load (e.g., ALL)
	 */
	private void loadReports(String type) {
		Message msg = new Message(MessageType.MANAGER_GET_REPORTS, type);
		BParkClientScenes.sendMessage(msg);
	}

	// ===== UI Update Methods =====

	/**
	 * Updates the dashboard with the latest report data.
	 * 
	 * @param reports List of ParkingReport objects
	 */
	public void updateReports(ArrayList<ParkingReport> reports) {
		Platform.runLater(() -> {
			currentReports.clear();
			currentReports.addAll(reports);

			for (ParkingReport report : reports) {
				if (report.getReportType().equals("PARKING_TIME")) {
					updateParkingTimeReport(report);
				}
			}
		});
	}

	/**
	 * Updates UI components with parking time report details.
	 * 
	 * @param report ParkingReport object
	 */
	private void updateParkingTimeReport(ParkingReport report) {

		lblTotalSpots.setText(report.getTotalSpots() + "");
		lblOccupied.setText(report.getOccupied() + "");
		lblAvailable.setText(report.getTotalSpots() - report.getOccupied() + "");
		lblReservations.setText(report.getpreOrderReservations() + "");

		if (lblTotalParkings != null) {
			lblTotalParkings.setText(String.valueOf(report.getTotalParkings()));
		}
		if (lblLateExits != null) {
			lblLateExits.setText(String.format("%d (%.1f%%)", report.getLateExits(), report.getLateExitPercentage()));
		}
		if (lblExtensions != null) {
			lblExtensions
					.setText(String.format("%d (%.1f%%)", report.getExtensions(), report.getExtensionPercentage()));
		}
		if (report.getTotalParkingTimePerDay() != null)
			updateChartTotalParkingTimePerDay(report.getTotalParkingTimePerDay());

		if (report.getHourlyDistribution() != null)
			updateChartHourlyDistribution(report.getHourlyDistribution());

		updateChartExtensionsPercentage(report.getExtensions(), report.getNoExtensions(), report.getTotalSubscribers());

		if (report.getLateExitsByHour() != null)
			updateChartLateExitsByHour(report.getLateExitsByHour());

		updateChartLateSubscribersRate(report.getLateSubscribers(), report.getTotalSubscribers());

		if (lblTotalLateExits != null)
			lblTotalLateExits.setText("Total Late Exits: " + report.getLateExits());

		if (lblTotalMonthHours != null) {
			int totalMonthHours = report.getTotalParkingTimePerDay().values().stream().mapToInt(Integer::intValue)
					.sum();
			report.setTotalMonthHours(totalMonthHours);
			lblTotalMonthHours.setText("Total Hours This Month: " + report.getTotalMonthHours());

		}

		updateChartReservationUsage(report.getpreOrderReservations(), report.getUsedReservations(),
				report.getCancelledReservations());

	}

	/**
	 * Updates the label showing the last data refresh time.
	 */
	private void updateLastRefreshTime() {
		if (lblLastUpdate != null) {
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			lblLastUpdate.setText("Last Update: " + timestamp);
		}
	}

	// ===== Utility Methods =====

	/**
	 * Shows an informational alert popup.
	 * 
	 * @param title   Title of the alert
	 * @param content Content of the alert
	 */
	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);

	}

	/**
	 * Disconnects the manager and exits the application.
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
	 * Sends a request to fetch the active parkings.
	 */
	@FXML
	private void loadActiveParkings() {
		Message msg = new Message(MessageType.GET_ACTIVE_PARKINGS, null);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Handles the action to show selected subscriber details.
	 */
	@FXML
	private void handleViewSubscriberDetails() {
		ParkingOrder selectedOrder = tableActiveParkings.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			String subscriberName = selectedOrder.getSubscriberName();
			Message msg = new Message(MessageType.GET_SUBSCRIBER_BY_NAME, subscriberName);
			BParkClientScenes.sendMessage(msg);
		} else {
			showAlert("Selection Required", "Please select a parking session from the table");
		}
	}

	/**
	 * Displays subscriber details in a dialog.
	 * 
	 * @param parkingSubscriber ParkingSubscriber object
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
	 * Updates the active parkings table.
	 * 
	 * @param parkings List of ParkingOrder objects
	 */
	public void updateActiveParkings(ObservableList<ParkingOrder> parkings) {
		Platform.runLater(() -> {
			tableActiveParkings.setItems(parkings);
		});
	}

	/**
	 * Sets up table column bindings for ParkingOrder data.
	 */
	private void setupTableColumns() {
		colParkingCode.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getParkingCode()));

		colSubscriberName.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubscriberName()));

		colSpot.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSpotNumber()));

		colEntryTime.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getFormattedEntryTime()));

		colExpectedExit.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
				cellData.getValue().getFormattedExpectedExitTime()));

		colType.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrderType()));
		colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getParkingCode()));
	}

	/**
	 * Requests the list of all subscribers.
	 */
	@FXML
	private void loadSubscribers() {
		Message msg = new Message(MessageType.GET_ALL_SUBSCRIBERS, null);
		BParkClientScenes.sendMessage(msg);
	}

	/**
	 * Updates the subscribers table.
	 * 
	 * @param subscribers List of ParkingSubscriber
	 */
	public void updateSubscriberTable(java.util.List<ParkingSubscriber> subscribers) {
		Platform.runLater(() -> {
			ObservableList<ParkingSubscriber> list = FXCollections.observableArrayList(subscribers);
			tableSubscribers.setItems(list);
		});
	}

	// =======================
	// Parking Report
	// =======================

	/**
	 * Updates bar chart of total parking time per day.
	 * 
	 * @param data Map of day to total hours
	 */
	private void updateChartTotalParkingTimePerDay(java.util.Map<String, Integer> data) {
		Platform.runLater(() -> {
			chartTotalParkingTimePerDay.getData().clear();
			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Total Parking Time Per Day");
			for (Map.Entry<String, Integer> entry : data.entrySet()) {
				series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
			}
			chartTotalParkingTimePerDay.getData().add(series);
		});
	}

	/**
	 * Updates hourly distribution bar chart.
	 * 
	 * @param data Map of hour to count
	 */
	private void updateChartHourlyDistribution(java.util.Map<String, Integer> data) {
		Platform.runLater(() -> {
			chartHourlyDistribution.getData().clear();
			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Hourly Distribution");
			for (Map.Entry<String, Integer> entry : data.entrySet()) {
				series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
			}
			chartHourlyDistribution.getData().add(series);
		});
	}

	/**
	 * Updates extension percentage pie chart and label.
	 * 
	 * @param extensions       Number of extensions
	 * @param noExtensions     Number of non-extended parkings
	 * @param totalSubscribers Total subscribers
	 */
	private void updateChartExtensionsPercentage(int extensions, int noExtensions, int totalSubscribers) {
		Platform.runLater(() -> {
			ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
					new PieChart.Data("Extensions", extensions), new PieChart.Data("No Extensions", noExtensions));
			chartExtensionsPercentage.setData(pieChartData);
			lblTotalExtensions.setText("Total Extensions: " + extensions);

			int total = extensions + noExtensions;
			double percent = (total == 0) ? 0 : (extensions * 100.0 / totalSubscribers);
			lblExtensionsPercent
					.setText(String.format("%.1f", percent) + "%" + " of Subscribers Extended Their Parking!");
		});
	}

	/**
	 * Updates late exits by hour bar chart.
	 * 
	 * @param data Map of hour to late exits count
	 */
	private void updateChartLateExitsByHour(java.util.Map<String, Integer> data) {
		Platform.runLater(() -> {
			chartLateExitsByHour.getData().clear();
			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Late Exits By Hour (from End Time)");
			for (Map.Entry<String, Integer> entry : data.entrySet()) {
				series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
			}
			chartLateExitsByHour.getData().add(series);
		});
	}

	/**
	 * Updates pie chart and label for late subscriber rate.
	 * 
	 * @param lateSubscribers  Number of late subscribers
	 * @param totalSubscribers Total subscribers
	 */
	private void updateChartLateSubscribersRate(int lateSubscribers, int totalSubscribers) {
		Platform.runLater(() -> {
			int onTime = totalSubscribers - lateSubscribers;
			ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
					new PieChart.Data("Late", lateSubscribers), new PieChart.Data("On Time", onTime));
			chartLateSubscribersRate.setData(pieChartData);
			double percent = (totalSubscribers == 0) ? 0 : (lateSubscribers * 100.0 / totalSubscribers);
			lblLateSubscriberPercent.setText(String.format("%.1f", percent) + "%" + " of Subscribers are late!");
		});
	}

	/**
	 * Updates reservation usage pie chart and associated labels.
	 * 
	 * @param preOrderReservations Number of open reservations
	 * @param used                 Number of used reservations
	 * @param cancelled            Number of cancelled reservations
	 */
	private void updateChartReservationUsage(int preOrderReservations, int used, int cancelled) {
		Platform.runLater(() -> {
			ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
					new PieChart.Data("Open Reservations", preOrderReservations),
					new PieChart.Data("Used Reservations", used),
					new PieChart.Data("Cancelled Reservations", cancelled));
			chartReservationUsage.setData(pieChartData);
			lblTotalReservations.setText("Total Reservations: " + (preOrderReservations + used + cancelled));
			lblUsedReservations.setText("used Reservations: " + used);
			lblPreOrderReservations.setText("Open Reservations: " + preOrderReservations);
			lblCancelledReservations.setText("Cancelled Reservations: " + cancelled);

		});
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