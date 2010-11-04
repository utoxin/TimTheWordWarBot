package Tim;

import java.io.Serializable;

public class FridgeItem implements Serializable {

	private static final long serialVersionUID = -1658316674659907588L;
	private String name;
	private boolean approved;

	public FridgeItem(String str, boolean flag) {
		this.name = str.toLowerCase();
		this.approved = flag;
	}

	public boolean equals(FridgeItem item) {
		return this.name.equalsIgnoreCase(item.name);
	}

	public boolean equals(String itemname) {
		return this.name.equalsIgnoreCase(itemname);
	}

	public boolean isApproved() {
		return this.approved;
	}
}
