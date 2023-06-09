import java.util.ArrayList;
import java.util.HashMap;

public class Assembler {

    static HashMap<String, String> symTab = new HashMap<>(); // key: symbol, value: locCtr
    static HashMap<String, String> opTab = new HashMap<>() {
        {

            put("ADD", "18");
            put("ADDF", "58");
            put("ADDR", "90");
            put("AND", "40");
            put("CLEAR", "B4");
            put("COMP", "28");
            put("COMPF", "88");
            put("COMPR", "A0");
            put("DIV", "24");
            put("DIVF", "64");
            put("DIVR", "9C");
            put("FIX", "C4");
            put("FLOAT", "C0");
            put("HIO", "F4");
            put("J", "3c");
            put("JEQ", "30");
            put("JGT", "34");
            put("JLT", "38");
            put("JSUB", "48");
            put("LDA", "00");
            put("LDB", "68");
            put("LDCH", "50");
            put("LDF", "70");
            put("LDL", "08");
            put("LDS", "6C");
            put("LDT", "74");
            put("LDX", "04");
            put("LPS", "D0");
            put("MUL", "20");
            put("MULF", "60");
            put("MULR", "98");
            put("NORM", "c8");
            put("OR", "44");
            put("RD", "D8");
            put("RMO", "AC");
            put("RSUB", "4C");
            put("SHIFTL", "A4");
            put("SHIFTR", "A8");
            put("SIO", "F0");
            put("SSK", "EC");
            put("STA", "0C");
            put("STB", "78");
            put("STCH", "54");
            put("STF", "80");
            put("STI", "D4");
            put("STL", "14");
            put("STS", "7C");
            put("STSW", "E8");
            put("STT", "84");
            put("STX", "10");
            put("SUB", "1C");
            put("SUBF", "5C");
            put("SUBR", "94");
            put("SVC", "B0");
            put("TD", "E0");
            put("TIO", "F8");
            put("TIX", "2C");
            put("TIXR", "B8");
            put("WD", "DC");
        }

    }; // key: mnemonic, value: opCode

    public static void main(String[] args) {
        passOne(FileManage.load(args[0]));
        passTwo(FileManage.load("intermediateFile.txt"));
    }

    public static boolean isComment(String[] line) {
        return line[0].equals(".");
    }

    public static boolean hasLabel(String[] line, boolean hasLoc) {
        if (hasLoc)
            return line.length > 3;
        return line.length > 2;
    }

    public static String getLabel(String[] line, boolean hasLoc) {
        if (hasLabel(line, hasLoc)) {
            if (hasLoc)
                return line[1];
            else
                return line[0];
        }
        return "";
    }

    public static String getOpcode(String[] line, boolean hasLoc) {
        if (hasLabel(line, hasLoc)) {
            if (hasLoc)
                return line[2];
            else
                return line[1];
        }
        if (hasLoc)
            return line[1];
        return line[0];
    }

    public static String getOperand(String[] line, boolean hasLoc) {
        if (hasLabel(line, hasLoc)) {
            if (hasLoc) {
                if (line.length == 2)
                    return "";
                return line[3];
            } else {
                if (line.length == 1)
                    return "";
                return line[2];
            }
        }
        if (hasLoc) {
            if (line.length == 2)
                return "";
            return line[2];
        }
        return line[1];
    }

    public static void setErrorFlag(String errorMessage) {
        System.out.println(errorMessage);
    }

    public static void passOne(ArrayList<String> sourceCode) {
        String[] firstLine = sourceCode.get(0).trim().split("\\s+");
        int locCtr = 0;
        boolean hasStartLabel = false;
        if (getOpcode(firstLine, false).equals("START")) {
            locCtr = Integer.parseInt(getOperand(firstLine, false), 16);
            hasStartLabel = true;
        }
        int startingAddress = locCtr;
        StringBuilder content = new StringBuilder("");
        for (int i = 0; i < sourceCode.size(); i++) {
            String[] line = sourceCode.get(i).trim().split("\\s+");

            if (isComment(line)) {
                content.append(sourceCode.get(i) + '\n');
                continue;
            }

            String loc = Integer.toHexString(locCtr).toUpperCase();

            if (hasStartLabel && i == 0) {
                content.append(loc + "\t" + sourceCode.get(i) + '\n');
                continue;
            }

            if (hasLabel(line, false)) {
                String label = getLabel(line, false);
                if (symTab.containsKey(label)) {
                    setErrorFlag("duplicate symbol");
                } else {
                    symTab.put(label, loc);
                }
            }

            String opCode = getOpcode(line, false);
            if (opCode.equals("END")) {
                String programLength = Integer.toHexString(locCtr - startingAddress).toUpperCase();
                content.append(sourceCode.get(i) + '\n');
                content.append("program length: " + programLength);
                continue;
            }

            if (opTab.containsKey(opCode)) {
                locCtr += 3;
            } else if (opCode.equals("WORD")) {
                locCtr += 3;
            } else if (opCode.equals("RESW")) {
                int operand = Integer.parseInt(getOperand(line, false));
                locCtr += 3 * operand;
            } else if (opCode.equals("RESB")) {
                int operand = Integer.parseInt(getOperand(line, false));
                locCtr += operand;
            } else if (opCode.equals("BYTE")) {
                String operand = getOperand(line, false);
                int chLen = operand.length() - 3;

                if (operand.charAt(0) == 'C') {
                    locCtr += chLen;
                } else if (operand.charAt(0) == 'X') {
                    locCtr += chLen / 2;
                }
            } else {
                setErrorFlag("invalid operation code");
            }

            content.append(loc + "\t" + sourceCode.get(i) + '\n');
        }
        FileManage.save("intermediateFile.txt", content.toString());
    }

    public static String padWithZero(String str) {
        while (str.length() < 6)
            str = "0" + str;
        return str;
    }

    public static StringBuilder writeHeaderRecord(String programName, String startingAddress, String programLength) {
        StringBuilder content = new StringBuilder("");
        content.append("H" + programName + "  " + padWithZero(startingAddress) + padWithZero(programLength) + '\n');
        return content;
    }

    public static StringBuilder textRecord = new StringBuilder("");
    public static String textHead = "";
    public static String curTextLine = "";

    public static void writeTextRecord(String objectCode, String loc) {
        if (curTextLine.equals("")) {
            intialTextRecord(loc);
        } else if (!fit(curTextLine, objectCode)) {
            writeTextLine();
            intialTextRecord(loc);
        }
        addObjectCode(objectCode);
    }

    public static boolean fit(String curTextLine, String objectCode) {
        if (curTextLine.length() + objectCode.length() > 60 || objectCode.equals(""))
            return false;
        return true;
    }

    public static void intialTextRecord(String loc) {
        textHead = "T" + padWithZero(loc);
        curTextLine = "";
    }

    public static void writeTextLine() {
        String length = Integer.toHexString(curTextLine.length() / 2).toUpperCase();
        curTextLine = curTextLine.toUpperCase();
        textRecord.append(textHead + length + curTextLine + "\n");
    }

    public static void addObjectCode(String objectCode) {
        curTextLine += objectCode;
    }

    public static String createObjectCode(String opCode, String operand) {
        boolean indexed = false;
        if (operand.contains(",")) {
            String[] list = operand.split(",");
            operand = list[0];
            indexed = true;
        }
        String tmp = "";
        if (operand != "") {
            int operandAddress = Integer.parseInt(symTab.get(operand), 16);
            int indexedsign = Integer.parseInt("8000", 16);
            if (indexed)
                tmp = Integer.toHexString(operandAddress + indexedsign);
            else
                tmp = Integer.toHexString(operandAddress);
        } else {
            tmp = "0000";
        }
        String objectCode = padWithZero(opTab.get(opCode) + tmp);
        return objectCode;
    }

    public static String writeEndRecord(String startingAddress) {
        return "E" + padWithZero(startingAddress);
    }

    public static void passTwo(ArrayList<String> intermediateFile) {
        String[] firstLine = intermediateFile.get(0).trim().split("\\s+");
        String[] lastLine = intermediateFile.get(intermediateFile.size() - 1).trim().split("\\s+");
        String startingAddress = "0";
        StringBuilder objectProgram = new StringBuilder("");
        if (getOpcode(firstLine, true).equals("START")) {
            startingAddress = firstLine[0];
            objectProgram
                    .append(writeHeaderRecord(getLabel(firstLine, true), startingAddress, getOperand(lastLine, false)));
        }
        StringBuilder content = new StringBuilder("Loc" + "\t" + "Source statement" + "\t" + "Object code " + "\n");
        for (int i = 0; i < intermediateFile.size(); i++) {
            String[] line = intermediateFile.get(i).trim().split("\\s+");

            if (isComment(line)) {
                content.append(intermediateFile.get(i) + '\n');
                continue;
            }

            String opCode = getOpcode(line, true);

            if (opCode.equals("START")) {
                content.append(intermediateFile.get(i) + '\n');
                continue;
            }

            if (getOpcode(line, false).equals("END")) {
                writeTextLine();
                objectProgram.append(textRecord);
                String endRecord = writeEndRecord(startingAddress);
                objectProgram.append(endRecord);
                content.append(intermediateFile.get(i));
                break;
            }

            String objectCode = "";
            if (opTab.containsKey(opCode)) {
                String operand = getOperand(line, true);
                if (operand != "") {
                    if (operand.contains(",")) {
                        operand = operand.split(",")[0];
                    }
                    if (!symTab.containsKey(operand)) {
                        setErrorFlag("undefined symbol");
                    }
                }
                objectCode = createObjectCode(opCode, operand);
            } else if (opCode.equals("BYTE")) {
                String operand = getOperand(line, true);
                String str = operand.split("\'")[1];
                if (operand.charAt(0) == 'C') {
                    for (int j = 0; j < str.length(); j++) {
                        int asciiCode = str.charAt(j);
                        objectCode += Integer.toHexString(asciiCode).toUpperCase();
                    }
                } else if (operand.charAt(0) == 'X') {
                    objectCode = str;
                }
            } else if (opCode.equals("WORD")) {
                String operand = getOperand(line, true);
                int size = Integer.parseInt(operand);
                objectCode = padWithZero(Integer.toHexString(size));
            } else if (opCode.equals("RESB") || opCode.equals("RESW")) {
                objectCode = "";
            }
            String loc = line[0];
            writeTextRecord(objectCode, loc);
            content.append(intermediateFile.get(i) + "\t" + objectCode + '\n');
        }
        FileManage.save("objectProgram.txt", objectProgram.toString());
        FileManage.save("listingFile.txt", content.toString());
    }
}