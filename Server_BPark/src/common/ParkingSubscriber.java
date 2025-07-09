package common;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Represents a parking subscriber in the ParkB system.
 * Contains subscriber information such as ID, contact details,
 * car information, user type, and parking order history.
 */
public class ParkingSubscriber implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Unique identifier for the subscriber */
    private int subscriberID;

    /** Unique code assigned to the subscriber */
    private String subscriberCode;

    /** Subscriber's first name */
    private String firstName;

    /** Subscriber's phone number */
    private String phoneNumber;

    /** Subscriber's email address */
    private String email;

    /** Subscriber's car license plate number */
    private String carNumber;

    /** Type of user: e.g., "sub" (subscriber), "emp" (employee), "mng" (manager) */
    private String userType;

    /** List of parking orders associated with this subscriber */
    private ArrayList<ParkingOrder> parkingHistory;

    /**
     * Default constructor. Initializes an empty parking history list.
     */
    public ParkingSubscriber() {
        this.parkingHistory = new ArrayList<>();
    }

    /**
     * Constructs a ParkingSubscriber with specified fields.
     *
     * @param subscriberID Unique subscriber identifier
     * @param subscriberCode Unique code for the subscriber
     * @param firstName Subscriber's first name
     * @param phoneNumber Subscriber's phone number
     * @param email Subscriber's email address
     * @param carNumber Subscriber's car license plate number
     * @param userType Type of user (e.g. sub, emp, mng)
     */
    public ParkingSubscriber(int subscriberID, String subscriberCode, String firstName,
                             String phoneNumber, String email, String carNumber, String userType) {
        this.subscriberID = subscriberID;
        this.subscriberCode = subscriberCode;
        this.firstName = firstName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.carNumber = carNumber;
        this.userType = userType;
        this.parkingHistory = new ArrayList<>();
    }

    /**
     * Returns the subscriber's unique identifier.
     * @return subscriberID
     */
    public int getSubscriberID() {
        return subscriberID;
    }

    /**
     * Sets the subscriber's unique identifier.
     * @param subscriberID Unique identifier to set
     */
    public void setSubscriberID(int subscriberID) {
        this.subscriberID = subscriberID;
    }

    /**
     * Returns the subscriber's unique code.
     * @return subscriberCode
     */
    public String getSubscriberCode() {
        return subscriberCode;
    }

    /**
     * Sets the subscriber's unique code.
     * @param subscriberCode Code to set
     */
    public void setSubscriberCode(String subscriberCode) {
        this.subscriberCode = subscriberCode;
    }

    /**
     * Returns the subscriber's first name.
     * @return firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the subscriber's first name.
     * @param firstName Name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the subscriber's phone number.
     * @return phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the subscriber's phone number.
     * @param phoneNumber Phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the subscriber's email address.
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the subscriber's email address.
     * @param email Email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the subscriber's car license plate number.
     * @return carNumber
     */
    public String getCarNumber() {
        return carNumber;
    }

    /**
     * Sets the subscriber's car license plate number.
     * @param carNumber Car number to set
     */
    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
    }

    /**
     * Returns the type of user.
     * @return userType
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the type of user.
     * @param userType User type to set (e.g. sub, emp, mng)
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Returns the parking history list for this subscriber.
     * @return list of ParkingOrder objects
     */
    public ArrayList<ParkingOrder> getParkingHistory() {
        return parkingHistory;
    }

    /**
     * Sets the parking history list for this subscriber.
     * @param parkingHistory List of ParkingOrder objects
     */
    public void setParkingHistory(ArrayList<ParkingOrder> parkingHistory) {
        this.parkingHistory = parkingHistory;
    }

    /**
     * Adds a new ParkingOrder to the subscriber's parking history.
     * @param order ParkingOrder to add
     */
    public void addParkingOrder(ParkingOrder order) {
        this.parkingHistory.add(order);
    }

    /**
     * Returns a string representation of the ParkingSubscriber,
     * including key fields but excluding parking history for brevity.
     *
     * @return formatted string describing the subscriber
     */
    @Override
    public String toString() {
        return "ParkingSubscriber{" +
                "subscriberID=" + subscriberID +
                ", subscriberCode='" + subscriberCode + '\'' +
                ", firstName='" + firstName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", carNumber='" + carNumber + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}



