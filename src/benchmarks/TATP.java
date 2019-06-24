package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TATP {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public TATP(int id) {
		this.id = id;
		p = new Properties();
		p.setProperty("id", String.valueOf(this.id));
		Object o;
		try {
			o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect("", p);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	//
	//
	//
	//
	//
	//

	public void deleteCallForwarding(String sub_nbr, int sf_type, int start_time) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT s_id FROM " + "SUBSCRIBER" + " WHERE sub_nbr = ?");
		stmt.setString(1, sub_nbr);
		ResultSet results = stmt.executeQuery();
		if (!results.next()) {
			System.out.println("INVALID sub_nbr");
		}
		long s_id = results.getLong("s_id");
		PreparedStatement stmt2 = connect.prepareStatement(
				"DELETE FROM " + "CALL_FORWARDING" + " WHERE s_id = ? AND sf_type = ? AND start_time = ?");
		stmt2.setLong(1, s_id);
		stmt2.setInt(2, sf_type);
		stmt2.setInt(3, start_time);
		stmt2.executeUpdate();
	}

	//
	//
	//
	//
	//
	//

	public void getAccessData(long s_id, int ai_type) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement(
				"SELECT data1, data2, data3, data4 FROM " + "ACCESS_INFO" + " WHERE s_id = ? AND ai_type = ?");
		stmt.setLong(1, s_id);
		stmt.setInt(2, ai_type);
		ResultSet results = stmt.executeQuery();
	}

	//
	//
	//
	//
	//
	//

	public void getNewDestination(int s_id, int sf_type, int start_time, int end_time) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT * " + "  FROM " + "SPECIAL_FACILITY"
				+ " WHERE s_id = ? " + "   AND sf_type = ? " + "   AND is_active = 1 ");
		stmt.setInt(1, s_id);
		stmt.setInt(2, sf_type);
		ResultSet results = stmt.executeQuery();
		while (!results.next()) {
			int loop_s_id = results.getInt("s_id");
			int loop_sf_type = results.getInt("sf_type");
			PreparedStatement stmt2 = connect.prepareStatement("SELECT numberx " + "  FROM " + "CALL_FORWARDING"
					+ " WHERE s_id = ? " + "   AND sf_type = ? " + " AND start_time <= ? AND  end_time > ? ");
			stmt2.setInt(1, loop_s_id);
			stmt2.setInt(2, loop_sf_type);
			stmt2.setInt(3, start_time);
			stmt2.setInt(4, end_time);
			ResultSet loop_results = stmt2.executeQuery();
			loop_results.next();
		}
	}

	//
	//
	//
	//
	//
	//

	public void getSubscriber(long s_id, int ai_type) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT * FROM " + "SUBSCRIBER" + " WHERE s_id = ?");
		stmt.setLong(1, s_id);
		ResultSet results = stmt.executeQuery();
		results.next();
	}

	//
	//
	//
	//
	//
	//

	public void insertCallForwarding(String sub_nbr, int sf_type, int start_time, int end_time, String numberx)
			throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT s_id FROM " + "SUBSCRIBER" + " WHERE sub_nbr = ?");
		stmt.setString(1, sub_nbr);
		ResultSet results = stmt.executeQuery();
		if (!results.next())
			System.out.println("Invalid s_id");
		long s_id = results.getLong("s_id");

		PreparedStatement stmt2 = connect
				.prepareStatement("SELECT sf_type FROM " + "SPECIAL_FACILITY" + " WHERE s_id = ?");
		stmt2.setLong(1, s_id);
		ResultSet results2 = stmt2.executeQuery();
		results2.next();

		// Inserting a new CALL_FORWARDING record only succeeds 30% of the time
		PreparedStatement stmt3 = connect
				.prepareStatement("INSERT INTO " + "CALL_FORWARDING" + " VALUES (?, ?, ?, ?, ?)");
		stmt3.setLong(1, s_id);
		stmt3.setInt(2, sf_type);
		stmt3.setInt(3, start_time);
		stmt3.setInt(4, end_time);
		stmt3.setString(5, numberx);
		stmt3.executeUpdate();
	}

	//
	//
	//
	//
	//
	//

	public void updateLocation(int location, String sub_nbr) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT s_id FROM " + "SUBSCRIBER" + " WHERE sub_nbr = ?");
		stmt.setString(1, sub_nbr);
		ResultSet results = stmt.executeQuery();

		if (results.next()) {
			long s_id = results.getLong("s_id");
			PreparedStatement stmt2 = connect
					.prepareStatement("UPDATE " + "SUBSCRIBER" + " SET vlr_location = ? WHERE s_id = ?");
			stmt2.setInt(1, location);
			stmt2.setLong(2, s_id);
			stmt2.executeUpdate();
		}
	}

	//
	//
	//
	//
	//
	//

	public void updateSubscriberData(long s_id, int bit_1, int data_a, int sf_type) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("UPDATE " + "SUBSCRIBER" + " SET bit_1 = ? WHERE s_id = ?");
		stmt.setInt(1, bit_1);
		stmt.setLong(2, s_id);
		stmt.executeUpdate();

		PreparedStatement stmt2 = connect
				.prepareStatement("UPDATE " + "SPECIAL_FACILITY" + " SET data_a = ? WHERE s_id = ? AND sf_type = ?");
		stmt2.setInt(1, data_a);
		stmt2.setLong(2, s_id);
		stmt2.setInt(3, sf_type);
		stmt2.executeUpdate();

	}

}
