package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Represents a parking order/session in the ParkB system.
 * Contains information about entry, exit, and parking details.
 */
public class ParkingOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the parking order.
     */
    private int orderID;

    /**
     * Unique parking code assigned to the user.
     */
    private String parkingCode;

    /**
     * Name of the subscriber associated with this order.
     */
    private String subscriberName;

    /**
     * Type of the order - either "ordered" (reservation) or "not ordered" (immediate).
     */
    private String orderType;

    /**
     * Actual entry time into the parking lot.
     */
    private LocalDateTime entryTime;

    /**
     * Actual exit time from the parking lot. Null if the vehicle is still parked.
     */
    private LocalDateTime exitTime;

    /**
     * Expected exit time for the parking session.
     */
    private LocalDateTime expectedExitTime;

    /**
     *  estimated Start Time for the parking session.
     */
     private LocalDateTime estimatedStartTime;

    /**
     * Flag indicating whether the vehicle exited late (past expected exit time).
     */

    private boolean isLate;

    /**
     * Flag indicating whether the parking order was extended.
     */
    private boolean isExtended;

    /**
     * Current status of the parking order, e.g., "Active" or "Completed".
     */
    private String status;

    /**
     * Number or identifier of the parking spot assigned to this order.
     */
    private String spotNumber;

    /**
     * Default constructor.
     */
    public ParkingOrder() {}

    /**
     * Constructs a ParkingOrder with the given details.
     *
     * @param orderID the unique identifier for this order
     * @param parkingCode the parking code associated with this order
     * @param subscriberName name of the subscriber making the order
     * @param orderType type of the order, either "ordered" or "not ordered"
     * @param entryTime time of vehicle entry into the parking lot
     * @param expectedExitTime expected exit time from the parking lot
     */
    public ParkingOrder(int orderID, String parkingCode, String subscriberName, 
                        String orderType, LocalDateTime entryTime, LocalDateTime expectedExitTime) {
        this.orderID = orderID;
        this.parkingCode = parkingCode;
        this.subscriberName = subscriberName;
        this.orderType = orderType;
        this.entryTime = entryTime;
        this.expectedExitTime = expectedExitTime;
        this.isLate = false;
        this.isExtended = false;
        this.status = "Active";
    }

    /**
     * Returns the unique identifier of this parking order.
     *
     * @return the order ID
     */
    public int getOrderID() {
        return orderID;
    }

    /**
     * Sets the unique identifier of this parking order.
     *
     * @param orderID the order ID to set
     */
    public void setOrderID(int orderID) {
        this.orderID = orderID;
    }

    /**
     * Returns the parking code associated with this order.
     *
     * @return the parking code
     */
    public String getParkingCode() {
        return parkingCode;
    }

    /**
     * Sets the parking code for this order.
     *
     * @param parkingCode the parking code to set
     */
    public void setParkingCode(String parkingCode) {
        this.parkingCode = parkingCode;
    }

    /**
     * Returns the subscriber's name for this order.
     *
     * @return the subscriber name
     */
    public String getSubscriberName() {
        return subscriberName;
    }

    /**
     * Sets the subscriber's name for this order.
     *
     * @param subscriberName the subscriber name to set
     */
    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    /**
     * Returns the type of the order ("ordered" or "not ordered").
     *
     * @return the order type
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * Sets the type of the order.
     *
     * @param orderType the order type to set
     */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     * Returns the entry time of the parking session.
     *
     * @return the entry time
     */
    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    /**
     * Sets the entry time of the parking session.
     *
     * @param entryTime the entry time to set
     */
    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    /**
     * Returns the actual exit time from the parking lot.
     *
     * @return the exit time, or null if still parked
     */
    public LocalDateTime getExitTime() {
        return exitTime;
    }

    /**
     * Sets the actual exit time of the parking session.
     *
     * @param exitTime the exit time to set
     */
    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    /**
     * Returns the expected exit time for the parking session.
     *
     * @return the expected exit time
     */
    public LocalDateTime getExpectedExitTime() {
        return expectedExitTime;
    }

    /**
     * Sets the expected exit time for the parking session.
     *
     * @param expectedExitTime the expected exit time to set
     */
    public void setExpectedExitTime(LocalDateTime expectedExitTime) {
        this.expectedExitTime = expectedExitTime;
    }

    /**
     * Returns whether the vehicle exited late.
     *
     * @return true if late, false otherwise
     */
    public boolean isLate() {
        return isLate;
    }

    /**
     * Sets the lateness flag.
     *
     * @param late true if exited late, false otherwise
     */
    public void setLate(boolean late) {
        isLate = late;
    }

    /**
     * Returns whether this order has been extended.
     *
     * @return true if extended, false otherwise
     */
    public boolean isExtended() {
        return isExtended;
    }

    /**
     * Sets the extended flag.
     *
     * @param extended true if extended, false otherwise
     */
    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    /**
     * Returns the status of the order (e.g., "Active" or "Completed").
     *
     * @return the order status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the order.
     *
     * @param status the status to set (e.g., "Active", "Completed")
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the assigned parking spot number.
     *
     * @return the spot number
     */
    public String getSpotNumber() {
        return spotNumber;
    }

    /**
     * Sets the assigned parking spot number.
     *
     * @param spotNumber the spot number to set
     */
    public void setSpotNumber(String spotNumber) {
        this.spotNumber = spotNumber;
    }

    
    public LocalDateTime getEstimatedStartTime() {
        return estimatedStartTime;
    }

    public void setEstimatedStartTime(LocalDateTime estimatedStartTime) {
        this.estimatedStartTime = estimatedStartTime;
    }
    
    // Utility methods


   /**
     * Returns the entry time formatted as "yyyy-MM-dd HH:mm:ss".
     *
     * @return formatted entry time, or "N/A" if entry time is null
     */
    public String getFormattedEntryTime() {
        if (entryTime != null) {
            return entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return "N/A";
    }

    /**
     * Returns the exit time formatted as "yyyy-MM-dd HH:mm:ss".
     *
     * @return formatted exit time, or "Still parked" if exit time is null
     */
    public String getFormattedExitTime() {
        if (exitTime != null) {
            return exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return "Still parked";
    }

    /**
     * Returns the expected exit time formatted as "yyyy-MM-dd HH:mm:ss".
     *
     * @return formatted expected exit time, or "N/A" if expected exit time is null
     */
    public String getFormattedExpectedExitTime() {
        if (expectedExitTime != null) {
            return expectedExitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return "N/A";
    }

    /**
     * Calculates the total parking duration in minutes.
     * If exit time is null, calculates duration from entry time until now.
     *
     * @return parking duration in minutes, or 0 if entry time is null
     */
    public long getParkingDurationMinutes() {
        if (entryTime != null) {
            LocalDateTime endTime = exitTime != null ? exitTime : LocalDateTime.now();
            return ChronoUnit.MINUTES.between(entryTime, endTime);
        }
        return 0;
    }

    /**
     * Returns the parking duration formatted as "<hours> hours, <minutes> minutes".
     *
     * @return formatted parking duration string
     */
    public String getParkingDurationFormatted() {
        long minutes = getParkingDurationMinutes();
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%d hours, %d minutes", hours, remainingMinutes);
    }

    /**
     * Checks if the vehicle is currently parked (i.e., no exit time and status is "Active").
     *
     * @return true if currently parked, false otherwise
     */
    public boolean isCurrentlyParked() {
        return exitTime == null && "Active".equals(status);
    }

    /**
     * Checks if this order is a reservation (orderType equals "ordered").
     *
     * @return true if it is a reservation, false otherwise
     */
    public boolean isReservation() {
        return "ordered".equals(orderType);
    }

    /**
     * Returns a string representation of the ParkingOrder object.
     *
     * @return string containing all fields and their values
     */
    @Override
    public String toString() {
        return "ParkingOrder{" +
                "orderID=" + orderID +
                ", parkingCode='" + parkingCode + '\'' +
                ", subscriberName='" + subscriberName + '\'' +
                ", orderType='" + orderType + '\'' +
                ", entryTime=" + entryTime +
                ", exitTime=" + exitTime +
                ", expectedExitTime=" + expectedExitTime +
                ", isLate=" + isLate +
                ", isExtended=" + isExtended +
                ", status='" + status + '\'' +
                ", spotNumber='" + spotNumber + '\'' +
                '}';
    }
}
