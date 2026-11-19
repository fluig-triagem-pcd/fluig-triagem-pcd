package com.totvs.startpublicprocess.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.customappkey.KeyVO;
import com.totvs.startpublicprocess.activate.oauth.Activate;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

/**
 * API publica (sem login) que abre UMA solicitacao do processo de triagem PCD.
 *
 * Seguranca:
 *  - empresa, processo e responsavel sao FIXOS aqui (o navegador nao escolhe);
 *  - so os campos conhecidos sao aceitos, validados e limitados em tamanho;
 *  - campo isca (honeypot) e limite de envios por IP;
 *  - dados de saude NAO sao gravados em log;
 *  - erros ao usuario sao genericos.
 */
@Path("/")
public class PublicPageRest {

    private static final Logger LOG = Logger.getLogger(PublicPageRest.class.getName());

    // ===================== CONFIGURACAO FIXA =====================
    private static final long TENANT_ID = 1L;
    private static final String PROCESS_ID = "triagem_pcd";
    /** Codigo do usuario que recebe a solicitacao (HOMOLOGACAO). Em producao, definir o responsavel correto. */
    private static final String TARGET_ASSIGNEE = "admdigital";
    private static final int TARGET_STATE = 0; // 0 = proxima atividade padrao

    private static final int MAX_BODY = 20000;
    private static final int LIMITE_POR_IP_HORA = 5;
    private static final int LIMITE_GERAL_HORA = 300;
    private static final long UMA_HORA = 3600000L;

    private static final Set<String> TIPOS = new HashSet<String>(
            Arrays.asList("Física", "Auditiva", "Visual", "Mental", "Múltipla"));
    private static final Set<String> ORIGENS = new HashSet<String>(
            Arrays.asList("Congênita", "Doença", "Pós-operatório", "Acidente comum", "Acidente de trabalho"));

    private static final Map<String, long[]> ENVIOS_POR_IP = new ConcurrentHashMap<String, long[]>();
    private static final long[] GERAL = new long[] { 0L, 0L }; // {inicioJanela, contagem}

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion() {
        return Response.ok("\"1.0.0\"").build();
    }

    @POST
    @Path("/pcd/enviar")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enviar(String body, @Context HttpServletRequest request) {
        try {
            if (body == null || body.length() > MAX_BODY) {
                return resposta(400, false, "Dados invalidos.", null);
            }

            JsonObject json;
            try (JsonReader reader = Json.createReader(new StringReader(body))) {
                json = reader.readObject();
            } catch (Exception e) {
                return resposta(400, false, "Dados invalidos.", null);
            }

            // campo isca: se preenchido, e robo. Finge sucesso e nao faz nada.
            if (!texto(json, "website", 100).isEmpty()) {
                return resposta(200, true, "Enviado.", null);
            }

            if (!permitido(ip(request))) {
                return resposta(429, false, "Muitas tentativas. Tente novamente mais tarde.", null);
            }

            String nome = texto(json, "nomeCompleto", 150);
            String funcao = texto(json, "funcao", 100);
            String cpf = texto(json, "cpf", 20).replaceAll("\\D", "");
            String whatsapp = texto(json, "whatsapp", 20);
            String tipo = texto(json, "tipoDeficiencia", 20);
            String origem = texto(json, "origemDeficiencia", 30);
            String dificuldades = texto(json, "dificuldades", 2000);
            String queixa = texto(json, "queixaSaude", 2000);
            String ciencia = texto(json, "ciencia", 3);

            if (nome.length() < 3) {
                return resposta(400, false, "Informe o nome completo.", null);
            }
            if (!cpfValido(cpf)) {
                return resposta(400, false, "Informe um CPF valido.", null);
            }
            if (whatsapp.replaceAll("\\D", "").length() < 10) {
                return resposta(400, false, "Informe o WhatsApp com DDD.", null);
            }
            if (!TIPOS.contains(tipo)) {
                return resposta(400, false, "Selecione o tipo de deficiencia.", null);
            }
            if (!ORIGENS.contains(origem)) {
                return resposta(400, false, "Selecione a origem da deficiencia.", null);
            }
            if (!"Sim".equals(ciencia)) {
                return resposta(400, false, "E necessario declarar ciencia e autorizar.", null);
            }

            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            fmt.setTimeZone(TimeZone.getTimeZone("America/Bahia"));

            JsonObjectBuilder campos = Json.createObjectBuilder()
                    .add("nomeCompleto", nome)
                    .add("funcao", funcao)
                    .add("cpf", formatarCpf(cpf))
                    .add("whatsapp", whatsapp)
                    .add("tipoDeficiencia", tipo)
                    .add("origemDeficiencia", origem)
                    .add("dificuldades", dificuldades)
                    .add("queixaSaude", queixa)
                    .add("ciencia", ciencia)
                    .add("dataEnvio", fmt.format(new Date()));

            String payload = Json.createObjectBuilder()
                    .add("targetState", TARGET_STATE)
                    .add("targetAssignee", TARGET_ASSIGNEE)
                    .add("subProcessTargetState", 0)
                    .add("comment", "")
                    .add("formFields", campos)
                    .build().toString();

            return iniciarProcesso(payload);

        } catch (Exception e) {
            // sem dados do formulario no log
            LOG.log(Level.SEVERE, "Falha inesperada ao enviar triagem PCD: " + e.getClass().getName());
            return resposta(500, false, "Nao foi possivel concluir o envio.", null);
        }
    }

    private Response iniciarProcesso(String payload) throws Exception {
        HttpURLConnection conn = null;
        try {
            KeyVO key = Keyring.getKeys(TENANT_ID, Activate.APP_KEY);
            OAuthConsumer consumer = new DefaultOAuthConsumer(key.getConsumerKey(), key.getConsumerSecret());
            consumer.setTokenWithSecret(key.getToken(), key.getTokenSecret());

            URL url = new URL(key.getDomainUrl() + "/process-management/api/v2/processes/" + PROCESS_ID + "/start");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            consumer.sign(conn);

            OutputStream out = conn.getOutputStream();
            out.write(payload.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            String resp = lerTudo(code >= 400 ? conn.getErrorStream() : conn.getInputStream());

            if (code >= 200 && code < 300) {
                String protocolo = null;
                try (JsonReader r = Json.createReader(new StringReader(resp))) {
                    JsonObject o = r.readObject();
                    JsonValue v = o.get("processInstanceId");
                    if (v != null && v.getValueType() != JsonValue.ValueType.NULL) {
                        protocolo = v.toString().replace("\"", "");
                    }
                } catch (Exception ignore) {
                    // resposta sem processInstanceId: segue sem protocolo
                }
                return resposta(200, true, "Enviado com sucesso.", protocolo);
            }

            // registra apenas codigo e inicio da resposta de erro (sem dados do formulario)
            LOG.log(Level.WARNING, "Fluig recusou o inicio do processo. HTTP " + code + " - "
                    + (resp.length() > 300 ? resp.substring(0, 300) : resp));
            return resposta(502, false, "Nao foi possivel concluir o envio.", null);

        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // ===================== utilitarios =====================

    private static Response resposta(int status, boolean ok, String mensagem, String protocolo) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("ok", ok).add("mensagem", mensagem);
        if (protocolo != null) {
            b.add("protocolo", protocolo);
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(b.build().toString()).build();
    }

    private static String texto(JsonObject o, String campo, int max) {
        JsonValue v = o.get(campo);
        if (v == null || v.getValueType() != JsonValue.ValueType.STRING) {
            return "";
        }
        String s = o.getString(campo);
        // remove caracteres de controle (mantem quebra de linha) e espacos nas pontas
        s = s.replaceAll("[\\p{Cntrl}&&[^\\n\\r]]", "").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String lerTudo(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        in.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String ip(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Limite simples em memoria: por IP e geral, por hora. */
    private static boolean permitido(String ip) {
        long agora = System.currentTimeMillis();
        synchronized (GERAL) {
            if (agora - GERAL[0] > UMA_HORA) {
                GERAL[0] = agora;
                GERAL[1] = 0;
            }
            if (GERAL[1] >= LIMITE_GERAL_HORA) {
                return false;
            }
            long[] e = ENVIOS_POR_IP.get(ip);
            if (e == null || agora - e[0] > UMA_HORA) {
                e = new long[] { agora, 0L };
                ENVIOS_POR_IP.put(ip, e);
            }
            if (e[1] >= LIMITE_POR_IP_HORA) {
                return false;
            }
            e[1]++;
            GERAL[1]++;
            if (ENVIOS_POR_IP.size() > 5000) {
                ENVIOS_POR_IP.clear();
            }
        }
        return true;
    }

    private static String formatarCpf(String c) {
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    private static boolean cpfValido(String c) {
        if (c == null || c.length() != 11 || c.matches("(\\d)\\1{10}")) {
            return false;
        }
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += (c.charAt(i) - '0') * (t + 1 - i);
            }
            int dv = (soma * 10) % 11;
            if (dv == 10) {
                dv = 0;
            }
            if (dv != c.charAt(t) - '0') {
                return false;
            }
        }
        return true;
    }
}
