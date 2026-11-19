package com.totvs.startpublicprocess.activate.oauth;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.component.activation.ActivationEvent;
import com.fluig.sdk.api.component.activation.ActivationListener;

/**
 * 
 * Classe de ativação para provisionar a criação de um OAuth Provider e um OAuth App
 * Essa funcionalidade está disponível apenas a partir da release 1.6.5 do fluig
 * 
 */
@Remote
@Stateless(mappedName = "activator/start-public-process", name = "activator/start-public-process")
public class Activate implements ActivationListener {

    // Será provisionado automaticamente um usuário integrado
    public static final String APP_KEY = "1234-5678-9876-5432";
    
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
        Keyring.provision(APP_KEY);
    }

}
