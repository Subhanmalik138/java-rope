package com.subhanmalik.rope;

import java.util.Objects;

/**
 * A rope: a string stored as a weight-balanced binary tree instead of one
 * flat array. Editing a huge text is cheap because insert, delete and
 * split only rebuild the O(log n) nodes on one path, and concatenation is
 * O(1) — it just creates a new root joining the two trees.
 *
 * <p>Ropes are immutable. Every edit returns a new rope; the original is
 * untouched and the two share the subtrees they have in common, so keeping
 * an old version around costs almost no extra memory.
 *
 * <p>Example:
 * <pre>{@code
 * Rope text = Rope.of("Hello, world!");
 * Rope edited = text.insert(7, "brave new ").delete(0, 0);
 * System.out.println(edited); // Hello, brave new world!
 * }</pre>
 */
public final class Rope {

    private final RopeNode root;

    private Rope(RopeNode root) {
        this.root = root;
    }

    /** An empty rope. */
    public static Rope empty() {
        return new Rope(null);
    }

    /** A rope holding exactly {@code text}. */
    public static Rope of(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return new Rope(RopeNode.fromString(text));
    }

    /** Number of characters in the rope. */
    public int length() {
        return root == null ? 0 : root.length;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /** Height of the underlying tree; 0 for an empty rope. */
    public int depth() {
        return root == null ? 0 : root.height;
    }

    /** Number of leaf chunks the text is split into. */
    public int leafCount() {
        return RopeNode.leafCount(root);
    }

    /** Character at {@code index}. Throws {@link IndexOutOfBoundsException}
     * for an index outside {@code [0, length)}. */
    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " out of bounds for length " + length());
        }
        return RopeNode.charAt(root, index);
    }

    /** A new rope holding this text followed by {@code other}. */
    public Rope concat(Rope other) {
        Objects.requireNonNull(other, "other must not be null");
        if (other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        return new Rope(RopeNode.concat(root, other.root));
    }

    /** A new rope holding this text followed by {@code text}. */
    public Rope concat(String text) {
        return concat(Rope.of(text));
    }

    /**
     * Splits the rope at {@code index}: the first rope holds characters
     * {@code [0, index)}, the second holds {@code [index, length)}.
     */
    public Rope[] split(int index) {
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                    "split index " + index + " out of bounds for length " + length());
        }
        RopeNode[] parts = RopeNode.split(root, index);
        return new Rope[] {new Rope(parts[0]), new Rope(parts[1])};
    }

    /** A new rope with {@code text} inserted at {@code index}. */
    public Rope insert(int index, String text) {
        Objects.requireNonNull(text, "text must not be null");
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                    "insert index " + index + " out of bounds for length " + length());
        }
        if (text.isEmpty()) {
            return this;
        }
        Rope[] parts = split(index);
        return parts[0].concat(Rope.of(text)).concat(parts[1]);
    }

    /**
     * A new rope with the characters in {@code [start, end)} removed.
     * Deleting the whole rope returns an empty rope.
     */
    public Rope delete(int start, int end) {
        if (start < 0 || end < start || end > length()) {
            throw new IndexOutOfBoundsException(
                    "delete range [" + start + ", " + end + ") out of bounds for length " + length());
        }
        if (start == end) {
            return this;
        }
        Rope[] first = split(start);
        Rope[] second = first[1].split(end - start);
        return first[0].concat(second[1]);
    }

    /** The characters in {@code [start, end)} as a plain string. */
    public String substring(int start, int end) {
        if (start < 0 || end < start || end > length()) {
            throw new IndexOutOfBoundsException(
                    "substring range [" + start + ", " + end + ") out of bounds for length " + length());
        }
        StringBuilder out = new StringBuilder(end - start);
        RopeNode.appendRange(root, start, end, out);
        return out.toString();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(length());
        RopeNode.appendAll(root, out);
        return out.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rope that)) {
            return false;
        }
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
