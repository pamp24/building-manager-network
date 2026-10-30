package com.buildingmanager.contact;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[^\\r\\n]*") String firstName,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[^\\r\\n]*") String lastName,
        @NotBlank @Size(max = 254) @Email @Pattern(regexp = "^[^\\s@,;<>\\\"()\\[\\]\\r\\n]+@[^\\s@,;<>\\\"()\\[\\]\\r\\n]+$") String email,
        @Size(max = 40) @Pattern(regexp = "[^\\r\\n]*") String phone,
        @NotBlank @Size(max = 5000) String message,
        @NotNull @AssertTrue Boolean privacyAccepted
) {}
