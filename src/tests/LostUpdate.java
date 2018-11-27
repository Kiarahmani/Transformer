package tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class LostUpdate {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public LostUpdate(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateBalance() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps = connect.prepareStatement("select * from A where id=?");
			ps.setInt(1, 1);
			ResultSet rs = ps.executeQuery();
			rs.next();

			ps = connect.prepareStatement("update A set balance = ? where id=?");
			int balance = rs.getInt("balance");
			ps.setInt(1,  balance + 10 );
			ps.setInt(2, 1);

			ps.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
