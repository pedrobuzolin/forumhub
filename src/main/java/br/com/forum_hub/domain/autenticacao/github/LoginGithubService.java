package br.com.forum_hub.domain.autenticacao.github;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LoginGithubService {

    private final String clientId = "Ov23liZb8fgf2wiz93Xf";
    private final String clientSecret = "240ffb83c98a0cd83de735717c58de76644f6be6";
    private final String redirectUri = "http://localhost:8080/login/github/autorizado";
    private final RestClient restClient;

    public LoginGithubService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String gerarUrl() {
        return "https://github.com/login/oauth/authorize?" +
                "client_id=" + clientId +
                "&redirect_uri=" +  redirectUri +
                "&scope=read:user,user:email";
    }

    public String obterToken(String code) {
        Map resposta = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("code", code, "client_id", clientId, "client_secret", clientSecret,"redirect_uri", redirectUri))
                .retrieve()
                .body(Map.class);
        return resposta.get("access_token").toString();
    }

    public String obterEmail(String code) {
        String token = obterToken(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        DadosEmail[] resposta = restClient.get()
                .uri("https://api.github.com/user/emails")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(DadosEmail[].class);

        for (DadosEmail dados : resposta) {
            if (dados.primary() && dados.verified()) {
                return dados.email();
            }
        }

        return null;
    }
}
