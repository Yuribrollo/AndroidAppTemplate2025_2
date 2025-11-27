package com.ifpr.androidapptemplate.ui.ai

import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.ifpr.androidapptemplate.R
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

class AiLogicFragment : Fragment() {

    private lateinit var promptInput: EditText
    private lateinit var resultText: TextView
    private lateinit var generateButton: Button
    private lateinit var imageButton: Button
    private lateinit var itemImageView: ImageView
    private lateinit var model: GenerativeModel

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ai_logic, container, false)

        // Ativa a seta de voltar
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        // Esconde menu inferior
        requireActivity().findViewById<View>(R.id.nav_view).visibility = View.GONE

        promptInput = view.findViewById(R.id.prompt_input)
        resultText = view.findViewById(R.id.result_text)
        generateButton = view.findViewById(R.id.btn_generate)
        imageButton = view.findViewById(R.id.btn_select_image)
        itemImageView = view.findViewById(R.id.bitmapImageView)

        model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                Glide.with(this).load(imageUri).into(itemImageView)
                resultText.text = "Imagem selecionada."
            }
        }

        imageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        generateButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isEmpty()) {
                resultText.text = "Digite um prompt."
                return@setOnClickListener
            }

            val drawable = itemImageView.drawable
            if (drawable != null) {
                val bitmap = itemImageView.drawToBitmap()
                generateWithImage(prompt, bitmap)
            } else {
                generateTextOnly(prompt)
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Mostra barra inferior novamente
        requireActivity().findViewById<View>(R.id.nav_view).visibility = View.VISIBLE

        // Remove seta
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun generateTextOnly(prompt: String) {
        lifecycleScope.launch {
            try {
                val response = model.generateContent(prompt)
                resultText.text = response.text ?: "Sem resposta."
            } catch (e: Exception) {
                resultText.text = "Erro: ${e.message}"
            }
        }
    }

    private fun generateWithImage(prompt: String, bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val input = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = model.generateContent(input)
                resultText.text = response.text ?: "Sem resposta."
            } catch (e: Exception) {
                resultText.text = "Erro: ${e.message}"
            }
        }
    }
}