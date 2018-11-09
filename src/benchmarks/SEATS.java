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
								"SELECT C_SATTR00, C_SATTR02, C_SATTR04, C_IATTR00, C_IATTR02, C_IATTR04, C_IATTR06, C_BALANCE, C_IATTR10, C_IATTR11 FROM CUSTOMER WHERE C_ID = ?");
						stmt2.setLong(1, extracted_c_id);
						ResultSet results2 = stmt2.executeQuery();
						results2.next();
						int oldBal = results2.getInt("C_BALANCE");
						int oldAttr10 = results2.getInt("C_IATTR10");
						int oldAttr11 = results2.getInt("C_IATTR11");
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
						int seats_left = results3.getInt(0);

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

						PreparedStatement stmt52 = connect
								.prepareStatement("UPDATE FLIGHT SET F_SEATS_LEFT = ?" + " WHERE F_ID = ? ");
						stmt52.setInt(1, seats_left + 1);
						stmt52.setLong(2, f_id);
						updated = stmt52.executeUpdate();

						// Update Customer's Balance
						PreparedStatement stmt6 = connect.prepareStatement(
								"UPDATE CUSTOMER SET C_BALANCE = ?, C_IATTR00 = ?, C_IATTR10 = ?,  C_IATTR11 = ? WHERE C_ID = ? ");
						stmt6.setLong(1, oldBal + (long) (-1 * r_price));
						stmt6.setLong(2, c_iattr00);
						stmt6.setLong(3, oldAttr10 - 1);
						stmt6.setLong(4, oldAttr11 - 1);
						stmt6.setLong(5, extracted_c_id);
						updated = stmt6.executeUpdate();

						// Update Customer's Frequent Flyer Information (Optional)
						if (ff_al_id != -1) {
							PreparedStatement stmt71 = connect.prepareStatement("SELECT FF_IATTR10 FROM FREQUENT_FLYER "
									+ " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
							stmt71.setLong(1, extracted_c_id);
							stmt71.setLong(2, ff_al_id);
							ResultSet results5 = stmt71.executeQuery();
							results5.next();
							int olAttr10 = results5.getInt(0);
							PreparedStatement stmt72 = connect
									.prepareStatement("UPDATE FREQUENT_FLYER SET FF_IATTR10 = ?" + " WHERE FF_C_ID = ? "
											+ "   AND FF_AL_ID = ?");
							stmt72.setLong(1, olAttr10 - 1);
							stmt72.setLong(2, extracted_c_id);
							stmt72.setLong(3, ff_al_id);
							updated = stmt72.executeUpdate();
						}
					}
				}
			} else {
				// Use the customer's id as a string
				PreparedStatement stmt2 = connect.prepareStatement(
						"SELECT C_SATTR00, C_SATTR02, C_SATTR04, C_IATTR00, C_IATTR02, C_IATTR04, C_IATTR06, C_BALANCE, C_IATTR10, C_IATTR11 FROM CUSTOMER WHERE C_ID = ?");
				stmt2.setLong(1, given_c_id);
				ResultSet results2 = stmt2.executeQuery();
				results2.next();
				int oldBal = results2.getInt("C_BALANCE");
				int oldAttr10 = results2.getInt("C_IATTR10");
				int oldAttr11 = results2.getInt("C_IATTR11");
				long c_iattr00 = results2.getLong(0) + 1;

				if (!results2.next()) {
					results2.close();
				}
				// select flight
				PreparedStatement stmt31 = connect.prepareStatement("SELECT F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ? ");
				stmt31.setLong(1, f_id);
				ResultSet results3 = stmt31.executeQuery();
				results3.next();
				int seats_left = results3.getInt(0);

				// select reservation
				PreparedStatement stmt32 = connect
						.prepareStatement("SELECT R_ID, R_SEAT, R_PRICE, R_IATTR00 FROM RESERVATION WHERE R_C_ID = ? ");
				stmt32.setLong(1, given_c_id);
				ResultSet results4 = stmt32.executeQuery();
				if (!results4.next() || !results3.next())
					return;
				results4.next();
				long r_id = results4.getLong(0);
				double r_price = results4.getDouble(2);
				results4.close();
				int updated = 0;
				// Now delete all of the flights that they have on this flight
				PreparedStatement stmt4 = connect
						.prepareStatement("DELETE FROM RESERVATION WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?");
				stmt4.setLong(1, r_id);
				stmt4.setLong(2, given_c_id);
				stmt4.setLong(3, f_id);
				updated = stmt4.executeUpdate();

				PreparedStatement stmt52 = connect
						.prepareStatement("UPDATE FLIGHT SET F_SEATS_LEFT = ?" + " WHERE F_ID = ? ");
				stmt52.setInt(1, seats_left + 1);
				stmt52.setLong(2, f_id);
				updated = stmt52.executeUpdate();

				// Update Customer's Balance
				PreparedStatement stmt6 = connect.prepareStatement(
						"UPDATE CUSTOMER SET C_BALANCE = ?, C_IATTR00 = ?, C_IATTR10 = ?,  C_IATTR11 = ? WHERE C_ID = ? ");
				stmt6.setLong(1, oldBal + (long) (-1 * r_price));
				stmt6.setLong(2, c_iattr00);
				stmt6.setLong(3, oldAttr10 - 1);
				stmt6.setLong(4, oldAttr11 - 1);
				stmt6.setLong(5, given_c_id);
				updated = stmt6.executeUpdate();

				// Update Customer's Frequent Flyer Information (Optional)
				if (ff_al_id != -1) {
					PreparedStatement stmt71 = connect.prepareStatement(
							"SELECT FF_IATTR10 FROM FREQUENT_FLYER " + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
					stmt71.setLong(1, given_c_id);
					stmt71.setLong(2, ff_al_id);
					ResultSet results5 = stmt71.executeQuery();
					results5.next();
					int olAttr10 = results5.getInt(0);
					PreparedStatement stmt72 = connect.prepareStatement(
							"UPDATE FREQUENT_FLYER SET FF_IATTR10 = ?" + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
					stmt72.setLong(1, olAttr10 - 1);
					stmt72.setLong(2, given_c_id);
					stmt72.setLong(3, ff_al_id);
					updated = stmt72.executeUpdate();

				}

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

	public void findFlights(long depart_aid, long arrive_aid, long start_date, long end_date, long distance)
			throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			final List<Long> arrive_aids = new ArrayList<Long>();
			arrive_aids.add(arrive_aid);
			final List<Object[]> finalResults = new ArrayList<Object[]>();
			if (distance > 0) {
				// First get the nearby airports for the departure and arrival cities
				PreparedStatement nearby_stmt = connect
						.prepareStatement("SELECT * " + "  FROM AIRPORT_DISTANCE WHERE D_AP_ID0 = ? "
								+ "   AND D_DISTANCE <= ? " + " ORDER BY D_DISTANCE ASC ");
				nearby_stmt.setLong(1, depart_aid);
				nearby_stmt.setLong(2, distance);
				ResultSet nearby_results = nearby_stmt.executeQuery();
				while (nearby_results.next()) {
					long aid = nearby_results.getLong(1);
					double aid_distance = nearby_results.getDouble(2);
					arrive_aids.add(aid);
				} // WHILE
				nearby_results.close();
				int num_nearby = arrive_aids.size();
				if (num_nearby > 0) {
					PreparedStatement f_stmt1 = connect.prepareStatement("SELECT F_ID, F_AL_ID, F_SEATS_LEFT, "
							+ " F_DEPART_AP_ID, F_DEPART_TIME, F_ARRIVE_AP_ID, F_ARRIVE_TIME, "
							+ " AL_NAME, AL_IATTR00, AL_IATTR01 " + " FROM FLIGHT WHERE F_DEPART_AP_ID = ? "
							+ "   AND F_DEPART_TIME >= ? AND F_DEPART_TIME <= ? ");
					// Set Parameters
					f_stmt1.setLong(1, depart_aid);
					f_stmt1.setLong(2, start_date);
					f_stmt1.setLong(3, end_date);

					ResultSet flightResults1 = f_stmt1.executeQuery();
					flightResults1.next();

					while (flightResults1.next()) {
						long f_depart_airport = flightResults1.getLong("F_DEPART_AP_ID");
						long f_arrive_airport = flightResults1.getLong("F_ARRIVE_AP_ID");
						PreparedStatement f_stmt2 = connect.prepareStatement(
								"SELECT AL_NAME, AL_IATTR00, AL_IATTR01 FROM AIRLINE WHERE AL_ID=?");
						f_stmt2.setInt(1, flightResults1.getInt("F_AL_ID"));
						ResultSet flightResults2 = f_stmt2.executeQuery();
						flightResults2.next();
				//		String al_name = flightResults2.getString("AL_NAME");
/*
						Object row[] = new Object[13];
						int r = 0;

						row[r++] = flightResults1.getLong("F_ID"); // [00] F_ID
						row[r++] = flightResults1.getLong("SEATS_LEFT"); // [01] SEATS_LEFT
						row[r++] = flightResults2.getString("AL_NAME"); // [02] AL_NAME

						// DEPARTURE AIRPORT
						PreparedStatement ai_stmt1 = connect.prepareStatement(
								"SELECT AP_CODE, AP_NAME, AP_CITY, AP_LONGITUDE, AP_LATITUDE, AP_CO_ID "
										+ " FROM AIRPORT WHERE AP_ID = ? ");
						ai_stmt1.setLong(1, f_depart_airport);
						ResultSet ai_results1 = ai_stmt1.executeQuery();
						ai_results1.next();
						PreparedStatement ai_stmt2 = connect.prepareStatement(
								"SELECT CO_ID, CO_NAME, CO_CODE_2, CO_CODE_3 " + " FROM COUNTRY WHERE CO_ID = ?");
						ai_stmt2.setLong(1, ai_results1.getLong("AP_CO_ID"));
						ResultSet ai_results2 = ai_stmt2.executeQuery();
						// save the results
						boolean adv = ai_results2.next();
						row[r++] = flightResults1.getInt("F_DEPART_TIME"); // [03] DEPART_TIME
						row[r++] = ai_results1.getString("AP_CODE"); // [04] DEPART_AP_CODE
						row[r++] = ai_results1.getString("AP_NAME"); // [05] DEPART_AP_NAME
						row[r++] = ai_results1.getString("AP_CITY"); // [06] DEPART_AP_CITY
						row[r++] = ai_results2.getString("CO_NAME"); // [07] DEPART_AP_COUNTRY
*/
					}

				}

			}

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

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
