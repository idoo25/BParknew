package client;

import common.Message;
import controllers.KioskController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utility class to show Kiosk app scenes (converted from BParkKioskApp)
 */
public class BParkKioskScenes {

    public static void showKioskScreen(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(BParkKioskScenes.class.getResource("/client/KioskMain.fxml"));
        Parent root = loader.load();

        // Set main stage in the controller to allow screen switching
        KioskController.setMainStage(stage);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(BParkKioskScenes.class.getResource("/css/BParkStyle.css").toExternalForm());
        stage.setTitle("BPark - Kiosk Terminal");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
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
        BParkLauncherApp.connectToServer();
    }

    public static void disconnect() {
        BParkLauncherApp.disconnect();
    }
}