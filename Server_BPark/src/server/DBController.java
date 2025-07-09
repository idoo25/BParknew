package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ||in SERVER||
 * 
 * DBController manages a pool of database connections using the Singleton
 * pattern. It provides thread-safe methods for acquiring and releasing
 * connections.
 * 
 * The class initializes a fixed-size connection pool upon first invocation, and
 * allows controlled access to connections for database operations.
 * 
 * @author Yair
 * @version 1.0
 */
public class DBController {

	/** Singleton instance of DBController */
	private static DBController instance = null;

	/** Queue representing the connection pool */
	private final Queue<Connection> connectionPool = new LinkedList<>();

	/** Fixed size of the connection pool */
	private final int POOL_SIZE = 6;

	/**
	 * Flag indicating whether the DB initialization succeeded (1 = success, 0 =
	 * failure)
	 */
	private final int successFlag;

	/**
	 * Private constructor. Establishes a pool of connections to the specified
	 * database.
	 *
	 * @param dbName   the name of the database to connect to
	 * @param password the root user's password for the database
	 */
	private DBController(String dbName, String password) {
		int flag = 0;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost/" + dbName + "?serverTimezone=Asia/Jerusalem";

			for (int i = 0; i < POOL_SIZE; i++) {
				Connection conn = DriverManager.getConnection(url, "root", password);
				connectionPool.add(conn);
			}

			System.out.println("Database connection established.");
			System.out.println("Initialized DB connection pool with " + connectionPool.size() + " connections.");

			flag = 1;
		} catch (Exception e) {
			System.err.println("Failed to connect to database: " + e.getMessage());
		}

		this.successFlag = flag;
	}

	/**
	 * Initializes the singleton instance of DBController. This method must be
	 * called once before calling {@link #getInstance()}.
	 *
	 * @param dbName   the name of the database
	 * @param password the root user's password for the database
	 */
	public static synchronized void initializeConnection(String dbName, String password) {
		if (instance == null) {
			instance = new DBController(dbName, password);
		}
	}

	/**
	 * Returns the singleton instance of DBController.
	 *
	 * @return the singleton DBController instance
	 * @throws IllegalStateException if
	 *                               {@link #initializeConnection(String, String)}
	 *                               was not called first
	 */
	public static DBController getInstance() {
		if (instance == null) {
			throw new IllegalStateException("DBController not initialized. Call initializeConnection() first.");
		}
		return instance;
	}

	/**
	 * Retrieves a database connection from the pool. Waits up to 5 seconds if no
	 * connection is currently available.
	 *
	 * @return a {@link Connection} object from the pool
	 * @throws RuntimeException if no connection becomes available within 5 seconds
	 */
	public synchronized Connection getConnection() {
		long startTime = System.currentTimeMillis();
		int waitCounter = 0;
		while (connectionPool.isEmpty()) {
			if (System.currentTimeMillis() - startTime > 5000) {
				throw new RuntimeException("Timeout: No available DB connections.");
			}
			if (waitCounter % 10 == 0) {
				System.out.println("Waiting for available DB connection...");
			}
			try {
				wait(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			waitCounter++;
		}

		Connection conn = connectionPool.remove();
		System.out.println("Connection taken. Remaining in pool: " + connectionPool.size());
		return conn;
	}

	/**
	 * Returns a used connection back to the pool and notifies waiting threads.
	 *
	 * @param conn the {@link Connection} to return to the pool
	 */
	public synchronized void releaseConnection(Connection conn) {
		if (conn != null) {
			connectionPool.add(conn);
			System.out.println("Connection returned. Now available in pool: " + connectionPool.size());
			notifyAll(); // Wake up waiting threads
		}
	}

	/**
	 * Returns a flag indicating the result of the database initialization.
	 *
	 * @return 1 if successful, 0 if failed
	 */
	public int getSuccessFlag() {
		return successFlag;
	}
}
