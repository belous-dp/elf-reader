package riscv;

public enum Type {
    R, I, S, B, U, J,
    SH, //shifts
    SYSTEM,
    CR, CI, CSS, CIW, CL, CS, CA, CB, CJ,
    CSH, //c.shifts + c.andi,
    CSYS,
    UNKNOWN
}
