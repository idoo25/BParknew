package server;

import controllers.ServerPortFrameController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * ||in SERVER||
 * 
 * ServerUI is the main entry point for the BPARK server application. It extends
 * the JavaFX {@link Application} class to launch the server's graphical user
 * interface (GUI) for configuring and monitoring server activity.
 * 
 * The server GUI allows the user to specify the port and start the server. This
 * class also provides a static method for running the server headlessly with a
 * given port.

 */
public class ServerUI extends Application {

	/** The default port to be used if none is specified */
	public static final int DEFAULT_PORT = 5555;

	/**
	 * The standard Java entry point. Launches the JavaFX application.
	 *
	 * @param args command-line arguments
	 * @throws Exception if the application launch fails
	 */
	public static void main(String args[]) throws Exception {
		launch(args);
	}

	/**
	 * Starts the JavaFX primary stage and displays the server configuration GUI.
	 *
	 * @param primaryStage the primary stage for this application
	 * @throws Exception if GUI initialization fails
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		ServerPortFrameController aFrame = new ServerPortFrameController();
		aFrame.start(primaryStage);
	}

	/**
	 * Starts the parking server with the specified port number. If parsing the port
	 * fails, the default port is used and an error is printed. In case of a failure
	 * to listen for clients, an error is logged and GUI status is updated.
	 *
	 * @param p the port number as a string
	 */
	public static void runServer(String p) {
		int port = 0;

		try {
			port = Integer.parseInt(p);
		} catch (Throwable t) {
			System.out.println("ERROR - Could not parse port number!");
		}

		ParkingServer sv = new ParkingServer(port);

		try {
			sv.listen();
		} catch (Exception ex) {
			ServerPortFrameController.str = "error";
			System.out.println("ERROR - Could not listen for clients!");
		}
	}
}
