package com.subhanmalik.rope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command-line front end for the rope.
 *
 * <pre>
 *   java -jar rope.jar demo
 *       Builds a rope from a paragraph, runs a few edits and prints the
 *       result plus tree statistics.
 *
 *   java -jar rope.jar stats &lt;file&gt;
 *       Loads a text file into a rope and reports length, tree depth and
 *       leaf count, next to the depth a perfectly balanced tree would have.
 *
 *   java -jar rope.jar edit &lt;file&gt; &lt;index&gt; &lt;text&gt;
 *       Inserts text at the given index and prints the edited document.
 *
 *   java -jar rope.jar delete &lt;file&gt; &lt;start&gt; &lt;end&gt;
 *       Deletes the characters in [start, end) and prints the result.
 *
 *   java -jar rope.jar bench [kilobytes]
 *       Appends 1 KB chunks until the rope holds the requested size and
 *       reports the time taken and the final tree depth.
 * </pre>
 */
public final class Main {

    private static final String SAMPLE =
            "A rope stores text as a balanced binary tree instead of one flat "
            + "array. Editing a large document is cheap because an insert or "
            + "delete only rebuilds the nodes on one path down the tree, while "
            + "concatenation just creates a new root joining two trees. This "
            + "makes ropes the classic data structure behind text editors.";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return;
        }
        switch (args[0]) {
            case "demo" -> demo();
            case "stats" -> {
                requireArgs(args, 2, "stats <file>");
                stats(Path.of(args[1]));
            }
            case "edit" -> {
                requireArgs(args, 4, "edit <file> <index> <text>");
                edit(Path.of(args[1]), Integer.parseInt(args[2]), args[3]);
            }
            case "delete" -> {
                requireArgs(args, 4, "delete <file> <start> <end>");
                delete(Path.of(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            }
            case "bench" -> bench(args.length > 1 ? Integer.parseInt(args[1]) : 1024);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                usage();
            }
        }
    }

    private static void demo() {
        Rope text = Rope.of(SAMPLE);
        System.out.println("Original (" + text.length() + " chars):");
        System.out.println(text);
        System.out.println();

        Rope edited = text.insert(2, "classic ");
        // Drop the final full stop, then add a closing line.
        edited = edited.delete(edited.length() - 1, edited.length())
                .concat(" - try it yourself.");
        System.out.println("After insert + delete + concat:");
        System.out.println(edited);
        System.out.println();

        Rope[] parts = edited.split(edited.length() / 2);
        System.out.println("Split in half:");
        System.out.println("  left : " + parts[0].substring(0, Math.min(40, parts[0].length())) + "...");
        System.out.println("  right: " + parts[1].substring(0, Math.min(40, parts[1].length())) + "...");
        printStats(edited);
    }

    private static void stats(Path file) throws IOException {
        Rope rope = Rope.of(Files.readString(file));
        System.out.println("file   : " + file);
        printStats(rope);
    }

    private static void edit(Path file, int index, String text) throws IOException {
        Rope rope = Rope.of(Files.readString(file));
        System.out.print(rope.insert(index, text));
    }

    private static void delete(Path file, int start, int end) throws IOException {
        Rope rope = Rope.of(Files.readString(file));
        System.out.print(rope.delete(start, end));
    }

    private static void bench(int kilobytes) {
        String chunk = "x".repeat(1024);
        long start = System.nanoTime();
        Rope rope = Rope.empty();
        for (int i = 0; i < kilobytes; i++) {
            rope = rope.concat(chunk);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("appended " + kilobytes + " x 1 KB chunks in " + elapsedMs + " ms");
        printStats(rope);
    }

    private static void printStats(Rope rope) {
        double balancedDepth = Math.ceil(Math.log(rope.length() + 1) / Math.log(2));
        System.out.println("length : " + rope.length() + " chars");
        System.out.println("depth  : " + rope.depth()
                + " (a perfectly balanced tree of this size is ~" + (int) balancedDepth + ")");
        System.out.println("leaves : " + rope.leafCount());
    }

    private static void requireArgs(String[] args, int expected, String form) {
        if (args.length < expected) {
            throw new IllegalArgumentException("Usage: rope.jar " + form);
        }
    }

    private static void usage() {
        System.out.println("Usage: java -jar rope.jar <demo|stats|edit|delete|bench> [args]");
        System.out.println("  demo                        run a short editing demo");
        System.out.println("  stats <file>                report length, depth and leaves");
        System.out.println("  edit <file> <index> <text>  insert text and print the result");
        System.out.println("  delete <file> <start> <end> delete a range and print the result");
        System.out.println("  bench [kilobytes]           time repeated 1 KB appends");
    }
}
