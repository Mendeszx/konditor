package com.api.konditor.infra.googleprovider.impl;

import com.api.konditor.infra.googleprovider.GoogleIdentityProvider;
import com.api.konditor.infra.googleprovider.response.GoogleUserResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Implementação do port {@link GoogleIdentityProvider} usando a biblioteca
 * {@code google-api-client}.
 *
 * <p>O {@link GoogleIdTokenVerifier} é construído uma única vez no startup
 * e reutilizado em todas as requisições — ele faz cache interno das chaves
 * públicas do Google, evitando chamadas HTTP desnecessárias.
 */
@Slf4j
@Component
public class GoogleIdentityProviderImpl implements GoogleIdentityProvider {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityProviderImpl(
            @Value("${security.google.client-id}") String clientId
    ) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();

        log.info("[GOOGLE] GoogleIdTokenVerifier inicializado para client-id={}", clientId);
    }

    /**
     * Verifica a assinatura, expiração e audience do Google ID Token.
     *
     * @param idToken token JWT emitido pelo Google Identity Services
     * @return dados do usuário extraídos do token validado
     * @throws BadCredentialsException se o token for inválido, expirado ou de outro client
     */
    @Override
    public GoogleUserResponse verificar(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                log.warn("[GOOGLE] Token Google inválido ou expirado");
                throw new BadCredentialsException("Google ID Token inválido ou expirado");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            log.debug("[GOOGLE] Token verificado com sucesso para sub={}", payload.getSubject());

            return new GoogleUserResponse(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );
        } catch (GeneralSecurityException | IOException e) {
            log.error("[GOOGLE] Erro ao verificar Google ID Token", e);
            throw new BadCredentialsException("Não foi possível verificar o Google ID Token");
        }
    }
}
