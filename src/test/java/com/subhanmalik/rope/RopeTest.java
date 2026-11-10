package com.subhanmalik.rope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RopeTest {

    @Test
    void emptyRopeHasZeroLengthAndEmptyString() {
        Rope rope = Rope.empty();
        assertEquals(0, rope.length());
        assertTrue(rope.isEmpty());
        assertEquals("", rope.toString());
        assertEquals(0, rope.depth());
        assertEquals(0, rope.leafCount());
    }

    @Test
    void ofRoundTripsShortAndLongText() {
        assertEquals("hello", Rope.of("hello").toString());
        String longText = "ab".repeat(5000);
        Rope rope = Rope.of(longText);
        assertEquals(longText, rope.toString());
        assertEquals(10_000, rope.length());
        // 10 KB of text is 10 leaves of 1 KB each, balanced depth is small.
        assertTrue(rope.depth() <= 6, "depth was " + rope.depth());
        assertEquals(10, rope.leafCount());
    }

    @Test
    void charAtReadsAcrossLeafBoundaries() {
        String text = "a".repeat(1024) + "b".repeat(1024) + "c";
        Rope rope = Rope.of(text);
        assertEquals('a', rope.charAt(0));
        assertEquals('a', rope.charAt(1023));
        assertEquals('b', rope.charAt(1024));
        assertEquals('b', rope.charAt(2047));
        assertEquals('c', rope.charAt(2048));
    }

    @Test
    void charAtRejectsOutOfBoundsIndexes() {
        Rope rope = Rope.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> rope.charAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.charAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> Rope.empty().charAt(0));
    }

    @Test
    void concatJoinsTextAndLeavesEmptyOperandsAlone() {
        Rope rope = Rope.of("foo").concat(Rope.of("bar")).concat("baz");
        assertEquals("foobarbaz", rope.toString());
        assertEquals(9, rope.length());
        assertEquals(Rope.of("foo"), Rope.of("foo").concat(Rope.empty()));
        assertEquals(Rope.of("foo"), Rope.empty().concat(Rope.of("foo")));
    }

    @Test
    void splitAtStartMiddleAndEnd() {
        Rope rope = Rope.of("0123456789");
        Rope[] atStart = rope.split(0);
        assertEquals("", atStart[0].toString());
        assertEquals("0123456789", atStart[1].toString());

        Rope[] middle = rope.split(4);
        assertEquals("0123", middle[0].toString());
        assertEquals("456789", middle[1].toString());

        Rope[] atEnd = rope.split(10);
        assertEquals("0123456789", atEnd[0].toString());
        assertEquals("", atEnd[1].toString());
    }

    @Test
    void splitInsideALeafAndAcrossLeaves() {
        // 3000 chars: leaves are [0,1024) [1024,2048) [2048,3000).
        Rope rope = Rope.of("x".repeat(1500) + "y".repeat(1500));
        Rope[] insideLeaf = rope.split(500);
        assertEquals(500, insideLeaf[0].length());
        assertEquals(2500, insideLeaf[1].length());
        assertEquals("x".repeat(500), insideLeaf[0].toString());

        Rope[] acrossLeaves = rope.split(1500);
        assertEquals("x".repeat(1500), acrossLeaves[0].toString());
        assertEquals("y".repeat(1500), acrossLeaves[1].toString());
        assertEquals(rope.toString(), acrossLeaves[0].toString() + acrossLeaves[1].toString());
    }

    @Test
    void splitRejectsOutOfBoundsIndexes() {
        Rope rope = Rope.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> rope.split(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.split(4));
    }

    @Test
    void insertAtStartMiddleAndEnd() {
        Rope rope = Rope.of("Hello world!");
        assertEquals(">>>Hello world!", rope.insert(0, ">>>").toString());
        assertEquals("Hello brave new world!", rope.insert(6, "brave new ").toString());
        assertEquals("Hello world!<<<", rope.insert(12, "<<<").toString());
        assertEquals(rope, rope.insert(3, ""));
    }

    @Test
    void deleteRemovesRangesAndWholeRope() {
        Rope rope = Rope.of("Hello brave new world!");
        assertEquals("Hello world!", rope.delete(6, 16).toString());
        assertEquals("brave new world!", rope.delete(0, 6).toString());
        assertEquals("Hello brave new ", rope.delete(16, 22).toString());
        assertEquals("", rope.delete(0, 22).toString());
        assertEquals(rope, rope.delete(3, 3));
    }

    @Test
    void deleteRejectsInvalidRanges() {
        Rope rope = Rope.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> rope.delete(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.delete(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.delete(0, 4));
    }

    @Test
    void substringReturnsExactRanges() {
        Rope rope = Rope.of("0123456789".repeat(300));
        assertEquals("01234", rope.substring(0, 5));
        assertEquals("56789", rope.substring(5, 10));
        // Range straddling a leaf boundary (leaf size 1024).
        assertEquals(rope.toString().substring(1000, 1100), rope.substring(1000, 1100));
        assertEquals("", rope.substring(7, 7));
        assertEquals(rope.toString(), rope.substring(0, rope.length()));
    }

    @Test
    void editsDoNotMutateTheOriginal() {
        Rope original = Rope.of("unchanged");
        Rope inserted = original.insert(0, ">>");
        Rope deleted = original.delete(0, 2);
        Rope concatenated = original.concat("!");
        assertEquals("unchanged", original.toString());
        assertEquals(">>unchanged", inserted.toString());
        assertEquals("changed", deleted.toString());
        assertEquals("unchanged!", concatenated.toString());
    }

    @Test
    void manySmallAppendsStayBalancedAndCorrect() {
        // 500 single-character appends would be a 500-deep chain without
        // rebalancing; the tree must stay shallow and the text exact.
        Rope rope = Rope.empty();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            char c = (char) ('a' + (i % 26));
            rope = rope.concat(String.valueOf(c));
            expected.append(c);
        }
        assertEquals(expected.toString(), rope.toString());
        assertTrue(rope.depth() <= 30, "depth was " + rope.depth());
    }

    @Test
    void manyRandomEditsMatchStringBuilder() {
        // Deterministic pseudo-random edits: the rope must agree with a
        // plain StringBuilder after every step.
        Rope rope = Rope.of("The quick brown fox jumps over the lazy dog.");
        StringBuilder expected = new StringBuilder(rope.toString());
        long seed = 42;
        for (int step = 0; step < 200; step++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int op = (int) ((seed >>> 33) % 3);
            int pos = (int) ((seed >>> 17) % (expected.length() + 1));
            if (op == 0) {
                String chunk = "ab" + (step % 10);
                rope = rope.insert(pos, chunk);
                expected.insert(pos, chunk);
            } else if (op == 1 && expected.length() > 0) {
                int end = Math.min(expected.length(), pos + (step % 5) + 1);
                rope = rope.delete(pos, end);
                expected.delete(pos, end);
            } else {
                rope = rope.concat("z");
                expected.append("z");
            }
            assertEquals(expected.toString(), rope.toString(), "mismatch at step " + step);
        }
    }

    @Test
    void largeRopeSurvivesSplitAndRejoin() {
        String text = "Lorem ipsum dolor sit amet. ".repeat(4000); // ~112 KB
        Rope rope = Rope.of(text);
        int mid = rope.length() / 2;
        Rope[] parts = rope.split(mid);
        assertEquals(text, parts[0].concat(parts[1]).toString());
        assertEquals(text.substring(0, mid), parts[0].toString());
        assertEquals(text.substring(mid), parts[1].toString());
    }

    @Test
    void equalsAndHashCodeDependOnContent() {
        assertEquals(Rope.of("same"), Rope.of("sa").concat("me"));
        assertEquals(Rope.of("same").hashCode(), Rope.of("same").hashCode());
        assertNotEquals(Rope.of("same"), Rope.of("different"));
        assertNotEquals(Rope.of("same"), "same");
    }

    @Test
    void ofRejectsNull() {
        assertThrows(NullPointerException.class, () -> Rope.of(null));
    }
}
