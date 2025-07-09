package controllers;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import common.ParkingOrder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Parking History window
 * Displays user's parking history in a separate window
 * 
 * NOTE: To fully support all formatting requirements, the following would need to be added:
 * 1. ParkingOrder.getEstimatedStartTime() method to show estimated start date for preorders
 * 2. The ParkingController.getParkingHistory() method would need to set the estimated start time
 *    from the database field pi.Estimated_start_time
 * 
 * Current implementation uses actual entry time for all order types due to these limitations.
 */
public class ParkingHistoryController implements Initializable {

    // UI Controls
    @FXML private Label lblUserName;
    @FXML private Label lblTotalSessions;
    @FXML private Label lblActiveSessions;
    @FXML private Label lblCompletedSessions;
    @FXML private Label lblStatus;
    @FXML private ComboBox<String> comboStatusFilter;
    
    // Table and columns
    @FXML private TableView<ParkingOrder> tableHistory;
    @FXML private TableColumn<ParkingOrder, String> colCode;
    @FXML private TableColumn<ParkingOrder, String> colDate;
    @FXML private TableColumn<ParkingOrder, String> colEntryTime;
    @FXML private TableColumn<ParkingOrder, String> colExitTime;
    @FXML private TableColumn<ParkingOrder, String> colSpotNumber;
    @FXML private TableColumn<ParkingOrder, String> colOrderType;
    @FXML private TableColumn<ParkingOrder, String> colStatus;
    @FXML private TableColumn<ParkingOrder, String> colDuration;
    @FXML private TableColumn<ParkingOrder, String> colLate;
    @FXML private TableColumn<ParkingOrder, String> colExtended;

    // Data
    private ObservableList<ParkingOrder> allHistory = FXCollections.observableArrayList();
    private ObservableList<ParkingOrder> filteredHistory = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilter();
        updateStatus("Ready");
    }

    /**
     * Set up the table columns and cell value factories
     */
    private void setupTable() {
        // Configure basic columns
        colCode.setCellValueFactory(new PropertyValueFactory<>("parkingCode"));
        colSpotNumber.setCellValueFactory(new PropertyValueFactory<>("spotNumber"));
        
        // Order Type with custom formatting
        colOrderType.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            String type = order.getOrderType();
            String status = order.getStatus();
            
            // Map database values to display values
            if ("yes".equalsIgnoreCase(type)) {
                // "yes" in IsOrderedEnum means it was a preorder/reservation
                return new javafx.beans.property.SimpleStringProperty("Preorder");
            } else if ("no".equalsIgnoreCase(type)) {
                // "no" in IsOrderedEnum means it was immediate parking
                return new javafx.beans.property.SimpleStringProperty("Immediate");
            }
            // Fallback for other potential values
            else if ("PREORDER".equalsIgnoreCase(type) || "PRE_ORDER".equalsIgnoreCase(type) || "preorder".equalsIgnoreCase(status)) {
                return new javafx.beans.property.SimpleStringProperty("Preorder");
            } else if ("IMMEDIATE".equalsIgnoreCase(type) || "REGULAR".equalsIgnoreCase(type)) {
                return new javafx.beans.property.SimpleStringProperty("Immediate");
            }
            
            // Default fallback
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });
        
        // Date column - For cancelled and preorder: show estimated start date, others: show actual entry date
        colDate.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            String status = order.getStatus();
            
            // For cancelled and preorder reservations, show estimated start date
            if ("cancelled".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status) || "preorder".equalsIgnoreCase(status)) {
                if (order.getEstimatedStartTime() != null) {
                    String dateStr = order.getEstimatedStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    return new javafx.beans.property.SimpleStringProperty(dateStr);
                }
            }
            
            // For all other statuses, show actual entry date if available
            if (order.getEntryTime() != null) {
                String dateStr = order.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return new javafx.beans.property.SimpleStringProperty(dateStr);
            }
            
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Entry Time (actual) - For preorder: show estimated start time, others: show actual entry time
        colEntryTime.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            String status = order.getStatus();
            
            // For preorder status, show estimated start time
            if ("preorder".equalsIgnoreCase(status)) {
                if (order.getEstimatedStartTime() != null) {
                    String formatted = order.getEstimatedStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    return new javafx.beans.property.SimpleStringProperty(formatted);
                }
            }
            
            // For all other statuses, show actual entry time if available
            if (order.getEntryTime() != null) {
                String formatted = order.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                return new javafx.beans.property.SimpleStringProperty(formatted);
            }
            
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Exit Time with custom logic based on status
        colExitTime.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            String status = order.getStatus();
            
            if ("Active".equalsIgnoreCase(status)) {
                return new javafx.beans.property.SimpleStringProperty("Still Parked");
            } else if ("Canceled".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                return new javafx.beans.property.SimpleStringProperty("Canceled");
            } else if ("Finished".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                if (order.getExitTime() != null) {
                    String formatted = order.getExitTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    return new javafx.beans.property.SimpleStringProperty(formatted);
                }
            }
            
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Duration with custom logic
        colDuration.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            String status = order.getStatus();
            
            if ("Canceled".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "preorder".equalsIgnoreCase(status)) {
                return new javafx.beans.property.SimpleStringProperty("0 hours 0 minutes");
            } else if ("Active".equalsIgnoreCase(status) || "Finished".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                // Calculate duration manually
                if (order.getEntryTime() != null) {
                    LocalDateTime startTime = order.getEntryTime();
                    LocalDateTime endTime = order.getExitTime();
                    
                    // For active sessions, use current time as end time
                    if (endTime == null || "Active".equalsIgnoreCase(status)) {
                        endTime = LocalDateTime.now();
                    }
                    
                    // Calculate duration
                    long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
                    long hours = totalMinutes / 60;
                    long minutes = totalMinutes % 60;
                    
                    return new javafx.beans.property.SimpleStringProperty(hours + " hours and " + minutes + " minutes");
                }
            }
            
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Late column
        colLate.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            // Assuming ParkingOrder has an isLate() method or late property
            boolean isLate = order.isLate(); // You may need to add this method to ParkingOrder
            return new javafx.beans.property.SimpleStringProperty(isLate ? "Yes" : "No");
        });
        
        // Extended column
        colExtended.setCellValueFactory(cellData -> {
            ParkingOrder order = cellData.getValue();
            // Assuming ParkingOrder has an isExtended() method or extended property
            boolean isExtended = order.isExtended(); // You may need to add this method to ParkingOrder
            return new javafx.beans.property.SimpleStringProperty(isExtended ? "Yes" : "No");
        });
        
        // Set up color coding for Status column
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> {
            return new TableCell<ParkingOrder, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        
                        // Status Color Coding: Preorder (Purple), Active (Green), Canceled (Red), Finished (Blue)
                        switch (status.toLowerCase()) {
                            case "preorder":
                                setStyle("-fx-text-fill: purple;");
                                break;
                            case "active":
                                setStyle("-fx-text-fill: green;");
                                break;
                            case "canceled":
                            case "cancelled":
                                setStyle("-fx-text-fill: red;");
                                break;
                            case "finished":
                            case "completed":
                                setStyle("-fx-text-fill: blue;");
                                break;
                            default:
                                setStyle("-fx-text-fill: black;");
                                break;
                        }
                    }
                }
            };
        });
        
        // Set up color coding for Late column
        colLate.setCellFactory(column -> {
            return new TableCell<ParkingOrder, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        
                        // Late Column: Yes (Red), No (Black)
                        if ("Yes".equals(item)) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            };
        });
        
        // Set up color coding for Extended column
        colExtended.setCellFactory(column -> {
            return new TableCell<ParkingOrder, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        
                        // Extended Column: Yes (Blue), No (Black)
                        if ("Yes".equals(item)) {
                            setStyle("-fx-text-fill: blue;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            };
        });
        
        // Special formatting for Exit Time to show "Canceled" in red
        colExitTime.setCellFactory(column -> {
            return new TableCell<ParkingOrder, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        
                        // Show "Canceled" in red text
                        if ("Canceled".equals(item)) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            };
        });

        // Bind the filtered list to the table
        tableHistory.setItems(filteredHistory);
    }

    /**
     * Set up the status filter combo box
     */
    private void setupFilter() {
        comboStatusFilter.getItems().addAll("All Sessions", "Active", "Completed", "Preorder", "Canceled");
        comboStatusFilter.setValue("All Sessions");
    }

    /**
     * Set the user name displayed in the window
     */
    public void setUserName(String userName) {
        Platform.runLater(() -> {
            lblUserName.setText("User: " + userName);
        });
    }

    /**
     * Load parking history data into the table
     */
    public void loadHistory(ArrayList<ParkingOrder> history) {
        Platform.runLater(() -> {
            allHistory.clear();
            allHistory.addAll(history);
            applyFilter();
            updateStatistics();
            updateStatus("Loaded " + history.size() + " parking records");
        });
    }

    /**
     * Apply the selected filter to the data
     */
    @FXML
    private void handleFilterChange() {
        applyFilter();
    }

    /**
     * Apply the current filter selection
     */
    private void applyFilter() {
        String selectedFilter = comboStatusFilter.getValue();
        filteredHistory.clear();
        
        switch (selectedFilter) {
            case "Active":
                allHistory.stream()
                    .filter(order -> "active".equalsIgnoreCase(order.getStatus()))
                    .forEach(filteredHistory::add);
                break;
            case "Completed":
                allHistory.stream()
                    .filter(order -> "finished".equalsIgnoreCase(order.getStatus()) || "completed".equalsIgnoreCase(order.getStatus()))
                    .forEach(filteredHistory::add);
                break;
            case "Preorder":
                allHistory.stream()
                    .filter(order -> "preorder".equalsIgnoreCase(order.getStatus()))
                    .forEach(filteredHistory::add);
                break;
            case "Canceled":
                allHistory.stream()
                    .filter(order -> "cancelled".equalsIgnoreCase(order.getStatus()) || "canceled".equalsIgnoreCase(order.getStatus()))
                    .forEach(filteredHistory::add);
                break;
            default: // "All Sessions"
                filteredHistory.addAll(allHistory);
                break;
        }
    }

    /**
     * Update the statistics labels
     */
    private void updateStatistics() {
        int total = allHistory.size();
        int active = (int) allHistory.stream()
            .filter(order -> "active".equalsIgnoreCase(order.getStatus()))
            .count();
        int completed = (int) allHistory.stream()
            .filter(order -> "finished".equalsIgnoreCase(order.getStatus()) || "completed".equalsIgnoreCase(order.getStatus()))
            .count();

        lblTotalSessions.setText(String.valueOf(total));
        lblActiveSessions.setText(String.valueOf(active));
        lblCompletedSessions.setText(String.valueOf(completed));
    }

    /**
     * Update the status message
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            lblStatus.setText(message);
        });
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        updateStatus("Refreshing...");
        // Send request for updated parking history
        client.BParkClientScenes.sendMessage(new common.Message(common.Message.MessageType.GET_PARKING_HISTORY, 
                                           client.BParkClientScenes.getCurrentUser()));
    }

    /**
     * Handle close button click
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblUserName.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Check if this controller's window is still showing
     */
    public boolean isWindowShowing() {
        try {
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            return stage.isShowing();
        } catch (Exception e) {
            return false;
        }
    }
}