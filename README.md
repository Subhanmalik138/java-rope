# Rope — a balanced string tree for Java

A rope stores text as a binary tree instead of one flat array. Editing a
large document is cheap: insert, delete and split only rebuild the nodes on
one path down the tree, and concatenation just creates a new root joining
two trees. That is why ropes are the classic data structure behind text
editors — and why they make a good interview topic: they combine binary
trees, amortised analysis and an immutable API in one small package.

## What it does

- `Rope.of(String)` — build a rope from any text
- `concat`, `insert`, `delete`, `split`, `substring`, `charAt` — the full
  editing API, all returning new ropes (the original is never modified)
- Automatic rebalancing, so a long chain of appends can never degenerate
  into a linked list
- `depth()` and `leafCount()` — inspect the tree shape (handy for seeing
  the balancing work)

## Design decisions

1. **Immutable nodes, shared subtrees.** Every node is final and every edit
   returns a new tree. Split and concat never copy text — they reuse the
   subtrees they already have — so keeping an old version of a document
   costs almost no extra memory.
2. **Chunked leaves (1 KB).** Leaves hold up to 1024 characters. Smaller
   chunks would make the tree needlessly deep; one giant leaf would make
   splitting a leaf as expensive as copying the whole string. 1 KB keeps
   both costs small.
3. **Rebalance by flatten-and-rebuild, triggered by a height check.** A
   balanced tree of *n* characters is about log₂(n) tall. After a concat,
   if the height exceeds 4·log₂(n) + 8 the tree is flattened to its leaves
   and rebuilt perfectly balanced. The check is O(1) (height is cached) and
   the rebuild is O(n) but rare, so a long append chain stays logarithmic
   in amortised cost — much simpler to get right than rotations, and
   provably bounded.

## Complexity

| Operation | Balanced tree |
| --- | --- |
| `charAt`, `split` | O(log n) |
| `insert`, `delete` | O(log n) amortised |
| `concat` | O(1) amortised |
| `substring`, `toString` | O(k) for k characters returned |

## Run it

Requires JDK 17+.

```bash
./build.sh            # compiles and builds out/rope.jar
java -jar out/rope.jar demo
```

CLI commands:

```bash
java -jar out/rope.jar demo                         # short editing demo
java -jar out/rope.jar stats <file>                 # length, depth, leaves
java -jar out/rope.jar edit <file> <index> <text>   # insert and print result
java -jar out/rope.jar delete <file> <start> <end>  # delete a range
java -jar out/rope.jar bench [kilobytes]            # time repeated 1 KB appends
```

## Run the tests

```bash
./build.sh test
```

18 JUnit 5 tests cover round-trips, splits inside and across leaves,
insert/delete at the boundaries, immutability of the original rope, a
200-step deterministic random-edit fuzz against `StringBuilder`, and the
key invariant: 500 single-character appends leave the tree shallow and the
text exact.

## Tech stack

- Java 17, zero dependencies
- JUnit 5 (console standalone) for tests
- Plain `javac`/`jar` build — no build tool required

## License

MIT — see [LICENSE](LICENSE).
