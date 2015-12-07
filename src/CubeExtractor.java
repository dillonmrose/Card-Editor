import java.io.FileWriter;
import java.util.HashMap;

public class CubeExtractor {
	
	final static HashMap<String, String> symbolMap = new HashMap<String, String>();

	public static void main(String[] args) throws Exception {
		String currentDir = System.getProperty("user.dir");
		String cubeUrl = "http://magic.wizards.com/en/articles/archive/legacy-cube-cardlist-2015-09-14#";
		String outputFile = currentDir + "/public/CardList.txt";

		CardList cards = new CardList(cubeUrl);

		FileWriter fw = new FileWriter(outputFile);
		fw.write(cards.toString());
		fw.close();
	}
}
