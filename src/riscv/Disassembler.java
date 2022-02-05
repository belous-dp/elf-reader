package riscv;

import elf.parser.BaseParser;
import elf.parser.ByteSource;
import elf.parser.FileByteSource;
import riscv.RV32.Commands;
import riscv.RV32.Instruction;

import java.util.ArrayList;
import java.util.List;

public class Disassembler extends BaseParser {
    private final boolean quiet;

    public Disassembler(int[] source) {
        this(source, true);
    }

    public Disassembler(int[] source, boolean quiet) {
        this(new FileByteSource(source), quiet);
    }

    private Disassembler(ByteSource source, boolean quiet) {
        super(source);
        this.quiet = quiet;
    }

    public List<Instruction> disassemble(int address) {
        final List<Instruction> res = new ArrayList<>();
        while (!eof()) {
            int h = takeHalf();
            if ((h & 0x3) == 0x3) {
                res.add(parseRV32(h | (takeHalf() << 0x10), address));
                address += 0x4;
            } else {
                res.add(parseRVC(h, address));
                address += 0x2;
            }
        }
        return res;
    }

    private Instruction parseRV32(int w, int address) {
        if ((w & 0x7f) == 0x33 && ((w >> 25) & 0x7f) == 0x1) {
            return parseRV32M(w, address);
        }
        return switch (w & 0x7f) {

            case 0x37 -> new Instruction(Type.U, Commands.LUI, w, parse32Immediate(w, Type.U),
                    Register.ZERO, Register.ZERO, parse32rd(w));

            case 0x17 -> new Instruction(Type.U, Commands.AUIPC, w, parse32Immediate(w, Type.U),
                    Register.ZERO, Register.ZERO, parse32rd(w));

            case 0x6f -> new Instruction(Type.J, Commands.JAL, w, parse32Immediate(w, Type.J),
                    Register.ZERO, Register.ZERO, parse32rd(w));

            case 0x67 -> new Instruction(Type.I, Commands.JALR, w, parse32Immediate(w, Type.I),
                    parse32rs1(w), Register.ZERO, parse32rd(w));

            case 0x63 -> new Instruction(Type.B, switch ((w >> 12) & 0x7) {
                case 0x0 -> Commands.BEQ;
                case 0x1 -> Commands.BNE;
                case 0x4 -> Commands.BLT;
                case 0x5 -> Commands.BGE;
                case 0x6 -> Commands.BLTU;
                case 0x7 -> Commands.BGEU;
                default -> unknownCommand("branch instruction", address, w);
            }, w, parse32Immediate(w, Type.B), parse32rs1(w), parse32rs2(w), Register.ZERO);

            case 0x3 -> new Instruction(Type.I, switch ((w >> 12) & 0x7) {
                case 0x0 -> Commands.LB;
                case 0x1 -> Commands.LH;
                case 0x2 -> Commands.LW;
                case 0x4 -> Commands.LBU;
                case 0x5 -> Commands.LHU;
                default -> unknownCommand("load instruction", address, w);
            }, w, parse32Immediate(w, Type.I), parse32rs1(w), Register.ZERO, parse32rd(w));

            case 0x23 -> new Instruction(Type.S, switch ((w >> 12) & 0x7) {
                case 0x0 -> Commands.SB;
                case 0x1 -> Commands.SH;
                case 0x2 -> Commands.SW;
                default -> unknownCommand("store instruction", address, w);
            }, w, parse32Immediate(w, Type.S), parse32rs1(w), parse32rs2(w), Register.ZERO);

            case 0x13 -> switch ((w >> 12) & 0x7) {
                case 0x0, 0x2, 0x3, 0x4, 0x6, 0x7 -> new Instruction(Type.I, switch ((w >> 12) & 0x7) {
                    case 0x0 -> (parse32Immediate(w, Type.I) == 0 && parse32rs1(w) == Register.ZERO &&
                            parse32rd(w) == Register.ZERO ? Commands.NOP : Commands.ADDI);
                    case 0x2 -> Commands.SLTI;
                    case 0x3 -> Commands.SLTIU;
                    case 0x4 -> Commands.XORI;
                    case 0x6 -> Commands.ORI;
                    case 0x7 -> Commands.ANDI;
                    default -> unknownCommand("arithmetic instruction", address, w);
                }, w, parse32Immediate(w, Type.I), parse32rs1(w), Register.ZERO, parse32rd(w));

                case 0x1, 0x5 -> new Instruction(Type.SH, switch ((w >> 12) & 0x7) {
                    case 0x1 -> Commands.SLLI;
                    case 0x5 -> switch ((w >> 25) & 0x7f) {
                        case 0x0 -> Commands.SRLI;
                        case 0x20 -> Commands.SRAI;
                        default -> unknownCommand("right shift instruction", address, w);
                    };
                    default -> unknownCommand("shift instruction", address, w);
                }, w, parse32Immediate(w, Type.SH), parse32rs1(w), Register.ZERO, parse32rd(w));

                default -> unknownInstruction("arithmetic instruction", address, w);
            };

            case 0x33 -> new Instruction(Type.R, switch ((w >> 12) & 0x7) {

                case 0x0 -> switch ((w >> 25) & 0x7f) {
                    case 0x0 -> Commands.ADD;
                    case 0x20 -> Commands.SUB;
                    default -> unknownCommand("add/sub instruction", address, w);
                };

                case 0x1 -> Commands.SLL;
                case 0x2 -> Commands.SLT;
                case 0x3 -> Commands.SLTU;
                case 0x4 -> Commands.XOR;

                case 0x5 -> switch ((w >> 25) & 0x7f) {
                    case 0x0 -> Commands.SRL;
                    case 0x20 -> Commands.SRA;
                    default -> unknownCommand("SR* type instruction", address, w);
                };

                case 0x6 -> Commands.OR;
                case 0x7 -> Commands.AND;

                default -> unknownCommand("arithmetic R-type instruction", address, w);
            }, w, 0, parse32rs1(w), parse32rs2(w), parse32rd(w));

            case 0x73 -> new Instruction(Type.SYSTEM, switch ((w >> 20) & 0x7ff) {
                case 0x0 -> Commands.ECALL;
                case 0x1 -> Commands.EBREAK;
                default -> unknownCommand("system call", address, w);
            }, w, parse32Immediate(w, Type.SYSTEM), Register.ZERO, Register.ZERO, Register.ZERO);

            default -> unknownInstruction("RV32I/M instruction", address, w);
        };
    }

    private int parse32Immediate(int w, Type type) {
        return switch (type) {
            case SYSTEM -> (w >> 20) & 0x1;
            case SH -> (w >> 20) & 0x1f;
            case I -> w >> 20;
            case S -> ((w >> 7) & 0x1f) | ((w >> 25) << 5);
            case B -> (((w >> 8) & 0xf) << 1) | (((w >> 25) & 0x3f) << 5) | (((w >> 7) & 0x1) << 11) |
                    ((w >> 31) << 12);
            case J -> (((w >> 21) & 0x3ff) << 1) | (((w >> 20) & 0x1) << 11) | (((w >> 12) & 0xff) << 12) |
                    ((w >> 31) << 20);
            case U -> ((w >> 12) & 0xfffff)/* << 12*/;
            default -> unknownImmediate("rv32i immediate", w, type);
        };
    }

    private Register parse32rs1(int w) {
        return Register.list.get((w >> 15) & 0x1f);
    }

    private Register parse32rs2(int w) {
        return Register.list.get((w >> 20) & 0x1f);
    }

    private Register parse32rd(int w) {
        return Register.list.get((w >> 7) & 0x1f);
    }

    private Instruction parseRV32M(int w, int address) {
        return new Instruction(Type.R, switch ((w >> 12) & 0x7) {
            case 0x0 -> Commands.MUL;
            case 0x1 -> Commands.MULH;
            case 0x2 -> Commands.MULHSU;
            case 0x3 -> Commands.MULHU;
            case 0x4 -> Commands.DIV;
            case 0x5 -> Commands.DIVU;
            case 0x6 -> Commands.REM;
            case 0x7 -> Commands.REMU;
            default -> unknownCommand("RV32M type", address, w);
        }, w, 0, parse32rs1(w), parse32rs2(w), parse32rd(w));
    }

    private Instruction parseRVC(int w, int address) {
        w &= 0xffff;
        return switch (w & 0x3) {
            case 0x0 -> switch ((w >> 13) & 0x7) {
                case 0x0 -> switch (w) {
                    case 0 -> unknownInstruction("Illegal instruction", address, w);
                    default -> new Instruction(Type.CIW, Commands.C_ADDI4SPN, w,
                            parse16Immediate(w, Type.CIW, Commands.C_ADDI4SPN),
                            Register.SP, Register.ZERO, parse16rd(w, Type.CIW));
                };

                case 0x2 -> new Instruction(Type.CL, Commands.C_LW, w,
                        parse16Immediate(w, Type.CL, Commands.C_LW),
                        parse16rs1(w, Type.CL), Register.ZERO, parse16rd(w, Type.CL));

                case 0x6 -> new Instruction(Type.CS, Commands.C_SW, w,
                        parse16Immediate(w, Type.CS, Commands.C_SW),
                        parse16rs1(w, Type.CS), parse16rs2(w, Type.CS), Register.ZERO);

                default -> unknownInstruction("or unsupported instruction", address, w);
            };

            case 0x1 -> switch ((w >> 13) & 0x7) {
                case 0x0 -> (parse16Immediate(w, Type.CI, Commands.C_NOP) == 0 ?
                        new Instruction(Type.CI, Commands.C_NOP, w, parse16Immediate(w, Type.CI, Commands.C_NOP),
                                Register.ZERO, Register.ZERO, Register.ZERO) :
                        new Instruction(Type.CI, Commands.C_ADDI, w, parse16Immediate(w, Type.CI, Commands.C_ADDI),
                                parse16rs1(w, Type.CI), Register.ZERO, parse16rd(w, Type.CI)));

                case 0x1 -> new Instruction(Type.CJ, Commands.C_JAL, w,
                        parse16Immediate(w, Type.CJ, Commands.C_JAL),
                        Register.ZERO, Register.ZERO, Register.ZERO);

                case 0x2 -> new Instruction(Type.CI, Commands.C_LI, w,
                        parse16Immediate(w, Type.CI, Commands.C_LI),
                        Register.ZERO, Register.ZERO, parse16rd(w, Type.CI));

                case 0x3 -> new Instruction(Type.CI, ((w >> 7) & 0x1f) == 0x2 ? Commands.C_ADDI16SP : Commands.C_LUI,
                        w, parse16Immediate(w, Type.CI, ((w >> 7) & 0x1f) == 0x2 ? Commands.C_ADDI16SP : Commands.C_LUI),
                        Register.ZERO, Register.ZERO, parse16rd(w, Type.CI));

                case 0x4 -> switch ((w >> 10) & 0x3) {
                    case 0x0, 0x1, 0x2 -> new Instruction(Type.CSH, switch ((w >> 10) & 0x3) {
                        case 0x0 -> Commands.C_SRLI;
                        case 0x1 -> Commands.C_SRAI;
                        case 0x2 -> Commands.C_ANDI;
                        default -> unknownCommand("c.shift instruction", address, w);
                    }, w, parse16Immediate(w, Type.CSH, switch ((w >> 10) & 0x3) {
                        case 0x0 -> Commands.C_SRLI;
                        case 0x1 -> Commands.C_SRAI;
                        case 0x2 -> Commands.C_ANDI;
                        default -> unknownCommand("c.shift instruction", address, w);
                    }), parse16rs1(w, Type.CSH), Register.ZERO, parse16rd(w, Type.CSH));

                    case 0x3 -> switch ((w >> 12) & 0x1) {
                        case 0x0 -> new Instruction(Type.CA, switch ((w >> 5) & 0x3) {
                            case 0x0 -> Commands.C_SUB;
                            case 0x1 -> Commands.C_XOR;
                            case 0x2 -> Commands.C_OR;
                            case 0x3 -> Commands.C_AND;
                            default -> unknownCommand("c.arithmetic instruction", address, w);
                        }, w, 0, parse16rs1(w, Type.CA), parse16rs2(w, Type.CA), parse16rd(w, Type.CA));
                        default -> unknownInstruction("c.arithmetic instruction", address, w);
                    };

                    default -> unknownInstruction("c.arithmetic instruction", address, w);
                };

                case 0x5 -> new Instruction(Type.CJ, Commands.C_J, w,
                        parse16Immediate(w, Type.CJ, Commands.C_J),
                        Register.ZERO, Register.ZERO, Register.ZERO);

                case 0x6, 0x7 -> new Instruction(Type.CB, switch ((w >> 13) & 0x7) {
                    case 0x6 -> Commands.C_BEQZ;
                    case 0x7 -> Commands.C_BNEZ;
                    default -> unknownCommand("c.branch instruction", address, w);
                }, w, parse16Immediate(w, Type.CB, switch ((w >> 13) & 0x7) {
                    case 0x6 -> Commands.C_BEQZ;
                    case 0x7 -> Commands.C_BNEZ;
                    default -> unknownCommand("c.branch instruction", address, w);
                }), parse16rs1(w, Type.CB), Register.ZERO, Register.ZERO);

                default -> unknownInstruction("c.arithmetic instruction", address, w);
            };

            case 0x2 -> switch ((w >> 13) & 0x7) {
                case 0x0 -> new Instruction(Type.CSH, Commands.C_SLLI, w,
                        parse16Immediate(w, Type.CSH, Commands.C_SLLI),
                        parse16rs1(w, Type.CI), Register.ZERO, parse16rd(w, Type.CI));

                case 0x2 -> new Instruction(Type.CI, Commands.C_LWSP, w,
                        parse16Immediate(w, Type.CI, Commands.C_LWSP),
                        Register.ZERO, Register.ZERO, parse16rd(w, Type.CI));

                case 0x4 -> switch ((w >> 12) & 0x1) {
                    case 0x0 -> switch ((w >> 2) & 0x1f) {
                        case 0x0 -> new Instruction(Type.CR, Commands.C_JR, w, 0,
                                parse16rs1(w, Type.CR), Register.ZERO, Register.ZERO);
                        default -> new Instruction(Type.CR, Commands.C_MV, w, 0,
                                Register.ZERO, parse16rs2(w, Type.CR), parse16rd(w, Type.CR));
                    };
                    default -> switch ((w >> 7) & 0x1f) {
                        case 0x0 -> new Instruction(Type.CSYS, Commands.C_EBREAK, w, 0,
                                Register.ZERO, Register.ZERO, Register.ZERO);
                        default -> switch ((w >> 2) & 0x1f) {
                            case 0x0 -> new Instruction(Type.CR, Commands.C_JALR, w, 0,
                                    parse16rs1(w, Type.CR), Register.ZERO, Register.ZERO);
                            default -> new Instruction(Type.CR, Commands.C_ADD, w, 0,
                                    parse16rs1(w, Type.CR), parse16rs2(w, Type.CR), parse16rd(w, Type.CR));
                        };
                    };
                };

                case 0x6 -> new Instruction(Type.CSS, Commands.C_SWSP, w,
                        parse16Immediate(w, Type.CSS, Commands.C_SWSP),
                        Register.ZERO, parse16rs2(w, Type.CSS), Register.ZERO);

                default -> unknownInstruction("system-r instruction", address, w);
            };

            default -> unknownInstruction("RVC instruction", address, w);
        };
    }

    private int parse16Immediate(int w, Type type, Commands command) {
        return switch (type) {
            case CI -> switch (command) {
                case C_LWSP -> (((w >> 12) & 0x1) << 5) | (((w >> 4) & 0x7) << 2) | (((w >> 2) & 0x3) << 6);
                case C_LI, C_ADDI, C_NOP -> signExt(((w >> 2) & 0x1f) | (((w >> 12) & 0x1) << 5), 5);
                case C_LUI -> signExt((((w >> 12) & 0x1) << 17) | (((w >> 2) & 0x1f) << 12), 17) >>> 12;
                case C_ADDI16SP -> signExt((((w >> 12) & 0x1) << 9) | (((w >> 2) & 0x1) << 5) |
                        (((w >> 3) & 0x3) << 7) | (((w >> 5) & 0x1) << 6) | (((w >> 6) & 0x1) << 4), 9);
                default -> unknownImmediate("CI type immediate", w, type);
            };

            case CSS -> (((w >> 7) & 0x3) << 6) | (((w >> 9) & 0xf) << 2);

            case CL, CS -> (((w >> 10) & 0x7) << 3) | (((w >> 5) & 0x1) << 6) | (((w >> 6) & 0x1) << 2);

            case CJ -> signExt((((w >> 2) & 0x1) << 5) | (((w >> 3) & 0x7) << 1) | (((w >> 6) & 0x1) << 7) |
                    (((w >> 7) & 0x1) << 6) | (((w >> 8) & 0x1) << 10) | (((w >> 9) & 0x3) << 8) |
                    (((w >> 11) & 0x1) << 4) | (((w >> 12) & 0x1) << 11), 11);

            case CB -> signExt((((w >> 10) & 0x3) << 3) | (((w >> 2) & 0x1) << 5) | (((w >> 3) & 0x3) << 1)
                    | (((w >> 5) & 0x3) << 6) | (((w >> 12) & 0x1) << 8), 8);

            case CSH -> signExt((((w >> 12) & 0x1) << 5) | ((w >> 2) & 0x1f), 5);

            case CIW -> (((w >> 5) & 0x1) << 3) | (((w >> 6) & 0x1) << 2) | (((w >> 7) & 0xf) << 6) |
                    (((w >> 11) & 0x3) << 4);

            default -> unknownImmediate("RVC immediate", w, type);
        };
    }

    private int signExt(int w, int signPos) {
        return ((w >> signPos) & 0x1) == 0x1 ? w | ((~0) ^ ((1 << signPos) - 1)) : w;
    }

    private Register parse16rs1(int w, Type type) {
        return Register.list.get(switch (type) {
            case CR, CI -> (w >> 7) & 0x1f;
            case CL, CS, CA, CB, CSH -> 0x8 | (w >> 7) & 0x7;
            default -> unknownImmediate("RVC rs1 type", w, type);
        });
    }


    private Register parse16rs2(int w, Type type) {
        return Register.list.get(switch (type) {
            case CR, CSS -> (w >> 2) & 0x1f;
            case CS, CA -> 0x8 | (w >> 2) & 0x7;
            default -> unknownImmediate("RVC rs2 type", w, type);
        });
    }

    private Register parse16rd(int w, Type type) {
        return Register.list.get(switch (type) {
            case CR, CI -> (w >> 7) & 0x1f;
            case CIW, CL -> 0x8 | (w >> 2) & 0x7;
            case CA, CSH -> 0x8 | (w >> 7) & 0x7;
            default -> unknownImmediate("RVC rd type", w, type);
        });
    }

    private int unknownImmediate(String message, int w, Type type) {
        if (quiet) {
            return 0;
        }
        throw new AssertionError("Unknown " + message + ". Instr: " + w + ". Type: " + type);
    }

    private Instruction unknownInstruction(String message, int address, int w) {
        return new Instruction(Type.UNKNOWN, unknownCommand(message, address, w), 0, 0,
                Register.ZERO, Register.ZERO, Register.ZERO);
    }

    private Commands unknownCommand(String message, int address, int w) {
        if (quiet) {
            return Commands.UNKNOWN;
        }
        throw new AssertionError("Unknown " + message + ". Adr:" + address + ". Instr: " + w);
    }

}
