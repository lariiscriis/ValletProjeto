package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.edu.fatecpg.valletprojeto.dao.UsuarioDao
import br.edu.fatecpg.valletprojeto.dao.VeiculoDao

class VeiculoViewModelFactory(private val usuarioDao: UsuarioDao, private val veiculoDao: VeiculoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VeiculoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VeiculoViewModel(usuarioDao, veiculoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
