package controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.TreeMap;

import common.ParkingReport;
import server.DBController;

/**
 * ||in SERVER||
 * 
 * ReportController handles report generation for the ParkB parking management
 * system. Updated to work with unified parkinginfo table structure
 */
public class ReportController {

	/**
	 * Initializes the report controller and sets up the database connection.
	 *
	 * @param dbname the name of the database
	 * @param pass   the password for the database user
	 */

	public ReportController(String dbname, String pass) {
		DBController.initializeConnection(dbname, pass);
//		conn = DBController.getInstance().getConnection();
	}

	/**
	 * Retrieves parking reports based on the specified report type.
	 *
	 * @param reportType The type of report to generate ("PARKING_TIME",
	 *                   "SUBSCRIBER_STATUS", or "ALL")
	 * @return ArrayList of ParkingReport objects for the selected type(s)
	 */
	public ArrayList<ParkingReport> getParkingReports(String reportType) {
		ArrayList<ParkingReport> reports = new ArrayList<>();

		switch (reportType.toUpperCase()) {
		case "PARKING_TIME":
			reports.add(generateParkingTimeReport());
			break;
		case "SUBSCRIBER_STATUS":
			reports.add(generateSubscriberStatusReport());
			break;
		case "ALL":
			reports.add(generateParkingTimeReport());
			reports.add(generateSubscriberStatusReport());
			break;
		default:
			System.out.println("Unknown report type: " + reportType);
			break;
		}

		return reports;
	}

	/**
	 * Generates and stores monthly reports for a given month and year.
	 *
	 * @param monthYear A string in the format "YYYY-MM" representing the target
	 *                  month.
	 * @return ArrayList of generated ParkingReport objects.
	 */

	public ArrayList<ParkingReport> generateMonthlyReports(String monthYear) {
		ArrayList<ParkingReport> monthlyReports = new ArrayList<>();

		try {
			// Parse the month-year string
			String[] parts = monthYear.split("-");
			int year = Integer.parseInt(parts[0]);
			int month = Integer.parseInt(parts[1]);

			LocalDate reportDate = LocalDate.of(year, month, 1);

			// Generate parking time report for the specific month
			ParkingReport parkingTimeReport = generateMonthlyParkingTimeReport(reportDate);
			if (parkingTimeReport != null) {
				monthlyReports.add(parkingTimeReport);
			}

			// Generate subscriber status report for the specific month
			ParkingReport subscriberReport = generateMonthlySubscriberStatusReport(reportDate);
			if (subscriberReport != null) {
				monthlyReports.add(subscriberReport);
			}

			// Store reports in database
			storeMonthlyReports(monthlyReports);

		} catch (Exception e) {
			System.out.println("Error generating monthly reports: " + e.getMessage());
		}

		return monthlyReports;
	}

	/**
	 * Generates a current parking time report based on the last 30 days. Includes
	 * statistics such as average duration, late exits, extensions, and usage
	 * distributions.
	 *
	 * @return ParkingReport object populated with parking time metrics.
	 */
	private ParkingReport generateParkingTimeReport() {
		ParkingReport report = new ParkingReport("PARKING_TIME", LocalDate.now());
		Connection conn = DBController.getInstance().getConnection();

		String qry = """
				SELECT
				    COUNT(*) as total_parkings,
				    AVG(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, NOW()))) as avg_duration,
				    SUM(CASE WHEN IsLate = 'yes' THEN 1 ELSE 0 END) as late_exits,
				    SUM(CASE WHEN IsExtended = 'yes' THEN 1 ELSE 0 END) as extensions,
				    MIN(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, NOW()))) as min_duration,
				    MAX(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, NOW()))) as max_duration
				FROM parkinginfo
				WHERE statusEnum IN ('active', 'finished')
				AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))




				""";

		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					report.setTotalParkings(rs.getInt("total_parkings"));
					report.setAverageParkingTime(rs.getDouble("avg_duration"));
					report.setLateExits(rs.getInt("late_exits"));
					report.setExtensions(rs.getInt("extensions"));
					report.setMinParkingTime(rs.getInt("min_duration"));
					report.setMaxParkingTime(rs.getInt("max_duration"));
					report.setTotalSpots(getTotalSpots());
					report.setOccupied(getOccupied());
					report.setpreOrderReservations(getPreOrderedReservations());
				}
			}
		} catch (SQLException e) {
			System.out.println("Error generating parking time report: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		report.setTotalParkingTimePerDay(getTotalParkingTimePerDay());
		report.setHourlyDistribution(getHourlyDistribution());
		report.setLateExitsByHour(getLateExitsByHour());
		report.setNoExtensions(getNoExtensions());
		report.setLateSubscribers(getLateSubscribers());
		report.setTotalSubscribers(getTotalSubscribers());
		report.setReservations(getUsedReservations() + getCancelledReservations());
		report.setUsedReservations(getUsedReservations());
		report.setCancelledReservations(getCancelledReservations());
		report.getpreOrderReservations();
		report.setpreOrderReservations(getPreOrderedReservations());
		report.setOccupied(getOccupied());
		report.setTotalSpots(getTotalSpots());
		return report;
	}

	/**
	 * Generates a current subscriber status report for the last 30 days. Includes
	 * active subscribers, reservation types, session durations, and cancellation
	 * stats.
	 *
	 * @return ParkingReport object populated with subscriber activity metrics.
	 */
	private ParkingReport generateSubscriberStatusReport() {
		ParkingReport report = new ParkingReport("SUBSCRIBER_STATUS", LocalDate.now());
		Connection conn = DBController.getInstance().getConnection();

		// Get active subscribers count
		String activeSubQry = """
								SELECT COUNT(DISTINCT User_ID) as active_subscribers
								FROM parkinginfo
								WHERE MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								""";

		// Get total orders, reservations, and immediate entries
		String ordersQry = """
								SELECT
								    COUNT(*) as total_orders,
								    SUM(CASE WHEN IsOrderedEnum = 'yes' THEN 1 ELSE 0 END) as reservations,
								    SUM(CASE WHEN IsOrderedEnum = 'no' THEN 1 ELSE 0 END) as immediate_entries,
								    AVG(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, NOW()))) as zavg_session_duration
								FROM parkinginfo
								WHERE MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								AND statusEnum IN ('active', 'finished')
								""";

		// Get cancelled reservations
		String cancelledQry = """
								SELECT COUNT(*) as cancelled_reservations
								FROM parkinginfo
								WHERE statusEnum = 'cancelled'
								AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								""";

		try {
			// Get active subscribers
			try (PreparedStatement stmt = conn.prepareStatement(activeSubQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setActiveSubscribers(rs.getInt("active_subscribers"));
					}
				}
			}

			// Get order statistics
			try (PreparedStatement stmt = conn.prepareStatement(ordersQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setTotalOrders(rs.getInt("total_orders"));
						report.setReservations(rs.getInt("reservations"));
						report.setImmediateEntries(rs.getInt("immediate_entries"));
						report.setAverageSessionDuration(rs.getDouble("avg_session_duration"));
					}
				}
			}

			// Get cancelled reservations
			try (PreparedStatement stmt = conn.prepareStatement(cancelledQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setCancelledReservations(rs.getInt("cancelled_reservations"));
					}
				}
			}

		} catch (SQLException e) {
			System.out.println("Error generating subscriber status report: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		report.setSubscribersPerDay(getSubscribersPerDay());
		report.setTotalSubscribers(getTotalSubscribers());
		report.setLateSubscribers(getLateSubscribers());

		return report;
	}

	/**
	 * Generates a parking time report for a specific calendar month.
	 *
	 * @param reportDate A LocalDate representing the target month (1st day of the
	 *                   month).
	 * @return ParkingReport object for that month, or null if no data available.
	 */
	private ParkingReport generateMonthlyParkingTimeReport(LocalDate reportDate) {
		ParkingReport report = new ParkingReport("PARKING_TIME", reportDate);
		Connection conn = DBController.getInstance().getConnection();

		String qry = """
				SELECT
				    COUNT(*) as total_parkings,
				    AVG(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, Estimated_end_time))) as avg_duration,
				    SUM(CASE WHEN IsLate = 'yes' THEN 1 ELSE 0 END) as late_exits,
				    SUM(CASE WHEN IsExtended = 'yes' THEN 1 ELSE 0 END) as extensions,
				    MIN(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, Estimated_end_time))) as min_duration,
				    MAX(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, Estimated_end_time))) as max_duration
				FROM parkinginfo
				WHERE YEAR(Estimated_start_time) = ? AND MONTH(Estimated_start_time) = ?
				AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND statusEnum IN ('active', 'finished')
				""";

		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			stmt.setInt(1, reportDate.getYear());
			stmt.setInt(2, reportDate.getMonthValue());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					report.setTotalParkings(rs.getInt("total_parkings"));
					report.setAverageParkingTime(rs.getDouble("avg_duration"));
					report.setLateExits(rs.getInt("late_exits"));
					report.setExtensions(rs.getInt("extensions"));
					report.setMinParkingTime(rs.getInt("min_duration"));
					report.setMaxParkingTime(rs.getInt("max_duration"));

					return report;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error generating monthly parking time report: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		return null;
	}

	/**
	 * Generates a subscriber status report for a specific calendar month.
	 *
	 * @param reportDate A LocalDate representing the target month (1st day of the
	 *                   month).
	 * @return ParkingReport object for that month, or null if no data available.
	 */
	private ParkingReport generateMonthlySubscriberStatusReport(LocalDate reportDate) {
		ParkingReport report = new ParkingReport("SUBSCRIBER_STATUS", reportDate);
		Connection conn = DBController.getInstance().getConnection();

		// Get active subscribers for the month
		String activeSubQry = """
				SELECT COUNT(DISTINCT User_ID) as active_subscribers
				FROM parkinginfo
				WHERE YEAR(Estimated_start_time) = ? AND MONTH(Estimated_start_time) = ?
				""";

		// Get monthly order statistics
		String ordersQry = """
				SELECT
				    COUNT(*) as total_orders,
				    SUM(CASE WHEN IsOrderedEnum = 'yes' THEN 1 ELSE 0 END) as reservations,
				    SUM(CASE WHEN IsOrderedEnum = 'no' THEN 1 ELSE 0 END) as immediate_entries,
				    AVG(TIMESTAMPDIFF(MINUTE, Actual_start_time, COALESCE(Actual_end_time, Estimated_end_time))) as avg_session_duration
				FROM parkinginfo
				WHERE YEAR(Estimated_start_time) = ? AND MONTH(Estimated_start_time) = ?
				AND statusEnum IN ('active', 'finished')
				""";

		// Get cancelled reservations for the month
		String cancelledQry = """
				SELECT COUNT(*) as cancelled_reservations
				FROM parkinginfo
				WHERE statusEnum = 'cancelled'
				AND YEAR(Estimated_start_time) = ? AND MONTH(Estimated_start_time) = ?
				""";

		try {
			// Get active subscribers
			try (PreparedStatement stmt = conn.prepareStatement(activeSubQry)) {
				stmt.setInt(1, reportDate.getYear());
				stmt.setInt(2, reportDate.getMonthValue());
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setActiveSubscribers(rs.getInt("active_subscribers"));
					}
				}
			}

			// Get order statistics
			try (PreparedStatement stmt = conn.prepareStatement(ordersQry)) {
				stmt.setInt(1, reportDate.getYear());
				stmt.setInt(2, reportDate.getMonthValue());
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setTotalOrders(rs.getInt("total_orders"));
						report.setReservations(rs.getInt("reservations"));
						report.setImmediateEntries(rs.getInt("immediate_entries"));
						report.setAverageSessionDuration(rs.getDouble("avg_session_duration"));
					}
				}
			}

			// Get cancelled reservations
			try (PreparedStatement stmt = conn.prepareStatement(cancelledQry)) {
				stmt.setInt(1, reportDate.getYear());
				stmt.setInt(2, reportDate.getMonthValue());
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						report.setCancelledReservations(rs.getInt("cancelled_reservations"));
					}
				}
			}

			return report;

		} catch (SQLException e) {
			System.out.println("Error generating monthly subscriber status report: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		return null;
	}

	/**
	 * Stores a list of monthly reports in the database. Each report is saved with
	 * its type and generation timestamp.
	 *
	 * @param reports List of ParkingReport objects to be saved.
	 */
	private void storeMonthlyReports(ArrayList<ParkingReport> reports) {
		String qry = "INSERT INTO reports (Report_Type, Generated_Date, Report_Data) VALUES (?, NOW(), ?)";
		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			for (ParkingReport report : reports) {
				stmt.setString(1, report.getReportType());
				stmt.setString(2, report.toString()); // Store as JSON or formatted string
				stmt.executeUpdate();
			}
			System.out.println("Monthly reports stored successfully");
		} catch (SQLException e) {
			System.out.println("Error storing monthly reports: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
	}

	/**
	 * Retrieves previously generated reports of a given type between specified
	 * dates.
	 *
	 * @param reportType The report type ("PARKING_TIME" or "SUBSCRIBER_STATUS")
	 * @param fromDate   Start date (inclusive) of the range
	 * @param toDate     End date (inclusive) of the range
	 * @return List of ParkingReport objects within the specified date range
	 */
	public ArrayList<ParkingReport> getHistoricalReports(String reportType, LocalDate fromDate, LocalDate toDate) {
		ArrayList<ParkingReport> reports = new ArrayList<>();
		Connection conn = DBController.getInstance().getConnection();

		String qry = """
				SELECT * FROM reports
				WHERE Report_Type = ?
				MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND DATE(Generated_Date) BETWEEN ? AND ?
				ORDER BY Generated_Date DESC
				""";

		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			stmt.setString(1, reportType);
			stmt.setString(2, fromDate.toString());
			stmt.setString(3, toDate.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					// This would need to be enhanced to parse the stored report data
					// For now, we'll create a basic report object
					ParkingReport report = new ParkingReport();
					report.setReportType(rs.getString("Report_Type"));
					Timestamp genDate = rs.getTimestamp("Generated_Date");
					if (genDate != null) {
						report.setReportDate(genDate.toLocalDateTime().toLocalDate());
					}
					reports.add(report);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error getting historical reports: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		return reports;
	}

	/**
	 * Calculates total parking time (in hours) per day for the current month.
	 *
	 * @return Map of date string to total hours of parking for that day
	 */
	private java.util.Map<String, Integer> getTotalParkingTimePerDay() {
		java.util.Map<String, Integer> map = new TreeMap<>();
		String qry = """
				SELECT
				  DATE(Actual_start_time) AS day,
				  CEIL(SUM(TIMESTAMPDIFF(MINUTE, Actual_start_time, Actual_end_time)) / 60) AS total_hours
				FROM parkinginfo
				WHERE statusEnum = 'finished'
				  AND Actual_start_time IS NOT NULL
				  AND Actual_end_time IS NOT NULL
				  AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				GROUP BY day
				ORDER BY day;
								""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String day = rs.getString("day");
					int totalHours = rs.getInt("total_hours");
					map.put(day, totalHours);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return map;
	}

	/**
	 * Retrieves the number of parking sessions per hour (0–23) over finished
	 * sessions.
	 *
	 * @return Map of hour strings ("HH:00") to entry counts
	 */
	private java.util.Map<String, Integer> getHourlyDistribution() {
		java.util.Map<String, Integer> map = new TreeMap<>();
		String qry = """
				    SELECT HOUR(Actual_start_time) as hour, COUNT(*) as cnt
				    FROM parkinginfo
				    WHERE statusEnum = 'finished'
				    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
					AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				    GROUP BY hour
				    ORDER BY hour
				""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String hour = String.format("%02d:00", rs.getInt("hour"));
					int cnt = rs.getInt("cnt");
					map.put(hour, cnt);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return map;
	}

	/**
	 * Retrieves a breakdown of late exits per hour of the day.
	 *
	 * @return Map of hour strings ("HH:00") to late exit counts
	 */
	private java.util.Map<String, Integer> getLateExitsByHour() {
		java.util.Map<String, Integer> map = new TreeMap<>();
		String qry = """
				    SELECT HOUR(Actual_end_time) as hour, COUNT(*) as cnt
				    FROM parkinginfo
				    WHERE IsLate = 'yes' AND Actual_end_time IS NOT NULL
				    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
					AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				    GROUP BY hour
				    ORDER BY hour
				""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String hour = String.format("%02d:00", rs.getInt("hour"));
					int cnt = rs.getInt("cnt");
					map.put(hour, cnt);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return map;
	}

	/**
	 * Retrieves the number of unique subscribers per day in recent parking
	 * sessions.
	 *
	 * @return Map of date strings to unique subscriber counts
	 */
	private java.util.Map<String, Integer> getSubscribersPerDay() {
		java.util.Map<String, Integer> map = new TreeMap<>();
		String qry = """
				    SELECT DATE(Actual_start_time) as day, COUNT(DISTINCT User_ID) as cnt
				    FROM parkinginfo
				    WHERE statusEnum IN ('active', 'finished')
				    AND Actual_start_time IS NOT NULL
				    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
					AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				    GROUP BY day
				    ORDER BY day
				""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String day = rs.getString("day");
					int cnt = rs.getInt("cnt");
					map.put(day, cnt);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return map;
	}

	/**
	 * Counts how many parking sessions were completed without extensions.
	 *
	 * @return Number of sessions without extensions
	 */
	private int getNoExtensions() {
		String qry = """
				    SELECT COUNT(*) as noext
				    FROM parkinginfo
				    WHERE IsExtended = 'no'
				    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
					AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				    AND  statusEnum IN ('active', 'finished')
				""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("noext");
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return 0;
	}

	/**
	 * Counts how many distinct users have at least one late exit.
	 *
	 * @return Number of late subscribers
	 */
	private int getLateSubscribers() {
		String qry = """
				    SELECT COUNT(DISTINCT User_ID) as cnt
				    FROM parkinginfo
				    WHERE IsLate = 'yes'
				      AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
					  AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

				""";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("cnt");
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return 0;
	}

	/**
	 * Returns the total number of users in the system.
	 *
	 * @return Total user count
	 */
	private int getTotalSubscribers() {
		String qry = "SELECT COUNT(*) as cnt FROM users";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(qry)) {
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("cnt");
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return 0;
	}

	/**
	 * Returns the number of used reservations (finished and ordered) from the last
	 * 30 days.
	 *
	 * @return Number of used reservations
	 */
	public int getUsedReservations() {
		int result = 0; // usedReservations
		Connection conn = DBController.getInstance().getConnection();

		String usedReservationsQry = """
								    SELECT COUNT(*) as used_reservations
								    FROM parkinginfo
								    WHERE IsOrderedEnum = 'yes'
								    AND statusEnum = 'finished'
								    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								""";

		try {

			try (PreparedStatement stmt = conn.prepareStatement(usedReservationsQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						result = rs.getInt("used_reservations");
					}
				}
			}

		} catch (SQLException e) {
			System.out.println("Error getting reservations usage data: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		return result;
	}

	/**
	 * Returns the number of reservations that were cancelled in the last 30 days.
	 *
	 * @return Number of cancelled reservations
	 */
	public int getCancelledReservations() {
		int result = 0; // cancelledReservations
		Connection conn = DBController.getInstance().getConnection();

		String cancelledReservationsQry = """
								    SELECT COUNT(*) as cancelled_reservations
								    FROM parkinginfo
								    WHERE IsOrderedEnum = 'yes'
								    AND statusEnum = 'cancelled'
								    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								""";

		try {

			try (PreparedStatement stmt = conn.prepareStatement(cancelledReservationsQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						result = rs.getInt("cancelled_reservations");
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error getting reservations usage data: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return result;
	}

	/**
	 * Returns the number of reservations that are currently in 'preorder' status.
	 *
	 * @return Number of pre-ordered reservations
	 */
	public int getPreOrderedReservations() {
		int result = 0; // cancelledReservations
		Connection conn = DBController.getInstance().getConnection();

		String cancelledReservationsQry = """
								    SELECT COUNT(*) as cancelled_reservations
								    FROM parkinginfo
								    WHERE IsOrderedEnum = 'yes'
								    AND statusEnum = 'preorder'
								    AND MONTH(Estimated_start_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
				AND YEAR(Estimated_start_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))

								""";

		try {

			try (PreparedStatement stmt = conn.prepareStatement(cancelledReservationsQry)) {
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						result = rs.getInt("cancelled_reservations");
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error getting reservations usage data: " + e.getMessage());
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return result;
	}

	/**
	 * Counts the number of currently occupied parking spots.
	 *
	 * @return Number of occupied parking spots
	 */
	private int getOccupied() {
		String qry = """
				    SELECT COUNT(*) AS cnt
				    FROM parkingspot
				    WHERE isOccupied = '1'
				""";

		int result = 0;
		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(qry); ResultSet rs = stmt.executeQuery()) {

			if (rs.next()) {
				result = rs.getInt("cnt"); // או rs.getInt(1)
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}

		return result;
	}

	/**
	 * Retrieves the total number of parking spots available in the system.
	 *
	 * Executes a SQL query on the 'parkingspot' table to count all defined parking
	 * spots, regardless of their status (occupied, free, reserved, etc.).
	 *
	 * @return The total number of parking spots in the database. Returns 0 if a
	 *         database error occurs.
	 */
	private int getTotalSpots() {
		String sql = "SELECT COUNT(*) FROM parkingspot";
		int result = 0;
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				result = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBController.getInstance().releaseConnection(conn);
		}
		return result;
	}

}