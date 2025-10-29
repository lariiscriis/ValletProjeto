    package br.edu.fatecpg.valletprojeto

    import android.net.Uri
    import android.os.Bundle
    import android.view.View
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import br.edu.fatecpg.valletprojeto.databinding.ActivityEditarPerfilAdministradorBinding
    import com.bumptech.glide.Glide
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import okhttp3.*
    import okhttp3.MediaType.Companion.toMediaTypeOrNull
    import okhttp3.RequestBody.Companion.asRequestBody
    import java.io.File
    import java.io.FileOutputStream
    import java.io.IOException

    class EditarPerfilAdministradorActivity : AppCompatActivity() {

        private lateinit var binding: ActivityEditarPerfilAdministradorBinding
        private val db = FirebaseFirestore.getInstance()
        private val auth = FirebaseAuth.getInstance()

        private var fotoSelecionadaUri: Uri? = null
        private var docIdUsuario: String? = null
        private var nomeEstacionamento: String? = null

        // Cloudinary
        private val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dkmbs6lyk/image/upload"
        private val uploadPreset = "appEstacionamento"

        private val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    fotoSelecionadaUri = uri
                    Glide.with(this).load(uri).centerCrop().into(binding.imgFotoEditar)
                }
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityEditarPerfilAdministradorBinding.inflate(layoutInflater)
            setContentView(binding.root)

            carregarDadosAdmin()


            binding.btnAlterarFoto.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            binding.btnSalvarPerfilAdmin.setOnClickListener {
                salvarAlteracoes()
            }

            binding.btnCancelar.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Cancelar edição")
                    .setMessage("Deseja realmente sair sem salvar as alterações?")
                    .setPositiveButton("Sim") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }

        }

        private fun carregarDadosAdmin() {
            val email = auth.currentUser?.email ?: return

            binding.progressBar.visibility = android.view.View.VISIBLE

            db.collection("usuario").whereEqualTo("email", email).get()
                .addOnSuccessListener { query ->
                    if (!query.isEmpty) {
                        val doc = query.documents[0]
                        docIdUsuario = doc.id
                        nomeEstacionamento = doc.getString("nome_empresa")

                        // Dados do admin
                        binding.edtNomeAdmin.setText(doc.getString("nome") ?: "")
                        binding.edtEmailAdmin.setText(doc.getString("email") ?: "")
                        binding.edtTelefoneAdmin.setText(doc.getString("telefone") ?: "")

                        val fotoUrl = doc.getString("fotoPerfil")
                        if (!fotoUrl.isNullOrEmpty()) {
                            Glide.with(this).load(fotoUrl).centerCrop().into(binding.imgFotoEditar)
                        }

                        // Dados do estacionamento
                        nomeEstacionamento?.let { carregarDadosEstacionamento(email)
                        }
                            ?: run { binding.progressBar.visibility = android.view.View.GONE }
                    } else {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Administrador não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun carregarDadosEstacionamento(emailAdmin: String) {
            db.collection("estacionamento")
                .whereEqualTo("adminUid", auth.currentUser?.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val estDoc = querySnapshot.documents[0]
                        binding.edtNomeEstacionamento.setText(estDoc.getString("nome") ?: "")
                        binding.edtEnderecoEstacionamento.setText(estDoc.getString("endereco") ?: "")
                        binding.edtVagasEstacionamento.setText(estDoc.getLong("quantidadeVagasTotal")?.toString() ?: "")
                        binding.edtCnpj.setText(estDoc.getString("cnpj") ?: "")
                        binding.edtCep.setText(estDoc.getString("cep") ?: "")
                        binding.edtCidade.setText(estDoc.getString("cidade") ?: "")
                        binding.edtEstado.setText(estDoc.getString("estado") ?: "")
                        binding.edtValorHora.setText(estDoc.getDouble("valorHora")?.toString() ?: "")
                        binding.edtHorarioAbertura.setText(estDoc.getString("horarioAbertura") ?: "")
                        binding.edtHorarioFechamento.setText(estDoc.getString("horarioFechamento") ?: "")



                    } else {
                        Toast.makeText(this, "Nenhum estacionamento encontrado para este administrador.", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro ao buscar estacionamento.", Toast.LENGTH_SHORT).show()
                }
        }



        private fun salvarAlteracoes() {
            val nome = binding.edtNomeAdmin.text.toString()
            val email = binding.edtEmailAdmin.text.toString()
            val telefone = binding.edtTelefoneAdmin.text.toString()

            if (docIdUsuario == null) {
                Toast.makeText(this, "Erro interno: ID do usuário nulo", Toast.LENGTH_LONG).show()
                return
            }

            binding.progressBar.visibility = android.view.View.VISIBLE

            if (fotoSelecionadaUri != null) {
                uploadImagemCloudinary { imageUrl ->
                    if (imageUrl != null) {
                        atualizarFirestoreAdmin(nome, email, telefone, imageUrl)
                    } else {
                        Toast.makeText(this, "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                atualizarFirestoreAdmin(nome, email, telefone, null)
            }
        }

        private fun uploadImagemCloudinary(onComplete: (String?) -> Unit) {
            val file = File(cacheDir, "upload_admin.jpg")
            try {
                contentResolver.openInputStream(fotoSelecionadaUri!!)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Erro ao processar imagem.", Toast.LENGTH_SHORT).show()
                onComplete(null)
                return
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder()
                .url(cloudinaryUrl)
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@EditarPerfilAdministradorActivity, "Falha no upload da imagem", Toast.LENGTH_SHORT).show()
                        onComplete(null)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = response.body?.string()
                    val regex = """"secure_url":"(.*?)"""".toRegex()
                    val match = regex.find(json ?: "")
                    val imageUrl = match?.groups?.get(1)?.value?.replace("\\/", "/")
                    runOnUiThread { onComplete(imageUrl) }
                }
            })
        }

        private fun atualizarFirestoreAdmin(nome: String, email: String, telefone: String, imageUrl: String?) {
            val novosDados = mutableMapOf<String, Any>(
                "nome" to nome,
                "email" to email,
                "telefone" to telefone
            )
            if (imageUrl != null) {
                novosDados["fotoPerfil"] = imageUrl
            }

            db.collection("usuario").document(docIdUsuario!!)
                .update(novosDados)
                .addOnSuccessListener {
                    salvarEstacionamento(imageUrl)
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Erro ao atualizar administrador.", Toast.LENGTH_SHORT).show()
                }
        }


        private fun salvarEstacionamento(imageUrl: String? = null) {
            val nome = binding.edtNomeEstacionamento.text.toString().trim()
            val emailAdmin = auth.currentUser?.email ?: ""
            if (nome.isEmpty()) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Nome do estacionamento é obrigatório.", Toast.LENGTH_SHORT).show()
                return
            }

            db.collection("estacionamento")
                .whereEqualTo("adminUid", auth.currentUser?.uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val ref = snapshot.documents[0].reference
                        val novosDados = mutableMapOf<String, Any>(
                            "nome" to nome,
                            "cnpj" to binding.edtCnpj.text.toString().trim(),
                            "endereco" to binding.edtEnderecoEstacionamento.text.toString().trim(),
                            "cep" to binding.edtCep.text.toString().trim(),
                            "cidade" to binding.edtCidade.text.toString().trim(),
                            "estado" to binding.edtEstado.text.toString().trim(),
                            "quantidadeVagasTotal" to (binding.edtVagasEstacionamento.text.toString().toIntOrNull() ?: 0),
                            "horarioAbertura" to binding.edtHorarioAbertura.text.toString().trim(),
                            "horarioFechamento" to binding.edtHorarioFechamento.text.toString().trim(),
                            "valorHora" to (binding.edtValorHora.text.toString().toDoubleOrNull() ?: 0.0),
                            "emailAdmin" to emailAdmin
                        )

                        imageUrl?.let { url ->
                            novosDados["fotoEstacionamentoUri"] = url
                        }


                        ref.update(novosDados)
                            .addOnSuccessListener {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Erro ao atualizar estacionamento.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Estacionamento não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro ao acessar Firestore.", Toast.LENGTH_SHORT).show()
                }
        }


    }
