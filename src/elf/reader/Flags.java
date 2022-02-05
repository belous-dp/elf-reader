package elf.reader;

public class Flags {
    public boolean all;
    public boolean inline;
    public boolean header;
    public boolean sectionHeaders;
    public boolean symbolTable;
    public boolean text;
    public boolean quiet;
    public boolean specialForVictoria;

    public Flags() {
        this(false, false, false, false, false, false, false, false);
    }

    public Flags(boolean all, boolean inline, boolean header, boolean sectionHeaders, boolean symbolTable,
                 boolean text, boolean quiet, boolean specialForVictoria) {
        this.all = all;
        this.inline = inline;
        this.header = header;
        this.sectionHeaders = sectionHeaders;
        this.symbolTable = symbolTable;
        this.text = text;
        this.quiet = quiet;
        this.specialForVictoria = specialForVictoria;
    }
}
