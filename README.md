# service-appointment

## Modelo de Dados (Diagrama ER)

Este projeto modela um sistema de agendamento de serviços com usuários, serviços, disponibilidades e agendamentos.

### Diagrama de Entidades e Relacionamentos

```mermaid
erDiagram
    USER {
        int id PK
        string nome
        string email
        string senha
        enum tipoUsuario "CLIENTE, PROFISSIONAL"
    }

    SERVICO {
        int id PK
        string nome
        string descricao
        int duracaoEmMinutos
        int profissional_id FK
    }

    DISPONIBILIDADE {
        int id PK
        int profissional_id FK
        enum diaDaSemana "SEGUNDA, TERÇA, QUARTA, QUINTA, SEXTA, SÁBADO, DOMINGO"
        string horaInicio
        string horaFim
    }

    AGENDAMENTO {
        int id PK
        int cliente_id FK
        int profissional_id FK
        int servico_id FK
        datetime dataHoraInicio
        datetime dataHoraFim
        enum status "AGENDADO, CONCLUIDO, CANCELADO_CLIENTE, CANCELADO_PROFISSIONAL"
    }

    USER ||--o{ SERVICO : "profissional de"
    USER ||--o{ DISPONIBILIDADE : "profissional tem"
    USER ||--o{ AGENDAMENTO : "cliente"
    USER ||--o{ AGENDAMENTO : "profissional"
    SERVICO ||--o{ AGENDAMENTO : "serviço agendado"
