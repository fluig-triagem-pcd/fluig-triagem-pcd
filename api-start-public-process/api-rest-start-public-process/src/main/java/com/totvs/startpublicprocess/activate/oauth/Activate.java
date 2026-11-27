package com.totvs.startpublicprocess.activate.oauth;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.fluig.sdk.api.component.activation.ActivationEvent;
import com.fluig.sdk.api.component.activation.ActivationListener;

/**
 * Classe de ativacao para provisionar a criacao de um OAuth Provider e um OAuth App.
 * Disponivel a partir da release 1.6.5 do fluig.
 *
 * ATENCAO: o provisionamento cria um usuario de integracao que consome licenca.
 */
@Remote
@Stateless(mappedName = "activator/start-public-process", name = "activator/start-public-process")
public class Activate implements ActivationListener {

    // Codigo proprio deste projeto (4 pares de 4 caracteres). Gera o usuario de integracao.
    public static final String APP_KEY = "7C41-5A92-B3D8-E106";

    @Override
    public String getArtifactFileName() {
        return "api-rest-start-public-process.war";
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void install(ActivationEvent event) {
    }

    @Override
    public void disable(ActivationEvent evt) {
    }

    @Override
    public void enable(ActivationEvent evt) throws Exception {
        // credenciais OAuth agora vem do pacote (fluig-oauth.properties); nada a provisionar
    }

}
