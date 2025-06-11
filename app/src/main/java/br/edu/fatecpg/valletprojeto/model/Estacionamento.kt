data class Estacionamento(
    // Dados Básicos
    var id: String = "",
    val nome: String,
    val cnpj: String,
    val telefone: String,
    val adminEmail: String = "",
    val dataCadastro: Any? = null,

    // Localização
    val endereco: String,
    val cep: String,

    // Estrutura
    val quantidadeVagasTotal: Int,
    val tiposVagasComum: Boolean,
    val tiposVagasIdosoPcd: Boolean,
    val quantidadeVagasComum: Int?,
    val quantidadeVagasIdosoPcd: Int?,
    val possuiCobertura: Boolean,
    val numeroPavimentos: Int? = null,

    // Tarifas e Horário
    val valorHora: Double,
    val valorDiario: Double,
    val horarioFuncionamento: String,

    // Configurações Adicionais
    val tempoMaxReservaHoras: Int = 2,
    val toleranciaReservaMinutos: Int,
    val fotoEstacionamentoUri: String? = null
)
