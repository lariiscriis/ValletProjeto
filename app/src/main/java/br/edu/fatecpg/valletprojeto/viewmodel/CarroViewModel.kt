package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.CarroDao
import br.edu.fatecpg.valletprojeto.model.Carro
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CarroViewModel : ViewModel() {

    fun cadastrarCarro(
        carro: Carro,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        CarroDao.cadastrarCarro(carro, onSuccess, onFailure)
    }

    fun listarCarrosDoUsuario(
        onSuccess: (List<Carro>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CarroDao.listarCarrosDoUsuario(onSuccess, onFailure)
    }

}
