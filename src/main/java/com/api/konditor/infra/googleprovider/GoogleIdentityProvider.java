package com.api.konditor.infra.googleprovider;

import com.api.konditor.infra.googleprovider.response.GoogleUserResponse;

/**
 * Port de domínio para verificação de identidade via Google.
 * Implementado pela infra em {@code GoogleIdentityProviderImpl}.
 */
public interface GoogleIdentityProvider {

    GoogleUserResponse verificar(String idToken);
}
