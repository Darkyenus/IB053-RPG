package ib053.frontend.cli;

/**
 *
 */
public class Ansi {

    public static final char ESC = 27;

    public static String cursorPosition(int line, int column) {
        return ESC+"["+line+";"+column+"H";
    }

    public static String cursorUp(int lines) {
        return ESC+"["+lines+"A";
    }

    public static String cursorDown(int lines) {
        return ESC+"["+lines+"B";
    }

    public static String cursorForward(int columns) {
        return ESC+"["+columns+"C";
    }

    public static String cursorBackward(int columns) {
        return ESC+"["+columns+"D";
    }

    public static final String SAVE_CURSOR = ESC+"[s";
    public static final String RESTORE_CURSOR = ESC+"[u";

    /** Erases screen and resets position to 0 0 */
    public static final String ERASE_DISPLAY = ESC+"[2J";
    /** Clears all characters from the cursor position to the
     end of the line (including the character at the cursor position). */
    public static final String ERASE_LINE = ESC+"[K";

    public enum TextAttribute {
        OFF(0),
        BOLD(1),
        UNDERSCORE(4),
        BLINK(5),
        REVERSE_VIDEO(7),
        CONCEALED(8),

        FG_BLACK(30),
        FG_RED(31),
        FG_GREEN(32),
        FG_YELLOW(33),
        FG_BLUE(34),
        FG_MAGENTA(35),
        FG_CYAN(36),
        FG_WHITE(37),

        BG_BLACK(40),
        BG_RED(41),
        BG_GREEN(42),
        BG_YELLOW(43),
        BG_BLUE(44),
        BG_MAGENTA(45),
        BG_CYAN(46),
        BG_WHITE(47);

        final int id;

        TextAttribute(int id) {
            this.id = id;
        }
    }

    public static CharSequence attribute(TextAttribute...attributes) {
        if (attributes.length == 0) return ESC+"[m";
        final StringBuilder sb = new StringBuilder(3 + attributes.length * 3);
        sb.append(ESC).append('[');
        for (int i = 0; i < attributes.length; i++) {
            if (i != 0) {
                sb.append(';');
            }
            sb.append(attributes[i].id);
        }
        sb.append('m');

        return sb;
    }

}
