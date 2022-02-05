package elf.reader;

import elf.ELFHeader;
import elf.SectionHeader;
import elf.Symbol;
import elf.parser.Parser;
import riscv.RV32.Commands;
import riscv.RV32.Instruction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.exit;

public class Reader {
    private final Parser parser;
    private final String readFrom;
    private final String writeTo;
    private Flags flags;
    private ELFHeader header;
    private List<SectionHeader> sectionHeaders;
    private List<Symbol> symbolTable;
    private List<Instruction> text;
    private int textAddress;
    private boolean symtabOk;
    private boolean textOk;


    public Reader(String[] args) throws IOException {
        if (args.length == 0) {
            printHelp();
        }
        final int offset = parseFlags(args);
        parser = new Parser(args[offset]);
        readFrom = args[offset];
        if (!flags.inline) {
            if (offset + 1 >= args.length) {
                throw new IllegalArgumentException("Expected output file name");
            }
            writeTo = args[offset + 1];
        } else {
            writeTo = null;
        }
        symtabOk = textOk = true;
    }

    private int parseFlags(String[] args) {
        flags = new Flags();
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            switch (args[i]) {
                case "-h", "--help" -> printHelp();
                case "-a", "--all" -> flags.all = true;
                case "-i", "--inline" -> flags.inline = true;
                case "-H", "--file-header" -> flags.header = true;
                case "-S", "--section-headers" -> flags.sectionHeaders = true;
                case "-s", "--symtab" -> flags.symbolTable = true;
                case "-t", "--text" -> flags.text = true;
                case "-q", "--quiet" -> flags.quiet = true;
            }
            i++;
        }
        if (flags.all) {
            flags.header = flags.sectionHeaders = flags.symbolTable = flags.text = true;
        }
//        Вывод в формате для домашней работы
//        if (i == 0 && args.length == 2) {
//            flags.specialForVictoria = true;
//            flags.symbolTable = flags.text = true;
//            flags.header = flags.sectionHeaders = false;
//            flags.inline = false;
//            flags.quiet = true;
//        }
        return i;
    }

    private void printHelp() {
        System.out.println("Утилита для чтения и дизассемблирования ELF-файлов.");
        System.out.println("Доступно декодирование заголовка ELF-файла, таблицы секций и таблицы меток.");
        System.out.println("Поддерживается дизассемблирование только RISC-V 32I/M/C архитектуры.");
        System.out.println("Использование:");
        System.out.println("  java -jar elfreader.jar [аргументы] <имя входного файла> [имя выходного файла]");
        System.out.println("Флаги/аргументы:");
        System.out.println("  \"-h\", \"--help\" -- отобразить текущее сообщение");
        System.out.println("  \"-i\", \"--inline\" -- выбрать режим вывода в консоль. В этом случае имя выходного " +
                "файла будет игнорироваться");
        System.out.println("  \"-H\", \"--file-header\" -- выводить заголовок файла");
        System.out.println("  \"-S\", \"--section-headers\" -- выводить таблицу секций");
        System.out.println("  \"-s\", \"--symtab\" -- выводить таблицу меток");
        System.out.println("  \"-t\", \"--text\" -- дизассемблировать и выводить секцию .text");
        System.out.println("  \"-a\", \"--all\" -- то же самое, что \"-H -S -s -t\"");
        System.out.println("  \"-q\", \"--quiet\" -- не бросать некритические исключения, а выводить их в файл");
        System.out.println();
        exit(0);
    }

    public void read() throws IOException {
        header = parser.parseELFHeader();
        if (flags.sectionHeaders || flags.symbolTable || flags.text) {
            sectionHeaders = parser.parseSectionHeaders(header.getSHOffset(),
                    header.getSHNumber(), header.getSHStringTableIndex());
        }
        if (flags.text) {
            boolean found = false;
            int textHeaderId = 0;
            for (; textHeaderId < sectionHeaders.size(); textHeaderId++) {
                if (sectionHeaders.get(textHeaderId).getName().equals(".text")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                textOk = false;
                if (!flags.quiet) {
                    throw new IllegalStateException(".text section not found");
                }
            }
            text = parser.parseText(sectionHeaders.get(textHeaderId), flags.quiet);
            textAddress = sectionHeaders.get(textHeaderId).getAddress();
        }
        if (flags.symbolTable || flags.text) {
            boolean found = false;
            int symtabHeaderId = 0;
            for (; symtabHeaderId < sectionHeaders.size(); symtabHeaderId++) {
                if (sectionHeaders.get(symtabHeaderId).getName().equals(".symtab")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                symtabOk = false;
                if (!flags.quiet) {
                    throw new IllegalStateException(".symtab section not found");
                }
            }
            symbolTable = parser.parseSymbolTable(sectionHeaders.get(symtabHeaderId),
                    sectionHeaders.get(sectionHeaders.get(symtabHeaderId).getLink()));
        }
    }

    public void write() {
        try (final BufferedWriter out = (flags.inline ?
                new BufferedWriter(new OutputStreamWriter(System.out)) :
                new BufferedWriter(new FileWriter(writeTo)))) {
            if (!flags.specialForVictoria) {
                out.write(String.format("Reading %s...%n%n", readFrom));
            }
            if (flags.header) {
                out.write("ELF Header:");
                out.write(System.lineSeparator());
                out.write(String.format("  %-38s %s%n", "Magic:", header.getMagic()));
                out.write(String.format("  %-38s %s%n", "Class:", header.getFileClass()));
                out.write(String.format("  %-38s %s%n", "Data:", header.getData()));
                out.write(String.format("  %-38s %s%n", "Type:", header.getType()));
                out.write(String.format("  %-38s %s%n", "Machine:", header.getMachine()));
                out.write(String.format("  %-38s %s%n", "Flags:", header.getFlags()));
                if (header.getSHOffset() > 0) {
                    out.write(String.format("  %-38s %06x%n", "Start of section headers:", header.getSHOffset()));
                    out.write(String.format("  %-38s %d%n", "Size of section headers:", header.getSHSize()));
                    out.write(String.format("  %-38s %d%n", "Number of section headers:", header.getSHNumber()));
                    out.write(String.format("  %-38s %d%n", "Section header string table index:",
                            header.getSHStringTableIndex()));
                } else {
                    out.write("No section headers");
                }
                out.write(System.lineSeparator());
            }
            if (flags.sectionHeaders) {
                out.write("Section Headers:");
                out.newLine();
                out.write(String.format("  [%2s] %-18s %-10s %-8s %-6s %-6s %2s%n", "Nr", "Name",
                        "Type", "Address", "Offset", "Size", "Lk"));
                for (int i = 0; i < header.getSHNumber(); i++) {
                    final SectionHeader sectionHeader = sectionHeaders.get(i);
                    out.write(String.format("  [%2d] %-18s %-10s %08x %06x %06x %2d%n",
                            i, sectionHeader.getName(), sectionHeader.getType(), sectionHeader.getAddress(),
                            sectionHeader.getOffset(), sectionHeader.getSize(), sectionHeader.getLink()));
                }
                out.newLine();
            }
            if (flags.specialForVictoria) {
                out.write(".text");
                out.newLine();
                printText(out);
                out.write(".symtab");
                out.newLine();
                printSymtable(out);
            } else {
                if (flags.symbolTable) {
                    if (symtabOk) {
                        printSymtable(out);
                    } else {
                        out.write("Symbol table not found");
                        out.newLine();
                    }
                }
                if (flags.text) {
                    if (symtabOk && textOk) {
                        printText(out);
                    } else {
                        if (!symtabOk && !flags.symbolTable) {
                            out.write("Symbol table not found");
                            out.newLine();
                        }
                        if (!textOk) {
                            out.write(".text section not found");
                            out.newLine();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Output error occurred" + e.getMessage());
        }
    }

    private void printText(BufferedWriter out) throws IOException {
        final Map<Integer, String> labels = new HashMap<>();
        for (Symbol symbol : symbolTable) {
            if (symbol.getType().equals("FUNC")) {
                labels.put(symbol.getValue(), symbol.getName());
            }
        }
        int address = textAddress;
        int unnamedLabels = 0;
        for (Instruction value : text) {
            final Commands command = value.getCommand();
            if (command == Commands.JAL || command == Commands.C_JAL || command == Commands.C_J ||
                    command == Commands.BEQ || command == Commands.BNE ||
                    command == Commands.BLT || command == Commands.BLTU ||
                    command == Commands.BGE || command == Commands.BGEU ||
                    command == Commands.C_BEQZ || command == Commands.C_BNEZ) {
                final int delta = value.getImmediate();
                if (!labels.containsKey(address + delta)) {
                    labels.put(address + delta, String.format("LOC_%05x", unnamedLabels++));
                }
            }
            if (value.getCommand().toString().startsWith("C_") || value.getCommand() == Commands.UNKNOWN) {
                address += 0x2;
            } else {
                address += 0x4;
            }
        }
        address = textAddress;
        if (!flags.specialForVictoria) {
            out.write("Disassembly of section .text:");
        }
        for (final Instruction instruction : text) {
            final String label = labels.getOrDefault(address, "");
            if (flags.specialForVictoria) {
                if (label.isEmpty()) {
                    out.write(String.format("%08x %10s  %s", address, "",
                            instruction.printInstruction(address, labels)));
                } else {
                    out.write(String.format("%08x %10s: %s", address, label,
                            instruction.printInstruction(address, labels)));
                }
            } else {
                if (!label.isEmpty() && !label.startsWith("LOC_")) {
                    out.write(String.format("%n%08x <%s>:%n", address, labels.get(address)));
                }
                out.write(String.format("   %08x ", address));
                out.write(!label.startsWith("LOC_") ? String.format("%12s", "") :
                        String.format(" %10s", label + ": "));

                // раскомментить если хотим выводить коды команд
//                        out.write(String.format("%08x   ", instruction.getInstruction()));
                out.write(instruction.printInstruction(address, labels));
            }
            if (instruction.getCommand().toString().startsWith("C_") || instruction.getCommand() == Commands.UNKNOWN) {
                address += 0x2;
            } else {
                address += 0x4;
            }
        }
        out.newLine();
    }

    private void printSymtable(BufferedWriter out) throws IOException {
        if (flags.specialForVictoria) {
            out.write(String.format("%s %-15s %7s %-8s %-8s %-8s %6s %s%n", "Symbol", "Value",
                    "Size", "Type", "Bind", "Vis", "Index", "Name"));
        } else {
            out.write(String.format("Symbol table '.symtab' contains %d entries:%n", symbolTable.size()));
            out.write(String.format("  [%3s] %-9s %7s %-10s %-8s %-7s %6s %s%n", "Nr", "Value",
                    "Size", "Type", "Bind", "Vis", "Idx", "Name"));
        }
        for (int i = 0; i < symbolTable.size(); i++) {
            final Symbol symbol = symbolTable.get(i);
            if (flags.specialForVictoria) {
                out.write(String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s%n", i, symbol.getValue(),
                        symbol.getSize(), symbol.getType(), symbol.getBinding(), symbol.getVisibility(),
                        symbol.getSectionHeaderIndex(), symbol.getName()));
            } else {
                out.write(String.format("  [%3d] 0x%-9x %5d %-10s %-8s %-7s %6s %s%n", i, symbol.getValue(),
                        symbol.getSize(), symbol.getType(), symbol.getBinding(), symbol.getVisibility(),
                        symbol.getSectionHeaderIndex(), symbol.getName()));
            }
        }
        out.newLine();
    }
}
