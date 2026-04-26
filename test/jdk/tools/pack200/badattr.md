# badattr.jar — Findings and Fix Documentation

`badattr.jar` is a test artefact used by `AttributeTests.java` (JDK bug 6746111) to
verify that `pack200 --repack` handles malformed or non-standard class attributes
gracefully instead of crashing.  The JAR contains two hand-crafted class files, each
triggering a distinct failure mode that required a separate fix in the Pack200.net
packer.

---

## Contents of badattr.jar

| Entry | Source | Deliberate defect |
|---|---|---|
| `Foo.class` | `Foo.java` | The `SourceFile` attribute name string was renamed to `XourceFile` in the constant pool using a binary editor, making it an unknown/unrecognised attribute. |
| `Test.class` | `Test.java` | The `AnnotationDefault` attribute's `attribute_length` field was set to `0` using a binary editor, which is structurally illegal (the attribute body must be at least 1 byte — a `value` element). |
| `Readme.txt` | — | Human-readable description of the defects. |

---

## Foo.class — Unknown attribute (`XourceFile`)

### What the file contains

`Foo.java` is a trivial class with a `main` method.  After compilation its constant
pool contained the string `"SourceFile"`.  That string was edited in-place to
`"XourceFile"`, turning the standard debug attribute into an attribute with an
unrecognised name.

```java
// Foo.java (source)
public class Foo {
    public static void main(String[] args) {}
}
```

### How the packer processes it

ASM's `ClassReader` reads the constant pool normally.  When it encounters the class
attribute named `XourceFile` it dispatches to `Segment.visitAttribute()`.  Because
`XourceFile` is unknown, the attribute action for the class context evaluates to
`PackingOptions.PASS`, and `Segment.passCurrentClass()` is called, which throws a
`Segment.PassException`.

That exception was already caught in `Segment.processClasses()` **before** this round
of fixes.  The path through `Foo.class` therefore worked correctly in the original
code.

### Expected packer output (from `AttributeTests.java`)

```
WARNING: Passing class file uncompressed due to unrecognized attribute: Foo.class
```

---

## Test.class — Zero-length `AnnotationDefault` attribute

### What the file contains

`Test.java` is a Java annotation interface declaring a single element `message()` with
a `default ""` value.

```java
// Test.java (source)
import java.lang.annotation.*;
public @interface Test {
    String message() default "";
}
```

After compilation the class file has exactly one method (`message`) with an
`AnnotationDefault` attribute.  In the hand-crafted class, the `attribute_length` field
of that attribute was set to `0` using a binary editor.  Per the JVM Specification
(§4.7.22), `attribute_length` for `AnnotationDefault` must be at least 1 (it must
contain a complete `element_value`).  A value of `0` is structurally illegal.

### Binary structure of the defect (simplified)

```
method "message"
  attribute "AnnotationDefault"
    attribute_length = 0x00000000   ← should be >= 1
    <empty body>                    ← ASM tries to read from here → AIOBE
```

### How the bug manifested (before fix)

ASM's `ClassReader` reads the declared `attribute_length` and advances its internal
read pointer into the class-file byte array accordingly.  When it subsequently calls
`ClassReader.readUnsignedShort()` or `ClassReader.readByte()` to parse the
`element_value` that should follow, it reads past the end of the attribute data
(or into the wrong region of the class file), producing a constant-pool index that
is out of range.  This ultimately throws an `ArrayIndexOutOfBoundsException` deep
inside ASM, which escaped `Segment.processClasses()` uncaught, crashing the packer.

Additionally, the ASM internal chain that read the attribute could call
`Pack200ClassReader.readUnsignedShort(index)` with `index == 0`.  The override
accessed `b[index - 1]` without a bounds check, so even reaching that point would
have thrown `ArrayIndexOutOfBoundsException` independently.

### Expected packer output (from `AttributeTests.java`)

```
WARNING: Passing class file uncompressed due to unknown class format: Test.class
```

---

## Fixes applied

### 1. `Pack200ClassReader.readUnsignedShort` — latent out-of-bounds access

**File:** `pack/src/main/java/org/apache/harmony/pack200/Pack200ClassReader.java`

The override reads `b[index - 1]` to detect whether the preceding bytecode was an
`ldc_w` (opcode 19).  This is valid for any normally encoded instruction stream, but
when ASM navigates constant-pool entries or attribute data it may call
`readUnsignedShort(0)`, at which point `b[0 - 1]` = `b[-1]` throws
`ArrayIndexOutOfBoundsException`.

**Fix:** added an `index > 0` guard before the array access.

```java
// Before
if (b[index - 1] == 19) { ... }

// After
if (index > 0 && b[index - 1] == 19) { ... }
```

The companion comment on `readUTF8`'s `offset == 0` guard was also corrected: that
check is semantically correct per JVM spec §4.1 (constant-pool index 0 is always null),
not a workaround for the `readUnsignedShort` bug.

### 2. `Segment.processClasses` — uncaught `ArrayIndexOutOfBoundsException` and missing log for `PassException`

**File:** `pack/src/main/java/org/apache/harmony/pack200/Segment.java`

The `try/catch` in `processClasses` only caught `PassException` (silently) and
`ArrayIndexOutOfBoundsException` separately.  Any other `RuntimeException` thrown by
ASM while parsing malformed attribute data escaped entirely and terminated the packer.
In addition, the `PassException` path logged nothing, giving no indication of why a
class was passed through.

**Fix:** replaced the two separate catch blocks with a single `catch (RuntimeException)`
block that logs a diagnostic warning message before delegating to the shared
`passClassThrough()` helper.  `PassException` (indicating an unrecognised attribute)
produces a distinct message from all other exceptions (indicating a structurally invalid
class file).

```java
} catch (RuntimeException e) {
    if (e instanceof PassException) {
        PackingUtils.log("WARNING: Passing class file uncompressed due to unrecognized attribute: "
                + classReader.getFileName());
    } else {
        PackingUtils.log("WARNING: Passing class file uncompressed due to unknown class format: "
                + classReader.getFileName());
    }
    passClassThrough(segmentUnit, classReader, e);
}
```

### 3. `Segment.passClassThrough` — extracted helper (refactor)

The pass-through logic (remove from class bands, register as a pass-file, restore raw
bytes) was duplicated inside the `catch (PassException)` block.  It was extracted into
a private `passClassThrough(SegmentUnit, Pack200ClassReader, Exception)` method,
eliminating the duplication and providing a single consistent entry point for both
exception types.

---

## Why BcBands state is not corrupted on pass-through

A potential concern when a `PassException` or other `RuntimeException` fires
mid-visit is whether partially written bytecode bands (`BcBands`) could be left in an
inconsistent state.

For `Test.class`:
* The `message()` method is an **abstract** annotation interface method — it has no
  `Code` attribute.
* ASM therefore never calls `visitCode()`, `visitInsn()`, etc.
* `BcBands` is never written for this class.

For `Foo.class`:
* The exception fires during class-level attribute visiting (`visitAttribute`), before
  any method visiting begins.
* `BcBands` is again never written for this class.

`ClassBands.removeCurrentClass()` reverses all class-level and field/method metadata
accumulated since `addClass()` was called, which is sufficient for both cases.

---

## References

* `AttributeTests.java` — JDK test that exercises `badattr.jar` (JDK bug 6746111, 8005252, 8008262)
* `Readme.txt` inside `badattr.jar` — original description of the two defects
* JVM Specification §4.7.22 — `AnnotationDefault` attribute structure
* JVM Specification §4.1 — constant-pool index 0 is always null
