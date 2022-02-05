package elf;

import java.util.Arrays;

public class ELFHeader {

    private int[] ei_mag;
    private int ei_class;
    private int ei_data;
    private int e_type;
    private int e_machine;
    private int e_shoff;
    private int e_flags;
    private int e_shentsize;
    private int e_shnum;
    private int e_shstrndx;

    public String getMagic() {
        if (ei_mag[0] == 0x7f && ei_mag[1] == 0x45 && ei_mag[2] == 0x4c && ei_mag[3] == 0x46) {
            return "7f 45 4c 46";
        } else {
            return "Not an ELF file" + Arrays.toString(ei_mag);
        }
    }

    public String getFileClass() {
        return switch (ei_class) {
            case 0 -> "Incorrect format"; //ELFCLASSNONE
            case 1 -> "ELF32"; //ELFCLASS32
            case 2 -> "ELF64"; //ELFCLASS64
            default -> throw new AssertionError("Unknown elf format");
        };
    }

    public String getData() {
        return switch (ei_data) {
            case 0 -> "Incorrect endianness"; //ELFDATANONE
            case 1 -> "Little endian"; //ELFDATA2LSB
            case 2 -> "Big endian"; //ELFDATA2MSB
            default -> throw new AssertionError("Unknown endianness");
        };
    }

    public String getType() {
        return switch (e_type) {
            case 0 -> "Incorrect type"; //ET_NONE
            case 1 -> "Relocatable"; //ET_REL
            case 2 -> "Executable"; //ET_EXEC
            case 3 -> "Shared"; //ET_DYN
            case 4 -> "Core"; //ET_CORE
            default -> throw new AssertionError("Unknown type");
        };
    }

    public String getMachine() {
        return switch (e_machine) {
            case 0x00 -> "No specific ISA";
            case 0x03 -> "x86";
            case 0x28 -> "ARM";
            case 0x3e -> "AMD x86-64";
            case 0xf3 -> "RISC-V";
            default -> throw new AssertionError("Unknown ISA");
        };
    }

    public int getSHOffset() {
        return e_shoff;
    }

    public String getFlags() {
        return switch (e_flags) {
            case 0x1 -> "0x1, RVC";
            default -> String.format("0x%x", e_flags);
        };
    }

    public int getSHSize() {
        return e_shentsize;
    }

    public int getSHNumber() {
        return e_shnum;
    }

    public int getSHStringTableIndex() {
        return e_shstrndx;
    }

    public void setMagic(int[] magic) {
        if (magic[0] == 0x7f && magic[1] == 0x45 && magic[2] == 0x4c && magic[3] == 0x46) {
            this.ei_mag = magic;
        } else {
            throw new IllegalArgumentException("Not an ELF file");
        }
    }

    public void setClass(int ei_class) {
        switch (ei_class) {
            case 0 -> throw new IllegalArgumentException("Incorrect elf format");
            case 1 -> this.ei_class = ei_class;
            default -> throw new IllegalArgumentException("Unsupported elf format");
        }
    }

    public void setData(int data) {
        switch (data) {
            case 0 -> throw new IllegalArgumentException("Incorrect endianness");
            case 1 -> this.ei_data = data;
            default -> throw new IllegalArgumentException("Unsupported endianness");
        }
    }

    public void setType(int type) {
        switch (type) {
            case 0 -> throw new IllegalArgumentException("Incorrect type");
            case 2 -> e_type = type;
            default -> throw new IllegalArgumentException("Unsupported type");
        }
    }

    public void setMachine(int machine) {
        switch (machine) {
            case 0xf3 -> this.e_machine = machine; //RISC-V
            default -> throw new IllegalArgumentException("Unsupported ISA");
        }
    }

    public void setSHoff(int sectionHeadersOffset) {
        if (sectionHeadersOffset < 0) {
            throw new IllegalArgumentException("Incorrect section headers offset");
        }
        this.e_shoff = sectionHeadersOffset;
    }

    public void setFlags(int flags) {
        if (flags < 0) {
            throw new IllegalArgumentException("Incorrect flags");
        }
        this.e_flags = flags;
    }

    public void setSHsize(int sectionHeadersSize) {
        if (sectionHeadersSize < 0) {
            throw new IllegalArgumentException("Incorrect section headers size");
        }
        this.e_shentsize = sectionHeadersSize;
    }

    public void setSHnum(int sectionHeadersNumber) {
        if (sectionHeadersNumber < 0) {
            throw new IllegalArgumentException("Incorrect section headers number");
        }
        this.e_shnum = sectionHeadersNumber;
    }

    public void setSHstrIndex(int sectionHeadersStringTableIndex) {
        if (sectionHeadersStringTableIndex < 0) {
            throw new IllegalArgumentException("Incorrect index of section names");
        }
        this.e_shstrndx = sectionHeadersStringTableIndex;
    }
}
