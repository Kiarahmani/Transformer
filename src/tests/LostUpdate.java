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

	public void updateBalance(int key, long f_id, long c_id, long seatnum, long attr_idx, long attr_val)
			throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);

			//
			PreparedStatement ps = connect.prepareStatement("select * from A where id=1");
			ResultSet rs1 = ps.executeQuery();
			rs1.next();
			int id = rs1.getInt("id");
			int balance = rs1.getInt("balance");
			System.out.println(id + balance);

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
