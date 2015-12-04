import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;

public class CardEditorDriver {

	private static class BufferedImageTranscoder extends ImageTranscoder {
		private BufferedImage image = null;

		@Override
		public BufferedImage createImage(int arg0, int arg1) {

			return image;
		}

		private void setImage(BufferedImage image) {
			this.image = image;
		}

		@Override
		public void writeImage(BufferedImage arg0, TranscoderOutput arg1)
				throws TranscoderException {
		}
	}

	static String currentDirectory;
	final static SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
			XMLResourceDescriptor.getXMLParserClassName());
	static HashMap<String, String> expansion2ExpansionCode = new HashMap<String, String>();
	static HashMap<String, HashMap<String, List<Point>>> expansionCode2TBPoints = new HashMap<String, HashMap<String, List<Point>>>();
	final static BufferedImageTranscoder bufferedImageTranscoder = new BufferedImageTranscoder();

	public static void main(String[] args) throws Exception {
		init();

		String data_file = currentDirectory
				+ "/public/LegacyCube.2015-09-14.txt";

		FileReader fr = new FileReader(data_file);
		BufferedReader br = new BufferedReader(fr);

		String header = br.readLine();
		HashMap<String, Integer> colIndexMap = getColIndexMap(header);

		int counter = 0;

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineCols = line.split("\t");
			System.out.println(lineCols[0]);

			String baseImageFile = currentDirectory + "/cards/original/"
					+ String.format("%03d", counter) + ".jpg";
			String boozeImageFile = currentDirectory + "/cards/booze/"
					+ String.format("%03d", counter) + ".png";
			String expansion_code = expansion2ExpansionCode
					.get(lineCols[colIndexMap.get("expansion")]);
			String type = lineCols[colIndexMap.get("type")];
			if (type.contains("Creature")) {
				type = "Creature";
			} else {
				type = "NonCreature";
			}

			String image_url = "http://magiccards.info/scans/en/"
					+ expansion_code + "/"
					+ lineCols[colIndexMap.get("number")] + ".jpg";
			downloadBaseImage(image_url, baseImageFile);

			String boozeText = "";
			if (lineCols.length > colIndexMap.get("booze_text")) {
				boozeText = lineCols[colIndexMap.get("booze_text")];
			}
			boozifyImage(baseImageFile, boozeImageFile, boozeText,
					expansion_code, type);
			counter++;
		}

		int sheetCounter = 0;
		int imageCounter = 0;
		BufferedImage sheet = new BufferedImage(312 * 3, 445 * 3,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = sheet.getGraphics();
		System.out.println("Creating sheet: " + sheetCounter);
		for (File cardImageFile : new File(currentDirectory + "/cards/booze/")
				.listFiles()) {
			if (cardImageFile.getName().startsWith("."))
				continue;

			System.out.println("\tSheet: " + sheetCounter);
			System.out.println("\tImage: " + imageCounter);
			System.out.println("\tCard: " + cardImageFile);
			BufferedImage cardImage = ImageIO.read(cardImageFile);
			g.drawImage(cardImage, 312 * (imageCounter % 3),
					445 * (imageCounter / 3), null);
			imageCounter++;

			if (imageCounter == 9) {
				ImageIO.write(sheet, "PNG",
						new File(currentDirectory + "/cards/sheets/sheet"
								+ String.format("%03d", sheetCounter) + ".png"));
				sheet = new BufferedImage(312 * 3, 445 * 3,
						BufferedImage.TYPE_INT_ARGB);
				g = sheet.getGraphics();
				imageCounter = 0;
				sheetCounter++;
				System.out.println("Creating sheet: " + sheetCounter);
			}
		}
		ImageIO.write(sheet, "PNG", new File(currentDirectory
				+ "/cards/sheets/sheet" + String.format("%03d", sheetCounter)
				+ ".jpg"));

		br.close();
	}

	private static void init() throws Exception {
		currentDirectory = System.getProperty("user.dir");
		String expansion2ExpansionCodeFile = currentDirectory
				+ "/public/Expansion Code Map.txt";
		String expansionCode2TBPointsFile = currentDirectory
				+ "/public/Textbox Data.txt";
		expansion2ExpansionCode = getExpansion2expansionCodeMap(expansion2ExpansionCodeFile);
		expansionCode2TBPoints = getExpansionCode2TBPointsMap(expansionCode2TBPointsFile);
	}

	private static HashMap<String, String> getExpansion2expansionCodeMap(
			String expansion2ExpansionCodeFile) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		FileReader fr = new FileReader(expansion2ExpansionCodeFile);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineSplit = line.split("\t");
			map.put(lineSplit[0], lineSplit[1]);
		}

		br.close();
		return map;
	}

	private static HashMap<String, HashMap<String, List<Point>>> getExpansionCode2TBPointsMap(
			String expansionCode2TBPointsFile) throws Exception {
		HashMap<String, HashMap<String, List<Point>>> map = new HashMap<String, HashMap<String, List<Point>>>();
		FileReader fr = new FileReader(expansionCode2TBPointsFile);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineSplit = line.split("\t");
			if (!map.containsKey(lineSplit[0])) {
				map.put(lineSplit[0], new HashMap<String, List<Point>>());
			}
			HashMap<String, List<Point>> type2TBPoints = map.get(lineSplit[0]);
			List<Point> TBPoints = new ArrayList<Point>();
			TBPoints.add(new Point(Integer.parseInt(lineSplit[2]), Integer
					.parseInt(lineSplit[3])));
			TBPoints.add(new Point(Integer.parseInt(lineSplit[4]), Integer
					.parseInt(lineSplit[5])));
			TBPoints.add(new Point(Integer.parseInt(lineSplit[6]), Integer
					.parseInt(lineSplit[7])));
			type2TBPoints.put(lineSplit[1], TBPoints);
			map.put(lineSplit[0], type2TBPoints);
		}

		br.close();
		return map;
	}

	private static void downloadBaseImage(String image_url, String baseImageFile)
			throws Exception {
		URL url = new URL(image_url);
		URLConnection connection = url.openConnection();
		InputStream input = connection.getInputStream();
		BufferedOutputStream outs = new BufferedOutputStream(
				new FileOutputStream(baseImageFile));
		int len;
		byte[] buf = new byte[1024];
		while ((len = input.read(buf)) > 0) {
			outs.write(buf, 0, len);
		}
		outs.close();
	}

	private static void boozifyImage(String baseImageFile,
			String boozeImageFile, String boozeText, String expansion_code,
			String type) throws Exception {
		BufferedImage bi = ImageIO.read(new File(baseImageFile));
		if (boozeText.length() > 0) {

			if (!expansionCode2TBPoints.containsKey(expansion_code)) {
				expansion_code = "Default";
			}
			List<Point> TBPoints = expansionCode2TBPoints.get(expansion_code)
					.get(type);
			Graphics2D ig2 = bi.createGraphics();

			clearTextBox(ig2, bi, TBPoints);
			writeTextBox(ig2, boozeText, TBPoints);
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

	private static void writeTextBox(Graphics2D ig2, String text,
			List<Point> TBPoints) throws Exception {

		int TLX = (int) TBPoints.get(0).getX();
		int TLY = (int) TBPoints.get(0).getY();
		int BRX1 = (int) TBPoints.get(1).getX();
		int BRY1 = (int) TBPoints.get(1).getY();

		float fontSize = 15;
		LinkedList<String> textLines = new LinkedList<String>();
		boolean fits = false;
		Font font = Font.createFont(Font.TRUETYPE_FONT, new File(
				currentDirectory + "/public/mplantin.ttf"));
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
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
					while (stringWidth > BRX1 - TLX) {
						int endPoint = textToDraw.length();
						if (textToDraw.contains(" ")
								&& textToDraw.contains("\\n")) {
							endPoint = Math.max(textToDraw.lastIndexOf(' '),
									textToDraw.lastIndexOf('\n'));
						} else if (textToDraw.contains(" ")) {
							endPoint = textToDraw.lastIndexOf(" ");
						} else if (textToDraw.contains("\\n")) {
							endPoint = textToDraw.lastIndexOf("\\n");
						}
						textToDraw = textToDraw.substring(0, endPoint);
						stringWidth = fontMetrics.stringWidth(textToDraw);
					}
					if (textToDraw.contains("\\n")) {
						textToDraw = textToDraw.substring(0,
								textToDraw.indexOf("\\n"));
					}
					textLines.add(textToDraw);
					textTemp = textTemp.substring(textToDraw.length()).trim();
					if (textTemp.startsWith("\\n")) {
						textTemp = textTemp.substring("\\n".length()).trim();
					}
					counter++;
				}
			}
			if (counter * stringHeight < BRY1 - TLY) {
				fits = true;
			}
			fontSize -= .1;

		}

		/*
		 * This is Code for including mana symbols and stuff need to handle
		 * resizing issues and stuff BufferedImage img = new BufferedImage(600,
		 * 600, BufferedImage.TYPE_INT_ARGB); File svg_URI_input = new
		 * File(currentDirectory+"/public/icons/T.svg"); TranscoderInput
		 * input_svg_image = new TranscoderInput(svg_URI_input.toString());
		 * 
		 * SVGDocument svg = factory.createSVGDocument(svg_URI_input.toString(),
		 * input_svg_image.getInputStream());
		 * 
		 * TranscodingHints hints = new TranscodingHints();
		 * hints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
		 * hints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION,
		 * svg.getImplementation());
		 * hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
		 * SVGConstants.SVG_NAMESPACE_URI);
		 * hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
		 * bufferedImageTranscoder.setTranscodingHints(hints);
		 * bufferedImageTranscoder.setImage(img);
		 * bufferedImageTranscoder.transcode(input_svg_image, null); img =
		 * resize(img,20,20);
		 */

		int counter = 1;
		stringHeight = (int) (fontMetrics.getAscent() * 1.2);
		ig2.setPaint(Color.black);
		for (String textLine : textLines) {
			ig2.drawString(textLine, TLX, TLY + stringHeight * counter++);
		}
	}

	private static void clearTextBox(Graphics2D ig2, BufferedImage bi,
			List<Point> TBPoints) {
		int TLX = (int) TBPoints.get(0).getX();
		int TLY = (int) TBPoints.get(0).getY();
		int BRX1 = (int) TBPoints.get(1).getX();
		int BRY1 = (int) TBPoints.get(1).getY();
		int BRX2 = (int) TBPoints.get(2).getX();
		int BRY2 = (int) TBPoints.get(2).getY();
		int colorRGB = bi.getRGB(TLX, TLY);
		ig2.setPaint(new Color(colorRGB));
		ig2.fillRect(TLX, TLY, BRX1 - TLX, BRY1 - TLY);
		ig2.fillRect(TLX, TLY, BRX2 - TLX, BRY2 - TLY);
	}
}
