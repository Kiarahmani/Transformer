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
			stmt = connect.prepareStatement(("SELECT R_ID FROM RESERVATION WHERE R_F_ID = ? and R_SEAT = ?"));
			stmt.setLong(1, f_id);
			stmt.setLong(2, seatnum);
			results = stmt.executeQuery();
			found = results.next();
			results.close();
			if (found)
				throw new Exception(String.format(" Seat %d is already reserved on flight #%d", seatnum, f_id));
			stmt = connect.prepareStatement("SELECT R_ID FROM RESERVATION WHERE R_F_ID = ? AND R_C_ID = ?");
			stmt.setLong(1, f_id);
			stmt.setLong(2, c_id);
			results = stmt.executeQuery();
			found = results.next();
			results.close();

			if (found == false)
				throw new Exception(
						String.format(" Customer %d does not have an existing reservation on flight #%d", c_id, f_id));

			String BASE_SQL = "UPDATE RESERVATION SET R_SEAT = ? WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?";
			final String ReserveSeat0 = "R_IATTR00";
			final String ReserveSeat1 = "R_IATTR01";
			final String ReserveSeat2 = "R_IATTR02";
			final String ReserveSeat3 = "R_IATTR03";
			String ReserveSeats[] = { ReserveSeat0, ReserveSeat1, ReserveSeat2, ReserveSeat3 };
			stmt = connect
					.prepareStatement("UPDATE RESERVATION SET R_SEAT = ? WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?");
			stmt.setLong(1, seatnum);
			// stmt.setLong(2, attr_val);
			stmt.setLong(2, r_id);
			stmt.setLong(3, c_id);
			stmt.setLong(4, f_id);
			int updated = stmt.executeUpdate();
			// XXX
			// if (updated != 1)
			// throw new Exception(
			// String.format("Failed to update reservation on flight %d for customer #%d -
			// Updated %d records",
			// f_id, c_id, updated));
			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

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
				PreparedStatement nearby_stmt = connect.prepareStatement("SELECT * " + "  FROM " + "AIRPORT_DISTANCE"
						+ " WHERE D_AP_ID0 = ? " + "   AND D_DISTANCE <= ? " + " ORDER BY D_DISTANCE ASC ");
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
				PreparedStatement f_stmt = connect.prepareStatement("SELECT F_ID, F_AL_ID, F_SEATS_LEFT, "
						+ " F_DEPART_AP_ID, F_DEPART_TIME, F_ARRIVE_AP_ID, F_ARRIVE_TIME, "
						+ " AL_NAME, AL_IATTR00, AL_IATTR01 " + " FROM " + "FLIGHT" + ", " + "AIRLINE"
						+ " WHERE F_DEPART_AP_ID = 1 " + "   AND F_DEPART_TIME >= ? AND F_DEPART_TIME <= ? "
						+ "   AND F_AL_ID = AL_ID ");
				

				// Set Parameters
				//f_stmt.setLong(1, depart_aid);
				//f_stmt.setLong(2, start_date);
				//f_stmt.setLong(3, end_date);
				//f_stmt.setLong(4, arrive_aids.get(0));
				//f_stmt.setLong(5, arrive_aids.get(1));
				//f_stmt.setLong(6, arrive_aids.get(2));
				
			
				// Process Result
				ResultSet flightResults = f_stmt.executeQuery();
/*
				PreparedStatement ai_stmt = connect
						.prepareStatement("SELECT AP_CODE, AP_NAME, AP_CITY, AP_LONGITUDE, AP_LATITUDE, "
								+ " CO_ID, CO_NAME, CO_CODE_2, CO_CODE_3 " + " FROM " + "\"AIRPORT\"" + ", " + "COUNTRY"
								+ " WHERE AP_ID = ? AND AP_CO_ID = CO_ID ");
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
				
				*/
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

}
