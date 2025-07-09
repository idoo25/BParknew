package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import common.Message;
import common.ParkingOrder;
import common.ParkingReport;
import common.ParkingSubscriber;
import controllers.AttendantController;
import controllers.ExtendParkingController;
import controllers.KioskController;
import controllers.LoginController;
import controllers.ManagerController;
import controllers.UpdateProfileController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;

public class ClientMessageHandler {

	/**
	 * Handle incoming Message objects from the server
	 */
	public static void handleMessage(Message message) {
		switch (message.getType()) {
		case SUBSCRIBER_LOGIN_RESPONSE:
			handleLoginResponse(message);
			break;

		case KIOSK_LOGIN_RESPONSE:
			handleKioskLoginResponse(message);
			break;

		case ENTER_PARKING_KIOSK_RESPONSE:
			handleEnterParkingKioskResponse(message);
			break;

		case RETRIEVE_CAR_KIOSK_RESPONSE:
			handleRetrieveCarKioskResponse(message);
			break;

		case FORGOT_CODE_KIOSK_RESPONSE:
			handleForgotCodeKioskResponse(message);
			break;

		case ACTIVATE_RESERVATION_KIOSK_RESPONSE:
			handleActivateReservationKioskResponse(message);
			break;

		case PARKING_AVAILABILITY_RESPONSE:
			handleParkingAvailability(message);
			break;

		case RESERVATION_RESPONSE:
			handleReservationResponse(message);
			break;

		case REGISTRATION_RESPONSE:
			handleRegistrationResponse(message);
			break;

		case LOST_CODE_RESPONSE:
			handleLostCodeResponse(message);
			break;

		case PARKING_HISTORY_RESPONSE:
			handleParkingHistory(message);
			break;

		case MANAGER_SEND_REPORTS:
			handleReports(message);
			break;

		case ACTIVE_PARKINGS_RESPONSE:
			handleActiveParkings(message);
			break;

		case UPDATE_SUBSCRIBER_RESPONSE:
			handleUpdateResponse(message);
			break;

		case SUBSCRIBER_DATA_RESPONSE:
			handleSubscriberDataResponse(message);
			break;

		case ACTIVATION_RESPONSE:
			handleActivationResponse(message);
			break;

		case CANCELLATION_RESPONSE:
			handleCancellationResponse(message);
			break;

		case EXTENSION_RESPONSE:
			handleExtendParkingResponse(message);
			break;

		case SHOW_SUBSCRIBER_DETAILS:
			ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();
			Platform.runLater(() -> {
				if (BParkClientScenes.getAttendantController() != null)
					BParkClientScenes.getAttendantController().showSubscriberDetails(subscriber);
				else if (BParkClientScenes.getManagerController() != null)
					BParkClientScenes.getManagerController().showSubscriberDetails(subscriber);
			});
			break;

		case SHOW_ALL_SUBSCRIBERS:
			List<ParkingSubscriber> subs = (List<ParkingSubscriber>) message.getContent();
			if (BParkClientScenes.getAttendantController() != null)
				BParkClientScenes.getAttendantController().updateSubscriberTable(subs);
			if (BParkClientScenes.getManagerController() != null)
				BParkClientScenes.getManagerController().updateSubscriberTable(subs);
			break;

		default:
			System.out.println("Unknown message type: " + message.getType());
		}
	}

	public static void handleStringMessage(String message) {
		String[] parts = message.split(" ", 2);
		String command = parts[0];
		String data = parts.length > 1 ? parts[1] : "";

		switch (command) {
		case "login:":
			handleStringLoginResponse(data);
			break;

		case "availableSpots":
			showAlert("Available Spots", "Current available spots: " + data);
			break;

		case "enterResult":
			showAlert("Entry Result", data);
			break;

		case "exitResult":
			showAlert("Exit Result", data);
			break;

		case "parkingCode":
			showAlert("Lost Code", "Your parking code is: " + data);
			break;

		case "reservationResult":
			showAlert("Reservation", data);
			break;

		default:
			System.out.println("Unknown string command: " + command);
		}
	}

	private static void handleLoginResponse(Message message) {
		ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();

		if (subscriber != null) {
			BParkClientScenes.setCurrentUser(subscriber.getSubscriberCode());
			BParkClientScenes.setUserType(subscriber.getUserType());
			BParkClientScenes.switchToMainScreen(subscriber.getUserType());

			Platform.runLater(() -> LoginController.getInstance().handleLoginSuccess(subscriber.getUserType()));
		} else {
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Login Failed");
				alert.setHeaderText(null);
				alert.setContentText("Invalid username/userCode or user not found.");
				alert.showAndWait();
				LoginController.getInstance().handleLoginFailed(null);
			});
		}
	}

	private static void handleKioskLoginResponse(Message message) {
		KioskController.handleKioskLoginResult(message.getContent());
	}

	private static void handleEnterParkingKioskResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Enter Parking", response);
	}

	private static void handleRetrieveCarKioskResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Retrieve Car", response);
	}

	private static void handleForgotCodeKioskResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Parking Code", response);
	}

	private static void handleActivateReservationKioskResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Activate Reservation", response);
	}

	private static void handleParkingAvailability(Message message) {
		Integer availableSpots = (Integer) message.getContent();
		showAlert("Parking Availability", "Available spots: " + availableSpots);
	}

	private static void handleReservationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.startsWith("SUCCESS") || response.contains("confirmed")) {
			showAlert("Reservation Success", response);
		} else {
			showAlert("Reservation Failed", response);
		}
	}

	private static void handleRegistrationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.startsWith("SUCCESS")) {
			showAlert("Registration Success", response);
		} else {
			showAlert("Registration Failed", response);
		}
	}

	private static void handleLostCodeResponse(Message message) {
		String code = (String) message.getContent();
		if (code.matches("\\d+")) {
			showAlert("Parking Code Recovery",
					"Your parking code is: " + code + "\n" + "This has also been sent to your email/SMS");
		} else {
			showAlert("Code Recovery Failed", code);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handleParkingHistory(Message message) {
		ArrayList<ParkingOrder> history = (ArrayList<ParkingOrder>) message.getContent();
		
		// Open the parking history window with the received data
		Platform.runLater(() -> {
			BParkClientScenes.showParkingHistoryWindow(history);
		});
	}

	@SuppressWarnings("unchecked")
	private static void handleReports(Message message) {
		ArrayList<ParkingReport> reports = (ArrayList<ParkingReport>) message.getContent();

		ManagerController managerController = BParkClientScenes.getManagerController();
		if (managerController != null) {
			managerController.updateReports(reports);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handleActiveParkings(Message message) {
		ArrayList<ParkingOrder> activeParkings = (ArrayList<ParkingOrder>) message.getContent();

		AttendantController controller = BParkClientScenes.getAttendantController();
		if (controller != null) {
			controller.updateActiveParkings(FXCollections.observableArrayList(activeParkings));
		}

		ManagerController managerController = BParkClientScenes.getManagerController();
		if (managerController != null) {
			managerController.updateActiveParkings(FXCollections.observableArrayList(activeParkings));
		}
	}

	private static void handleUpdateResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Update Profile", response);
	}

	private static void handleSubscriberDataResponse(Message message) {
		ParkingSubscriber subscriber = (ParkingSubscriber) message.getContent();
		Platform.runLater(() -> {
			UpdateProfileController controller = BParkClientScenes.getUpdateProfileController();
			controller.setFieldPrompts(subscriber.getEmail(), subscriber.getPhoneNumber(), subscriber.getCarNumber());
		});
	}

	private static void handleActivationResponse(Message message) {
		String response = (String) message.getContent();
		if (response.contains("successful") || response.contains("activated")) {
			showAlert("Reservation Activated", response);
		} else {
			showAlert("Activation Failed", response);
		}
	}

	// String message handlers (legacy)

	private static void handleStringLoginResponse(String data) {
		if (!data.equals("None")) {
			BParkClientScenes.setUserType(data);
			BParkClientScenes.switchToMainScreen(data);
		} else {
			showAlert("Login Failed", "Invalid credentials");
		}
	}

	private static void handleStringAvailableSpots(String data) {
		showAlert("Available Spots", "Current available spots: " + data);
	}

	private static void handleCancellationResponse(Message message) {
		String response = (String) message.getContent();
		showAlert("Reservation Cancellation", response);
	}

	private static void handleExtendParkingResponse(Message message) {
		String response = (String) message.getContent();

		// show popup as before
		if (response.contains("Parking time extended")) {

			showAlert("Extension Successful", response);
			ExtendParkingController controller = BParkClientScenes.getExtendParkingController();
			if (controller != null) {
				controller.setStatusMessage("Extension successful!", "green");
			}
		} else {
			showAlert("Extension Failed", response);
			ExtendParkingController controller = BParkClientScenes.getExtendParkingController();
			if (controller != null) {
				controller.setStatusMessage(response, "red");
			}
		}
	}

	public static byte[] serialize(Message msg) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(msg);
			out.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Object deserialize(Object msg) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) msg);
				ObjectInputStream in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static void showAlert(String title, String content) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(content);
			alert.showAndWait();
		});
	}
}