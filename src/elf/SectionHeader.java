package elf;

public class SectionHeader {

    private int sh_name;
    private int sh_type;
    private int sh_address;
    private int sh_offset;
    private int sh_size;
    private int sh_link;
    private String strName;

    public int getAddress() {
        return sh_address;
    }

    public void setAddress(int address) {
        this.sh_address = address;
    }

    public int getNameOffset() {
        return sh_name;
    }

    public void setNameOffset(int nameOffset) {
        if (nameOffset < 0) {
            throw new IllegalArgumentException("Section header name cannot be negative");
        }
        this.sh_name = nameOffset;
    }

    public String getType() {
        return switch (sh_type) {
            case 0x0 -> "NULL"; //"Section header table entry unused";
            case 0x1 -> "PROGBITS"; //"Program data";
            case 0x2 -> "SYMTAB"; //"Symbol table";
            case 0x3 -> "STRTAB"; //"String table";
            case 0x4 -> "RELA";
            case 0x5 -> "HASH";
            case 0x6 -> "DYNAMIC";
            case 0x7 -> "NOTE";
            case 0x8 -> "NOBITS";
            case 0x9 -> "REL";
            case 0xa -> "SHLIB";
            case 0xb -> "DYNSYM";
            case 0xe -> "INIT_ARRAY";
            case 0xf -> "FINI_ARRAY";
            case 0x10 -> "PREINIT_ARRAY";
            case 0x11 -> "GROUP";
            case 0x12 -> "SYMTAB_SHNDX";
            default -> "UNKNOWN"; //"Unknown type";
        };
    }

    public void setType(int type) {
        if (type < 0) {
            throw new IllegalArgumentException("Section header type cannot be negative");
        }
        this.sh_type = type;
    }

    public int getOffset() {
        return sh_offset;
    }

    public void setOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Section header offset cannot be negative");
        }
        this.sh_offset = offset;
    }

    public int getSize() {
        return sh_size;
    }

    public void setSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Section header size cannot be negative");
        }
        this.sh_size = size;
    }

    public int getLink() {
        return sh_link;
    }

    public void setLink(int link) {
        if (link < 0) {
            throw new IllegalArgumentException("Link to the section cannot be negative");
        }
        this.sh_link = link;
    }

    public String getName() {
        return strName;
    }

    public void setName(String name) {
        this.strName = name;
    }
}
