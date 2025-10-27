package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.UsuarioDao
import br.edu.fatecpg.valletprojeto.model.Usuario

class CadastroViewModel : ViewModel() {
    private val usuarioDao = UsuarioDao()

    fun cadastrarUsuario(
        usuario: Usuario,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        usuarioDao.cadastrarUsuario(usuario, onSuccess, onError)
    }
}
