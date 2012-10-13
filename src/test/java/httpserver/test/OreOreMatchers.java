package httpserver.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

public class OreOreMatchers {

    private OreOreMatchers() {
    }

    public static ByteArrayMatcher array(byte[] expected) {
        return new ByteArrayMatcher(expected);
    }

    public static InputStreamMatcher inputStream(String expected) {
        return new InputStreamMatcher(new ByteArrayInputStream(
            expected.getBytes()));
    }

    public static class ByteArrayMatcher extends TypeSafeMatcher<byte[]> {

        private final byte[] expected;

        public ByteArrayMatcher(byte[] expected) {
            this.expected = expected;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expected);
        }

        @Override
        public boolean matchesSafely(byte[] item) {
            return Arrays.equals(expected, item);
        }
    }

    public static class InputStreamMatcher extends TypeSafeMatcher<InputStream> {

        private InputStream expected;

        public InputStreamMatcher(InputStream expected) {
            this.expected = expected;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expected);
        }

        @Override
        public boolean matchesSafely(InputStream item) {
            try {
                int a, b;
                do {
                    a = item.read();
                    b = expected.read();
                    if (a != b) {
                        return false;
                    }
                } while (a != -1 && b != -1);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
