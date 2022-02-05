package elf;

public class StringTable {
    private final int[] data;

    public StringTable(int[] data) {
        this.data = data;
    }

    public String getString(int offset) {
        if (offset < 0 || offset > data.length) {
            throw new IllegalArgumentException("invalid bounds for section header name table");
        }
        StringBuilder res = new StringBuilder();
        while (offset < data.length && data[offset] != 0) {
            res.append((char) data[offset++]);
        }
        return res.toString();
    }
}
