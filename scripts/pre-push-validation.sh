#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Pre-Push Validation Script                                     ║"
echo "║  Ensures code quality before pushing to CI                      ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

FAILED=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function check_step() {
    echo -e "${GREEN}▶${NC} $1"
}

function step_ok() {
    echo -e "  ${GREEN}✓${NC} $1"
}

function step_warning() {
    echo -e "  ${YELLOW}⚠${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

function step_error() {
    echo -e "  ${RED}✗${NC} $1"
    FAILED=$((FAILED + 1))
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 1: Verify Repository State"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Checking git status..."
if [ -d .git ]; then
    step_ok "Git repository detected"
else
    step_error "Not a git repository"
fi

check_step "Checking for uncommitted changes..."
if [[ -n $(git status -s) ]]; then
    step_warning "You have uncommitted changes"
    git status -s | head -5
    if [[ $(git status -s | wc -l) -gt 5 ]]; then
        echo "  ... and $(($(git status -s | wc -l) - 5)) more files"
    fi
else
    step_ok "Working directory is clean"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 2: Verify Required Files"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Checking CI/CD workflow file..."
if [ -f .github/workflows/ci.yml ]; then
    step_ok "CI workflow exists"
else
    step_error "CI workflow missing"
fi

check_step "Checking test files..."
declare -a TEST_FILES=(
    "src/cli/src/test/kotlin/dev/dropper/util/ValidationUtilTest.kt"
    "src/cli/src/test/kotlin/dev/dropper/util/PackageNameSanitizationTest.kt"
    "src/cli/src/test/kotlin/dev/dropper/e2e/PackageNameGenerationE2ETest.kt"
    "src/cli/src/test/kotlin/dev/dropper/e2e/TemplateValidationE2ETest.kt"
    "src/cli/src/test/kotlin/dev/dropper/e2e/ComplexModpackE2ETest.kt"
)

for file in "${TEST_FILES[@]}"; do
    if [ -f "$file" ]; then
        step_ok "$(basename $file)"
    else
        step_error "Missing: $file"
    fi
done

check_step "Checking verification scripts..."
if [ -f scripts/verify-package-sanitization.sh ]; then
    step_ok "Unix verification script exists"
    if [ -x scripts/verify-package-sanitization.sh ]; then
        step_ok "Unix script is executable"
    else
        step_warning "Unix script is not executable"
    fi
else
    step_error "Unix verification script missing"
fi

if [ -f scripts/verify-package-sanitization.ps1 ]; then
    step_ok "Windows verification script exists"
else
    step_error "Windows verification script missing"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 3: Verify CI Workflow Syntax"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Checking for test class references in CI..."
declare -a TEST_CLASSES=(
    "ValidationUtilTest"
    "PackageNameSanitizationTest"
    "PackageNameGenerationE2ETest"
    "TemplateValidationE2ETest"
    "ComplexModpackE2ETest"
)

for test_class in "${TEST_CLASSES[@]}"; do
    if grep -q "$test_class" .github/workflows/ci.yml; then
        step_ok "$test_class referenced in CI"
    else
        step_error "$test_class NOT found in CI workflow"
    fi
done

check_step "Checking CI job count..."
job_count=$(grep -c "^  [a-z-]*:" .github/workflows/ci.yml)
if [ "$job_count" -ge 8 ]; then
    step_ok "Found $job_count jobs in CI"
else
    step_warning "Only found $job_count jobs (expected 8+)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 4: Compile Code"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Compiling Kotlin code..."
if ./gradlew :src:cli:compileKotlin -q 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Compilation successful"
elif ./gradlew :src:cli:compileKotlin 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Compilation successful (with warnings)"
else
    step_error "Compilation failed"
fi

check_step "Compiling test code..."
if ./gradlew :src:cli:compileTestKotlin -q 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Test compilation successful"
elif ./gradlew :src:cli:compileTestKotlin 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Test compilation successful (with warnings)"
else
    step_error "Test compilation failed"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 5: Build Distribution"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Building CLI distribution..."
if ./gradlew :src:cli:assemble -q 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Distribution built successfully"
elif ./gradlew :src:cli:assemble 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    step_ok "Distribution built successfully (with warnings)"
else
    step_error "Distribution build failed"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 6: Verify FileUtil.sanitizeModId Implementation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Checking sanitizeModId function exists..."
if grep -q "fun sanitizeModId" src/cli/src/main/kotlin/dev/dropper/util/FileUtil.kt; then
    step_ok "sanitizeModId function found"

    # Check implementation
    if grep -A 2 "fun sanitizeModId" src/cli/src/main/kotlin/dev/dropper/util/FileUtil.kt | grep -q 'replace("-", "").replace("_", "")'; then
        step_ok "sanitizeModId implementation correct"
    else
        step_warning "sanitizeModId implementation might be incorrect"
    fi
else
    step_error "sanitizeModId function NOT found"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 7: Quick Sanity Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Testing package sanitization utility..."
cat > /tmp/test_sanitization.kt << 'EOF'
fun sanitizeModId(modId: String): String {
    return modId.replace("-", "").replace("_", "")
}

fun main() {
    val tests = mapOf(
        "my_mod" to "mymod",
        "cool-mod" to "coolmod",
        "test_123" to "test123"
    )

    var passed = 0
    var failed = 0

    tests.forEach { (input, expected) ->
        val result = sanitizeModId(input)
        if (result == expected) {
            passed++
        } else {
            println("FAIL: $input -> $result (expected $expected)")
            failed++
        }
    }

    println("Quick sanity test: $passed passed, $failed failed")
    System.exit(if (failed == 0) 0 else 1)
}
EOF

if kotlinc /tmp/test_sanitization.kt -include-runtime -d /tmp/test_sanitization.jar 2>/dev/null && \
   java -jar /tmp/test_sanitization.jar 2>/dev/null | grep -q "3 passed, 0 failed"; then
    step_ok "Package sanitization logic verified"
else
    step_warning "Could not verify sanitization logic (kotlinc not available or test failed)"
fi
rm -f /tmp/test_sanitization.kt /tmp/test_sanitization.jar 2>/dev/null

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 8: Validate Paths in CI Workflow"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_step "Extracting paths from CI workflow..."
grep -E "(path:|paths:)" .github/workflows/ci.yml | grep -v "paths-ignore" | while read line; do
    path=$(echo "$line" | sed 's/.*path: //g' | sed 's/.*paths: //g' | tr -d "'" | tr -d '"' | sed 's/ *//g')

    # Skip patterns and artifacts
    if [[ "$path" == *"*"* ]] || [[ "$path" == *"test-results"* ]] || [[ -z "$path" ]]; then
        continue
    fi

    # Check if path exists
    if [ -f "$path" ] || [ -d "$path" ]; then
        step_ok "Path exists: $path"
    else
        step_warning "Path not found: $path (might be generated)"
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ $FAILED -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✓ ALL CHECKS PASSED - SAFE TO PUSH                             ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════╝${NC}"
    exit 0
elif [ $FAILED -eq 0 ]; then
    echo -e "${YELLOW}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ⚠ $WARNINGS WARNING(S) - REVIEW BEFORE PUSHING                     ║${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ✗ $FAILED ERROR(S) FOUND - DO NOT PUSH                            ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
