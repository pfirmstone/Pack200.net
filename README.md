# Pack200.net

A Java implementation of the Pack200 archive format (JSR 200), extended to
support the constant-pool additions introduced in Java 7–17.  Based on the
Apache Harmony unpacker, licensed under the Apache License 2.0.

---

## Table of Contents

1. [Overview](#overview)
2. [Pack200 Standard Support](#pack200-standard-support)
   - [Core Standard (JSR 200 / JDK 5–6)](#core-standard-jsr-200--jdk-56)
   - [Java 7–8 Extensions (`archive_options` bit 3)](#java-78-extensions-archive_options-bit-3)
   - [Java 9–17 Supplementary Extensions (`archive_options` bit 13)](#java-917-supplementary-extensions-archive_options-bit-13)
3. [Archive Options Bit-Field Reference](#archive-options-bit-field-reference)
4. [Hardening Against Untrusted Input](#hardening-against-untrusted-input)
   - [1. Header-count ceiling guards](#1-header-count-ceiling-guards)
   - [2. Band-headers buffer allocation guard](#2-band-headers-buffer-allocation-guard)
   - [3. Reference-array bounds checks](#3-reference-array-bounds-checks)
   - [4. File-size long→int truncation guard](#4-file-size-longint-truncation-guard)
   - [5. Attribute-definition-count truncation guard](#5-attribute-definition-count-truncation-guard)
   - [6. Constant-pool index upper-bound check](#6-constant-pool-index-upper-bound-check)
   - [7. PopulationCodec favoured-table index guard](#7-populationcodec-favoured-table-index-guard)
   - [8. Inner-class offset upper-bound checks](#8-inner-class-offset-upper-bound-checks)
   - [9. Constant-pool offset overflow guard](#9-constant-pool-offset-overflow-guard)
   - [10. Malformed-descriptor guard](#10-malformed-descriptor-guard)
   - [11. Sanitised error reporting](#11-sanitised-error-reporting)
5. [Limit Constants](#limit-constants)
6. [Error Handling Contract](#error-handling-contract)
7. [Building and Testing](#building-and-testing)

---

## Overview

Pack200 is a JAR-compression format designed specifically for Java class files.
It exploits the structure of class files (constant pools, bytecode patterns,
attribute layouts) to achieve compression ratios typically 5–10× better than
raw gzip on a JAR.

This implementation extends the original JSR 200 specification to handle the
additional constant-pool entry types introduced by subsequent Java versions, and
adds comprehensive input validation so the unpacker can be used safely against
archives from untrusted sources.

---

## Pack200 Standard Support

### Core Standard (JSR 200 / JDK 5–6)

The core Pack200 format is defined in [JSR 200](https://jcp.org/en/jsr/detail?id=200)
and documented in the JDK 5/6 `pack200` tool.  All standard features are
supported:

| Segment-header field | Description |
|---|---|
| `cp_Utf8_count` | UTF-8 constant-pool entries |
| `cp_Int_count` | Integer constants |
| `cp_Float_count` | Float constants |
| `cp_Long_count` | Long constants |
| `cp_Double_count` | Double constants |
| `cp_String_count` | String constants |
| `cp_Class_count` | Class references |
| `cp_Signature_count` | Signature strings |
| `cp_Descr_count` | NameAndType descriptors |
| `cp_Field_count` | Field references |
| `cp_Method_count` | Method references |
| `cp_Imethod_count` | Interface-method references |

Archive versions supported:

| `archive_minver` | `archive_majver` | JDK baseline |
|---|---|---|
| 7 | 150 | JDK 5/6 (original JSR 200) |
| 1 | 160 | Extended (Java 7–8) |
| 1 | 170 | Extended (Java 9–17) |

### Java 7–8 Extensions (`archive_options` bit 3)

When `archive_options` bit 3 (`HAVE_CP_EXTRA_COUNTS`) is set, four additional
constant-pool count fields are read from the segment header:

| Field | Constant-pool type | JVM spec tag |
|---|---|---|
| `cp_MethodHandle_count` | `CONSTANT_MethodHandle` | 15 |
| `cp_MethodType_count` | `CONSTANT_MethodType` | 16 |
| `cp_BootstrapMethod_count` | Bootstrap method side table | — |
| `cp_InvokeDynamic_count` | `CONSTANT_InvokeDynamic` | 18 |

These entries are required to unpack archives containing `invokedynamic`
bytecode and lambda expressions (introduced in Java 7/8).

### Java 9–17 Supplementary Extensions (`archive_options` bit 13)

When `archive_options` bit 13 (`HAVE_CP_SUPPLEMENTARY`) is set, three further
count fields are read:

| Field | Constant-pool type | JVM spec tag | Introduced |
|---|---|---|---|
| `cp_Module_count` | `CONSTANT_Module` | 19 | Java 9 |
| `cp_Package_count` | `CONSTANT_Package` | 20 | Java 9 |
| `cp_Dynamic_count` | `CONSTANT_Dynamic` | 17 | Java 11 |

These are implemented in
`org.apache.harmony.unpack200.bytecode.CPModule`,
`org.apache.harmony.unpack200.bytecode.CPPackage`, and
`org.apache.harmony.unpack200.bytecode.CPDynamic` respectively.

> **Note**: JDK-8161256 (`CP_GROUP` / `CP_BYTES`) is tracked as future work and
> is not yet implemented.

---

## Archive Options Bit-Field Reference

The `archive_options` word in the segment header is a 14-bit field (bits 14+
are reserved and must be zero):

| Bit | Constant | Meaning |
|---|---|---|
| 0 | `HAVE_SPECIAL_FORMATS` | Band headers and attribute-definition tables are present |
| 1 | `HAVE_CP_NUMBERS` | Numeric CP counts (int/float/long/double) are present |
| 2 | `HAVE_ALL_CODE_FLAGS` | Code-attribute flags present on all methods |
| 3 | `HAVE_CP_EXTRA_COUNTS` | Java 7–8 extra CP counts present |
| 4 | `HAVE_FILE_HEADERS` | File-count and archive-size fields present |
| 5 | `DEFLATE_HINT` | Default deflate hint for file entries |
| 6 | `HAVE_FILE_MODTIME` | Per-file modification times present |
| 7 | `HAVE_FILE_OPTIONS` | Per-file option flags present |
| 8 | `HAVE_FILE_SIZE_HI` | High 32 bits of file sizes present |
| 9 | `HAVE_CLASS_FLAGS_HI` | High bits of class access flags present |
| 10 | `HAVE_FIELD_FLAGS_HI` | High bits of field access flags present |
| 11 | `HAVE_METHOD_FLAGS_HI` | High bits of method access flags present |
| 12 | `HAVE_CODE_FLAGS_HI` | High bits of code-attribute flags present |
| 13 | `HAVE_CP_SUPPLEMENTARY` | Java 9–17 supplementary CP counts present |

---

## Hardening Against Untrusted Input

All validation failures throw
`org.apache.harmony.unpack200.common.Pack200Exception` with a descriptive
message.  No raw stack traces, internal class names, or codec details are
included in exception messages propagated to callers.

### 1. Header-count ceiling guards

**File**: `SegmentHeader.java`  
**Method**: `checkCount(long value, int max, String fieldName)`

Every count/size scalar decoded from the archive header is validated against a
sane upper bound before being stored.  The helper is called for:

- All CP entry counts (`cp_Utf8_count`, `cp_Int_count`, …, `cp_Dynamic_count`)
- `file_count`, `ic_count`, `class_count`
- `band_headers_size`
- `attr_definition_count`

**Threat**: A single malicious header field with a value in the billions would
cause `OutOfMemoryError` during array allocation before any data is read.

**Fix applied**: `checkCount` throws `Pack200Exception` for any value outside
`[0, max]`.  See [Limit Constants](#limit-constants) for the ceiling values.

---

### 2. Band-headers buffer allocation guard

**File**: `SegmentHeader.java`  
**Location**: `parseArchiveSpecialCounts()` → `read()`

`band_headers_size` is checked with `checkCount` (ceiling: 1 MiB) before the
`new byte[bandHeadersSize]` allocation.

**Threat**: Without the check, a 4-byte header encoding `0x7FFFFFFF` would
immediately exhaust heap.

---

### 3. Reference-array bounds checks

**File**: `BandSet.java`  
**Methods**: `getReferences(int[], String[])` and `getReferences(int[][], String[])`

Both overloads now validate every index before accessing the reference array:

```java
if (idx < 0 || idx >= reference.length)
    throw new Pack200Exception(
        "Index " + idx + " out of bounds for reference array of length " + reference.length);
```

This mirrors the existing validation in `parseReferences()`.

**Threat**: An out-of-range index previously caused an uncaught
`ArrayIndexOutOfBoundsException` with a JVM-generated message that could expose
internal array offsets.

---

### 4. File-size long→int truncation guard

**File**: `FileBands.java`  
**Method**: `processFileBits()`

Before casting `fileSize[i]` to `int`:

```java
if (rawSize < 0 || rawSize > Integer.MAX_VALUE)
    throw new Pack200Exception("Invalid file size at index " + i + ": " + rawSize);
```

A running cumulative total is also tracked to detect zip-bomb style inflation
via many individually-reasonable file sizes:

```java
totalBytes += rawSize;
if (totalBytes < 0)   // long overflow
    throw new Pack200Exception("Total file size overflow");
```

**Threat**: A file-size of 4 GB+1 silently truncated to 1 byte; a negative
long caused `NegativeArraySizeException`.

---

### 5. Attribute-definition-count truncation guard

**File**: `SegmentHeader.java`  
**Method**: `setAttributeDefinitionCount(long value)`

Validates with `checkCount(value, MAX_ATTR_DEF_COUNT, "attr_definition_count")`
before the narrowing `(int)` cast.

**Threat**: A value > `Integer.MAX_VALUE` silently truncated by the cast.

---

### 6. Constant-pool index upper-bound check

**File**: `SegmentConstantPool.java`  
**Methods**: `getValue()`, `getConstantPoolEntry()`, `getClassSpecificPoolEntry()`, `getInitMethodPoolEntry()`

The `long → int` cast is guarded with:

```java
if (value > Integer.MAX_VALUE || value < -1)
    throw new Pack200Exception("Invalid constant pool index: " + value);
```

`ArrayIndexOutOfBoundsException` from the underlying pool arrays is caught and
converted to `Pack200Exception` to avoid leaking internal state.

**Threat**: A large positive `long` value truncated by `(int)` would alias to a
small or negative index; an `ArrayIndexOutOfBoundsException` would leak the
internal array length in the JVM's default message.

---

### 7. PopulationCodec favoured-table index guard

**File**: `codec/PopulationCodec.java`  
**Method**: `decodeInts(int n, InputStream in)`

Before accessing `favoured[index - 1]`:

```java
if (index < 1 || index > k + 1)
    throw new Pack200Exception(
        "Favoured index " + index + " out of range [1, " + (k + 1) + "]");
```

where `k + 1` is the number of entries stored in the favoured table.

**Threat**: A token value larger than the stored favourite count caused an
unchecked `ArrayIndexOutOfBoundsException`.

---

### 8. Inner-class offset upper-bound checks

**File**: `IcBands.java`  
**Method**: `read(InputStream in)`

Both the outer-class and inner-name index arrays are validated:

```java
// outer class index
int idx = icOuterClassInts[i] - 1;
if (idx < 0 || idx >= cpClass.length)
    throw new Pack200Exception(
        "ic_outer_class index " + icOuterClassInts[i]
        + " out of range for cpClass length " + cpClass.length);

// inner name index
int idx = icNameInts[i] - 1;
if (idx < 0 || idx >= cpUTF8.length)
    throw new Pack200Exception(
        "ic_name index " + icNameInts[i]
        + " out of range for cpUTF8 length " + cpUTF8.length);
```

**Threat**: A value of `Integer.MAX_VALUE` passes the existing `!= 0` check and
subtracting 1 gives `Integer.MAX_VALUE - 1`, which is far past the end of any
real array.

---

### 9. Constant-pool offset overflow guard

**File**: `CpBands.java`  
**Method**: `read(InputStream in)`

The sequential accumulation of `intOffset`, `floatOffset`, `longOffset`, … now
uses `Math.addExact()`:

```java
try {
    intOffset = cpUTF8.length;
    floatOffset = Math.addExact(intOffset, cpInt.length);
    longOffset  = Math.addExact(floatOffset, cpFloat.length);
    // … and so on for all offset fields
} catch (ArithmeticException e) {
    throw new Pack200Exception(
        "Constant pool offset overflow: total CP size exceeds Integer.MAX_VALUE");
}
```

**Threat**: If any individual CP count were near `Integer.MAX_VALUE`, the
running sum would silently overflow to a negative value and all subsequent CP
index arithmetic would access incorrect data.

---

### 10. Malformed-descriptor guard

**File**: `ClassBands.java`  
**Method**: `parseFieldAttrBands(InputStream in)`

Descriptors read from the archive are expected to contain a `:` separator
(Pack200 format: `name:type`).  Both uses of `indexOf(':')` are now guarded:

```java
int colon = desc.indexOf(':');
if (colon < 0)
    throw new Pack200Exception("Malformed field descriptor (missing ':'): " + desc);
String type = desc.substring(colon + 1);
```

**Threat**: A descriptor with no `:` previously caused
`StringIndexOutOfBoundsException` at `substring(-1 + 1)` = `substring(0)` — or
worse, at `substring(colon + 1)` when `colon == -1` evaluates to
`substring(0)`, which happens to succeed, silently misinterpreting the entire
string as the type.  The explicit check makes the condition unambiguous.

---

### 11. Sanitised error reporting

**File**: `BandSet.java`  
**Method**: `decodeBandInt(String name, InputStream in, BHSDCodec defaultCodec, int[] counts)`

The previous catch block re-threw with the cause chained:

```java
// Before
throw new RuntimeException("Problem decoding band: " + name
    + " default codec: " + defaultCodec, e);
```

This has been changed to:

```java
// After
throw new Pack200Exception("Problem decoding band: " + name);
```

The cause is dropped to avoid propagating internal codec implementation details
(class names, codec parameters) to callers.

**Threat**: The original `RuntimeException` message included `defaultCodec.toString()`,
which exposes internal BHSD codec parameters (b, h, s, d values) and can help
an attacker craft more precise inputs.

---

## Limit Constants

The following constants in `SegmentHeader` define the validation ceilings.
They are intentionally generous (well above any legitimate use) while still
being orders of magnitude below values that would exhaust typical JVM heap:

| Constant | Value | Used for |
|---|---|---|
| `MAX_CP_ENTRY_COUNT` | 2,000,000 | Every CP entry-count field |
| `MAX_CLASS_FILE_COUNT` | 1,000,000 | `file_count`, `class_count`, `ic_count` |
| `MAX_BAND_HEADERS_SIZE` | 1,048,576 (1 MiB) | `band_headers_size` |
| `MAX_ATTR_DEF_COUNT` | 65,536 | `attr_definition_count` |

These constants are `package-private` so they can be referenced by unit tests.

---

## Error Handling Contract

| Condition | Exception thrown |
|---|---|
| Archive magic bytes wrong | `java.lang.Error("Bad header")` |
| Unsupported archive version | `Pack200Exception("Invalid segment … version")` |
| Reserved option bits set | `Pack200Exception("Some unused flags are non-zero")` |
| Any header count exceeds ceiling | `Pack200Exception("Invalid <field>: <value> (must be in [0, <max>])")` |
| CP index out of range | `Pack200Exception("Constant pool index <n> out of range …")` |
| File size too large / overflow | `Pack200Exception("Invalid file size …")` |
| Reference array index out of range | `Pack200Exception("Index <n> out of bounds …")` |
| Malformed field descriptor | `Pack200Exception("Malformed field descriptor …")` |
| PopulationCodec token out of range | `Pack200Exception("Favoured index <n> out of range …")` |
| CP offset integer overflow | `Pack200Exception("Constant pool offset overflow …")` |
| Premature end of stream | `java.io.EOFException` |
| Other I/O problems | `java.io.IOException` |

All `Pack200Exception` messages include the field name and the offending value.
No internal class names, array lengths (beyond what is necessary to describe the
constraint), or codec implementation details are included.

---

## Building and Testing

```bash
# Full build with tests
mvn test -Dmaven.test.failure.ignore=true --no-transfer-progress -Dbnd.skip=true
```

> **Note**: The `-Dbnd.skip=true` flag is required because `bnd-maven-plugin`
> 5.0.0 throws `ConcurrentModificationException` on Java 17 when OSGi metadata
> generation is active.

The test suite (`unpack/src/test/java`) includes:
- Round-trip archive tests covering standard and extended CP types
- Codec unit tests (`BandSetTest`, `BcBandsTest`, `ClassBandsTest`, …)
- Constant-pool type tests (`CPDynamicTest`, `CPModuleTest`, `CPPackageTest`)
- `ArchiveTest` — end-to-end unpack of real JAR files
