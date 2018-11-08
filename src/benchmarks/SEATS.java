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
	 * (1) DELETE RESERVATION
	 * 
	 */
	public void deleteReservation(long f_id, long given_c_id, String c_id_str, String ff_c_id_str, long ff_al_id,
			int cidGiven) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			// If we weren't given the customer id, then look it up
			if (cidGiven == 0) {
				boolean has_al_id = false;
				long extracted_c_id;
				// Use the customer's id as a string
				if (c_id_str != "" && c_id_str.length() > 0) {

					PreparedStatement stmt1 = connect.prepareStatement("SELECT C_ID FROM CUSTOMER WHERE C_ID_STR = ?");
					stmt1.setString(1, c_id_str);
					ResultSet results1 = stmt1.executeQuery();

					if (results1.next()) {
						extracted_c_id = results1.getLong(0);
						if (has_al_id)
							ff_al_id = results1.getLong(1);
						PreparedStatement stmt2 = connect.prepareStatement(
								"SELECT C_SATTR00, C_SATTR02, C_SATTR04, C_IATTR00, C_IATTR02, C_IATTR04, C_IATTR06 FROM CUSTOMER WHERE C_ID = ?");
						stmt2.setLong(1, extracted_c_id);
						ResultSet results2 = stmt2.executeQuery();
						results2.next();
						long c_iattr00 = results2.getLong(0) + 1;

						if (!results2.next()) {
							results2.close();
						}
						// select flight
						PreparedStatement stmt31 = connect
								.prepareStatement("SELECT F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ? ");
						stmt31.setLong(1, f_id);
						ResultSet results3 = stmt31.executeQuery();
						results3.next();
						long seats_left = results3.getLong(0);

						// select reservation
						PreparedStatement stmt32 = connect.prepareStatement(
								"SELECT R_ID, R_SEAT, R_PRICE, R_IATTR00 FROM RESERVATION WHERE R_C_ID = ? ");
						stmt32.setLong(1, extracted_c_id);
						ResultSet results4 = stmt32.executeQuery();
						if (!results4.next() || !results3.next())
							return;
						results4.next();
						long r_id = results4.getLong(0);
						double r_price = results4.getDouble(2);
						results4.close();
						int updated = 0;
						// Now delete all of the flights that they have on this flight
						PreparedStatement stmt4 = connect.prepareStatement(
								"DELETE FROM RESERVATION WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?");
						stmt4.setLong(1, r_id);
						stmt4.setLong(2, extracted_c_id);
						stmt4.setLong(3, f_id);
						updated = stmt4.executeUpdate();

						// Update Available Seats on Flight
						PreparedStatement stmt51 = connect
								.prepareStatement("SELECT F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ? ");
						stmt51.setLong(1, f_id);
						ResultSet results5 = stmt51.executeQuery();
						results5.next();
						int oldLeft = results5.getInt(0);

						PreparedStatement stmt52 = connect
								.prepareStatement("UPDATE FLIGHT SET F_SEATS_LEFT = ?" + " WHERE F_ID = ? ");
						stmt52.setInt(1, oldLeft + 1);
						stmt52.setLong(2, f_id);
						updated = stmt52.executeUpdate();

					}
				}
			} else {
				// if c_id is given
			}

			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	/*
	 * 
	 * (2) FIND FLIGHTS
	 * 
	 */

	/*
	 * 
	 * public void findFlights() throws Exception { try {
	 * 
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); connect =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally {
	 * 
	 * }
	 * 
	 * }
	 */
	/*
	 * 
	 * (3) FIND OPEN SEATS
	 * 
	 */

	/*
	 * public void findOpenSeats() throws Exception { try {
	 * 
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); connect =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally {
	 * 
	 * }
	 * 
	 * }
	 */
	/*
	 * 
	 * (4) NEW RESERVATION
	 * 
	 */
	/*
	 * public void newReservation() throws Exception { try {
	 * 
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); connect =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally {
	 * 
	 * }
	 * 
	 * }
	 */
	/*
	 * 
	 * (5) UPDATE CUSTOMER
	 * 
	 */
	/*
	 * public void updateCustomer() throws Exception { try {
	 * 
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); connect =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally {
	 * 
	 * }
	 * 
	 * }
	 */
	/*
	 * 
	 * (6) UPDATE RESERVATION
	 * 
	 */
	/*
	 * public void updateReservation() throws Exception { try {
	 * 
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); connect =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally {
	 * 
	 * }
	 * 
	 * }
	 */

}
