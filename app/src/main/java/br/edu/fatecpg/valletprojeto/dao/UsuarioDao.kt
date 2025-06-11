import br.edu.fatecpg.valletprojeto.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioDao {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun cadastrarUsuario(
        usuario: Usuario,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(usuario.email, usuario.senha)
            .addOnSuccessListener {
                val data = hashMapOf(
                    "email" to usuario.email,
                    "nome" to usuario.nome,
                    "tipo_user" to usuario.tipo_user,
                    "data_criacao" to FieldValue.serverTimestamp()
                )
                usuario.cnh?.let { data["cnh"] = it }
                usuario.nome_empresa?.let { data["nome_empresa"] = it }
                usuario.cargo?.let { data["cargo"] = it }

                db.collection("usuario").add(data)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        auth.currentUser?.delete()
                        onError(e.message ?: "Erro ao salvar usuário")
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Erro ao criar usuário")
            }
    }
}
