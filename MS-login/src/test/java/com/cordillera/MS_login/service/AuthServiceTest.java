package com.cordillera.MS_login.service;

import com.cordillera.MS_login.dto.LoginRequest;
import com.cordillera.MS_login.dto.LoginResponse;
import com.cordillera.MS_login.dto.RegistroRequest;
import com.cordillera.MS_login.dto.UsuarioResponse;
import com.cordillera.MS_login.entity.RolEntity;
import com.cordillera.MS_login.entity.Usuario;
import com.cordillera.MS_login.kafka.AuthEventPublisher;
import com.cordillera.MS_login.repository.RolRepository;
import com.cordillera.MS_login.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RolRepository     rolRepository;
    @Mock JwtService        jwtService;
    @Mock PasswordEncoder   passwordEncoder;
    @Mock AuthEventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    private RolEntity rolAdmin;
    private RolEntity rolUsuario;
    private Usuario   usuarioActivo;

    @BeforeEach
    void setUp() {
        rolAdmin = RolEntity.builder()
                .id(1).nombre("ADMIN").activo(true)
                .createdAt(LocalDateTime.now()).build();

        rolUsuario = RolEntity.builder()
                .id(3).nombre("USUARIO").activo(true)
                .createdAt(LocalDateTime.now()).build();

        usuarioActivo = Usuario.builder()
                .id(1L)
                .nombre("Administrador del Sistema")
                .email("admin@cordillera.cl")
                .passwordHash("$2b$10$hashedPassword")
                .rol(rolAdmin)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Test
    void login_exitoso_retornaToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@cordillera.cl");
        request.setPassword("Admin123!");

        when(usuarioRepository.findByEmail("admin@cordillera.cl"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Admin123!", "$2b$10$hashedPassword"))
                .thenReturn(true);
        when(jwtService.generarToken(eq("admin@cordillera.cl"), anyMap()))
                .thenReturn("jwt.token.generado");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt.token.generado");
        assertThat(response.getTipo()).isEqualTo("Bearer");
        assertThat(response.getNombre()).isEqualTo("Administrador del Sistema");
        assertThat(response.getRol()).isEqualTo("ADMIN");
        verify(eventPublisher).publicarLogin("admin@cordillera.cl");
    }

    @Test
    void login_emailNoExiste_lanzaExcepcion() {
        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@cordillera.cl");
        request.setPassword("cualquier");

        when(usuarioRepository.findByEmail("noexiste@cordillera.cl"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void login_passwordIncorrecto_lanzaExcepcion() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@cordillera.cl");
        request.setPassword("passwordMal");

        when(usuarioRepository.findByEmail("admin@cordillera.cl"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("passwordMal", "$2b$10$hashedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    void login_usuarioInactivo_lanzaExcepcion() {
        Usuario inactivo = Usuario.builder()
                .id(2L).nombre("Bloqueado").email("bloqueado@cordillera.cl")
                .passwordHash("hash").rol(rolUsuario).activo(false)
                .createdAt(LocalDateTime.now()).build();

        LoginRequest request = new LoginRequest();
        request.setEmail("bloqueado@cordillera.cl");
        request.setPassword("algo");

        when(usuarioRepository.findByEmail("bloqueado@cordillera.cl"))
                .thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactivo");
    }

    // ── registro ───────────────────────────────────────────────────────────

    @Test
    void registro_exitoso_retornaUsuarioResponse() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Nuevo Cliente");
        request.setEmail("nuevo@cordillera.cl");
        request.setPassword("Pass1234");

        Usuario guardado = Usuario.builder()
                .id(10L).nombre("Nuevo Cliente").email("nuevo@cordillera.cl")
                .passwordHash("hash_encoded").rol(rolUsuario).activo(true)
                .createdAt(LocalDateTime.now()).build();

        when(usuarioRepository.existsByEmail("nuevo@cordillera.cl")).thenReturn(false);
        when(rolRepository.findByNombre("USUARIO")).thenReturn(Optional.of(rolUsuario));
        when(passwordEncoder.encode("Pass1234")).thenReturn("hash_encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioResponse response = authService.registro(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("nuevo@cordillera.cl");
        assertThat(response.getRol()).isEqualTo("USUARIO");
        assertThat(response.getActivo()).isTrue();
    }

    @Test
    void registro_emailDuplicado_lanzaExcepcion() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Duplicado");
        request.setEmail("admin@cordillera.cl");
        request.setPassword("Pass1234");

        when(usuarioRepository.existsByEmail("admin@cordillera.cl")).thenReturn(true);

        assertThatThrownBy(() -> authService.registro(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email ya está en uso");
    }

    // ── listarUsuarios ─────────────────────────────────────────────────────

    @Test
    void listarUsuarios_retornaListaCompleta() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioActivo));

        List<UsuarioResponse> lista = authService.listarUsuarios();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getEmail()).isEqualTo("admin@cordillera.cl");
    }

    @Test
    void listarUsuarios_sinRegistros_retornaListaVacia() {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        assertThat(authService.listarUsuarios()).isEmpty();
    }
}
