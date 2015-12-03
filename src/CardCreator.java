import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

public class CardCreator {
	static HashMap<String, List<Integer>> expansion2draw = new HashMap<String, List<Integer>>();
	public static void main(String[] args) throws IOException, FontFormatException {

		init();
		String directory = "/Volumes/Users/drose/Dropbox/Booze Cube/";
		String expansion_code_map_file = directory + "Expansion Code Map.txt";
		HashMap<String, String> expansion_code_map = getExpansionCodeMap(expansion_code_map_file);
		
		String data_file = directory + "LegacyCube.2015-09-14.txt";

		FileReader fr = new FileReader(data_file);
		BufferedReader br = new BufferedReader(fr);

		String header = br.readLine();
		HashMap<String, Integer> colIndexMap = getColIndexMap(header);

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineCols = line.split("\t");
			System.out.println(lineCols[0]);
			
			String baseImageFile = directory + "Cards/Base/"
					+ lineCols[colIndexMap.get("image_file")];
			String boozeImageFile = directory + "Cards/Booze/"
					+ lineCols[colIndexMap.get("image_file")];
			String expansion_code = expansion_code_map.get(lineCols[colIndexMap.get("expansion")]);
			String image_url = "http://magiccards.info/scans/en/"+expansion_code+"/"+lineCols[colIndexMap.get("number")]+".jpg";
			String boozeText = "";
			if (lineCols.length > colIndexMap.get("booze_text")) {
				boozeText = lineCols[colIndexMap.get("booze_text")];
			}
			downloadBaseImage(image_url, baseImageFile);
			boozifyImage(baseImageFile, boozeImageFile, boozeText, expansion_code);
		}

		br.close();
	}

	private static void init() {
		List<Integer> defaultValues = new ArrayList<Integer>();
		defaultValues.add(20);
		defaultValues.add(285);
		defaultValues.add(265);
		defaultValues.add(120);
		expansion2draw.put("Default", defaultValues);
	}

	private static HashMap<String, String> getExpansionCodeMap(
			String expansion_code_map_file) throws IOException {
		HashMap<String, String> expansion_code_map = new HashMap<String, String>();
		FileReader fr = new FileReader(expansion_code_map_file);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineSplit = line.split("\t");
			expansion_code_map.put(lineSplit[0], lineSplit[1]);
		}
		
		br.close();
		return expansion_code_map;
	}

	private static void downloadBaseImage(String image_url, String baseImageFile) throws IOException{
		URL url = new URL(image_url);
        URLConnection connection = url.openConnection();
        InputStream input = connection.getInputStream();
        BufferedOutputStream outs = new BufferedOutputStream(new FileOutputStream(baseImageFile));
        int len;
        byte[] buf = new byte[1024];
        while ((len = input.read(buf)) > 0) {
            outs.write(buf, 0, len);
        }
        outs.close();
	}
	
	private static void boozifyImage(String baseImageFile,
			String boozeImageFile, String boozeText, String expansion_code) throws IOException, FontFormatException {
		BufferedImage bi = ImageIO.read(new File(baseImageFile));
		if (boozeText.length() > 0) {

			if(!expansion2draw.containsKey(expansion_code)){
				expansion_code = "Default";
			}
			Graphics2D ig2 = bi.createGraphics();

			clearTextBox(ig2, bi, expansion_code);
			writeTextBox(ig2, boozeText, expansion_code);
		}
		ImageIO.write(bi, "PNG", new File(boozeImageFile));
	}
	
	public static BufferedImage resize(BufferedImage img, int newW, int newH) {  
	    int w = img.getWidth();  
	    int h = img.getHeight();  
	    BufferedImage dimg = new BufferedImage(newW, newH, img.getType());  
	    Graphics2D g = dimg.createGraphics();  
	    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	    RenderingHints.VALUE_INTERPOLATION_BILINEAR);  
	    g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);  
	    g.dispose();  
	    return dimg;  
	} 

	private static HashMap<String, Integer> getColIndexMap(String header) {
		HashMap<String, Integer> colIndexMap = new HashMap<String, Integer>();
		int counter = 0;
		for (String colHeader : header.split("\t")) {
			colIndexMap.put(colHeader, counter++);
		}
		return colIndexMap;
	}

	private static void writeTextBox(Graphics2D ig2,
			String text, String expansion_code) throws FileNotFoundException, FontFormatException, IOException {

		int startWidth = expansion2draw.get(expansion_code).get(0);
		int startHeight = expansion2draw.get(expansion_code).get(1);
		int width = expansion2draw.get(expansion_code).get(2);
		int height = expansion2draw.get(expansion_code).get(3);
		
		float fontSize = 15;
		LinkedList<String> textLines = new LinkedList<String>();
		boolean fits = false;
		Font font = Font.createFont(Font.TRUETYPE_FONT, new File("/Volumes/Users/drose/Dropbox/Booze Cube/mplantin.ttf"));
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
		FontMetrics fontMetrics = null;
		int stringHeight = 0;

		while (!fits) {
            font = font.deriveFont(fontSize);
			ig2.setFont(font);
			fontMetrics = ig2.getFontMetrics();
			textLines.clear();

			String textTemp = text.replace("\"", "");

			int counter = 1;
			while (!textTemp.isEmpty()) {
				stringHeight = (int) (fontMetrics.getAscent() * 1.1);
				while (textTemp.length() > 0) {
					String textToDraw = textTemp;
					int stringWidth = fontMetrics.stringWidth(textToDraw);
					stringHeight = (int) (fontMetrics.getAscent() * 1.1);
					while (stringWidth > width) {
						int endPoint = textToDraw.length();
						if(textToDraw.contains(" ") && textToDraw.contains("\\n")){
							endPoint = Math.max(textToDraw.lastIndexOf(' '), textToDraw.lastIndexOf('\n'));
						}
						else if(textToDraw.contains(" ")){
							endPoint = textToDraw.lastIndexOf(" ");
						}
						else if(textToDraw.contains("\\n")){
							endPoint = textToDraw.lastIndexOf("\\n");
						}
						textToDraw = textToDraw.substring(0, endPoint);
						stringWidth = fontMetrics.stringWidth(textToDraw);
					}
					if(textToDraw.contains("\\n")){
						textToDraw = textToDraw.substring(0,textToDraw.indexOf("\\n"));
					}
					textLines.add(textToDraw);
					textTemp = textTemp.substring(textToDraw.length()).trim();
					if(textTemp.startsWith("\\n")){
						textTemp = textTemp.substring("\\n".length()).trim();
					}
					counter++;
				}
			}
			if (counter * stringHeight < height) {
				fits = true;
			}
			fontSize-=.3;

		}


		int counter = 1;
		stringHeight = (int) (fontMetrics.getAscent() * 1.1);
		ig2.setPaint(Color.black);
		for (String textLine : textLines) {
			ig2.drawString(textLine, startWidth, startHeight + stringHeight * counter++);
		}
	}

	private static void clearTextBox(Graphics2D ig2,
			BufferedImage bi, String expansion_code) {
		int startWidth = expansion2draw.get(expansion_code).get(0);
		int startHeight = expansion2draw.get(expansion_code).get(1);
		int width = expansion2draw.get(expansion_code).get(2);
		int height = expansion2draw.get(expansion_code).get(3);
		int colorRGB = bi.getRGB(startWidth, startHeight);
		ig2.setPaint(new Color(colorRGB));
		ig2.fillRect(startWidth, startHeight, width, height);
	}
}
