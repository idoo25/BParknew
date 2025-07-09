package controllers;

import client.BParkClientScenes;
import common.Message;
import common.Message.MessageType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller class for handling the parking extension feature in the JavaFX GUI.
 * <p>
 * This controller is responsible for:
 * <ul>
 *     <li>Initializing the hour selection combo box.</li>
 *     <li>Validating user input (parking code and duration).</li>
 *     <li>Sending extension requests to the server via {@link BParkClientApp}.</li>
 *     <li>Displaying success or error messages to the user.</li>
 * </ul>
 * 
 * <p>This controller is intended to be used with an FXML file defining the UI.</p>
 */

public class ExtendParkingController {
	
	 /**
     * Singleton-reference to the current instance of this controller.
     * */
	 public static ExtendParkingController instance;

	
	 /**
	 * Text field for the user to enter their unique parking code.
	 */
    @FXML private TextField codeField;
    
    /**
     * ComboBox for selecting the number of hours to extend the parking.
     */
    @FXML private ComboBox<String> hoursCombo;
    
    /**
     * Label used to display validation messages from server responses.
     */
    @FXML private Label statusLabel;
   

    /**
     * Initializes the controller after its root element has been completely processed.
     * <p>
     * This method:
     * <ul>
     *     <li>Assigns the static instance reference.</li>
     *     <li>Populates the {@code hoursCombo} with values from 1 to 4 hours.</li>
     *     <li>Sets the default selected value to "1".</li>
     * </ul>
     */
    @FXML
    public void initialize() {
    	instance = this;
        hoursCombo.getItems().addAll("1", "2", "3", "4");
        hoursCombo.setValue("1");
    }
    
    

    /**
     * Handles the submit button click event.
     * <p>
     * This method performs the following:
     * <ul>
     *     <li>Retrieves the parking code and extension hours selected by the user.</li>
     *     <li>Validates that the code field is not empty.</li>
     *     <li>Creates a {@link Message} object with type {@code REQUEST_EXTENSION} and sends it to the server.</li>
     * </ul>
     */
    @FXML
    private void handleSubmit() {
        String code = codeField.getText();
        String hours = hoursCombo.getValue();

        if (code == null || code.trim().isEmpty()) {
            statusLabel.setText("Please enter a valid code.");
            return;
        }

        String extensionData = code + "," + hours;
        // Create and send the extension request message
        Message msg = new Message(MessageType.REQUEST_EXTENSION, extensionData);
        BParkClientScenes.sendMessage(msg);
        
      
        
    }
    
    
    /**
     * Updates the status label with a custom message and text color.
     * <p>
     * This method is typically used to inform the user of the result of an action, 
     * such as a successful extension or an input error.
     * It can be called from other parts of the application.
     *
     * @param msg   The text message to be displayed in the status label.
     * @param color The color of the text, defined using a CSS color name  
     *              or a hexadecimal color code.
     */
    public void setStatusMessage(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
  
}
