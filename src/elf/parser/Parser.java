package elf.parser;

import elf.ELFHeader;
import elf.SectionHeader;
import elf.StringTable;
import elf.Symbol;
import riscv.Disassembler;
import riscv.RV32.Instruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser extends BaseParser {
    public Parser(String source) throws IOException {
        this(new FileByteSource(source));
    }

    public Parser(FileByteSource source) {
        super(source);
    }

    public ELFHeader parseELFHeader() {
        ELFHeader header = new ELFHeader();
        header.setMagic(takeLen(4));
        header.setClass(take());
        header.setData(take());
        setPointer(0x10);
        header.setType(takeHalf());
        header.setMachine(takeHalf());
        setPointer(0x20);
        header.setSHoff(takeWord());
        header.setFlags(takeWord());
        setPointer(0x2e);
        header.setSHsize(takeHalf());
        header.setSHnum(takeHalf());
        header.setSHstrIndex(takeHalf());
        return header;
    }

    public List<SectionHeader> parseSectionHeaders(int offset, int number, int shstrtabid) {
        List<SectionHeader> res = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            SectionHeader sh = new SectionHeader();
            setPointer(offset + 0x28 * i);
            sh.setNameOffset(takeWord());
            sh.setType(takeWord());
            setPointer(offset + 0x28 * i + 0xc);
            sh.setAddress(takeWord());
            sh.setOffset(takeWord());
            sh.setSize(takeWord());
            sh.setLink(takeWord());
            res.add(sh);
        }
        if (shstrtabid < 0 || shstrtabid >= number) {
            throw error("Section header name table index is out of bounds");
        }
        setPointer(res.get(shstrtabid).getOffset());
        StringTable shstrtab = new StringTable(takeLen(res.get(shstrtabid).getSize()));
        for (SectionHeader sh : res) {
            sh.setName(shstrtab.getString(sh.getNameOffset()));
        }
        return res;
    }

    public List<Symbol> parseSymbolTable(SectionHeader symbolTableHeader, SectionHeader symbolNamesTableHeader) {
        final int numberOfEntries = symbolTableHeader.getSize() / 16;
        final List<Symbol> res = new ArrayList<>();
        setPointer(symbolNamesTableHeader.getOffset());
        final StringTable symbolNames = new StringTable(takeLen(symbolNamesTableHeader.getSize()));
        setPointer(symbolTableHeader.getOffset());
        for (int i = 0; i < numberOfEntries; i++) {
            final Symbol symbol = new Symbol();
            symbol.setNameOffset(takeWord());
            symbol.setName(symbolNames.getString(symbol.getNameOffset()));
            symbol.setValue(takeWord());
            symbol.setSize(takeWord());
            symbol.setInformation(take());
            symbol.setOther(take());
            symbol.setSectionHeaderIndex(takeHalf());
            res.add(symbol);
        }
        return res;
    }

    public List<Instruction> parseText(SectionHeader textHeader, boolean quiet) {
        setPointer(textHeader.getOffset());
        Disassembler disassembler = new Disassembler(takeLen(textHeader.getSize()), quiet);
        return disassembler.disassemble(textHeader.getAddress());
    }

}
