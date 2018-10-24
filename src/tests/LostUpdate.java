package tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

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

	public void updateBalance(int key, String name, boolean shouldPrint, double anotherDouble) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select C_ID_STR,C_BALANCE from CUSTOMER where C_ID = ?");
			int x = 230;
			ps.setInt(1, key + x);
			ResultSet rs = ps.executeQuery();
			rs.next();
			int kir =  rs.getInt(1);   // TODO : this should be able to moved around without error
			int id = rs.getInt("id");
			int balance = rs.getInt("balance");
			int sum = id + balance + 15;
			System.out.println("SUM: " + sum);

			ResultSet rs2 = null;
			PreparedStatement ps2;
			// if (rs.next()) {
			ps2 = connect.prepareStatement(
					"update DEPARTMENT set D_Name = Broke,D_FUNDS = ? where D_ID = 1 and D_FUNDS > = 10000");
			
			ps2.setInt(1,kir);
			ps2.executeUpdate();
			ps2.close();
			// } else {
			/*
			 * ps2 = connect.prepareStatement("select * from CUSTOMER where C_ID_STR = ?");
			 * ps2.setString(1, "Kiarash"); rs2 = ps2.executeQuery();
			 * System.out.println(rs2); //} PreparedStatement ps3 =
			 * connect.prepareStatement(
			 * "INSERT INTO DEPARTMENT (D_ID, D_ADDR, D_FUNDS, D_NAME) VALUES (5,Yeager2550, ?,Sales)"
			 * ); ps3.setLong(1, rs2.getLong("C_BALANCE")); ps3.executeUpdate(); ps.close();
			 */
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
