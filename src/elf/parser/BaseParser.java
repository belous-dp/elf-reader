package elf.parser;

public class BaseParser {
    private final static int END = -1;
    private final ByteSource source;
    private int cur;

    public BaseParser(final ByteSource source) {
        this.source = source;
        take();
    }

    protected int take() {
        final int res = cur;
        cur = source.hasNext() ? source.next() : END;
        return res;
    }

    protected int take(final int[] buf, final int len) {
        if (buf.length < len) {
            throw error("Destination buffer size is too small");
        }
        int i = 0;
        while (i < len && !eof()) {
            buf[i++] = take();
        }
        return i;
    }

    protected int[] takeLen(int len) {
        if (len < 0) {
            throw error("Number of symbols cannot be negative");
        }
        int[] res = new int[len];
        if (take(res, len) < len) {
            throw error("Not enough bytes for parsing");
        }
        return res;
    }

    protected int takeWord() {
        final int a = take();
        final int b = take();
        final int c = take();
        final int d = take();
        if (a < 0 || b < 0 || c < 0 || d < 0) {
            throw error("Expected byte, EOF found");
        }
        return a | (b << 0x8) | (c << 0x10) | (d << 0x18);
    }

    protected int takeHalf() {
        final int a = take();
        final int b = take();
        if (a < 0 || b < 0) {
            throw error("Expected byte, EOF found");
        }
        return (a | (b << 0x8)) & 0xffff;
    }

    protected boolean test(int expected) {
        return cur == expected;
    }

    protected boolean take(int expected) {
        if (test(expected)) {
            take();
            return true;
        }
        return false;
    }

    protected void setPointer(int position) {
        source.setPointer(position);
        take();
    }

    protected boolean eof() {
        return take(END);
    }

    protected IllegalArgumentException error(final String message) {
        return source.error(message);
    }
}
