package com.totvs.startpublicprocess.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.customappkey.KeyVO;
import com.totvs.startpublicprocess.activate.oauth.Activate;
import com.totvs.startpublicprocess.util.ErrorStatus;
import com.totvs.startpublicprocess.util.RestConstant;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

@Path("/")
public class PublicPageRest {

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getVersion() {
        return Response.status(Response.Status.OK).entity("1.0.0").build();
    }

    @POST
    @Path("/{tenantId}/start-process/{processId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSearch(@PathParam("tenantId") Long tenantId, @PathParam("processId") String processId,
            @NotNull String body) {
        HttpURLConnection conn = null; 
    	
        try {
         	KeyVO key = Keyring.getKeys(tenantId, Activate.APP_KEY);
        	
            OAuthConsumer config = config(key);
            URL url = new URL(key.getDomainUrl() + "/process-management/api/v2/processes/" + processId + "/start");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(RestConstant.REQUEST_METHOD_POST);
            conn.setRequestProperty("Accept-Charset", RestConstant.UTF_8_ENCODE);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            config.sign(conn);

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());

            String response = getConnectionResponse(conn);
            int code = conn.getResponseCode();
            
            System.out.printf("RESPONSE: %d - %s: data: %s%n", code, conn.getResponseMessage(), response);

            return Response.status(code).entity(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
        } finally {
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception e) {
					System.err.printf("Ocorreu um erro ao finalizar a conexão: %s%n", e.getMessage());
				}
			}
		}
    }

    private OAuthConsumer config(KeyVO key) {
        OAuthConsumer consumer = new DefaultOAuthConsumer(key.getConsumerKey(), key.getConsumerSecret());
        consumer.setTokenWithSecret(key.getToken(), key.getTokenSecret());
        return consumer;
    }
    
    private String getConnectionResponse(HttpURLConnection conn) throws IOException {
    	Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), RestConstant.UTF_8_ENCODE));
    	
    	StringBuilder sb = new StringBuilder();
    	for (int c = reader.read(); c != -1; c = reader.read()) {
            sb.append((char) c);
        }
    	
    	return sb.toString();
    }
    
    
}
