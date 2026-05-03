package io.myforevermusic.api.modules.auth.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthRegistrationRequest(
    @NotBlank(message = "Display name is required.")
    @Size(min = 2, max = 40, message = "Display name must be between 2 and 40 characters.")
    String displayName,

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    @Size(max = 320, message = "Email must be 320 characters or fewer.")
    String email,

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one number."
    )
    String password,

    @NotBlank(message = "Preferred platform is required.")
    String preferredPlatformId,

    boolean marketingOptIn,

    @AssertTrue(message = "Terms of service must be accepted.")
    boolean acceptedTerms,

    @AssertTrue(message = "Privacy policy must be accepted.")
    boolean acceptedPrivacyPolicy
) {
}
