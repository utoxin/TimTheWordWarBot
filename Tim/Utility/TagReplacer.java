package Tim.Utility;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Tim;
import org.apache.commons.lang3.text.StrSubstitutor;

public class TagReplacer {
	private HashMap<String, String> dynamicTags       = new HashMap<>();
	private Pattern                 replacementFinder = Pattern.compile("%\\(((.*?)(\\.\\d+)?)\\)");

	public String doTagReplacment(String input) {
		setInternalDynamicTags(input);
		StrSubstitutor sub = new StrSubstitutor(dynamicTags, "%(", ")");
		return sub.replace(input);
	}

	private void setInternalDynamicTags(String input) {
		Matcher replacements = replacementFinder.matcher(input);
		while (replacements.find()) {
			String fullTag = replacements.group(1);
			String bareTag = replacements.group(2);

			if (handleListValue(bareTag)) {
				setDynamicTag(fullTag, getRandomListValue(bareTag));
			}
		}
	}

	private boolean handleListValue(String tag) {
		switch (tag) {
			case "acolor":
			case "colour":
			case "flavor":
			case "flavour":
			case "item":
			case "number":
				return true;

			default:
				return Tim.db.dynamic_lists.containsKey(tag);
		}
	}

	public void setDynamicTag(String tag, String value) {
		dynamicTags.put(tag, value);
	}

	private String getRandomListValue(String list) {
		switch (list) {
			case "acolor":
				String color = Tim.db.dynamic_lists.get("color")
												   .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("color")
																							 .size()));
				switch (color.charAt(0)) {
					case 'a':
					case 'e':
					case 'i':
					case 'o':
					case 'u':
						color = "n " + color;
						break;
					default:
						color = " " + color;
				}

				return color;

			case "color":
			case "colour":
				return Tim.db.dynamic_lists.get("color")
										   .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("color")
																					 .size()));

			case "flavor":
			case "flavour":
				return Tim.db.flavours.get(Tim.rand.nextInt(Tim.db.flavours.size()));

			case "item":
				return Tim.db.items.get(Tim.rand.nextInt(Tim.db.items.size()));

			case "number":
				return String.format("%d", Tim.rand.nextInt(100));

			default:
				return Tim.db.dynamic_lists.get(list)
										   .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get(list)
																					 .size()));
		}
	}
}
