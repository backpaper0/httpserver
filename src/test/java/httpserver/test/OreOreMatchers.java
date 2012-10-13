package httpserver.test;

import java.util.Arrays;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

public class OreOreMatchers {

    private OreOreMatchers() {
    }

    public static ByteArrayMatcher array(byte[] expected) {
        return new ByteArrayMatcher(expected);
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
}
