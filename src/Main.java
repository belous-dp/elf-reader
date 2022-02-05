import elf.reader.Reader;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            final Reader reader = new Reader(args);
            reader.read();
            reader.write();
        } catch (IOException e) {
            System.out.println("Input error occurred: " + e.getMessage());
        }
    }
}
