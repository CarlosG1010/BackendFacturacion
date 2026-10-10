package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.AuditoriaEmpresa;
import system.solutions.com.api_sunat.entity.AuditoriaGlobal;
import system.solutions.com.api_sunat.repository.AuditoriaEmpresaRepository;
import system.solutions.com.api_sunat.repository.AuditoriaGlobalRepository;
import system.solutions.com.api_sunat.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AuditoriaServiceImpl implements AuditoriaService{

    private final AuditoriaGlobalRepository globalRepository;
    private final AuditoriaEmpresaRepository empresaRepository;

    @Override
    @Transactional
    public void registrarAuditoriaGlobal(AuditoriaGlobal auditoria) {
        auditoria.setFechaHora(LocalDateTime.now());
        globalRepository.save(auditoria);
    }

    @Override
    @Transactional
    public void registrarAuditoriaEmpresa(AuditoriaEmpresa auditoria) {
        auditoria.setFechaHora(LocalDateTime.now());
        empresaRepository.save(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaGlobal> consultarAuditoriaGlobal(LocalDateTime inicio, LocalDateTime fin) {
        return globalRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaEmpresa> consultarAuditoriaEmpresa(LocalDateTime inicio, LocalDateTime fin) {
        return empresaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
    }
}
