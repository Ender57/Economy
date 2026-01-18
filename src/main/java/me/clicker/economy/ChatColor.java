package me.clicker.economy;

import com.hypixel.hytale.server.core.Message;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public enum ChatColor {
    BLACK('0', new Color(0, 0, 0), false),
    DARK_BLUE('1', new Color(0, 0, 170), false),
    DARK_GREEN('2', new Color(0, 170, 0), false),
    DARK_AQUA('3', new Color(0, 170, 170), false),
    DARK_RED('4', new Color(170, 0, 0), false),
    DARK_PURPLE('5', new Color(170, 0, 170), false),
    GOLD('6', new Color(255, 170, 0), false),
    GRAY('7', new Color(170, 170, 170), false),
    DARK_GRAY('8', new Color(85, 85, 85), false),
    BLUE('9', new Color(85, 85, 255), false),
    GREEN('a', new Color(85, 255, 85), false),
    AQUA('b', new Color(85, 255, 255), false),
    RED('c', new Color(255, 85, 85), false),
    LIGHT_PURPLE('d', new Color(255, 85, 255), false),
    YELLOW('e', new Color(255, 255, 85), false),
    WHITE('f', new Color(255, 255, 255), false),
    BOLD('l', null, true),
    ITALIC('o', null, true),
    RESET('r', null, true);

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FLOR]");
    private final static Map<Character, ChatColor> BY_CHAR = new HashMap<>();

    private final char code;
    private final Color color;
    private final boolean isFormat;

    ChatColor(char code, Color color, boolean isFormat) {
        this.code = code;
        this.color = color;
        this.isFormat = isFormat;
    }

    public char getChar() {
        return code;
    }

    public Color getColor() {
        return color;
    }

    public static ChatColor getByChar(char code) {
        return BY_CHAR.get(Character.toLowerCase(code));
    }

    public static String stripColor(final String input) {
        if (input == null) {
            return null;
        }

        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static Message toMessage(final String input) {
        if (input == null || input.isEmpty()) {
            return Message.raw("");
        }

        Message root = null;
        Color currentColor = null;
        var bold = false;
        var italic = false;
        var buffer = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            var c = input.charAt(i);

            if (c == '&' && i + 1 < input.length()) {
                var code = Character.toLowerCase(input.charAt(++i));
                var chatColor = getByChar(code);

                if (chatColor != null) {
                    if (!buffer.isEmpty()) {
                        var part = Message.raw(buffer.toString());

                        if (currentColor != null) {
                            part.color(currentColor);
                        }

                        part.bold(bold);
                        part.italic(italic);

                        if (root == null) {
                            root = part;
                        } else {
                            root.insert(part);
                        }

                        buffer.setLength(0);
                    }

                    if (chatColor.isFormat) {
                        switch (chatColor) {
                            case BOLD -> bold = true;
                            case ITALIC -> italic = true;
                            case RESET -> {
                                currentColor = null;
                                bold = false;
                                italic = false;
                            }
                        }
                    } else {
                        currentColor = chatColor.color;
                    }

                    continue;
                }
            }

            buffer.append(c);
        }

        if (!buffer.isEmpty()) {
            Message part = Message.raw(buffer.toString());

            if (currentColor != null) {
                part.color(currentColor);
            }

            part.bold(bold);
            part.italic(italic);

            if (root == null) {
                root = part;
            } else {
                root.insert(part);
            }
        }

        return root == null ? Message.raw("") : root;
    }

    static {
        for (ChatColor cc : values()) {
            BY_CHAR.put(cc.code, cc);
        }
    }
}