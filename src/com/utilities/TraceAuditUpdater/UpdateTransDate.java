import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateTransDate {

    // Matches dates like '05/21/26'
    private static final Pattern DATE_PATTERN =
            Pattern.compile("'(\\d{2})/(\\d{2})/(\\d{2})'");

    public static void main(String[] args) throws IOException {

        Path input = Path.of("D:\\input.sql");
        Path output = Path.of("D:\\output.sql");

        String sql = Files.readString(input);

        Matcher matcher = DATE_PATTERN.matcher(sql);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {

            String month = matcher.group(1);
            String day = matcher.group(2);
            String year = "20" + matcher.group(3);

            String replacement =
                    "TO_DATE('" + month + "/" + day + "/" + year +
                            "','%m/%d/%Y')";

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);

        Files.writeString(output, sb.toString());

        System.out.println("Done.");
    }
}