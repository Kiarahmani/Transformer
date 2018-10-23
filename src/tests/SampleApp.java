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
			PreparedStatement f_stmt = connect
					.prepareStatement("SELECT F_STATUS, F_BASE_PRICE, F_SEATS_TOTAL, F_SEATS_LEFT, "
							+ "       (F_BASE_PRICE + (F_BASE_PRICE * (1 - (F_SEATS_LEFT / F_SEATS_TOTAL)))) AS F_PRICE "
							+ "  FROM " + "" + " WHERE F_ID = ?");
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
			PreparedStatement s_stmt = connect
					.prepareStatement("SELECT R_ID, R_F_ID, R_SEAT " + "  FROM " + "" + " WHERE R_F_ID = ?");
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

	public void updateReservation(long r_id, long f_id, long c_id, long seatnum, long attr_idx, long attr_val)
			throws Exception {

		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			assert (attr_idx >= 0);
			boolean found;
			PreparedStatement stmt = null;
			ResultSet results = null;
			stmt = connect.prepareStatement(("SELECT R_ID " + "  FROM " + "res" + " WHERE R_F_ID = ? and R_SEAT = ?"));
			stmt.setLong(1, f_id);
			stmt.setLong(2, seatnum);
			results = stmt.executeQuery();
			found = results.next();
			results.close();
			if (found)
				throw new Exception(String.format(" Seat %d is already reserved on flight #%d", seatnum, f_id));
			stmt = connect.prepareStatement("SELECT R_ID " + "  FROM " + "res" + " WHERE R_F_ID = ? AND R_C_ID = ?");
			stmt.setLong(1, f_id);
			stmt.setLong(2, c_id);
			results = stmt.executeQuery();
			found = results.next();
			results.close();
			if (found == false)
				throw new Exception(
						String.format(" Customer %d does not have an existing reservation on flight #%d", c_id, f_id));

			String BASE_SQL = "UPDATE " + "res" + "   SET R_SEAT = ?, %s = ? "
					+ " WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?";
			final String ReserveSeat0 = "R_IATTR00";
			final String ReserveSeat1 = "R_IATTR01";
			final String ReserveSeat2 = "R_IATTR02";
			final String ReserveSeat3 = "R_IATTR03";
			String ReserveSeats[] = { ReserveSeat0, ReserveSeat1, ReserveSeat2, ReserveSeat3 };
			stmt = connect.prepareStatement(String.format(BASE_SQL, ReserveSeats[(int) attr_idx]));
			stmt.setLong(1, seatnum);
			stmt.setLong(2, attr_val);
			stmt.setLong(3, r_id);
			stmt.setLong(4, c_id);
			stmt.setLong(5, f_id);
			int updated = stmt.executeUpdate();
			if (updated != 1)
				throw new Exception(
						String.format("Failed to update reservation on flight %d for customer #%d - Updated %d records",
								f_id, c_id, updated));

			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

	// ----------------------------------------------------------------------------------------------------------------------
	public void findFlights(long depart_aid, long arrive_aid, long start_date, long end_date, long distance)
			throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			assert (start_date != end_date);
			final List<Long> arrive_aids = new ArrayList<Long>();
			arrive_aids.add(arrive_aid);

			final List<Object[]> finalResults = new ArrayList<Object[]>();

			if (distance > 0) {
				// First get the nearby airports for the departure and arrival cities
				PreparedStatement nearby_stmt = connect.prepareStatement("select some stuff");
				nearby_stmt.setLong(1, depart_aid);
				nearby_stmt.setLong(2, distance);
				ResultSet nearby_results = nearby_stmt.executeQuery();
				while (nearby_results.next()) {
					long aid = nearby_results.getLong(1);
					double aid_distance = nearby_results.getDouble(2);
					arrive_aids.add(aid);
				} // WHILE
				nearby_results.close();
			}
			// H-Store doesn't support IN clauses, so we'll only get nearby flights to
			// nearby arrival cities
			int num_nearby = arrive_aids.size();
			if (num_nearby > 0) {
				PreparedStatement f_stmt = connect.prepareStatement("select some other stuff");
				assert (f_stmt != null);

				// Set Parameters
				f_stmt.setLong(1, depart_aid);
				f_stmt.setLong(2, start_date);
				f_stmt.setLong(3, end_date);
				for (int i = 0, cnt = Math.min(3, num_nearby); i < cnt; i++) {
					f_stmt.setLong(4 + i, arrive_aids.get(i));
				} // FOR

				// Process Result
				ResultSet flightResults = f_stmt.executeQuery();

				PreparedStatement ai_stmt = connect.prepareStatement("now we are doing select on airports?!");
				ResultSet ai_results = null;
				while (flightResults.next()) {
					long f_depart_airport = flightResults.getLong(4);
					long f_arrive_airport = flightResults.getLong(6);

					Object row[] = new Object[13];
					int r = 0;

					row[r++] = flightResults.getLong(1); // [00] F_ID
					row[r++] = flightResults.getLong(3); // [01] SEATS_LEFT
					row[r++] = flightResults.getString(8); // [02] AL_NAME

					// DEPARTURE AIRPORT
					ai_stmt.setLong(1, f_depart_airport);
					ai_results = ai_stmt.executeQuery();
					boolean adv = ai_results.next();
					assert (adv);
					row[r++] = flightResults.getDate(5); // [03] DEPART_TIME
					row[r++] = ai_results.getString(1); // [04] DEPART_AP_CODE
					row[r++] = ai_results.getString(2); // [05] DEPART_AP_NAME
					row[r++] = ai_results.getString(3); // [06] DEPART_AP_CITY
					row[r++] = ai_results.getString(7); // [07] DEPART_AP_COUNTRY
					ai_results.close();

					// ARRIVAL AIRPORT
					ai_stmt.setLong(1, f_arrive_airport);
					ai_results = ai_stmt.executeQuery();
					adv = ai_results.next();
					assert (adv);
					row[r++] = flightResults.getDate(7); // [08] ARRIVE_TIME
					row[r++] = ai_results.getString(1); // [09] ARRIVE_AP_CODE
					row[r++] = ai_results.getString(2); // [10] ARRIVE_AP_NAME
					row[r++] = ai_results.getString(3); // [11] ARRIVE_AP_CITY
					row[r++] = ai_results.getString(7); // [12] ARRIVE_AP_COUNTRY
					ai_results.close();

					finalResults.add(row);
				} // WHILE
					// ai_stmt.close();
				flightResults.close();
				// f_stmt.close();
			}
			System.out.println("results:");
			for (Object o1 : finalResults)
				System.out.println(o1);

			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

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

			// Flight Information
			stmt = connect.prepareStatement("SELECT F_AL_ID, F_SEATS_LEFT, " + "table" + ".* " + "  FROM " + "table"
					+ ", " + "table" + " WHERE F_ID = ? AND F_AL_ID = AL_ID");
			stmt.setLong(1, f_id);
			//
			rs = stmt.executeQuery();
			//
			found = rs.next();
			if (found == false) {
				rs.close();
				throw new Exception(String.format(" Invalid flight #%d", f_id));
			}
			long airline_id = rs.getLong(1);
			long seats_left = rs.getLong(2);
			rs.close();
			if (seats_left <= 0) {
				throw new Exception(String.format(" No more seats available for flight #%d", f_id));
			}
			// Check if Seat is Available
			stmt = connect.prepareStatement("SELECT R_ID " + "  FROM " + "table" + " WHERE R_F_ID = ? and R_SEAT = ?");
			stmt.setLong(1, f_id);
			stmt.setLong(2, seatnum);
			//
			rs = stmt.executeQuery();
			//
			found = rs.next();
			rs.close();
			if (found)
				throw new Exception(String.format(" Seat %d is already reserved on flight #%d", seatnum, f_id));

			// Check if the Customer already has a seat on this flight
			stmt = connect.prepareStatement("SELECT R_ID " + "  FROM " + "table" + " WHERE R_F_ID = ? AND R_C_ID = ?");
			stmt.setLong(1, f_id);
			stmt.setLong(2, c_id);
			//
			rs = stmt.executeQuery();
			//
			found = rs.next();
			rs.close();
			if (found)
				throw new Exception(
						String.format(" Customer %d already owns on a reservations on flight #%d", c_id, f_id));

			// Get Customer Information
			stmt = connect.prepareStatement(
					"SELECT C_BASE_AP_ID, C_BALANCE, C_SATTR00 " + "  FROM " + "table" + " WHERE C_ID = ? ");
			stmt.setLong(1, c_id);
			//
			rs = stmt.executeQuery();
			//
			found = rs.next();
			rs.close();
			if (found == false) {
				throw new Exception(String.format(" Invalid customer id: %d / %s", c_id, c_id));
			}
			stmt = connect.prepareStatement("INSERT INTO " + "table" + " (" + "   R_ID, " + "   R_C_ID, "
					+ "   R_F_ID, " + "   R_SEAT, " + "   R_PRICE, " + "   R_IATTR00, " + "   R_IATTR01, "
					+ "   R_IATTR02, " + "   R_IATTR03, " + "   R_IATTR04, " + "   R_IATTR05, " + "   R_IATTR06, "
					+ "   R_IATTR07, " + "   R_IATTR08 " + ") VALUES (" + "   ?, " + "   ?, " + "   ?, " + "   ?, "
					+ "   ?, " + "   ?, " + "   ?, " + "   ?, " + "   ?, " + "   ?, " + "   ?, " + "   ?, " + "   ?, "
					+ "   ? " + ")");
			stmt.setLong(1, r_id);
			stmt.setLong(2, c_id);
			stmt.setLong(3, f_id);
			stmt.setLong(4, seatnum);
			stmt.setLong(5, 2L);
			for (int i = 0; i < attrs.length; i++) {
				stmt.setLong(6 + i, attrs[i]);
			} // FOR
				//
			int updated = stmt.executeUpdate();
			//
			if (updated != 1) {
				String msg = String.format(
						"Failed to add reservation for flight #%d - Inserted %d records for InsertReservation", f_id,
						updated);
				throw new Exception(msg);
			}
			stmt = connect.prepareStatement(
					"UPDATE " + "table" + "   SET F_SEATS_LEFT = F_SEATS_LEFT - 1 " + " WHERE F_ID = ? ");
			stmt.setLong(1, f_id);
			//
			updated = stmt.executeUpdate();
			//
			if (updated != 1) {
				throw new Exception(
						String.format("Failed to add reservation for flight #%d - Updated %d records for UpdateFlight",
								f_id, updated));
			}

			stmt = connect.prepareStatement("UPDATE " + "table" + "   SET C_IATTR10 = C_IATTR10 + 1, "
					+ "       C_IATTR11 = C_IATTR11 + 1, " + "       C_IATTR12 = ?, " + "       C_IATTR13 = ?, "
					+ "       C_IATTR14 = ?, " + "       C_IATTR15 = ? " + " WHERE C_ID = ? ");
			stmt.setLong(1, attrs[0]);
			stmt.setLong(2, attrs[1]);
			stmt.setLong(3, attrs[2]);
			stmt.setLong(4, attrs[3]);
			stmt.setLong(5, c_id);
			//
			updated = stmt.executeUpdate();
			//
			if (updated != 1)
				throw new Exception(String.format(
						"Failed to add reservation for flight #%d - Updated %d records for UpdateCustomer", f_id,
						updated));
			stmt = connect.prepareStatement("UPDATE " + "table" + "   SET FF_IATTR10 = FF_IATTR10 + 1, "
					+ "       FF_IATTR11 = ?, " + "       FF_IATTR12 = ?, " + "       FF_IATTR13 = ?, "
					+ "       FF_IATTR14 = ? " + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
			stmt.setLong(1, attrs[4]);
			stmt.setLong(2, attrs[5]);
			stmt.setLong(3, attrs[6]);
			stmt.setLong(4, attrs[7]);
			stmt.setLong(5, c_id);
			stmt.setLong(6, airline_id);
			//
			updated = stmt.executeUpdate();
			//
			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

	public void updateCustomer(Long c_id, String c_id_str, Long update_ff, long attr0, long attr1) {
		try {

			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			PreparedStatement stmt;
			ResultSet rs;
			// Use C_ID_STR to get C_ID

			if (c_id == null) {

				assert (c_id_str != null);
				assert (c_id_str.isEmpty() == false);

				stmt = connect.prepareStatement("SELECT C_ID " + "  FROM " + "table" + " WHERE C_ID_STR = ? ");
				stmt.setString(1, c_id_str);
				System.out.println("q1");
				rs = stmt.executeQuery();
				if (rs.next()) {
					c_id = rs.getLong(1);
				} else {
					rs.close();
					throw new Exception(
							String.format("No Customer information record found for string '%s'", c_id_str));
				}
				rs.close();
			}
			assert (c_id != null);

			stmt = connect.prepareStatement("SELECT * " + "  FROM " + "table" + " WHERE C_ID = ? ");
			stmt.setLong(1, c_id);
			System.out.println("q2");
			rs = stmt.executeQuery();
			if (rs.next() == false) {
				rs.close();
				throw new Exception(String.format("No Customer information record found for id '%d'", c_id));
			}
			assert (c_id == rs.getLong(1));
			long base_airport = rs.getLong(3);
			rs.close();

			// Get their airport information
			stmt = connect.prepareStatement(
					"SELECT * " + "  FROM " + "table" + ", " + "table" + " WHERE AP_ID = ? AND AP_CO_ID = CO_ID ");
			stmt.setLong(1, base_airport);
			System.out.println("q3");
			ResultSet airport_results = stmt.executeQuery();
			boolean adv = airport_results.next();
			airport_results.close();
			assert (adv);

			if (update_ff != null) {
				stmt = connect.prepareStatement("SELECT * FROM " + "table" + " WHERE FF_C_ID = ?");
				stmt.setLong(1, c_id);
				System.out.println("q4");
				ResultSet ff_results = stmt.executeQuery();
				while (ff_results.next()) {
					long ff_al_id = ff_results.getLong(2);
					stmt = connect.prepareStatement("UPDATE " + "table" + "   SET FF_IATTR00 = ?, "
							+ "       FF_IATTR01 = ? " + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ? ");
					stmt.setLong(1, attr0);
					stmt.setLong(2, attr1);
					stmt.setLong(3, c_id);
					stmt.setLong(4, ff_al_id);
					System.out.println(ff_al_id);
					stmt.executeUpdate();
				} // WHILE
				ff_results.close();
				stmt = connect.prepareStatement(
						"UPDATE " + "table" + "   SET C_IATTR00 = ?, " + "       C_IATTR01 = ? " + " WHERE C_ID = ?");
				stmt.setLong(1, attr0);
				stmt.setLong(2, attr1);
				stmt.setLong(3, c_id);
				System.out.println("u2");
				int updated = stmt.executeUpdate();
				if (updated != 1) {
					String msg = String.format("Failed to update customer #%d - Updated %d records", c_id, updated);
					throw new Exception(msg);
				}
			}

			connect.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	
	
	// ----------------------------------------------------------------------------------------------------------------------
		public void deleteReservation(long f_id, Long c_id, String c_id_str, String ff_c_id_str, Long ff_al_id)
				throws Exception {
			try {
				Object o = Class.forName("MyDriver").newInstance();
				DriverManager.registerDriver((Driver) o);
				Driver driver = DriverManager.getDriver("jdbc:mydriver://");
				connect = driver.connect(null, p);
				connect.setAutoCommit(false);
				PreparedStatement stmt = null;
				// If we weren't given the customer id, then look it up
				if (c_id == null) {
					boolean has_al_id = false;

					// Use the customer's id as a string
					if (c_id_str != null && c_id_str.length() > 0) {
						stmt = connect.prepareStatement("yet another select");
						stmt.setString(1, c_id_str);
					}
					// Otherwise use their FrequentFlyer information
					else {
						assert (ff_c_id_str.isEmpty() == false);
						assert (ff_al_id != null);
						stmt = connect.prepareStatement("another select");
						stmt.setString(1, ff_c_id_str);
						has_al_id = true;
					}
					ResultSet results = stmt.executeQuery();
					if (results.next()) {
						c_id = results.getLong(1);
						if (has_al_id)
							ff_al_id = results.getLong(2);
					} else {
						results.close();
						throw new Exception(
								String.format("No Customer record was found [c_id_str=%s, ff_c_id_str=%s, ff_al_id=%s]",
										c_id_str, ff_c_id_str, ff_al_id));
					}
					results.close();
				}
				// Now get the result of the information that we need
				// If there is no valid customer record, then throw an abort
				// This should happen 5% of the time
				stmt = connect.prepareStatement("this should be a select");
				stmt.setLong(1, c_id);
				stmt.setLong(2, f_id);
				ResultSet results = stmt.executeQuery();
				if (results.next() == false) {
					results.close();
					throw new Exception(String.format("No Customer information record found for id '%d'", c_id));
				}
				long c_iattr00 = results.getLong(4) + 1;
				long seats_left = results.getLong(8);
				long r_id = results.getLong(9);
				double r_price = results.getDouble(11);
				results.close();
				int updated = 0;
				// Now delete all of the flights that they have on this flight
				stmt = connect.prepareStatement("wow a delete !" );
				stmt.setLong(1, r_id);
				stmt.setLong(2, c_id);
				stmt.setLong(3, f_id);
				updated = stmt.executeUpdate();
				assert (updated == 1);

				// Update Available Seats on Flight
				stmt = connect.prepareStatement("more update");
				stmt.setLong(1, f_id);
				updated = stmt.executeUpdate();

				// Update Customer's Balance
				stmt = connect.prepareStatement("another update");
				stmt.setLong(1, (long) (-1 * r_price));
				stmt.setLong(2, c_iattr00);
				stmt.setLong(3, c_id);
				updated = stmt.executeUpdate();
				assert (updated == 1);

				// Update Customer's Frequent Flyer Information (Optional)
				if (ff_al_id != null) {
					stmt = connect.prepareStatement("some update ..");
					stmt.setLong(1, c_id);
					stmt.setLong(2, ff_al_id);
					updated = stmt.executeUpdate();
					assert (updated == 1) : String.format("Failed to update FrequentFlyer info [c_id=%d, ff_al_id=%d]",
							c_id, ff_al_id);
				}

				connect.commit();
			} catch (Exception e) {
				throw e;
			} finally {
			}

		}
	
	
	
	
	
	
	
	
	
	
	
}
