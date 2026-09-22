package br.com.forum_hub.domain.usuario;

import br.com.forum_hub.infra.email.EmailService;
import br.com.forum_hub.infra.exception.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, EmailService emailService) {
        this.repository = repository;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmailIgnoreCaseAndVerificadoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado!"));
    }

    @Transactional
    public Usuario cadastrar(@Valid DadosCadastroUsuario dados) {
        Optional<Usuario> optionalUsuario = repository.findByEmailIgnoreCaseAndVerificadoTrue(dados.email());

        if(optionalUsuario.isPresent()){
            throw new RegraDeNegocioException("Já existe uma conta cadastrada com esse email ou nome de usuário!");
        }
        String senhaCriptografada = encoder.encode(dados.senha());

        Usuario usuario = new Usuario(dados, senhaCriptografada);

        emailService.enviarEmailVerificacao(usuario);
        return repository.save(usuario);
    }

    @Transactional
    public void verificarEmail(String codigo) {
        Usuario usuario = repository.findByTokenVerificacao(codigo).orElseThrow();
        usuario.verificar();
    }
}
