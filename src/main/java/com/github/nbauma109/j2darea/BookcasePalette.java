package com.github.nbauma109.j2darea;

import java.awt.Color;

/** Aged timber, backing, metal and book-cloth schemes for fitted bookcases. */
public enum BookcasePalette {

    AUTO("Automatic", null, null, null, null),
    DARK_OAK("Dark oak", c(57, 36, 23), c(27, 21, 17), c(130, 87, 36), books(76,31,28, 43,55,44, 95,66,29, 41,43,58)),
    WALNUT("Walnut", c(72, 45, 27), c(32, 24, 18), c(139, 98, 45), books(87,39,31, 52,62,45, 106,75,34, 51,45,61)),
    RED_MAHOGANY("Red mahogany", c(72, 32, 24), c(30, 19, 17), c(145, 96, 39), books(97,50,31, 40,60,50, 108,79,38, 46,42,63)),
    BLACKENED_WOOD("Blackened oak", c(43, 31, 23), c(22, 18, 15), c(112, 74, 34), books(77,34,31, 45,57,41, 98,68,31, 39,48,59)),
    HONEY_OAK("Honey oak", c(100, 62, 29), c(42, 29, 18), c(153, 105, 44), books(87,35,28, 44,62,46, 111,77,31, 46,45,64)),
    MOSSY_OAK("Old brown oak", c(74, 52, 31), c(31, 25, 18), c(129, 86, 36), books(80,37,30, 40,56,37, 101,72,32, 43,46,60)),
    ASH("Dusty brown oak", c(91, 69, 47), c(39, 31, 23), c(136, 93, 43), books(84,42,33, 49,62,46, 106,76,36, 49,50,65)),
    BURGUNDY_OAK("Reddish oak", c(65, 34, 29), c(29, 21, 18), c(139, 89, 39), books(102,54,34, 45,59,44, 113,82,41, 47,41,59)),
    GREEN_PAINT("Fumed oak", c(62, 43, 29), c(28, 22, 17), c(128, 86, 39), books(88,38,31, 48,57,34, 103,73,35, 41,47,61)),
    BLUE_PAINT("Bog oak", c(50, 35, 27), c(24, 20, 17), c(126, 84, 39), books(87,37,30, 45,59,44, 106,75,34, 56,43,59)),
    OLD_IVORY("Pale brown oak", c(116, 82, 43), c(45, 32, 20), c(141, 94, 39), books(93,38,29, 44,60,41, 104,72,31, 45,45,62)),
    RUSTIC_PINE("Rustic brown pine", c(105, 66, 33), c(42, 29, 18), c(142, 94, 37), books(94,40,30, 51,64,45, 110,78,37, 47,47,62)),
    PLUM_WOOD("Rosewood", c(66, 35, 30), c(28, 20, 18), c(132, 84, 36), books(91,44,31, 43,59,44, 105,73,34, 44,40,59)),
    SMOKED_CHESTNUT("Smoked chestnut", c(75, 49, 34), c(31, 24, 19), c(124, 80, 34), books(81,34,29, 44,55,42, 98,67,30, 41,42,56));

    private final String displayName;
    private final Color wood;
    private final Color backing;
    private final Color trim;
    private final Color[] bookColors;

    BookcasePalette(String displayName, Color wood, Color backing, Color trim, Color[] bookColors) {
        this.displayName = displayName;
        this.wood = wood;
        this.backing = backing;
        this.trim = trim;
        this.bookColors = bookColors;
    }

    public String getDisplayName() { return displayName; }
    Color getWood() { return wood; }
    Color getBacking() { return backing; }
    Color getTrim() { return trim; }
    Color getBookColor(int index) { return bookColors[Math.floorMod(index, bookColors.length)]; }
    int getBookColorCount() { return bookColors.length; }

    public static BookcasePalette fromSeed(long seed) {
        BookcasePalette[] palettes = values();
        int count = palettes.length - 1;
        int index = (int) (GroundNoise.hash(seed, 3253L, 9011L) * count) % count;
        return palettes[index + 1];
    }

    private static Color c(int red, int green, int blue) { return new Color(red, green, blue); }

    private static Color[] books(int... channels) {
        Color[] colors = new Color[channels.length / 3];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = c(channels[i * 3], channels[i * 3 + 1], channels[i * 3 + 2]);
        }
        return colors;
    }
}
