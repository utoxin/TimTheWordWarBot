package Tim;

import java.util.ArrayList;
import java.io.*;

public class Fridge implements Serializable {

	private static final long serialVersionUID = 6583411952855142866L;
	private String filename;
	private ArrayList<FridgeItem> items;

	public Fridge(String filename) {
		this.filename = filename;
		try {
		} catch (Exception e) {
			// Something went wrong. Maybe we don't have a file??
			this.items = new ArrayList<FridgeItem>();
			this.items.add(new FridgeItem("coffee", true));
			this.items.add(new FridgeItem("hot chocolate", true));
			this.items.add(new FridgeItem("confetti", true));
		}
	}

	public void addItem(String itemname) {
		if (!this.items.contains(itemname)) {
		}
	}
}
