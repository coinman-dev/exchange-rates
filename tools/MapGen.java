import java.util.*;
import java.util.stream.*;
public class MapGen {
    public static void main(String[] a) {
        // currency -> set of country codes, from CLDR locale data
        Map<String, Set<String>> cur2countries = new TreeMap<>();
        for (Locale l : Locale.getAvailableLocales()) {
            String cc = l.getCountry();
            if (cc == null || cc.length() != 2) continue;
            try {
                Currency c = Currency.getInstance(l);
                if (c == null) continue;
                cur2countries.computeIfAbsent(c.getCurrencyCode(), k -> new TreeSet<>()).add(cc);
            } catch (Exception ignored) {}
        }
        System.out.println("{");
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : cur2countries.entrySet()) {
            lines.add("  \"" + e.getKey() + "\": [" +
                e.getValue().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]");
        }
        System.out.println(String.join(",\n", lines));
        System.out.println("}");
    }
}
