package tests;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SampleApp {
	private Connection connect = null;
	private Statement stmt = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public SampleApp(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}
/*
	public void findOpenSeats(long f_id) throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			// 150 seats
			final long seatmap[] = new long[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1 };
			assert (seatmap.length == 5);

			// First calculate the seat price using the flight's base price
			// and the number of seats that remaining
			PreparedStatement f_stmt = connect.prepareStatement("SELECT F_STATUS, F_BASE_PRICE, F_SEATS_TOTAL, F_SEATS_LEFT, "
					+ "       (F_BASE_PRICE + (F_BASE_PRICE * (1 - (F_SEATS_LEFT / F_SEATS_TOTAL)))) AS F_PRICE " + "  FROM "
					+ ""+ " WHERE F_ID = ?");
			f_stmt.setLong(1, f_id);
			ResultSet f_results = f_stmt.executeQuery();
			boolean adv = f_results.next();
			assert (adv);
			// long status = results[0].getLong(0);
			double base_price = f_results.getDouble(2);
			long seats_total = f_results.getLong(3);
			long seats_left = f_results.getLong(4);
			double seat_price = f_results.getDouble(5);
			f_results.close();
			double _seat_price = base_price + (base_price * (1.0 - (seats_left / (double) seats_total)));
			PreparedStatement s_stmt = connect.prepareStatement("SELECT R_ID, R_F_ID, R_SEAT " + "  FROM "
					+"" + " WHERE R_F_ID = ?");
			s_stmt.setLong(1, f_id);
			ResultSet s_results = s_stmt.executeQuery();
			while (s_results.next()) {
				long r_id = s_results.getLong(1);
				int seatnum = s_results.getInt(3);
				assert (seatmap[seatnum] == -1) : "Duplicate seat reservation: R_ID=" + r_id;
				seatmap[seatnum] = 1;
			} // WHILE
			s_results.close();
			int ctr = 0;
			Object[][] returnResults = new Object[5][];
			for (int i = 0; i < seatmap.length; ++i) {
				if (seatmap[i] == -1) {
					// Charge more for the first seats
					double price = seat_price * (i < 4 ? 2.0 : 1.0);
					Object[] row = new Object[] { f_id, i, price };
					returnResults[ctr++] = row;
					if (ctr == returnResults.length)
						break;
				}
			} // FOR

			// print the available saets
			for (Object[] o1 : returnResults) {
				for (Object o2 : o1)
					System.out.println(o2);
				System.out.println("====================");
			}
			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}
*/
	public void newReservation(long r_id, long c_id, long f_id, long seatnum, double price, long attrs[])
			throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			PreparedStatement stmt;
			ResultSet rs;
			boolean found;


			
			stmt = connect.prepareStatement("UPDATE " + "" + "   SET F_SEATS_LEFT = F_SEATS_LEFT - 1 " + " WHERE F_ID = ? ");
			stmt.setLong(1, f_id);
			rs = stmt.executeQuery();
			
			connect.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

	}
	public void test(long r_id, long c_id, long f_id, long seatnum, double price, long attrs[])
			throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			PreparedStatement stmt;
			ResultSet rs;
			boolean found;

			// Flight Information
			stmt = connect.prepareStatement("SELECT " + "" + "   SET F_SEATS_LEFT = F_SEATS_LEFT - 1 " + " WHERE F_ID = ? ");
			stmt.setLong(1, f_id);
			rs = stmt.executeQuery();
			
		
			connect.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

	}

	

}
