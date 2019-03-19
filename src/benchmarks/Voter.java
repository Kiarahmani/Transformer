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

public class Voter {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public Voter(int id) {
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

	public void vote(long voteId, long phoneNumber, int contestantNumber, long maxVotesPerPhoneNumber)
			throws SQLException {
		PreparedStatement stmt = connect
				.prepareStatement("SELECT contestant_number FROM CONTESTANTS WHERE contestant_number = ?");
		stmt.setInt(1, contestantNumber);
		ResultSet rs = stmt.executeQuery();
		if (!rs.next()) {
			System.out.println("ERR_INVALID_CONTESTANT");
		}

		PreparedStatement stmt2 = connect.prepareStatement("SELECT state FROM AREA_CODE_STATE WHERE area_code = ?");
		stmt2.setLong(1, phoneNumber);
		ResultSet rs2 = stmt2.executeQuery();
		// rs2.next();
		if (rs2.next()) {
			PreparedStatement stmt3 = connect.prepareStatement(
					"INSERT INTO VOTES (vote_id, phone_number, state, contestant_number, created) VALUES (?, ?, ?, ?, ?)");
			stmt3.setLong(1, voteId);
			stmt3.setLong(2, phoneNumber);
			stmt3.setString(3, rs2.getString(1));
			stmt3.setInt(4, contestantNumber);
			stmt3.setInt(5, (int) System.currentTimeMillis());
			stmt3.executeUpdate();	
		}
		else {
			PreparedStatement stmt3 = connect.prepareStatement(
					"INSERT INTO VOTES (vote_id, phone_number, state, contestant_number, created) VALUES (?, ?, ?, ?, ?)");
			stmt3.setLong(1, voteId);
			stmt3.setLong(2, phoneNumber);
			stmt3.setString(3, "XX");
			stmt3.setInt(4, contestantNumber);
			stmt3.setInt(5, (int) System.currentTimeMillis());
			stmt3.executeUpdate();
		}
		
	}

}
