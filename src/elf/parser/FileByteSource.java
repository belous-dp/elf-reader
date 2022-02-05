package elf.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FileByteSource implements ByteSource {
    private final int[] buf;
    private int pos;

    public FileByteSource(String source) throws IOException {
        try (final InputStream in = new FileInputStream(source)) {
            final int fileSize = (int) new File(source).length();
            if (fileSize == 0) {
                throw error("File is empty");
            }
            buf = new int[fileSize];
            final byte[] tmp = new byte[fileSize];
            in.read(tmp);
            for (int i = 0; i < fileSize; i++) {
                buf[i] = ((int) tmp[i]) & 0xff;
            }
            pos = 0;
        }
    }

    public FileByteSource(int[] source) {
        if (source.length == 0) {
            throw error("Data is empty");
        }
        buf = Arrays.copyOf(source, source.length);
        pos = 0;
    }

    @Override
    public boolean hasNext() {
        return pos < buf.length;
    }

    @Override
    public int next() {
        if (!hasNext()) {
            throw error("EOF reached but trying to read");
        }
        return buf[pos++];
    }

    @Override
    public void setPointer(int position) {
        if (position < 0 || position >= buf.length) {
            throw error("Illegal pointer position: " + position);
        }
        pos = position;
    }

    @Override
    public IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at pos: " + pos);
    }
}
