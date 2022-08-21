import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class regx_test {

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("[/][A-z]]");
        Matcher matcher = pattern.matcher("foom.txt");

        if (matcher.find()) {
            System.out.println(matcher.group(0));
        }
    }
}
