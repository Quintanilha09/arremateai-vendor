package com.arremateai.vendor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.arremateai.vendor.domain.*;
import com.arremateai.vendor.dto.*;
import com.arremateai.vendor.exception.BusinessException;
import com.arremateai.vendor.repository.*;
import com.arremateai.vendor.validator.EmailCorporativoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendedorService {

    private final UsuarioRepository usuarioRepository;
    private final DocumentoVendedorRepository documentoRepository;
    private final HistoricoStatusVendedorRepository historicoRepository;
    private final CodigoVerificacaoRepository codigoVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailCorporativoValidator emailCorporativoValidator;
    private final CnpjValidationService cnpjValidationService;
    private final AdminNotificationService adminNotificationService;
    private final VerificacaoService verificacaoService;
    private final DocumentStorageService documentStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, String> registrarVendedor(CadastroVendedorRequest request) {
        log.info("Iniciando cadastro de vendedor - CNPJ: {}", request.getCnpj());
        emailCorporativoValidator.validarEmailCorporativo(request.getEmailCorporativo());
        if (usuarioRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("Já existe um vendedor cadastrado com este CNPJ");
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Este email já está em uso");
        }
        if (usuarioRepository.findByEmailCorporativo(request.getEmailCorporativo()).isPresent()) {
            throw new BusinessException("Este email corporativo já está em uso");
        }
        cnpjValidationService.validarCnpj(request.getCnpj());
        try {
            String dadosJson = objectMapper.writeValueAsString(request);
            verificacaoService.enviarCodigoVerificacao(request.getEmailCorporativo(), dadosJson);
        } catch (BusinessException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao processar cadastro", e);
            throw new BusinessException("Erro ao processar cadastro. Tente novamente.");
        }
        return Map.of(
                "message", "Código de verificação enviado para o email corporativo.",
                "emailCorporativo", request.getEmailCorporativo()
        );
    }

    @Transactional
    public VendedorResponse verificarEmailCorporativo(String emailCorporativo, String codigo) {
        CodigoVerificacao cv = verificacaoService.verificarCodigo(emailCorporativo, codigo);
        if (cv == null) {
            throw new BusinessException("Código de verificação inválido ou expirado");
        }
        CadastroVendedorRequest cadastro;
        try {
            cadastro = objectMapper.readValue(cv.getDadosCadastro(), CadastroVendedorRequest.class);
        } catch (Exception e) {
            log.error("Erro ao deserializar dados do cadastro", e);
            throw new BusinessException("Erro ao processar verificação. Realize o cadastro novamente.");
        }
        Usuario vendedor = new Usuario();
        vendedor.setNome(cadastro.getNome());
        vendedor.setEmail(cadastro.getEmail());
        vendedor.setEmailCorporativo(cadastro.getEmailCorporativo());
        vendedor.setEmailCorporativoVerificado(true);
        vendedor.setSenha(passwordEncoder.encode(cadastro.getSenha()));
        vendedor.setTelefone(cadastro.getTelefone());
        vendedor.setCpf(cadastro.getCpf());
        vendedor.setCnpj(cadastro.getCnpj());
        vendedor.setRazaoSocial(cadastro.getRazaoSocial());
        vendedor.setNomeFantasia(cadastro.getNomeFantasia());
        vendedor.setInscricaoEstadual(cadastro.getInscricaoEstadual());
        vendedor.setTipo(TipoUsuario.VENDEDOR);
        vendedor.setStatusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS);
        vendedor.setAtivo(true);
        vendedor = usuarioRepository.save(vendedor);
        log.info("Vendedor criado: {}", vendedor.getId());
        adminNotificationService.notificarNovoVendedor(vendedor);
        registrarHistorico(vendedor, null, StatusVendedor.PENDENTE_DOCUMENTOS,
                "Cadastro realizado", vendedor);
        return toResponse(vendedor);
    }

    @Transactional
    public void reenviarCodigoVerificacao(String emailCorporativo) {
        CodigoVerificacao existente = codigoVerificacaoRepository
                .findFirstByEmailOrderByCreatedAtDesc(emailCorporativo.trim().toLowerCase())
                .orElseThrow(() -> new BusinessException(
                        "Nenhum cadastro pendente para este email. Realize o cadastro novamente."));
        if (existente.getDadosCadastro() == null) {
            throw new BusinessException("Erro ao reenviar código. Realize o cadastro novamente.");
        }
        verificacaoService.enviarCodigoVerificacao(emailCorporativo, existente.getDadosCadastro());
    }

    @Transactional(readOnly = true)
    public VendedorResponse buscarMeuStatus(UUID vendedorId) {
        return toResponse(buscarVendedorPorId(vendedorId));
    }

    @Transactional(readOnly = true)
    public List<DocumentoVendedorResponse> listarMeusDocumentos(UUID vendedorId) {
        return documentoRepository.findByUsuarioId(vendedorId)
                .stream().map(this::toDocResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentoVendedorResponse uploadDocumento(UUID vendedorId, TipoDocumento tipo, MultipartFile arquivo) {
        Usuario vendedor = buscarVendedorPorId(vendedorId);
        if (vendedor.getStatusVendedor() != StatusVendedor.PENDENTE_DOCUMENTOS
                && vendedor.getStatusVendedor() != StatusVendedor.REJEITADO) {
            throw new IllegalStateException("Vendedor não pode enviar documentos neste momento");
        }
        String url = documentStorageService.salvarDocumento(arquivo);
        DocumentoVendedor doc = DocumentoVendedor.builder()
                .usuario(vendedor)
                .tipo(tipo)
                .nomeArquivo(arquivo.getOriginalFilename())
                .url(url)
                .tamanhoBytes(arquivo.getSize())
                .mimeType(arquivo.getContentType())
                .status(StatusDocumento.PENDENTE)
                .build();
        doc = documentoRepository.save(doc);
        verificarDocumentosCompletos(vendedor);
        return toDocResponse(doc);
    }

    private void verificarDocumentosCompletos(Usuario vendedor) {
        boolean temCNPJ = documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.CNPJ_RECEITA);
        boolean temContratoOuMEI = documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.CONTRATO_SOCIAL)
                || documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.MEI);
        boolean temDocResponsavel = documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.RG_RESPONSAVEL)
                || documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.CNH_RESPONSAVEL);
        boolean temEndereco = documentoRepository.existsByUsuarioIdAndTipo(vendedor.getId(), TipoDocumento.COMPROVANTE_ENDERECO);

        if (temCNPJ && temContratoOuMEI && temDocResponsavel && temEndereco
                && vendedor.getStatusVendedor() == StatusVendedor.PENDENTE_DOCUMENTOS) {
            StatusVendedor anterior = vendedor.getStatusVendedor();
            vendedor.setStatusVendedor(StatusVendedor.PENDENTE_APROVACAO);
            usuarioRepository.save(vendedor);
            registrarHistorico(vendedor, anterior, StatusVendedor.PENDENTE_APROVACAO,
                    "Todos os documentos obrigatórios enviados", null);
            log.info("Vendedor {} avançou para PENDENTE_APROVACAO", vendedor.getId());
        }
    }

    private void registrarHistorico(Usuario vendedor, StatusVendedor anterior,
                                    StatusVendedor novo, String motivo, Usuario alteradoPor) {
        HistoricoStatusVendedor h = HistoricoStatusVendedor.builder()
                .usuario(vendedor)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .motivo(motivo)
                .alteradoPor(alteradoPor)
                .build();
        historicoRepository.save(h);
    }

    public Usuario buscarVendedorPorId(UUID id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor não encontrado"));
        if (u.getTipo() != TipoUsuario.VENDEDOR) {
            throw new BusinessException("Usuário não é um vendedor");
        }
        return u;
    }

    public VendedorResponse toResponse(Usuario v) {
        return VendedorResponse.builder()
                .id(v.getId())
                .nome(v.getNome())
                .email(v.getEmail())
                .cnpj(v.getCnpj())
                .razaoSocial(v.getRazaoSocial())
                .nomeFantasia(v.getNomeFantasia())
                .inscricaoEstadual(v.getInscricaoEstadual())
                .emailCorporativo(v.getEmailCorporativo())
                .emailCorporativoVerificado(v.getEmailCorporativoVerificado())
                .statusVendedor(v.getStatusVendedor())
                .motivoRejeicao(v.getMotivoRejeicao())
                .aprovadoEm(v.getAprovadoEm())
                .nomeAprovadoPor(v.getAprovadoPor() != null ? v.getAprovadoPor().getNome() : null)
                .createdAt(v.getCreatedAt())
                .build();
    }

    public DocumentoVendedorResponse toDocResponse(DocumentoVendedor d) {
        return DocumentoVendedorResponse.builder()
                .id(d.getId())
                .tipo(d.getTipo())
                .tipoDescricao(d.getTipo().getDescricao())
                .nomeArquivo(d.getNomeArquivo())
                .url(d.getUrl())
                .tamanhoBytes(d.getTamanhoBytes())
                .mimeType(d.getMimeType())
                .status(d.getStatus())
                .motivoRejeicao(d.getMotivoRejeicao())
                .nomeAnalisadoPor(d.getAnalisadoPor() != null ? d.getAnalisadoPor().getNome() : null)
                .analisadoEm(d.getAnalisadoEm())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
