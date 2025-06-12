package br.edu.fatecpg.valletprojeto.service
import br.edu.fatecpg.valletprojeto.model.Endereco
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
interface ViaCepService {
    @GET("{cep}/json/")
    suspend fun buscarEndereco(@Path("cep") cep: String): Response<Endereco>
}
