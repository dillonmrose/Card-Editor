import java.util.HashMap;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

public class Card {
	String name = "";
	String cardNumber = "";
	String type = "";
	String expansion = "";
	String rulesText = "";
	String boozeText = "";
	String mana = "";
	String power = "";
	String toughness = "";
	String cmc = "";

	public Card(String sourceUrl, HashMap<String, String> symbolMap)
			throws Exception {
		HashMap<String, Tag> id2Tag = new HashMap<String, Tag>();
		Parser parser = new Parser();
		NodeFilter filter = new AndFilter(new TagNameFilter("div"),
				new AndFilter(new HasAttributeFilter("id"),
						new HasAttributeFilter("class")));

		parser.setResource(sourceUrl);
		NodeList nodeList = parser.parse(filter);

		for (Node node : nodeList.toNodeArray()) {
			if (node instanceof Tag) {
				Tag tag = (Tag) node;
				String idAttribute = tag.getAttribute("id");
				String classAttribute = tag.getAttribute("class");
				if (classAttribute.contains("row")) {
					id2Tag.put(idAttribute.substring(
							"ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_"
									.length(), idAttribute.lastIndexOf("Row")),
							getValueTag(tag));
				}
			}
		}

		if (id2Tag.containsKey("name")) {
			name = getBasicValue(id2Tag.get("name"));
		}
		if (id2Tag.containsKey("type")) {
			type = getBasicValue(id2Tag.get("type"));
		}
		if (id2Tag.containsKey("number")) {
			cardNumber = getBasicValue(id2Tag.get("number"));
		}
		if (id2Tag.containsKey("set")) {
			expansion = getBasicValue(id2Tag.get("set"));
		}
		if (id2Tag.containsKey("text")) {
			rulesText = getTextValue(id2Tag.get("text"), symbolMap);
		}
		if (id2Tag.containsKey("cmc")) {
			cmc = getBasicValue(id2Tag.get("cmc"));
		}
		if (id2Tag.containsKey("mana")) {
			mana = getManaValue(id2Tag.get("mana"), symbolMap);
		}
		if (type.contains("Creature") && id2Tag.containsKey("pt")) {
			String[] pt = getBasicValue(id2Tag.get("pt")).split("/");
			power = pt[0];
			toughness = pt[1];
		}
	}

	public String getBasicValue(Tag t) {
		return normalize(t.toPlainTextString().trim());
	}

	public String getManaValue(Tag t, HashMap<String, String> symbolMap) {
		StringBuilder mana = new StringBuilder();
		NodeList symbols = t.getChildren();
		for (Node symbol : symbols.toNodeArray()) {
			if (symbol instanceof Tag) {
				Tag symbolTag = (Tag) symbol;
				mana.append("\\");
				mana.append(symbolMap.get(symbolTag.getAttribute("alt")));
			}
		}
		return normalize(mana.toString());
	}

	public String getTextValue(Tag t, HashMap<String, String> symbolMap) {
		StringBuilder text = new StringBuilder();
		NodeList textLines = t.getChildren();
		for (Node textLine : textLines.toNodeArray()) {
			NodeList textParts = textLine.getChildren();
			if (textParts == null) {
				text.append(textLine.toPlainTextString().trim());
			} else {
				StringBuilder textPartString = new StringBuilder();
				for (Node textPart : textParts.toNodeArray()) {
					if (textPart instanceof Tag) {
						Tag textPartTag = (Tag) textPart;
						if(textPartTag.getTagName().equals("IMG")){
							textPartString.append("\\");
							textPartString.append(symbolMap.get(textPartTag.getAttribute("alt")));
						}else{
							textPartString.append(textPartTag.toPlainTextString().trim());
						}
					}else{
						textPartString.append(" ");
						textPartString.append(textPart.toHtml().trim());
						textPartString.append(" ");
					}
				}
				text.append(textPartString.toString().trim());
			}
			text.append("\\n");
		}
		String finalText = text.toString();
		while(finalText.contains("  ")){
			finalText = finalText.replace("  ", " ");
		}
		finalText = finalText.trim();
		if(finalText.startsWith("\\n")){
			finalText = finalText.substring(2);
		}
		if(finalText.endsWith("\\n")){
			finalText = finalText.substring(0,finalText.length()-2);
		}
		finalText = finalText.trim();
		return normalize(finalText);
	}

	public Tag getValueTag(Tag tag) {
		NodeList children = tag.getChildren();
		for (Node node : children.toNodeArray()) {
			if (node instanceof Tag) {
				Tag childTag = (Tag) node;
				String classAttribute = childTag.getAttribute("class");
				if (classAttribute.equals("value")) {
					return childTag;
				}
			}
		}
		return null;
	}
	
	public String normalize(String s){
		for(int i = 0; i < s.length(); i++){
			if(s.charAt(i) == 8212){
				s = s.substring(0, i) + "-" + s.substring(i+1);
			}
			if(s.charAt(i) == 8226){
				s = s.substring(0, i) + ">" + s.substring(i+1);
			}
			if(s.charAt(i) == 8722){
				s = s.substring(0, i) + "-" + s.substring(i+1);
			}
			if(s.charAt(i) == 8216 || s.charAt(i) == 39){
				s = s.substring(0, i) + "'" + s.substring(i+1);
			}
			if(s.charAt(i) == 198){
				s = s.substring(0, i) + "Ae" + s.substring(i+1);
			}
		}
		while(s.contains("  ")){
			s = s.replace("  ", " ");
		}
		return s;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("\t");
		sb.append(type);
		sb.append("\t");
		sb.append(cmc);
		sb.append("\t");
		sb.append(mana);
		sb.append("\t");
		sb.append(expansion);
		sb.append("\t");
		sb.append(cardNumber);
		sb.append("\t");
		sb.append(power);
		sb.append("\t");
		sb.append(toughness);
		sb.append("\t");
		sb.append(rulesText);
		sb.append("\t");
		sb.append(boozeText);
		return sb.toString();
	}
}
