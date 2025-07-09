package client;

import java.util.ArrayList;

import common.Message;
import common.ParkingOrder;
import controllers.AttendantController;
import controllers.ExtendParkingController;
import controllers.ManagerController;
import controllers.ParkingHistoryController;
import controllers.SubscriberController;
import controllers.UpdateProfileController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utility class to show Client app scenes (converted from BParkClientApp)
 */
public class BParkClientScenes {

	// Current user info
	private static String currentUser;
	private static int currentUserID;
	private static String userType; // "sub", "emp", "mng"

	private static AttendantController attendantController;
	private static ManagerController managerController;

	public static void showLoginScreen(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(BParkClientScenes.class.getResource("/client/Login.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root);
		scene.getStylesheets().add(BParkClientScenes.class.getResource("/css/BParkStyle.css").toExternalForm());
		stage.setTitle("BPark - Login");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	public static void switchToMainScreen(String userType) {
		try {
			Stage stage = BParkLauncherApp.getPrimaryStage();
			Parent root = null;

			switch (userType) {
			case "sub":
				FXMLLoader subLoader = new FXMLLoader(
						BParkClientScenes.class.getResource("/client/SubscriberMain.fxml"));
				root = subLoader.load();
				SubscriberController controller = subLoader.getController();

				// Move loadHomeView AFTER controller is fully loaded
				Platform.runLater(controller::loadHomeView);

				// Set the user name in the bottom label
				controller.setUserName(getCurrentUser());

				stage.setTitle("BPark - Subscriber Portal");
				break;

			case "emp":
				FXMLLoader empLoader = new FXMLLoader(
						BParkClientScenes.class.getResource("/client/AttendantMain.fxml"));
				root = empLoader.load();
				AttendantController attendantController = empLoader.getController();
				attendantController.setUserName(getCurrentUser());
				stage.setTitle("BPark - Attendant Portal");
				break;

			case "mng":
				FXMLLoader mngLoader = new FXMLLoader(BParkClientScenes.class.getResource("/client/ManagerMain.fxml"));
				root = mngLoader.load();
				stage.setTitle("BPark - Manager Portal");
				break;
			}

			if (root != null) {
				Scene scene = new Scene(root);
				stage.setScene(scene);
				stage.show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Utility methods for sending messages (delegate to launcher)
	public static void sendMessage(Message msg) {
		BParkLauncherApp.sendMessage(msg);
	}

	public static void sendStringMessage(String msg) {
		BParkLauncherApp.sendStringMessage(msg);
	}

	public static boolean isConnected() {
		return BParkLauncherApp.isConnected();
	}

	public static void connectToServer() {
		if (!isConnected()) {
			BParkLauncherApp.connectToServer();
		}
	}

	public static void disconnect() {
		BParkLauncherApp.disconnect();
	}

	// Getters and setters
	public static UpdateProfileController getUpdateProfileController() {
		return UpdateProfileController.instance;
	}

	public static ExtendParkingController getExtendParkingController() {
		return ExtendParkingController.instance;
	}

	public static String getCurrentUser() {
		return currentUser;
	}

	public static void setCurrentUser(String user) {
		currentUser = user;
	}

	public static int getCurrentUserID() {
		return currentUserID;
	}

	public static void setCurrentUserID(int userID) {
		currentUserID = userID;
	}

	public static String getUserType() {
		return userType;
	}

	public static void setUserType(String type) {
		userType = type;
	}

	public static void setAttendantController(AttendantController controller) {
		attendantController = controller;
	}

	public static AttendantController getAttendantController() {
		return attendantController;
	}

	public static void setManagerController(ManagerController controller) {
		managerController = controller;
	}

	public static ManagerController getManagerController() {
		return managerController;
	}

	public static void returnToLogin() {
		try {
//			// Send disconnect notification if connected
//			if (BParkLauncherApp.getClient() != null && BParkLauncherApp.getClient().isConnected()) {
//				sendStringMessage("ClientDisconnect");
//			}
//
//			// Close current connection
//			if (BParkLauncherApp.getClient() != null && BParkLauncherApp.getClient().isConnected()) {
//				BParkLauncherApp.getClient().closeConnection();
//			}

			// Get current stage
			Stage currentStage = getCurrentStage();

			// Load login screen
			FXMLLoader loader = new FXMLLoader(BParkClientScenes.class.getResource("/client/Login.fxml"));
			Parent root = loader.load();

			Scene scene = new Scene(root);
			scene.getStylesheets().add(BParkClientScenes.class.getResource("/css/BParkStyle.css").toExternalForm());

			currentStage.setScene(scene);
			currentStage.setTitle("BPark - Login");
			currentStage.setResizable(false);

			// Clear current user data
			currentUser = null;
			currentUserID = 0;
			userType = null;

//			// Reconnect to server for next login
//			connectToServer();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error returning to login: " + e.getMessage());
		}
	}

	public static void exitApplication() {
		try {
			// Send disconnect notification if connected
			if (BParkLauncherApp.getClient() != null && BParkLauncherApp.getClient().isConnected()) {
				sendStringMessage("ClientDisconnect");
			}

			// Close connection
			if (BParkLauncherApp.getClient() != null && BParkLauncherApp.getClient().isConnected()) {
				BParkLauncherApp.getClient().closeConnection();
			}

			// Exit application
			Platform.exit();
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
			// Force exit even if there's an error
			System.exit(0);
		}
	}

	private static Stage getCurrentStage() {
		// Get the primary stage or any showing stage
		return Stage.getWindows().stream().filter(window -> window instanceof Stage && window.isShowing())
				.map(window -> (Stage) window).findFirst().orElse(new Stage());
	}

	/**
	 * Opens a parking history window with the provided history data.
	 * 
	 * @param history List of parking orders to display
	 */
	public static void showParkingHistoryWindow(ArrayList<ParkingOrder> history) {
		try {
			// Load the parking history FXML
			FXMLLoader loader = new FXMLLoader(BParkClientScenes.class.getResource("/client/ParkingHistoryView.fxml"));
			Parent root = loader.load();

			// Get the controller and set up the data
			ParkingHistoryController controller = loader.getController();
			if (currentUser != null) {
				controller.setUserName(currentUser);
			}
			controller.loadHistory(history);

			// Create a new stage for the parking history window
			Stage historyStage = new Stage();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(BParkClientScenes.class.getResource("/css/BParkStyle.css").toExternalForm());

			historyStage.setTitle("Parking History - " + (currentUser != null ? currentUser : "User"));
			historyStage.setScene(scene);
			historyStage.setResizable(true);
			historyStage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error opening parking history window: " + e.getMessage());
		}
	}
}