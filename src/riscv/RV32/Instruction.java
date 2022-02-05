package riscv.RV32;

import riscv.Register;
import riscv.Type;

import java.util.Map;

public class Instruction {
    private final Type type;
    private final Commands command;
    private final int instruction;
    private final int immediate;
    private final Register rs1;
    private final Register rs2;
    private final Register rd;
    private final String scom;

    public Instruction(Type type, Commands command, int instruction, int immediate,
                       Register rs1, Register rs2, Register rd) {
        this.type = type;
        this.command = command;
        this.instruction = instruction;
        this.immediate = immediate;
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.rd = rd;
        this.scom = command.toString().startsWith("C_") ?
                "c." + command.toString().substring(2).toLowerCase() :
                command.toString().toLowerCase();
    }

    public Commands getCommand() {
        return command;
    }

    public int getImmediate() {
        return immediate;
    }

    public int getInstruction() {
        return instruction;
    }

    public String printInstruction(final int address, final Map<Integer, String> labels) {
        String res;
        if (type == Type.UNKNOWN || command == Commands.UNKNOWN) {
            res = "unknown_command\n";
        } else if (command == Commands.LB || command == Commands.LH || command == Commands.LW ||
                command == Commands.LBU || command == Commands.LHU ||
                command == Commands.C_LW || command == Commands.JALR) {
            res = String.format("%-6s %s, %d(%s)%n", scom,
                    rd.toString().toLowerCase(), immediate, rs1.toString().toLowerCase());
        } else if (command == Commands.C_LWSP) {
            res = String.format("%-6s %s, %d(%s)%n", scom,
                    rd.toString().toLowerCase(), immediate, "sp");
        } else if (command == Commands.SB || command == Commands.SH || command == Commands.SW ||
                command == Commands.C_SW) {
            res = String.format("%-6s %s, %d(%s)%n", scom,
                    rs2.toString().toLowerCase(), immediate, rs1.toString().toLowerCase());
        } else if (command == Commands.C_SWSP) {
            res = String.format("%-6s %s, %d(%s)%n", scom,
                    rs2.toString().toLowerCase(), immediate, "sp");
        } else if (command == Commands.NOP || command == Commands.C_NOP) {
            res = scom;
        } else if (command == Commands.JAL) {
            res = String.format("%-6s %s, %s%n", scom,
                    rd.toString().toLowerCase(), labels.get(immediate + address));
        } else if (command == Commands.C_JAL || command == Commands.C_J) {
            res = String.format("%-6s %s%n", scom, labels.get(immediate + address));
        } else if (command == Commands.C_JR || command == Commands.C_JALR) {
            res = String.format("%-6s %s%n", scom, rs1.toString().toLowerCase());
        } else if (command == Commands.BEQ || command == Commands.BNE ||
                command == Commands.BLT || command == Commands.BLTU ||
                command == Commands.BGE || command == Commands.BGEU) {
            res = String.format("%-6s %s, %s, %s%n", scom,
                    rs1.toString().toLowerCase(), rs2.toString().toLowerCase(), labels.get(immediate + address));
        } else if (command == Commands.C_BEQZ || command == Commands.C_BNEZ) {
            res = String.format("%-6s %s, %s%n", scom,
                    rs1.toString().toLowerCase(), labels.get(immediate + address));
        } else if (command == Commands.C_MV) {
            res = String.format("%-6s %s, %s%n", scom,
                    rd.toString().toLowerCase(), rs2.toString().toLowerCase());
        } else {
            final String formatRdImm = String.format("%-6s %s, %d%n", scom,
                    rd.toString().toLowerCase(), immediate);

            final String formatRdRs1Imm = String.format("%-6s %s, %s, %d%n", scom,
                    rd.toString().toLowerCase(), rs1.toString().toLowerCase(), immediate);

            final String formatRdRs1Rs2 = String.format("%-6s %s, %s, %s%n", scom,
                    rd.toString().toLowerCase(), rs1.toString().toLowerCase(), rs2.toString().toLowerCase());

            final String formatRs1Rs2Imm = String.format("%-6s %s, %s, %d%n", scom,
                    rs1.toString().toLowerCase(), rs2.toString().toLowerCase(), immediate);

            final String formatRs1Rs2 = String.format("%-6s %s, %s%n", scom,
                    rs1.toString().toLowerCase(), rs2.toString().toLowerCase());

            res = switch (type) {
                case R -> formatRdRs1Rs2;
                case I, SH, CSH, CIW -> formatRdRs1Imm;
                case S, B -> formatRs1Rs2Imm;
                case U, J -> formatRdImm;
                case SYSTEM, CSYS -> String.format("%-6s%n", scom);
                case CJ -> String.format("%-6s %d%n", scom, immediate);
                case CB -> String.format("%-6s %s, %d%n", scom, rs1.toString().toLowerCase(), immediate);
                case CI -> switch (command) {
                    case C_LI, C_LUI -> formatRdImm;
                    case C_ADDI -> formatRdRs1Imm;
                    case C_ADDI16SP -> formatRdImm;
                    default -> throw new AssertionError("Unknown instruction. Type: " + type +
                            ", Command: " + command + ", Instruction: " + instruction);
                };
                case CR, CA -> formatRs1Rs2;
                default -> throw new AssertionError("Unknown instruction. Type: " + type +
                        ", Command: " + command + ", Instruction: " + instruction);
            };
        }
        return res;
    }
}
