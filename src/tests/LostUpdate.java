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

	public void updateBalance(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			int j = 1000;
			// System.out.println(j + 100);
			j = j * 30;
			PreparedStatement ps = connect.prepareStatement("select * from A where id=?");
			ps.setInt(1, ++j);
			ResultSet rs1 = ps.executeQuery();
			rs1.next();
			int i = 900;
			int id = rs1.getInt("id");
			
			if (rs1.next()) {
				ps = connect.prepareStatement("update A set balance = ? where id=?");
				int balance = rs1.getInt("balance");
				ps.setInt(1, id + balance);
				i += 100;
				ps.setInt(2, i*j);

				ps.executeUpdate();
			}

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
