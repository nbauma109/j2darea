package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility methods for XML-based serialization of j2darea model objects.
 * Reading uses XPath so that missing elements silently return defaults —
 * this makes the format forward/backward compatible across model changes.
 */
public final class XmlIO {

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private XmlIO() {}

    // -----------------------------------------------------------------------
    // Document-level helpers
    // -----------------------------------------------------------------------

    public static Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().newDocument();
    }

    public static Document parseDocument(byte[] xmlBytes)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlBytes));
    }

    public static byte[] documentToBytes(Document doc) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Write helpers
    // -----------------------------------------------------------------------

    public static Element addElement(Document doc, Element parent, String tag) {
        Element child = doc.createElement(tag);
        parent.appendChild(child);
        return child;
    }

    public static void addText(Document doc, Element parent, String tag, String value) {
        Element child = doc.createElement(tag);
        child.setTextContent(value != null ? value : "");
        parent.appendChild(child);
    }

    public static void addInt(Document doc, Element parent, String tag, int value) {
        addText(doc, parent, tag, String.valueOf(value));
    }

    public static void addBoolean(Document doc, Element parent, String tag, boolean value) {
        addText(doc, parent, tag, String.valueOf(value));
    }

    public static void writePolygon(Document doc, Element parent, String tag, Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            addText(doc, parent, tag, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < polygon.npoints; i++) {
            if (i > 0) sb.append(' ');
            sb.append(polygon.xpoints[i]).append(',').append(polygon.ypoints[i]);
        }
        addText(doc, parent, tag, sb.toString());
    }

    public static void writePoint(Document doc, Element parent, String tag, Point point) {
        Point p = point != null ? point : new Point();
        addText(doc, parent, tag, p.x + "," + p.y);
    }

    public static void writePointList(Document doc, Element parent, String tag, List<Point> points) {
        if (points == null || points.isEmpty()) {
            addText(doc, parent, tag, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Point p : points) {
            if (p == null) continue;
            if (!first) sb.append(' ');
            sb.append(p.x).append(',').append(p.y);
            first = false;
        }
        addText(doc, parent, tag, sb.toString());
    }

    public static void writeImage(Document doc, Element parent, String tag, BufferedImage image)
            throws IOException {
        Element imgEl = doc.createElement(tag);
        if (image != null) {
            imgEl.setAttribute("type", String.valueOf(image.getType()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            imgEl.setTextContent(Base64.getEncoder().encodeToString(baos.toByteArray()));
        }
        parent.appendChild(imgEl);
    }

    public static void writeBase64Bytes(Document doc, Element parent, String tag, byte[] data) {
        addText(doc, parent, tag,
            (data != null && data.length > 0) ? Base64.getEncoder().encodeToString(data) : "");
    }

    // -----------------------------------------------------------------------
    // Read helpers (XPath-based — returns defaults for missing elements)
    // -----------------------------------------------------------------------

    public static String readText(Element el, String childTag, String defaultValue) {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            String result = (String) xpath.evaluate(childTag + "/text()", el, XPathConstants.STRING);
            return (result != null && !result.isEmpty()) ? result : defaultValue;
        } catch (XPathExpressionException e) {
            return defaultValue;
        }
    }

    public static int readInt(Element el, String childTag, int defaultValue) {
        String text = readText(el, childTag, null);
        if (text == null || text.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean readBoolean(Element el, String childTag, boolean defaultValue) {
        String text = readText(el, childTag, null);
        if (text == null || text.isEmpty()) return defaultValue;
        return Boolean.parseBoolean(text.trim());
    }

    public static Polygon readPolygon(Element el, String tag) {
        String text = readText(el, tag, "").trim();
        if (text.isEmpty()) return new Polygon();
        String[] pairs = text.split("\\s+");
        int[] xpoints = new int[pairs.length];
        int[] ypoints = new int[pairs.length];
        int count = 0;
        for (String pair : pairs) {
            String[] xy = pair.split(",");
            if (xy.length >= 2) {
                try {
                    xpoints[count] = Integer.parseInt(xy[0].trim());
                    ypoints[count] = Integer.parseInt(xy[1].trim());
                    count++;
                } catch (NumberFormatException ignored) {}
            }
        }
        return new Polygon(xpoints, ypoints, count);
    }

    public static Point readPoint(Element el, String tag) {
        String text = readText(el, tag, "0,0").trim();
        String[] xy = text.split(",");
        if (xy.length >= 2) {
            try {
                return new Point(Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim()));
            } catch (NumberFormatException ignored) {}
        }
        return new Point();
    }

    public static List<Point> readPointList(Element el, String tag) {
        String text = readText(el, tag, "").trim();
        List<Point> points = new ArrayList<>();
        if (text.isEmpty()) return points;
        for (String pair : text.split("\\s+")) {
            String[] xy = pair.split(",");
            if (xy.length >= 2) {
                try {
                    points.add(new Point(Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim())));
                } catch (NumberFormatException ignored) {}
            }
        }
        return points;
    }

    public static BufferedImage readImage(Element el, String tag) throws IOException {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(tag, el, XPathConstants.NODESET);
            if (nodes.getLength() == 0) return null;
            Element imgEl = (Element) nodes.item(0);
            String b64 = imgEl.getTextContent();
            if (b64 == null || b64.trim().isEmpty()) return null;
            byte[] pngBytes = Base64.getDecoder().decode(b64.trim());
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (decoded == null) return null;
            String typeStr = imgEl.getAttribute("type");
            if (typeStr != null && !typeStr.isEmpty()) {
                try {
                    int requestedType = Integer.parseInt(typeStr);
                    if (requestedType > 0 && requestedType != decoded.getType()) {
                        BufferedImage converted = new BufferedImage(
                            decoded.getWidth(), decoded.getHeight(), requestedType);
                        converted.getGraphics().drawImage(decoded, 0, 0, null);
                        return converted;
                    }
                } catch (NumberFormatException ignored) {}
            }
            return decoded;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public static byte[] readBase64Bytes(Element el, String tag) {
        String text = readText(el, tag, "").trim();
        if (text.isEmpty()) return new byte[0];
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    public static Element getChildElement(Element el, String tag) {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(tag, el, XPathConstants.NODESET);
            return (nodes.getLength() > 0) ? (Element) nodes.item(0) : null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public static NodeList getChildElements(Element el, String path) {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            return (NodeList) xpath.evaluate(path, el, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
