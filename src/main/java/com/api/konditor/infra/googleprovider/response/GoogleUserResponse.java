package com.api.konditor.infra.googleprovider.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response object holding the user data extracted from a validated Google ID Token.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserResponse {

    private String googleId;
    private String email;
    private String name;
}
