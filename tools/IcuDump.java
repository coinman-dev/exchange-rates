import java.io.*;
import java.util.*;
public class IcuDump {
    public static void main(String[] args) throws Exception {
        Locale ru = Locale.forLanguageTag("ru");
        Locale en = Locale.forLanguageTag("en");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        String line;
        while ((line = r.readLine()) != null) {
            String c = line.trim();
            if (c.isEmpty()) continue;
            try {
                Currency cur = Currency.getInstance(c);
                String symEn = cur.getSymbol(en);
                String symRu = cur.getSymbol(ru);
                out.println(String.join("\t", c,
                        cur.getDisplayName(en), cur.getDisplayName(ru),
                        symEn.equals(c) ? "" : symEn,
                        symRu.equals(c) ? "" : symRu,
                        String.valueOf(cur.getDefaultFractionDigits())));
            } catch (Exception e) {
                out.println(String.join("\t", c, "", "", "", "", "-1"));
            }
        }
    }
}
