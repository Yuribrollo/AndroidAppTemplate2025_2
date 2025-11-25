package com.ifpr.androidapptemplate.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.StorageReference
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Item
import com.ifpr.androidapptemplate.databinding.FragmentDashboardBinding
import android.location.Location
//import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationResult
import android.location.Geocoder
import java.util.Locale


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private lateinit var nomeEditText: EditText
    private lateinit var especieEditText: EditText
    private lateinit var dataEditText: EditText
    private lateinit var descEditText: EditText
    private lateinit var itemImageView: ImageView
    private var imageUri: Uri? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var lastKnownLocation: Location? = null
    private lateinit var locationTextView: TextView

    //TODO("Declare aqui as outras variaveis do tipo EditText que foram inseridas no layout")
    private lateinit var salvarButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val view = _binding!!.root

        // Inicializa elementos da interface
        itemImageView = view.findViewById(R.id.image_item)
        salvarButton = view.findViewById(R.id.salvarItemButton)
        selectImageButton = view.findViewById(R.id.button_select_image)
        nomeEditText = view.findViewById(R.id.NomeItemEditText)
        especieEditText = view.findViewById(R.id.EspecieEditText)
        dataEditText = view.findViewById(R.id.DataEditText)
        descEditText = view.findViewById(R.id.DescEditText)
        locationTextView = view.findViewById(R.id.locationTextView)
        val getLocationButton = view.findViewById<Button>(R.id.button_get_location)

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inicializa localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Evento do botão "Selecionar imagem"
        selectImageButton.setOnClickListener {
            openFileChooser()
        }

        // Evento do botão "Obter localização"
        getLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 100
                )
            } else {
                getCurrentLocation()
            }
        }

        // Evento do botão "Salvar"
        salvarButton.setOnClickListener {
            salvarItem()
        }

        return view
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun salvarItem() {
        //TODO("Capture aqui o conteudo que esta nos outros editTexts que foram criados")
        val nome = nomeEditText.text.toString().trim()
        val especie = especieEditText.text.toString().trim()
        val data = dataEditText.text.toString().trim()
        val desc = descEditText.text.toString().trim()

        if (nome.isEmpty() || especie.isEmpty() || data.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT)
                .show()
            return
        }
        uploadImageToFirestore()
    }


    private fun uploadImageToFirestore() {
        if (imageUri != null) {
            val inputStream = context?.contentResolver?.openInputStream(imageUri!!)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                val nome = nomeEditText.text.toString().trim()
                val especie = especieEditText.text.toString().trim()
                val data = dataEditText.text.toString().trim()
                val desc = descEditText.text.toString().trim()
                //TODO("Capture aqui o conteudo que esta nos outros editTexts que foram criados")
                val endereco = locationTextView.text.toString()
                val item = Item(nome,  especie, data, desc, base64Image, endereco)

                saveItemIntoDatabase(item)
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.data != null
        ) {
            imageUri = data.data
            Glide.with(this).load(imageUri).into(itemImageView)
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener { location ->
            location?.let {
                lastKnownLocation = it
                displayAddress(it)
            }
        }
    }

    private fun displayAddress(location: Location) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0)  // endereço completo
            } else {
                "Endereço não encontrado"
            }
            locationTextView.text = address
        } catch (e: Exception) {
            locationTextView.text = "Erro ao obter endereço: ${e.message}"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) { // o mesmo código que você usou no requestPermissions
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Permissão negada. Não é possível acessar a localização.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveItemIntoDatabase(item: Item) {
        //TODO("Altere a raiz que sera criada no seu banco de dados do realtime database.
        // Renomeie a raiz itens")
        databaseReference = FirebaseDatabase.getInstance().getReference("itens")

        // Cria uma chave unica para o novo item
        val itemId = databaseReference.push().key
        if (itemId != null) {
            databaseReference.child(auth.uid.toString()).child(itemId).setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item cadastrado com sucesso!", Toast.LENGTH_SHORT)
                        .show()
                    requireActivity().supportFragmentManager.popBackStack()
                }.addOnFailureListener {
                    Toast.makeText(context, "Falha ao cadastrar o item", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Erro ao gerar o ID do item", Toast.LENGTH_SHORT).show()
        }
    }
}