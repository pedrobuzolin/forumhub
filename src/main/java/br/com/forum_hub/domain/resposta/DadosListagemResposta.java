package br.com.forum_hub.domain.resposta;

import java.time.LocalDateTime;

public record DadosListagemResposta(
        Long id,
        String mensagem,
        String autor,
        LocalDateTime dataCriacao,
        Boolean solucao
) {
    public DadosListagemResposta(Resposta resposta) {
        this(resposta.getId(), resposta.getMensagem(), resposta.getAutor().getNomeCompleto(), resposta.getDataCriacao(), resposta.ehSolucao());
    }
}
