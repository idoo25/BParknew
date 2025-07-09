package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Message;
import common.ParkingOrder;
import common.ParkingReport;
import common.ParkingSubscriber;
import common.Message.MessageType;
import controllers.ParkingController;
import controllers.ReportController;
import controllers.ServerPortFrameController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * ||in SERVER||
 * 
 * Main server class for the BPARK system. Listens for incoming client
 * connections and handles message processing, routing requests to appropriate
 * controller classes. Supports handling kiosk operations, subscriber logins,
 * reservations, parking history, reports, and system shutdown.
 */
public class ParkingServer extends AbstractServer {


	/** Default port number for the server. */
	final public static Integer DEFAULT_PORT = 5555;

	/** Controller responsible for managing parking logic. */
	public static ParkingController parkingController;

	/** Controller responsible for generating and retrieving reports. */
	public static ReportController reportController;

	/** Reference to the server GUI window for displaying client connections. */
	public static ServerPortFrameController spf;

	/** Map to track client connections and their statuses. */
	public Map<String, String> clientsMap = new HashMap<>(); // IP -> status

	/** IP address and port on which the server is running. */
	public static String serverIp;

	/**
	 * Constructs a new ParkingServer on the given port.
	 *
	 * @param port the port number to listen on.
	 */
	public ParkingServer(int port) {
		super(port);
		try {
			serverIp = InetAddress.getLocalHost().getHostAddress() + ":" + port;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Handles all incoming messages from clients.
	 *
	 * @param msg    the received message object.
	 * @param client the connection to the client.
	 */
	public synchronized void handleMessageFromClient(Object msg, ConnectionToClient client) {
		System.out.println("Message received: " + msg + " from " + client);

		try {
			if (msg instanceof byte[]) {
				msg = deserialize(msg);
			}

			if (msg instanceof Message) {
				handleMessageObject((Message) msg, client);
			} else if (msg instanceof String) {
				handleStringMessage((String) msg, client);
			}

		} catch (Exception e) {
			System.err.println("General error in handleMessageFromClient: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Processes structured Message objects received from clients.
	 *
	 * @param message the message to process.
	 * @param client  the client that sent the message.
	 * @throws IOException if a communication error occurs.
	 */
	private synchronized void handleMessageObject(Message message, ConnectionToClient client) throws IOException {
		Message ret;

		try {
			switch (message.getType()) {
			case KIOSK_ID_LOGIN:
				handleKioskIdLogin(message, client);
				break;

			case KIOSK_RF_LOGIN:
				handleKioskRFLogin(message, client);
				break;

			case ENTER_PARKING_KIOSK:
				handleEnterParkingKiosk(message, client);
				break;

			case RETRIEVE_CAR_KIOSK:
				handleRetrieveCarKiosk(message, client);
				break;

			case FORGOT_CODE_KIOSK:
				handleForgotCodeKiosk(message, client);
				break;

			case ACTIVATE_RESERVATION_KIOSK:
				handleActivateReservationKiosk(message, client);
				break;

			case SUBSCRIBER_LOGIN:
				String[] loginParts = ((String) message.getContent()).split(",");
				if (loginParts.length < 2) {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, "ERROR: Missing username or user code");
					client.sendToClient(serialize(ret));
					break;
				}

				String username = loginParts[0].trim();
				String userCode = loginParts[1].trim();

				ParkingSubscriber subscriber = parkingController.getUserInfo(username);

				if (subscriber != null && String.valueOf(subscriber.getSubscriberID()).equals(userCode)) {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, subscriber);
				} else {
					ret = new Message(MessageType.SUBSCRIBER_LOGIN_RESPONSE, null);
				}

				client.sendToClient(serialize(ret));
				break;

			case CHECK_PARKING_AVAILABILITY:
				int availableSpots = parkingController.getAvailableParkingSpots();
				ret = new Message(MessageType.PARKING_AVAILABILITY_RESPONSE, availableSpots);
				client.sendToClient(serialize(ret));
				break;

			case RESERVE_PARKING:
				String[] reservationData = ((String) message.getContent()).split(",");
				String reservationUserName = reservationData[0]; // ← RENAMED
				String reservationDate = reservationData[1];
				String reservationResult = parkingController.makeReservation(reservationUserName, reservationDate);
				ret = new Message(MessageType.RESERVATION_RESPONSE, reservationResult);
				client.sendToClient(serialize(ret));
				break;

			case REGISTER_SUBSCRIBER:
				// Expected format: "attendantUserName,name,phone,email,carNumber,userName"
				String registrationData = (String) message.getContent();
				String[] regParts = registrationData.split(",");

				if (regParts.length >= 6) {
					String attendantUserName = regParts[0].trim();
					String name = regParts[1].trim();
					String phone = regParts[2].trim();
					String email = regParts[3].trim();
					String carNumber = regParts[4].trim();
					String subscriberUserName = regParts[5].trim(); // ← RENAMED

					String registrationResult = parkingController.registerNewSubscriber(attendantUserName, name, phone,
							email, carNumber, subscriberUserName);
					ret = new Message(MessageType.REGISTRATION_RESPONSE, registrationResult);
				} else {
					ret = new Message(MessageType.REGISTRATION_RESPONSE, "ERROR: Invalid registration data format");
				}
				client.sendToClient(serialize(ret));
				break;

			case REQUEST_LOST_CODE:
				String lostCodeUserName = (String) message.getContent(); // ← RENAMED
				String lostCodeResult = parkingController.sendLostParkingCode(lostCodeUserName);
				ret = new Message(MessageType.LOST_CODE_RESPONSE, lostCodeResult);
				client.sendToClient(serialize(ret));
				break;

			case GET_PARKING_HISTORY:
				String historyUserName = (String) message.getContent(); // ← RENAMED
				ArrayList<ParkingOrder> history = parkingController.getParkingHistory(historyUserName);
				ret = new Message(MessageType.PARKING_HISTORY_RESPONSE, history);
				client.sendToClient(serialize(ret));
				break;

			case MANAGER_GET_REPORTS:
				String reportType = (String) message.getContent();
				ArrayList<ParkingReport> reports = reportController.getParkingReports(reportType);
				ret = new Message(MessageType.MANAGER_SEND_REPORTS, reports);
				client.sendToClient(serialize(ret));
				break;

			case GET_ACTIVE_PARKINGS:
				ArrayList<ParkingOrder> activeParkings = parkingController.getActiveParkings();
				ret = new Message(MessageType.ACTIVE_PARKINGS_RESPONSE, activeParkings);
				client.sendToClient(serialize(ret));
				break;

			case UPDATE_SUBSCRIBER_INFO:
				String updateResult = parkingController.updateSubscriberInfo((String) message.getContent());
				ret = new Message(MessageType.UPDATE_SUBSCRIBER_RESPONSE, updateResult);
				client.sendToClient(serialize(ret));
				break;

			case GENERATE_MONTHLY_REPORTS:
				String monthYear = (String) message.getContent();
				ArrayList<ParkingReport> monthlyReports = reportController.generateMonthlyReports(monthYear);
				ret = new Message(MessageType.MONTHLY_REPORTS_RESPONSE, monthlyReports);
				client.sendToClient(serialize(ret));
				break;

			case CANCEL_RESERVATION:
				// Expected format: "userName,reservationCode"
				String[] cancelData = ((String) message.getContent()).split(",", 2);
				if (cancelData.length != 2) {
					ret = new Message(MessageType.CANCELLATION_RESPONSE, "ERROR: Invalid cancellation data format");
				} else {
					try {
						String cancelUserName = cancelData[0].trim();
						int reservationCode = Integer.parseInt(cancelData[1].trim());
						String cancelResult = parkingController.cancelReservation(cancelUserName, reservationCode);
						ret = new Message(MessageType.CANCELLATION_RESPONSE, cancelResult);
					} catch (NumberFormatException e) {
						ret = new Message(MessageType.CANCELLATION_RESPONSE, "ERROR: Invalid reservation code format");
					}
				}
				client.sendToClient(serialize(ret));
				break;

			case GET_SUBSCRIBER_BY_NAME:
				String subscriberName = (String) message.getContent();
				subscriber = parkingController.getSubscriberByName(subscriberName);
				ret = new Message(MessageType.SHOW_SUBSCRIBER_DETAILS, subscriber);
				client.sendToClient(serialize(ret));
				break;

			case GET_ALL_SUBSCRIBERS:
				List<ParkingSubscriber> allSubs = parkingController.getAllSubscribers();
				Message response = new Message(MessageType.SHOW_ALL_SUBSCRIBERS, (Serializable) allSubs);
				client.sendToClient(serialize(response));
				break;

			case REQUEST_EXTENSION:
				try {
					String[] parts = ((String) message.getContent()).split(",");
					if (parts.length != 2) {
						ret = new Message(MessageType.EXTENSION_RESPONSE, "Invalid extension format.");
					} else {
						String parkingCode = parts[0].trim();
						int additionalHours = Integer.parseInt(parts[1].trim());
						String result = parkingController.extendParkingTime(parkingCode, additionalHours);

						ret = new Message(MessageType.EXTENSION_RESPONSE, result);
					}
				} catch (NumberFormatException e) {
					ret = new Message(MessageType.EXTENSION_RESPONSE, "Invalid number format for extension hours.");
				}
				client.sendToClient(serialize(ret));
				break;

			case REQUEST_SUBSCRIBER_DATA: {
				String userName = (String) message.getContent();
				ParkingSubscriber userInfo = parkingController.getUserInfo(userName); // use your DB instance
				response = new Message(MessageType.SUBSCRIBER_DATA_RESPONSE, userInfo);
				client.sendToClient(serialize(response));
				break;
			}

			default:
				System.out.println("Unknown message type: " + message.getType());
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, "Server error");
			client.sendToClient(serialize(ret));
		}
	}

	/**
	 * Handles login from kiosk using username and userID.
	 *
	 * @param message the login request message.
	 * @param client  the client initiating the request.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleKioskIdLogin(Message message, ConnectionToClient client) throws IOException {
		String combined = (String) message.getContent();
		String[] parts = combined.split(",");
		Message ret;

		if (parts.length != 2) {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, "");
			client.sendToClient(serialize(ret));
			return;
		}

		String username = parts[0].trim();
		int userID;
		try {
			userID = Integer.parseInt(parts[1].trim());
		} catch (NumberFormatException e) {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, "");
			client.sendToClient(serialize(ret));
			return;
		}

		String name = parkingController.getNameByUsernameAndUserID(username, userID);
		if (name != null) {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, name + "," + userID);
		} else {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, "");
		}
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles RFID login request from kiosk.
	 *
	 * @param message the login request message.
	 * @param client  the client initiating the request.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleKioskRFLogin(Message message, ConnectionToClient client) throws IOException {
		int rfUserID = (Integer) message.getContent();
		String nameByID = parkingController.getNameByUserID(rfUserID);
		Message ret;
		if (nameByID != null) {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, nameByID + "," + rfUserID);
		} else {
			ret = new Message(MessageType.KIOSK_LOGIN_RESPONSE, "");
		}
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles entrance of vehicle into the parking via kiosk.
	 *
	 * @param message the request message containing user ID.
	 * @param client  the kiosk client.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleEnterParkingKiosk(Message message, ConnectionToClient client) throws IOException {
		int enteringUserID = (Integer) message.getContent();
		Message ret;
		if (parkingController.isParkingFull()) {
			ret = new Message(MessageType.ENTER_PARKING_KIOSK_RESPONSE, "FULL");
		} else {
			String entryResult = parkingController.enterParking(enteringUserID);
			ret = new Message(MessageType.ENTER_PARKING_KIOSK_RESPONSE, entryResult);
		}
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles vehicle retrieval request from kiosk.
	 *
	 * @param message the request containing the parking code.
	 * @param client  the kiosk client.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleRetrieveCarKiosk(Message message, ConnectionToClient client) throws IOException {
		int parkingCode = (Integer) message.getContent();
		String retrievalResult = parkingController.retrieveCarByCode(parkingCode);
		Message ret = new Message(MessageType.RETRIEVE_CAR_KIOSK_RESPONSE, retrievalResult);
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles forgotten parking code request from kiosk.
	 *
	 * @param message the request containing the user ID.
	 * @param client  the kiosk client.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleForgotCodeKiosk(Message message, ConnectionToClient client) throws IOException {
		int forgotUserID = (Integer) message.getContent();
		String code = parkingController.sendLostParkingCode(forgotUserID);
		Message ret = new Message(MessageType.FORGOT_CODE_KIOSK_RESPONSE, code);
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles parking reservation activation request from kiosk.
	 *
	 * @param message the request containing reservation ID.
	 * @param client  the kiosk client.
	 * @throws IOException if a communication error occurs.
	 */
	private void handleActivateReservationKiosk(Message message, ConnectionToClient client) throws IOException {
		int parkingInfoID = (Integer) message.getContent();
		String activateResult = parkingController.enterParkingWithReservation(parkingInfoID);
		Message ret = new Message(MessageType.ACTIVATE_RESERVATION_KIOSK_RESPONSE, activateResult);
		client.sendToClient(serialize(ret));
	}

	/**
	 * Handles simple string commands from clients (e.g., disconnect).
	 *
	 * @param message the command message.
	 * @param client  the client that sent the message.
	 */
	private synchronized void handleStringMessage(String message, ConnectionToClient client) {
		String[] arr = message.split("\\s");

		try {
			switch (arr[0]) {
			case "ClientDisconnect":
				disconnect(client);
				break;

			default:
				System.out.println("Unknown string command: " + arr[0]);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("error " + e.getMessage());
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	/**
	 * Serializes a Message object to a byte array.
	 *
	 * @param msg the Message to serialize.
	 * @return the byte array representation of the message.
	 */
	private byte[] serialize(Message msg) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(byteStream);
			out.writeObject(msg);
			out.flush();
			return byteStream.toByteArray();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Deserializes a byte array back into a Message object.
	 *
	 * @param msg the object containing the byte array.
	 * @return the deserialized Message or null on failure.
	 */
	private Object deserialize(Object msg) {
		try {
			byte[] messageBytes = (byte[]) msg;
			ByteArrayInputStream byteStream = new ByteArrayInputStream(messageBytes);
			ObjectInputStream objectStream = new ObjectInputStream(byteStream);
			return objectStream.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Called when the server starts listening for connections. Initializes the
	 * parking spots system.
	 */
	protected void serverStarted() {
		System.out.println("ParkB Server listening for connections on port " + getPort());
		parkingController.initializeParkingSpots();
	}

	/**
	 * Called when the server stops listening for connections. Also shuts down
	 * auto-cancellation service.
	 */
	protected void serverStopped() {
		System.out.println("ParkB Server has stopped listening for connections.");
		if (parkingController != null) {
			parkingController.shutdown();
			System.out.println("Auto-cancellation service shut down successfully");
		}
	}

	/**
	 * Triggered when a new client connects.
	 *
	 * @param client the connected client.
	 */
	@Override
	protected synchronized void clientConnected(ConnectionToClient client) {
		String clientIP = client.getInetAddress().getHostAddress();
		clientsMap.put(clientIP, "ClientIP: " + client.getInetAddress().getHostAddress() + " status: connected");

		System.out.println("Client connected: " + clientIP);

		if (spf != null) {
			spf.printConnection(clientsMap);
		}
	}

	/**
	 * Handles disconnection of a client.
	 *
	 * @param client the disconnected client.
	 */
	protected synchronized void disconnect(ConnectionToClient client) {
		String clientIP = client.getInetAddress().getHostAddress();
		clientsMap.put(clientIP, "disconnected");
		clientsMap.put(clientIP, "ClientIP: " + client.getInetAddress().getHostAddress() + " status: disconnected");
		System.out.println("Client disconnected: " + clientIP);

		if (spf != null) {
			spf.printConnection(clientsMap);
		}
	}

	/**
	 * Gracefully shuts down the server and any internal services.
	 */
	public synchronized void shutdown() {
		if (parkingController != null) {
			parkingController.shutdown();
		}
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Entry point for starting the server via command line.
	 *
	 * @param args optional port number as the first argument.
	 */
	public static void main(String[] args) {
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Throwable t) {
			port = DEFAULT_PORT;
		}

		ParkingServer sv = new ParkingServer(port);

		try {
			sv.listen();
		} catch (Exception ex) {
			System.out.println("ERROR - Could not listen for clients!");
		}
	}


}