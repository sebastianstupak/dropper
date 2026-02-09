# Edge Case Testing Results

Test Date: Mon, Feb  9, 2026  4:57:03 AM
Project: Dropper CLI
Test Directory: /d/dev/minecraft-mod-versioning-example/examples/simple-mod

---

## Invalid Names - Test 1

**Test:** Mod ID with spaces
**Command:** `create item test item`
**Expected:** should_fail
**Exit Code:** 1
**Result:** PASS

**Output:**
```

FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring project ':1_21_1-fabric'.
> Version config not found at D:\dev\minecraft-mod-versioning-example\examples\simple-mod\versions\1_21_1\config.yml

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 8m 19s
```

---

## Invalid Names - Test 2

**Test:** Mod ID with uppercase
**Command:** `create item TestItem`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Invalid Names - Test 3

**Test:** Special characters
**Command:** `create item test@item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Invalid Names - Test 4

**Test:** Dots in name
**Command:** `create item test.item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Invalid Names - Test 5

**Test:** Slashes in name
**Command:** `create item test/item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Invalid Names - Test 6

**Test:** Starting with number
**Command:** `create item 123test`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Invalid Names - Test 7

**Test:** Empty string
**Command:** `create item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Length Validation - Test 8

**Test:** 65 characters
**Command:** `create item aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Length Validation - Test 9

**Test:** 100 characters
**Command:** `create item bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Reserved Keywords - Test 10

**Test:** Java keyword: class
**Command:** `create item class`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Reserved Keywords - Test 11

**Test:** Java keyword: public
**Command:** `create item public`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Reserved Keywords - Test 12

**Test:** Java keyword: void
**Command:** `create item void`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Reserved Keywords - Test 13

**Test:** Reserved: minecraft
**Command:** `create item minecraft`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Numbers Only - Test 14

**Test:** Only numbers
**Command:** `create item 12345`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Numbers Only - Test 15

**Test:** Valid with numbers
**Command:** `create item test_item_123`
**Expected:** should_pass
**Exit Code:** 127
**Result:** FAIL

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Duplicates - Test 16

**Test:** Create initial item
**Command:** `create item duplicate_test_item`
**Expected:** should_pass
**Exit Code:** 127
**Result:** FAIL

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Duplicates - Test 17

**Test:** Create same item again
**Command:** `create item duplicate_test_item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Special Chars - Test 18

**Test:** Hyphens (invalid)
**Command:** `create item test-item`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Special Chars - Test 19

**Test:** Underscores (valid)
**Command:** `create item valid_underscore_item`
**Expected:** should_pass
**Exit Code:** 127
**Result:** FAIL

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Special Chars - Test 20

**Test:** Multiple underscores
**Command:** `create item test___item`
**Expected:** should_pass
**Exit Code:** 127
**Result:** FAIL

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Special Chars - Test 21

**Test:** Starting underscore
**Command:** `create item _test`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

## Special Chars - Test 22

**Test:** Ending underscore
**Command:** `create item test_`
**Expected:** should_fail
**Exit Code:** 127
**Result:** PASS

**Output:**
```
./test-edge-cases.sh: line 45: ./gradlew: No such file or directory
```

---

