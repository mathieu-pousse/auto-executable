package app;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.StringJoiner;

/**
 * Created by Mathieu POUSSE, long time ago...
 */
public class Bootstrap {

    /**
     * Run me !
     * @param args various stuffs
     */
    public static void main(String[] args) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String arg : args) {
            joiner.add(arg);
        }
        System.out.printf("Running w/ [%s]\n", joiner.toString());
        System.out.printf("Complex hash = %s", RandomStringUtils.randomAlphanumeric(64));
    }
}
