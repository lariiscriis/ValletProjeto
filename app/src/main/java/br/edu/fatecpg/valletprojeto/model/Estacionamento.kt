package br.edu.fatecpg.valletprojeto.model

data class Estacionamento(
    var id: String = "",
    val nome: String = "",
    val cnpj: String = "",
    val telefone: String = "",
    val adminUid: String? = null,
    val adminEmail: String = "",
    val dataCadastro: Any? = null,
    val endereco: String = "",
    val cep: String = "",
    val tiposVagasComum: Boolean = false,
    val tiposVagasIdosoPcd: Boolean = false,
    val quantidadeVagasComum: Int? = null,
    val quantidadeVagasIdosoPcd: Int? = null,
    val possuiCobertura: Boolean = false,
    val numeroPavimentos: Int? = null,
    val valorDiario: Double = 0.0,
    val tempoMaxReservaHoras: Int = 2,
    val toleranciaReservaMinutos: Int = 10,
    val fotoEstacionamentoUri: String? = null,
    var vagasDisponiveis: Int? = null,
    var distanciaMetros: Int? = null,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
    val quantidadeVagasTotal: Int? = null,
    val horarioAbertura: String? = null,
    val horarioFechamento: String? = null,
    val valorHora: Double? = null,
    val geohash: String? = null,

)
{
    fun estaAberto(): Boolean {
        return try {
            val horaAbertura = horarioAbertura ?: return true
            val horaFechamento = horarioFechamento ?: return true

            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val agora = sdf.parse(sdf.format(java.util.Date()))
            val abertura = sdf.parse(horaAbertura)
            val fechamento = sdf.parse(horaFechamento)

            if (abertura != null && fechamento != null && agora != null) {
                if (fechamento.before(abertura)) {
                    agora.after(abertura) || agora.before(fechamento)
                } else {
                    agora.after(abertura) && agora.before(fechamento)
                }
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }
}
