import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomKey {
    private String name;
    private int pseudoCode;
    private List<Integer> rawCodes;
    public CustomKey(String name, int pseudoCode, List<Integer> rawCodes) {
        this.name = name;
        this.pseudoCode = pseudoCode;
        this.rawCodes = rawCodes;
    }

    public String getName() { return name; }
    public int getPseudoCode() { return pseudoCode; }
    public List<Integer> getRawCodes() { return rawCodes; }

    // format: Name:10001:524+16+134
    public String toFileString() {
        String codes = rawCodes.stream().map(String::valueOf).collect(Collectors.joining("+"));
        return name + ":" + pseudoCode + ":" + codes;
    }

    public static CustomKey fromFileString(String line) {
        try {
            String[] parts = line.split(":");
            String[] codeStrings = parts[2].split("\\+");
            List<Integer> codes = new ArrayList<>();
            for (String s : codeStrings) codes.add(Integer.parseInt(s));
            return new CustomKey(parts[0], Integer.parseInt(parts[1]), codes);
        } catch (Exception e) { return null; }
    }
}