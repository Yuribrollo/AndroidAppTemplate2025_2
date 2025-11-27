package com.ifpr.androidapptemplate.ui.ai

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.ifpr.androidapptemplate.R
import kotlinx.coroutines.launch

class AiLogicActivity : AppCompatActivity() {

    private lateinit var promptInput: EditText
    private lateinit var resultText: TextView
    private lateinit var generateButton: Button
    private lateinit var imageButton: Button
    private lateinit var itemImageView: ImageView

    private lateinit var model: GenerativeModel
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_ai_logic) // você usava este layout mesmo

        // Campos da tela
        promptInput = findViewById(R.id.prompt_input)
        resultText = findViewById(R.id.result_text)
        generateButton = findViewById(R.id.btn_generate)
        imageButton = findViewById(R.id.btn_select_image)
        itemImageView = findViewById(R.id.bitmapImageView)

        // Modelo original
        model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        // Seleção de imagem
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                Glide.with(this).load(uri).into(itemImageView)
                resultText.text = "Imagem selecionada. Pronto para gerar."
            } else {
                resultText.text = "Nenhuma imagem selecionada."
            }
        }

        imageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Botão gerar
        generateButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()

            if (prompt.isEmpty()) {
                resultText.text = "Digite um prompt para continuar."
                return@setOnClickListener
            }

            val drawable = itemImageView.drawable
            if (drawable == null) {
                resultText.text = "Selecione uma imagem antes."
                return@setOnClickListener
            }

            val bitmap = itemImageView.drawToBitmap()
            generateFromPrompt(prompt, bitmap)
        }
    }

    private fun generateFromPrompt(prompt: String, bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val promptImage = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = model.generateContent(promptImage)
                resultText.text = response.text ?: "Nenhuma resposta recebida."
            } catch (e: Exception) {
                resultText.text = "Erro ao gerar: ${e.message}"
            }
        }
    }
}
