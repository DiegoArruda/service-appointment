package com.deloitte.service_appointment.Services;

import com.deloitte.service_appointment.DTOs.Agendamento.AgendamentoDashboardDTO;
import com.deloitte.service_appointment.DTOs.AgendamentoRequestDTO;
import com.deloitte.service_appointment.DTOs.AgendamentoResponseDTO;

import java.util.List;

public interface AgendamentoService extends CrudService<Long, AgendamentoResponseDTO, AgendamentoRequestDTO>{

    void cancelarAgendamentoPorCliente(Long agendamentoId);
     List<AgendamentoDashboardDTO> buscarAgendamentosFuturosDoCliente(Long clienteId);
     List<AgendamentoDashboardDTO> buscarAgendamentosFuturosDoProfissional(Long profissionalId);
     List<AgendamentoDashboardDTO> buscarAgendamentosProfissional(Long profissionalId);
    void cancelarAgendamentoPorProfissional(Long agendamentoId);
    void completarAgendamentoProfissional(Long agendamentoId);
}
