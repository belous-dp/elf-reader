package elf.parser;

public interface ByteSource {
    boolean hasNext();

    int next();

    void setPointer(int position);

    IllegalArgumentException error(String message);
}
