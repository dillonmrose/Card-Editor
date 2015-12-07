import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

public class CardList {
	static HashMap<String, String> symbolMap = null;
	List<Card> cards;

	public CardList() throws Exception {
		loadSymbolMap();
		cards = new ArrayList<Card>();
	}

	public CardList(List<String> cardUrls) throws Exception {
		this();
		for (String cardUrl : cardUrls) {
			System.out.println("\t" + cardUrl);
			try {
				Card card = new Card(cardUrl, symbolMap);
				cards.add(card);
			} catch (Exception e) {
				continue;
			}
		}
	}

	public CardList(String cubeUrl) throws Exception {
		this();
		List<String> cardUrls = getCardUrls(cubeUrl);
		for (String cardUrl : cardUrls) {
			System.out.println("\t" + cardUrl);
			try {
				Card card = new Card(cardUrl, symbolMap);
				cards.add(card);
			} catch (Exception e) {
				continue;
			}
		}
	}

	public void add(Card card) {
		cards.add(card);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name");
		sb.append("\t");
		sb.append("type");
		sb.append("\t");
		sb.append("cmc");
		sb.append("\t");
		sb.append("mana");
		sb.append("\t");
		sb.append("expansion");
		sb.append("\t");
		sb.append("cardNumber");
		sb.append("\t");
		sb.append("power");
		sb.append("\t");
		sb.append("toughness");
		sb.append("\t");
		sb.append("rulesText");
		sb.append("\t");
		sb.append("boozeText");
		sb.append("\n");
		for (Card card : cards) {
			String cardString = card.toString();
			if (!cardString.trim().isEmpty()) {
				sb.append(card.toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public static void loadSymbolMap() throws Exception {
		String symbolMapFile = System.getProperty("user.dir")
				+ "/public/Symbol Map.txt";
		if (symbolMap == null) {
			symbolMap = new HashMap<String, String>();
			Scanner sc = new Scanner(new FileReader(symbolMapFile));
			while (sc.hasNext()) {
				String line = sc.nextLine();
				String[] lineSplit = line.split("\t");
				symbolMap.put(lineSplit[0], lineSplit[1]);
			}
			sc.close();
		}
	}

	public static List<String> getCardUrls(String cubeUrl) throws Exception {
		System.out.println("Getting card urls from: " + cubeUrl);
		List<String> cardUrls = new ArrayList<String>();

		Parser parser = new Parser();
		NodeFilter filter = new AndFilter(new TagNameFilter("a"),
				new HasAttributeFilter("href"));

		parser.setResource(cubeUrl);
		NodeList nodeList = parser.parse(filter);

		for (Node node : nodeList.toNodeArray()) {
			if (node instanceof Tag) {
				Tag tag = (Tag) node;
				String cardUrl = tag.getAttribute("href");
				if (cardUrl
						.contains("http://gatherer.wizards.com/Pages/Card/Details.aspx?name=")) {
					cardUrls.add(cardUrl);
				}
			}
		}
		return cardUrls;
	}
}
