---
name: tdd-feature
description: Build API features using TDD - write red test, implement minimal code to pass, iterate
tags: [tdd, test, development, api]
---

# TDD Feature Development

Incremental test-driven development for Spring Boot REST API features. Write one failing test, implement just enough production code to pass it, verify, repeat.

## When to use

- Building a new REST endpoint with validation, service logic, and persistence
- Adding behavior to existing endpoints
- The user says "write this TDD-style" or "step by step with tests"

## Prerequisites

- Java 26, Maven installed
- `mvn test -Dtest=<ClassName>` works

## The TDD Loop

Repeat until the feature is complete:

### 1. Write one red test

Pick the **next smallest verifiable behavior**:

| Order | What | Example test name |
|---|---|---|
| 1st | Request validation (null, blank, length, format) | `shouldReturn400_whenPasswordIsBlank` |
| 2nd | Missing required headers/cookies | `shouldReturn400_whenCookieIsMissing` |
| 3rd | Business rule failures (mismatch, expired, not found) | `shouldReturn400_whenPasswordsDoNotMatch` |
| Last | Success path | `shouldReturn200_whenPasswordResetSuccessful` |

**Write the test, run it, confirm it fails** (red).

```bash
mvn test -Dtest=PasswordControllerTest#forgotPassword_shouldReturn400_whenPasswordIsBlank
```

Expected: test fails (class/method missing, or wrong behavior).

### 2. Implement minimal production code

Add **only** what's needed to pass this one test:

- Validation test failing? → Add `@NotBlank` / `@Size` / `@Pattern` to the DTO
- Cookie missing test failing? → Add `@CookieValue` to controller method
- Business logic test failing? → Add service method, throw the right exception
- Success test failing? → Return the actual response DTO with the message

**Run the test again** — it should pass (green).

```bash
mvn test -Dtest=PasswordControllerTest#forgotPassword_shouldReturn400_whenPasswordIsBlank
```

Expected: test passes.

### 3. Run all tests in the class

```bash
mvn test -Dtest=PasswordControllerTest
```

Expected: all pass. If new test broke an old one, fix the production code (not the old test).

### 4. Move to next behavior

Go back to step 1 with the next test case. Keep tests focused:

- **One assertion per test** when practical
- **Name tests by behavior**: `shouldReturn400_whenPasswordIsBlank`, not `testPassword1`
- **Use helper methods** for common setup: `assertValidationError(request, expectedMessage)`

## Code patterns

### Controller test structure

```java
@WebMvcTest(PasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PasswordControllerTest extends BaseControllerTest {
    
    @MockitoBean
    private PasswordService passwordService;
    
    private static final String URL = "/api/v1/auth/forgot-password";
    private static final Cookie VALID_COOKIE = new Cookie("email_verification_token", "valid-token");
    
    // Helper for validation errors
    private void assertValidationError(ForgotPasswordRequest request, String expectedMessage) throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(VALID_COOKIE)  // always send cookie — validation checks fire AFTER cookie check
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(expectedMessage)));
    }
    
    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsBlank() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("")
                .confirmPassword("Valid@1234")
                .build();
        
        assertValidationError(request, "Password is required");
    }
}
```

### Service implementation

```java
@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private final EmailVerificationTokenService tokenService;
    
    public void resetPassword(String token, ForgotPasswordRequest request) {
        // 1. Check password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }
        
        // 2. Consume token (atomic get-and-delete)
        EmailVerificationToken verified = tokenService.consumeToken(token);
        if (verified == null) {
            throw new InvalidVerificationTokenException("Invalid or expired verification token");
        }
        
        // 3. TODO: hash + persist password for verified.getEmail()
    }
}
```

## Test execution order

Spring `@Valid` fires **after** `@CookieValue` / `@PathVariable` / `@RequestParam` binding. So:

- **Validation tests** → must send cookie/headers, or you get `MISSING_COOKIE` instead of `VALIDATION_ERROR`
- **Missing cookie tests** → send no cookie, expect `MISSING_COOKIE`

## When to mock vs real call

| Layer | Mock? |
|---|---|
| Controller test → service | **Mock** — `@MockitoBean`, use `doThrow` / `when(...).thenReturn(...)` |
| Service test → repository | **Mock** — `@MockitoBean` |
| Integration test | **Real** — `@SpringBootTest`, no mocks |

Controller tests focus on: routing, request validation, response mapping, exception handling.

## Gotchas

- **Test sees `MISSING_COOKIE` when you expect `VALIDATION_ERROR`?** → Add the cookie to the request. Spring checks `@CookieValue` before `@Valid`.
- **All tests pass locally, fail in CI?** → Check test isolation — leftover mocks, shared state in `@BeforeEach`.
- **Mock not being called?** → Verify the mock is for the interface/class the controller actually injects. If controller has `PasswordService` but you mocked `PasswordServiceImpl`, Spring won't find it.

## Example session

```bash
# 1. Write red test for blank password
mvn test -Dtest=PasswordControllerTest#forgotPassword_shouldReturn400_whenPasswordIsBlank
# → Fails: field has no validation

# 2. Add @NotBlank to DTO
# Edit ForgotPasswordRequest.java, add @NotBlank(message = "Password is required")

# 3. Run test again
mvn test -Dtest=PasswordControllerTest#forgotPassword_shouldReturn400_whenPasswordIsBlank
# → Passes

# 4. Run all tests in class
mvn test -Dtest=PasswordControllerTest
# → All pass

# 5. Next test: password too short
# Repeat from step 1
```

## Definition of done for one test

- [ ] Test written and runs
- [ ] Test failed first (red)
- [ ] Minimal production code added
- [ ] Test now passes (green)
- [ ] All other tests in the class still pass
- [ ] No commented-out code, no TODOs in production code (TODOs in tests are fine for "next behavior")

## When to stop and refactor

After 3–5 tests pass, if you see duplication in production code (not tests), **refactor**:

- Extract a helper method
- Move validation logic to a shared validator class
- Consolidate exception handling

Run all tests after refactoring. If they still pass, commit. If not, revert and try smaller refactor.

## Commit strategy

One commit per red→green cycle is too granular. Instead:

- Commit after a **logical group** of tests passes (e.g. "all validation tests green")
- Commit message: `Add password reset validation` (the behavior), not `Add 5 tests`
- Include both test and production code in the same commit

---

**Next steps after feature complete:**

1. Run full test suite: `mvn test`
2. Check coverage (optional): `mvn verify` (if jacoco configured)
3. Manual smoke test via `mvn spring-boot:run` + `curl`
4. Commit, push, open PR
