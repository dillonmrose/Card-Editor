import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;

public class CardEditorDriver {
	/*
	 * private static class BufferedImageTranscoder extends ImageTranscoder {
	 * private BufferedImage image = null;
	 * 
	 * @Override public BufferedImage createImage(int arg0, int arg1) {
	 * 
	 * return image; }
	 * 
	 * private void setImage(BufferedImage image) { this.image = image; }
	 * 
	 * @Override public void writeImage(BufferedImage arg0, TranscoderOutput
	 * arg1) throws TranscoderException { } }
	 */

	static String currentDirectory;
	final static SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
			XMLResourceDescriptor.getXMLParserClassName());
	static HashMap<String, String> expansion2ExpansionCode = new HashMap<String, String>();
	static HashMap<String, HashMap<String, List<Point>>> expansionCode2TBPoints = new HashMap<String, HashMap<String, List<Point>>>();
	// final static BufferedImageTranscoder bufferedImageTranscoder = new
	// BufferedImageTranscoder();
	final static int finalWidth = 312;
	final static int finalHeight = 445;
	final static String imageType = "png";

	// set to true if you dont have original images
	private static boolean downloadOriginalImages = false;

	public static void main(String[] args) throws Exception {
		init();

		String data_file = currentDirectory + "/public/CardList.Boozed.txt";

		Scanner sc = new Scanner(new FileReader(data_file));

		sc.nextLine();
		int counter = 0;

		while (sc.hasNext()) {
			String line = sc.nextLine();
			Card card = new Card(line);
			System.out.println(card.name);

			String originalImageFile = currentDirectory + "/cards/original/"
					+ String.format("%03d", counter) + ".jpg";
			String boozeImageFile = currentDirectory + "/cards/booze/"
					+ String.format("%03d", counter) + "." + imageType;
			String expansion_code = expansion2ExpansionCode.get(card.expansion);
			String type = card.type;
			if (type.contains("Creature")) {
				type = "Creature";
			} else {
				type = "NonCreature";
			}

			if (CardEditorDriver.downloadOriginalImages) {
				String image_url = "http://magiccards.info/scans/en/"
						+ expansion_code + "/" + card.number + ".jpg";
				System.out.println("downloading image " + image_url);
				downloadBaseImage(image_url, originalImageFile);
			}

			boozifyImage(originalImageFile, boozeImageFile, card.boozeText,
					expansion_code, type);

			counter++;
			break;
		}
		sc.close();

		System.out.println("Create webpage!");
		FileWriter fw = new FileWriter(currentDirectory + "/cards/booze.html");
		fw.write("<!DOCTYPE html><html lang=\"en\"><body>");

		for (File cardImageFile : new File(currentDirectory + "/cards/booze/")
				.listFiles()) {
			if (!cardImageFile.getName().endsWith(imageType))
				continue;
			fw.write("<img src=\"" + cardImageFile
					+ "\" width=\"218\" height=\"314\"\\>");

		}
		fw.write("<\\body><\\html>");
		fw.close();

	}

	private static void init() throws Exception {
		currentDirectory = System.getProperty("user.dir");
		String expansion2ExpansionCodeFile = currentDirectory
				+ "/public/Expansion Code Map.txt";
		String expansionCode2TBPointsFile = currentDirectory
				+ "/public/Textbox Data.txt";
		expansion2ExpansionCode = getExpansion2expansionCodeMap(expansion2ExpansionCodeFile);
		expansionCode2TBPoints = getExpansionCode2TBPointsMap(expansionCode2TBPointsFile);

		File cardsDir = new File(currentDirectory + "/cards");
		if (!cardsDir.exists()) {
			cardsDir.mkdir();
		}
		File originalCardsDir = new File(currentDirectory + "/cards/original");
		if (!originalCardsDir.exists()) {
			originalCardsDir.mkdir();
		}
		File boozeCardsDir = new File(currentDirectory + "/cards/booze");
		if (!boozeCardsDir.exists()) {
			boozeCardsDir.mkdir();
		}
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
			TBPoints.add(new Point(Double.parseDouble(lineSplit[2]), Double
					.parseDouble(lineSplit[3])));
			TBPoints.add(new Point(Double.parseDouble(lineSplit[4]), Double
					.parseDouble(lineSplit[5])));
			TBPoints.add(new Point(Double.parseDouble(lineSplit[6]), Double
					.parseDouble(lineSplit[7])));
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

			clearTextBox(bi, ig2, TBPoints);
			writeTextBox(bi, ig2, boozeText, TBPoints);
		}
		ImageIO.write(bi, imageType, new File(boozeImageFile));
	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
		Graphics2D g = dimg.createGraphics();

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g.drawImage(img, 0, 0, newW, newH, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		return dimg;
	}

	private static void writeTextBox(BufferedImage bi, Graphics2D ig2,
			String text, List<Point> TBPoints) throws Exception {

		int TLX = (int) (TBPoints.get(0).getX() * bi.getWidth());
		int TLY = (int) (TBPoints.get(0).getY() * bi.getHeight());
		int BRX1 = (int) (TBPoints.get(1).getX() * bi.getWidth());
		int BRY1 = (int) (TBPoints.get(1).getY() * bi.getHeight());

		float fontSize = 16;
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

	private static void clearTextBox(BufferedImage bi, Graphics2D ig2,
			List<Point> TBPoints) {
		int TLX = (int) (TBPoints.get(0).getX() * bi.getWidth());
		int TLY = (int) (TBPoints.get(0).getY() * bi.getHeight());
		int BRX1 = (int) (TBPoints.get(1).getX() * bi.getWidth());
		int BRY1 = (int) (TBPoints.get(1).getY() * bi.getHeight());
		int BRX2 = (int) (TBPoints.get(2).getX() * bi.getWidth());
		int BRY2 = (int) (TBPoints.get(2).getY() * bi.getHeight());
		int colorRGB = bi.getRGB(TLX, TLY);
		ig2.setPaint(new Color(colorRGB));
		ig2.fillRect(TLX, TLY, BRX1 - TLX, BRY1 - TLY);
		ig2.fillRect(TLX, TLY, BRX2 - TLX, BRY2 - TLY);
	}
}
