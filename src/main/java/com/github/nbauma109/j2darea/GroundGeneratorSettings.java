package com.github.nbauma109.j2darea;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parameters of the randomized ground generator.
 *
 * <p>The settings fully determine the generated ground, so a project only has to
 * store these values instead of a full background bitmap: reopening the project
 * regenerates exactly the same image.
 */
public class GroundGeneratorSettings implements Externalizable {

    public static final int MIN_PATCH_SIZE = 40;
    public static final int MAX_PATCH_SIZE = 1600;

    private long seed;
    private int patchSize;
    private double edgeIrregularity;
    private double edgeSoftness;
    private final Map<GroundMaterial, Double> coverage = new EnumMap<>(GroundMaterial.class);
    private double grassToneVariation;
    private double grassDryness;
    private double brightness;
    private double detailAmount;
    private double flowerDensity;
    private double pebbleDensity;

    public GroundGeneratorSettings() {
        seed = new Random().nextLong();
        patchSize = 620;
        edgeIrregularity = 0.45d;
        edgeSoftness = 0.5d;
        coverage.put(GroundMaterial.SAND, 0d);
        coverage.put(GroundMaterial.EARTH, 0.13d);
        coverage.put(GroundMaterial.STONE, 0.02d);
        grassToneVariation = 0.55d;
        grassDryness = 0d;
        brightness = 1d;
        detailAmount = 0.55d;
        flowerDensity = 0.45d;
        pebbleDensity = 0.4d;
    }

    public GroundGeneratorSettings(GroundGeneratorSettings source) {
        this();
        if (source != null) {
            copyFrom(source);
        }
    }

    public void copyFrom(GroundGeneratorSettings source) {
        if (source == null) {
            return;
        }
        seed = source.seed;
        patchSize = source.patchSize;
        edgeIrregularity = source.edgeIrregularity;
        edgeSoftness = source.edgeSoftness;
        coverage.clear();
        coverage.putAll(source.coverage);
        grassToneVariation = source.grassToneVariation;
        grassDryness = source.grassDryness;
        brightness = source.brightness;
        detailAmount = source.detailAmount;
        flowerDensity = source.flowerDensity;
        pebbleDensity = source.pebbleDensity;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public void randomizeSeed() {
        seed = new Random().nextLong();
    }

    public int getPatchSize() {
        return patchSize;
    }

    public void setPatchSize(int patchSize) {
        this.patchSize = Math.max(MIN_PATCH_SIZE, Math.min(MAX_PATCH_SIZE, patchSize));
    }

    public double getEdgeIrregularity() {
        return edgeIrregularity;
    }

    public void setEdgeIrregularity(double edgeIrregularity) {
        this.edgeIrregularity = clampUnit(edgeIrregularity);
    }

    public double getEdgeSoftness() {
        return edgeSoftness;
    }

    public void setEdgeSoftness(double edgeSoftness) {
        this.edgeSoftness = clampUnit(edgeSoftness);
    }

    public double getCoverage(GroundMaterial material) {
        Double value = coverage.get(material);
        return value != null ? value.doubleValue() : 0d;
    }

    public void setCoverage(GroundMaterial material, double value) {
        if (material == null || material == GroundMaterial.GRASS) {
            return;
        }
        coverage.put(material, Double.valueOf(clampUnit(value)));
    }

    public double getGrassToneVariation() {
        return grassToneVariation;
    }

    public void setGrassToneVariation(double grassToneVariation) {
        this.grassToneVariation = clampUnit(grassToneVariation);
    }

    /** Dryness in {@code [-1, 1]}: negative is a cold blue-green, positive a dry yellow-green. */
    public double getGrassDryness() {
        return grassDryness;
    }

    public void setGrassDryness(double grassDryness) {
        this.grassDryness = Math.max(-1d, Math.min(1d, grassDryness));
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }

    public double getDetailAmount() {
        return detailAmount;
    }

    public void setDetailAmount(double detailAmount) {
        this.detailAmount = clampUnit(detailAmount);
    }

    public double getFlowerDensity() {
        return flowerDensity;
    }

    public void setFlowerDensity(double flowerDensity) {
        this.flowerDensity = clampUnit(flowerDensity);
    }

    /** Density of the loose stones scattered over bare and rocky ground. */
    public double getPebbleDensity() {
        return pebbleDensity;
    }

    public void setPebbleDensity(double pebbleDensity) {
        this.pebbleDensity = clampUnit(pebbleDensity);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(seed);
        out.writeInt(patchSize);
        out.writeDouble(edgeIrregularity);
        out.writeDouble(edgeSoftness);
        GroundMaterial[] patchMaterials = GroundMaterial.patchMaterials();
        out.writeInt(patchMaterials.length);
        for (GroundMaterial material : patchMaterials) {
            out.writeUTF(material.name());
            out.writeDouble(getCoverage(material));
        }
        out.writeDouble(grassToneVariation);
        out.writeDouble(grassDryness);
        out.writeDouble(brightness);
        out.writeDouble(detailAmount);
        out.writeDouble(flowerDensity);
        out.writeDouble(pebbleDensity);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        seed = in.readLong();
        setPatchSize(in.readInt());
        setEdgeIrregularity(in.readDouble());
        setEdgeSoftness(in.readDouble());
        int materialCount = in.readInt();
        for (int i = 0; i < materialCount; i++) {
            String name = in.readUTF();
            double value = in.readDouble();
            setCoverage(parseMaterial(name), value);
        }
        setGrassToneVariation(in.readDouble());
        setGrassDryness(in.readDouble());
        setBrightness(in.readDouble());
        setDetailAmount(in.readDouble());
        setFlowerDensity(in.readDouble());
        setPebbleDensity(in.readDouble());
    }

    public Element toXml(Document doc, String tag) {
        Element el = doc.createElement(tag);
        XmlIO.addText(doc, el, "seed", String.valueOf(seed));
        XmlIO.addInt(doc, el, "patchSize", patchSize);
        XmlIO.addText(doc, el, "edgeIrregularity", String.valueOf(edgeIrregularity));
        XmlIO.addText(doc, el, "edgeSoftness", String.valueOf(edgeSoftness));
        Element coverageEl = XmlIO.addElement(doc, el, "coverage");
        for (GroundMaterial material : GroundMaterial.patchMaterials()) {
            Element materialEl = XmlIO.addElement(doc, coverageEl, "material");
            materialEl.setAttribute("name", material.name());
            materialEl.setTextContent(String.valueOf(getCoverage(material)));
        }
        XmlIO.addText(doc, el, "grassToneVariation", String.valueOf(grassToneVariation));
        XmlIO.addText(doc, el, "grassDryness", String.valueOf(grassDryness));
        XmlIO.addText(doc, el, "brightness", String.valueOf(brightness));
        XmlIO.addText(doc, el, "detailAmount", String.valueOf(detailAmount));
        XmlIO.addText(doc, el, "flowerDensity", String.valueOf(flowerDensity));
        XmlIO.addText(doc, el, "pebbleDensity", String.valueOf(pebbleDensity));
        return el;
    }

    public void fromXml(Element el) {
        if (el == null) {
            return;
        }
        seed = readLong(el, "seed", seed);
        setPatchSize(XmlIO.readInt(el, "patchSize", patchSize));
        setEdgeIrregularity(readDouble(el, "edgeIrregularity", edgeIrregularity));
        setEdgeSoftness(readDouble(el, "edgeSoftness", edgeSoftness));
        org.w3c.dom.NodeList materialNodes = XmlIO.getChildElements(el, "coverage/material");
        if (materialNodes != null) {
            for (int i = 0; i < materialNodes.getLength(); i++) {
                Element materialEl = (Element) materialNodes.item(i);
                GroundMaterial material = parseMaterial(materialEl.getAttribute("name"));
                if (material != null) {
                    setCoverage(material, parseDouble(materialEl.getTextContent(), getCoverage(material)));
                }
            }
        }
        setGrassToneVariation(readDouble(el, "grassToneVariation", grassToneVariation));
        setGrassDryness(readDouble(el, "grassDryness", grassDryness));
        setBrightness(readDouble(el, "brightness", brightness));
        setDetailAmount(readDouble(el, "detailAmount", detailAmount));
        setFlowerDensity(readDouble(el, "flowerDensity", flowerDensity));
        setPebbleDensity(readDouble(el, "pebbleDensity", pebbleDensity));
    }

    private static GroundMaterial parseMaterial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return GroundMaterial.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static long readLong(Element el, String tag, long defaultValue) {
        String text = XmlIO.readText(el, tag, null);
        if (text == null || text.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double readDouble(Element el, String tag, double defaultValue) {
        return parseDouble(XmlIO.readText(el, tag, null), defaultValue);
    }

    private static double parseDouble(String text, double defaultValue) {
        if (text == null || text.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double clampUnit(double value) {
        if (value < 0d) {
            return 0d;
        }
        return value > 1d ? 1d : value;
    }
}
