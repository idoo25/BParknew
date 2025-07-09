package controllers;

import java.util.Collection;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.ParkingServer;
import server.ServerUI;

/**
 * ||in SERVER||
 * 
 * ServerPortFrame provides the JavaFX GUI interface for managing the ParkB
 * server. It supports automatic database configuration, server startup, client
 * connection display, and shutdown functionality.
 */
public class ServerPortFrameController extends Application {
	public static String str = "";

	@FXML
	private Button btnExit = null;

	@FXML
	private TextField textMessage;

	@FXML
	private TextField serverip;

	@FXML
	private TextArea txtClientConnection;

	ServerPortFrameController controller;

	/**
	 * Starts the JavaFX GUI and automatically launches the server.
	 *
	 * @param primaryStage the primary stage for the GUI.
	 * @throws Exception if FXML loading or server startup fails.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/serverGUI/ServerGUI.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root);
		controller = loader.getController();
		ParkingServer.spf = this;
		primaryStage.setTitle("ParkB Server Management");
		primaryStage.setScene(scene);
		primaryStage.show();

		// Auto-start server when GUI launches
		autoStartServer();
	}

	/**
	 * Automatically configures the DB and starts the server. If connection is
	 * successful, displays system info.
	 */
	private void autoStartServer() {
		Platform.runLater(() -> {
			try {
				String dbName = "bpark";
				String dbPassword = "Aa123456";

				controller.textMessage.setText("Auto-starting ParkB Server...");

				ParkingServer.parkingController = new ParkingController(dbName, dbPassword);
				ParkingServer.reportController = new ReportController(dbName, dbPassword);

				if (ParkingServer.parkingController.successFlag == 1) {
					ServerUI.runServer(ParkingServer.DEFAULT_PORT.toString());
					controller.serverip.setText(ParkingServer.serverIp);
					controller.textMessage.setText("ParkB Server Running Successfully!");

					showSystemInfo();
				} else {
					controller.textMessage.setText("Database connection failed! Check MySQL server.");
				}
			} catch (Exception e) {
				controller.textMessage.setText("Error starting server: " + e.getMessage());
			}
		});
	}

	/**
	 * Handles the Exit button click event. Gracefully shuts down the server and
	 * stops background services.
	 *
	 * @param event ActionEvent from the Exit button.
	 * @throws Exception if shutdown fails.
	 */
	@FXML
	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("Shutting down ParkB Server");

		if (ParkingServer.parkingController != null) {
			ParkingServer.parkingController.shutdown();
			System.out.println("Auto-cancellation service stopped during shutdown");
		}

		System.exit(0);
	}

	/**
	 * Updates the text area in the GUI with the last connected client only.
	 *
	 * @param clientsMap a map of ConnectionToClient to a string describing the
	 *                   client.
	 */
	public void printConnection(Map<String, String> clientsMap) {
		Platform.runLater(() -> {
			String lastClient = "";
			Collection<String> values = clientsMap.values();
			if (!values.isEmpty()) {

				lastClient = values.stream().reduce((first, second) -> second).orElse("");
			}

			if (controller != null && controller.txtClientConnection != null) {
				String currentText = controller.txtClientConnection.getText();
				String header = lastClient.contains("disconnected") ? "Client Disconnected:" : "Client Connected:";
				if (currentText.contains("=== ParkB Server")) {
					controller.txtClientConnection.setText(
							currentText.split("Waiting for clients")[0] + header + "\n" + lastClient + "\n");
				} else {
					controller.txtClientConnection.setText(lastClient + "\n");
				}
			}
		});
	}

	/**
	 * Displays system information in the GUI after successful startup. Includes DB,
	 * port, server IP, auto-cancellation policy, etc.
	 */
	private void showSystemInfo() {
		Platform.runLater(() -> {
			StringBuilder systemInfo = new StringBuilder();
			systemInfo.append("=== ParkB Server Auto-Started ===\n");
			systemInfo.append("Database: bpark (Auto-configured)\n");
			systemInfo.append("MySQL: localhost:3306 (Connected)\n");
			systemInfo.append("Username: root\n");
			systemInfo.append("Server IP: ").append(ParkingServer.serverIp).append("\n");
			systemInfo.append("Port: ").append(ParkingServer.DEFAULT_PORT).append("\n");
			systemInfo.append("Parking Spots: 10 (Auto-initialized)\n");
			systemInfo.append("Auto-Cancellation: ACTIVE (15-min rule)\n");
			systemInfo.append("Reservation Flow: preorder → active → finished\n");
			systemInfo.append("Late Policy: Auto-cancel after 15 minutes\n");
			systemInfo.append("Auto-start: SUCCESS\n");
			systemInfo.append("Status: Ready to accept client connections\n");
			systemInfo.append("================================\n\n");
			systemInfo.append("Monitor console for auto-cancellation messages:\n");
			systemInfo.append("✅ AUTO-CANCELLED: Reservation X for UserY\n\n");
			systemInfo.append("Waiting for clients to connect...\n");

			if (controller != null && controller.txtClientConnection != null) {
				controller.txtClientConnection.setText(systemInfo.toString());
			}
		});
	}
}
