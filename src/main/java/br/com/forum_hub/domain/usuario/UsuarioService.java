package br.com.forum_hub.domain.usuario;

import br.com.forum_hub.domain.perfil.DadosPerfil;
import br.com.forum_hub.domain.perfil.Perfil;
import br.com.forum_hub.domain.perfil.PerfilNome;
import br.com.forum_hub.domain.perfil.PerfilRepository;
import br.com.forum_hub.infra.email.EmailService;
import br.com.forum_hub.infra.exception.RegraDeNegocioException;
import br.com.forum_hub.infra.security.HierarquiaService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
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
    private final PerfilRepository perfilRepository;
    private final HierarquiaService hierarquiaService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, EmailService emailService, PerfilRepository perfilRepository, HierarquiaService hierarquiaService) {
        this.repository = repository;
        this.encoder = encoder;
        this.emailService = emailService;
        this.perfilRepository = perfilRepository;
        this.hierarquiaService = hierarquiaService;
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

        Perfil perfil = perfilRepository.findByNome(PerfilNome.ESTUDANTE);
        Usuario usuario = new Usuario(dados, senhaCriptografada, perfil);

        emailService.enviarEmailVerificacao(usuario);
        return repository.save(usuario);
    }

    @Transactional
    public void verificarEmail(String codigo) {
        Usuario usuario = repository.findByTokenVerificacao(codigo).orElseThrow();
        usuario.verificar();
    }

    public Usuario buscarPeloNomeUsuario(String nomeUsuario) {
        return repository.findByNomeUsuarioIgnoreCaseAndVerificadoTrueAndAtivoTrue(nomeUsuario)
                .orElseThrow(() -> new RegraDeNegocioException("Usuario não encontrado!"));
    }

    @Transactional
    public Usuario editarPerfil(Usuario usuario, @Valid DadosEdicaoUsuario dados) {
        usuario.alterarDados(dados);
        return repository.save(usuario);
    }

    @Transactional
    public void alterarSenha(DadosAlteracaoSenha dados, Usuario logado) {
        if(!encoder.matches(dados.senhaAtual(), logado.getPassword())){
            throw new RegraDeNegocioException("Senha digitada não confere com senha atual!");
        }

        if(!dados.novaSenha().equals(dados.novaSenhaConfirmada())){
            throw new RegraDeNegocioException("Senha e confirmação não conferem!");
        }

        String senhaCriptografada = encoder.encode(dados.novaSenha());
        logado.alterarSenha(senhaCriptografada);
        repository.save(logado);
    }

    @Transactional
    public void desativarUsuario(Long id, Usuario logado) {
        Usuario usuario = repository.findById(id).orElseThrow();

        if(hierarquiaService.usuarioNaoTemPermissoes(logado, usuario, "ROLE_ADMIN"))
            throw new AccessDeniedException("Não é possivel realizar essa operação!");

        usuario.desativar();
    }

    @Transactional
    public Usuario adicionarPerfil(Long id, @Valid DadosPerfil dados) {
        Usuario usuario = repository.findById(id).orElseThrow();
        Perfil perfil = perfilRepository.findByNome(dados.perfilNome());
        usuario.adicionarPerfil(perfil);
        return usuario;
    }

    @Transactional
    public Usuario removerPerfil(Long id, @Valid DadosPerfil dados) {
        Usuario usuario = repository.findById(id).orElseThrow();
        Perfil perfil = perfilRepository.findByNome(dados.perfilNome());
        usuario.removerPerfil(perfil);
        return usuario;
    }

    public void reativarUsuario(Long id) {
        var usuario = repository.findById(id).orElseThrow();
        usuario.reativar();
    }
}
