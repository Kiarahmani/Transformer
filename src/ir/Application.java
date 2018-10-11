package ir;

import java.util.ArrayList;

// This class captures the intermediate representation of db-backed programs which are extracted from Gimple representations
public class Application {
	private ArrayList<Transaction> txns;

	public Application() {
		txns = new ArrayList<Transaction>();
	}

	public void addTxn(Transaction txn) {
		this.txns.add(txn);
	}
	
	public void printApp () {
		System.out.println("\n\n=====\nExtracted Application: ");
		for (Transaction t:txns)
			t.printTxn();
	}

}
