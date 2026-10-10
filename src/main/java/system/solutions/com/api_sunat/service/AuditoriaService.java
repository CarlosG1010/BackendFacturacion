package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.AuditoriaEmpresa;
import system.solutions.com.api_sunat.entity.AuditoriaGlobal;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaService {

    void registrarAuditoriaGlobal(AuditoriaGlobal auditoria);
    void registrarAuditoriaEmpresa(AuditoriaEmpresa auditoria);
    List<AuditoriaGlobal> consultarAuditoriaGlobal(LocalDateTime inicio, LocalDateTime fin);
    List<AuditoriaEmpresa> consultarAuditoriaEmpresa(LocalDateTime inicio, LocalDateTime fin);

}
