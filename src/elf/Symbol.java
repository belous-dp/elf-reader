package elf;

public class Symbol {
    private int ste_name;
    private int ste_value;
    private int ste_size;
    private int ste_info;
    private int ste_other;
    private int ste_shndx;
    private String strName;

    public String getName() {
        return strName;
    }

    public void setName(String name) {
        this.strName = name;
    }

    public int getNameOffset() {
        return ste_name;
    }

    public void setNameOffset(int nameOffset) {
        if (nameOffset < 0) {
            throw new IllegalArgumentException("symbol table name cannot be negative");
        }
        this.ste_name = nameOffset;
    }

    public int getValue() {
        return ste_value;
    }

    public void setValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("symbol table address cannot be negative");
        }
        this.ste_value = value;
    }

    public int getSize() {
        return ste_size;
    }

    public void setSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("symbol table size cannot be negative");
        }
        this.ste_size = size;
    }

    public String getType() {
        return switch (ste_info & 0xf) {
            case 0x0 -> "NOTYPE";
            case 0x1 -> "OBJECT";
            case 0x2 -> "FUNC";
            case 0x3 -> "SECTION";
            case 0x4 -> "FILE";
            case 0x5 -> "COMMON";
            case 0x6 -> "TLS";
            case 0xa -> "LOOS";
            case 0xc -> "HIOS";
            case 0xd -> "LOPROC";
            case 0xf -> "HIPROC";
            default -> throw new AssertionError("unknown symbol type");
        };
    }

    public String getBinding() {
        return switch (ste_info >> 4) {
            case 0x0 -> "LOCAL";
            case 0x1 -> "GLOBAL";
            case 0x2 -> "WEAK";
            case 0xa -> "LOOS";
            case 0xc -> "HIOS";
            case 0xd -> "LOPROC";
            case 0xf -> "HIPROC";
            default -> throw new AssertionError("unknown symbol binding");
        };
    }

    public String getVisibility() {
        return switch (ste_other & 0x3) {
            case 0x0 -> "DEFAULT";
            case 0x1 -> "INTERNAL";
            case 0x2 -> "HIDDEN";
            case 0x3 -> "PROTECTED";
            case 0x4 -> "EXPORTED";
            case 0x5 -> "SINGLETON";
            case 0x6 -> "ELIMINATE";
            default -> throw new AssertionError("unknown symbol visibility");
        };
    }

    public void setInformation(int information) {
        if (information < 0) {
            throw new IllegalArgumentException("symbol information cannot be negative");
        }
        this.ste_info = information;
    }

    public void setOther(int other) {
        if (other < 0) {
            throw new IllegalArgumentException("symbol other information cannot be negative");
        }
        this.ste_other = other;
    }

    public String getSectionHeaderIndex() {
        return switch (ste_shndx) {
            case 0 -> "UND";
            case 0xff20 -> "LOOS";
            case 0xff3f -> "HIOS";
            case 0xfff1 -> "ABS";
            case 0xfff2 -> "COMMON";
            default -> String.valueOf(ste_shndx);
        };
    }

    public void setSectionHeaderIndex(int sectionHeaderIndex) {
        if (sectionHeaderIndex < 0) {
            throw new IllegalArgumentException("symbol table header index cannot be negative");
        }
        this.ste_shndx = sectionHeaderIndex;
    }
}
