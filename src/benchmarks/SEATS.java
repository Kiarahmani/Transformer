package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SEATS {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public SEATS(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}
	
	/*
	 * 
	 * 	(1) DELETE RESERVATION
	 * 
	 */
	public void deleteReservation() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	/*
	 * 
	 * 	(2) FIND FLIGHTS
	 * 
	 */
	public void findFlights() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
		
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	/*
	 * 
	 *  (3) FIND OPEN SEATS
	 * 
	 */
	public void findOpenSeats() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
		
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	/*
	 * 
	 * 	(4) NEW RESERVATION
	 * 
	 */
	public void newReservation() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
		
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	/*
	 * 
	 * 	(5) UPDATE CUSTOMER
	 * 
	 */
	public void updateCustomer() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
		
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	/*
	 * 
	 * 	(6) UPDATE RESERVATION
	 * 
	 */
	public void updateReservation() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
		
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
