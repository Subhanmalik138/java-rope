package com.subhanmalik.rope;

import java.util.ArrayList;
import java.util.List;

/**
 * One node of the rope tree. Nodes are immutable: every field is final and
 * every operation returns a new tree, so subtrees can be shared freely
 * between ropes (split and concat never copy text).
 *
 * <p>A leaf holds a text chunk of at most {@link #MAX_LEAF} characters.
 * An internal node joins two subtrees and caches the total character count
 * and the height of its subtree, which keeps {@code charAt}, {@code split}
 * and rebalancing cheap.
 */
final class RopeNode {

    /** Largest chunk stored in a single leaf. Big enough to keep the tree
     * shallow, small enough that splitting a leaf is a cheap substring. */
    static final int MAX_LEAF = 1024;

    /** Non-null for leaves, null for internal nodes. */
    final String text;

    /** Null for leaves. */
    final RopeNode left;

    /** Null for leaves. */
    final RopeNode right;

    /** Total characters in this subtree. */
    final int length;

    /** Height of this subtree: 1 for a leaf. */
    final int height;

    private RopeNode(String text) {
        this.text = text;
        this.left = null;
        this.right = null;
        this.length = text.length();
        this.height = 1;
    }

    private RopeNode(RopeNode left, RopeNode right) {
        this.text = null;
        this.left = left;
        this.right = right;
        this.length = left.length + right.length;
        this.height = 1 + Math.max(left.height, right.height);
    }

    boolean isLeaf() {
        return text != null;
    }

    static RopeNode leaf(String text) {
        return new RopeNode(text);
    }

    /**
     * Joins two trees. If the result grows too tall for its size it is
     * rebuilt balanced (see {@link #rebalance}), so a long chain of
     * appends can never degenerate into a linked list.
     */
    static RopeNode concat(RopeNode a, RopeNode b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        RopeNode joined = new RopeNode(a, b);
        return needsRebalance(joined) ? rebalance(joined) : joined;
    }

    /**
     * Splits the tree so the left part holds the first {@code index}
     * characters and the right part holds the rest. Runs in O(log n) on a
     * balanced tree: it walks one path down and re-joins the pieces on the
     * way back up.
     */
    static RopeNode[] split(RopeNode node, int index) {
        if (node == null) {
            return new RopeNode[] {null, null};
        }
        if (node.isLeaf()) {
            if (index <= 0) {
                return new RopeNode[] {null, node};
            }
            if (index >= node.length) {
                return new RopeNode[] {node, null};
            }
            return new RopeNode[] {
                leaf(node.text.substring(0, index)),
                leaf(node.text.substring(index))
            };
        }
        if (index < node.left.length) {
            RopeNode[] parts = split(node.left, index);
            return new RopeNode[] {parts[0], concat(parts[1], node.right)};
        }
        if (index == node.left.length) {
            return new RopeNode[] {node.left, node.right};
        }
        RopeNode[] parts = split(node.right, index - node.left.length);
        return new RopeNode[] {concat(node.left, parts[0]), parts[1]};
    }

    /** Character at {@code index}; O(log n) on a balanced tree. */
    static char charAt(RopeNode node, int index) {
        RopeNode current = node;
        while (!current.isLeaf()) {
            if (index < current.left.length) {
                current = current.left;
            } else {
                index -= current.left.length;
                current = current.right;
            }
        }
        return current.text.charAt(index);
    }

    /** Appends the characters in {@code [from, to)} to {@code out}. */
    static void appendRange(RopeNode node, int from, int to, StringBuilder out) {
        if (node == null || from >= to) {
            return;
        }
        if (node.isLeaf()) {
            out.append(node.text, from, to);
            return;
        }
        int leftEnd = node.left.length;
        if (from < leftEnd) {
            appendRange(node.left, from, Math.min(to, leftEnd), out);
        }
        if (to > leftEnd) {
            appendRange(node.right, Math.max(0, from - leftEnd), to - leftEnd, out);
        }
    }

    /** Appends the whole subtree's text to {@code out}, leaf by leaf. */
    static void appendAll(RopeNode node, StringBuilder out) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            out.append(node.text);
            return;
        }
        appendAll(node.left, out);
        appendAll(node.right, out);
    }

    /** Builds a balanced tree from a string, chunking it into leaves. */
    static RopeNode fromString(String text) {
        if (text.isEmpty()) {
            return null;
        }
        List<RopeNode> leaves = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_LEAF) {
            leaves.add(leaf(text.substring(i, Math.min(text.length(), i + MAX_LEAF))));
        }
        return buildBalanced(leaves, 0, leaves.size());
    }

    /** Number of leaves in the subtree. */
    static int leafCount(RopeNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        return leafCount(node.left) + leafCount(node.right);
    }

    // -- balancing ---------------------------------------------------------

    /**
     * A tree needs rebalancing when it is much taller than a balanced tree
     * holding the same amount of text. A balanced tree of {@code n}
     * characters is about log2(n) tall; anything beyond four times that
     * (plus a small constant) is a degenerate append chain, not a tree.
     */
    private static boolean needsRebalance(RopeNode node) {
        double balanced = Math.log(node.length + 1) / Math.log(2);
        return node.height > 4 * balanced + 8;
    }

    /**
     * Flattens the tree to its leaves and rebuilds it perfectly balanced.
     * O(n) work, but it only runs when the height check above fires, so the
     * amortised cost of a long append chain stays logarithmic.
     */
    static RopeNode rebalance(RopeNode node) {
        List<RopeNode> leaves = new ArrayList<>();
        collectLeaves(node, leaves);
        return buildBalanced(leaves, 0, leaves.size());
    }

    private static void collectLeaves(RopeNode node, List<RopeNode> out) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            out.add(node);
            return;
        }
        collectLeaves(node.left, out);
        collectLeaves(node.right, out);
    }

    private static RopeNode buildBalanced(List<RopeNode> leaves, int from, int to) {
        if (to - from == 1) {
            return leaves.get(from);
        }
        int mid = (from + to) / 2;
        return new RopeNode(
                buildBalanced(leaves, from, mid),
                buildBalanced(leaves, mid, to));
    }
}
