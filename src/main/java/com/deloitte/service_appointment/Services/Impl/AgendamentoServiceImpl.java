package com.deloitte.service_appointment.Services.Impl;

import com.deloitte.service_appointment.DTOs.Agendamento.AgendamentoDashboardDTO;
import com.deloitte.service_appointment.DTOs.AgendamentoRequestDTO;
import com.deloitte.service_appointment.DTOs.AgendamentoResponseDTO;
import com.deloitte.service_appointment.DTOs.Mappers.AgendamentoMapper;
import com.deloitte.service_appointment.Entities.Agendamento;
import com.deloitte.service_appointment.Entities.Servico;
import com.deloitte.service_appointment.Entities.User;
import com.deloitte.service_appointment.Repositories.AgendamentoRepository;
import com.deloitte.service_appointment.Repositories.ServicoRepository;
import com.deloitte.service_appointment.Repositories.UserRepository;
import com.deloitte.service_appointment.Services.AgendamentoService;
import com.deloitte.service_appointment.enums.Status;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendamentoServiceImpl implements AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AgendamentoMapper mapper;

    private static final long HORAS_ANTECEDENCIA_CANCELAMENTO = 24;



    @Transactional(readOnly = true)
    @Override
    public List<AgendamentoResponseDTO> findAll() {
        List<Agendamento> agendamentos = agendamentoRepository.findAll();
        return agendamentos.stream()
                .map(AgendamentoMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public AgendamentoResponseDTO findById(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado com id " + id));
        return AgendamentoMapper.toDTO(agendamento);
    }

    @Override
    public AgendamentoResponseDTO create(AgendamentoRequestDTO entity) {

        validateHorarios(entity.getDataHoraInicio(), entity.getDataHoraFim());

        User profissional = userRepository.findById(entity.getProfissionalId())
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        Servico servico = servicoRepository.findById(entity.getServicoId())
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));
        User cliente = userRepository.findById(entity.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendamento agendamento = AgendamentoMapper.toEntity(entity);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setCliente(cliente);
        Agendamento savedAgendamento = agendamentoRepository.save(agendamento);
        return AgendamentoMapper.toDTO(savedAgendamento);
    }

    @Transactional
    @Override
    public AgendamentoResponseDTO update(Long id, AgendamentoRequestDTO entity) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado com id " + id));
        AgendamentoMapper.updateEntity(agendamento, entity);
        Agendamento updatedAgendamento = agendamentoRepository.save(agendamento);
        return AgendamentoMapper.toDTO(updatedAgendamento);
    }
    @Transactional
    @Override
    public void delete(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado com id " + id));
        agendamentoRepository.delete(agendamento);
    }



    @Transactional
    @Override
    public void cancelarAgendamentoPorCliente(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (agendamento.getStatus() == Status.CANCELADO_CLIENTE) {
            throw new IllegalStateException("Agendamento já está cancelado pelo cliente");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime horarioLimiteCancelamento = agendamento.getDataHoraInicio().minusHours(HORAS_ANTECEDENCIA_CANCELAMENTO);

        if (agora.isAfter(horarioLimiteCancelamento)) {
            throw new IllegalStateException("Não é possível cancelar o agendamento com menos de "
                    + HORAS_ANTECEDENCIA_CANCELAMENTO + " horas de antecedência");
        }

        agendamento.setStatus(Status.CANCELADO_CLIENTE);
        agendamentoRepository.save(agendamento);
    }

    @Override
    public List<AgendamentoDashboardDTO> buscarAgendamentosFuturosDoCliente(Long clienteId) {
        User cliente = userRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        List<Agendamento> agendamentos = agendamentoRepository
                .findByClienteAndDataHoraInicioAfter(cliente, LocalDateTime.now());

        return agendamentos.stream()
                .map(mapper::convertToDashboardDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgendamentoDashboardDTO> buscarAgendamentosFuturosDoProfissional(Long profissionalId) {
        LocalDateTime now = LocalDateTime.now();
        List<Agendamento> agendamentos = agendamentoRepository.findByProfissionalIdAndDataHoraInicioAfter(profissionalId, now);
        return agendamentos.stream()
                .map(mapper::convertToDashboardDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgendamentoDashboardDTO> buscarAgendamentosProfissional(Long profissionalId) {
        List<Agendamento> agendamentos = agendamentoRepository.findByProfissionalId(profissionalId);
        return agendamentos.stream()
                .map(mapper::convertToDashboardDTO)
                .collect(Collectors.toList());
    }
    @Override
    public void cancelarAgendamentoPorProfissional(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (agendamento.getStatus() == Status.CANCELADO_CLIENTE) {
            throw new IllegalStateException("Agendamento já está cancelado pelo cliente");
        }else if(agendamento.getStatus() == Status.CONCLUIDO){
            throw  new IllegalStateException("Agendamento já foi concluído");
        }

        agendamento.setStatus(Status.CANCELADO_PROFISSIONAL);
        agendamentoRepository.save(agendamento);
    }

    @Override
    public void completarAgendamentoProfissional(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (agendamento.getStatus() == Status.CANCELADO_CLIENTE) {
            throw new IllegalStateException("Agendamento já está cancelado pelo cliente");
        }else if(agendamento.getStatus() == Status.CANCELADO_PROFISSIONAL){
            throw  new IllegalStateException("Você já cancelou o agendamento");
        }

        agendamento.setStatus(Status.CONCLUIDO);
        agendamentoRepository.save(agendamento);
    }

    private void validateHorarios(LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim) {

        if (dataHoraInicio.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data e hora de início não podem estar no passado");
        }


        if (dataHoraFim.isBefore(dataHoraInicio) || dataHoraFim.isEqual(dataHoraInicio)) {
            throw new IllegalArgumentException("A data e hora de fim devem ser posteriores à data e hora de início");
        }


        Duration duration = Duration.between(dataHoraInicio, dataHoraFim);
        if (duration.toHours() < 1) {
            throw new IllegalArgumentException("O agendamento deve ter duração mínima de 1 hora");
        }
    }

}
