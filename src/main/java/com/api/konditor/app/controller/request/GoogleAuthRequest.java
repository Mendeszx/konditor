package com.api.konditor.app.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload recebido pelo frontend após o usuário autenticar com o Google.
 *
 * <p>O campo {@code idToken} corresponde ao {@code credential} retornado
 * pelo Google Identity Services SDK no callback do frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    @NotBlank(message = "idToken é obrigatório")
    private String idToken;
}
