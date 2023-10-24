package com.example.listadecompras

import Item
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.listadecompras.ItemAdapter
import com.example.listadecompras.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {
    private val items = mutableListOf<Item>()
    private lateinit var adapter: ItemAdapter
    private val ITEM_PREFS_KEY = "item_prefs_key"
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextItem = findViewById<EditText>(R.id.editTextItem)
        val buttonAdd = findViewById<Button>(R.id.buttonAdd)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ItemAdapter(items)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Carregue os itens do Firebase Firestore após a inicialização do adapter
        loadItemsFromFirestore()

        buttonAdd.setOnClickListener {
            val itemName = editTextItem.text.toString()
            if (itemName.isNotEmpty()) {
                val newItem = Item(System.currentTimeMillis(), itemName, false)
                items.add(newItem)
                adapter.notifyItemInserted(items.size - 1)
                editTextItem.text.clear()

                // Salve os itens no Firebase Firestore
                saveItemsToFirestore()
            } else {
                showEmptyItemAlert()
            }
        }

        val buttonDelete = findViewById<Button>(R.id.buttonDelete)
        buttonDelete.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showEmptyItemAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Erro")
        builder.setMessage("Você está tentando adicionar um item vazio.")
        builder.setPositiveButton("OK", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmação")
        builder.setMessage("Tem certeza de que deseja excluir os itens marcados?")

        builder.setPositiveButton("Sim") { _, _ ->
            // O usuário confirmou a exclusão, então você pode remover os itens marcados aqui
            removeCheckedItems()
        }

        builder.setNegativeButton("Não") { _, _ ->
            // O usuário cancelou a exclusão, não é necessário fazer nada
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun removeCheckedItems() {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.checked) {
                iterator.remove()
            }
        }
        adapter.notifyDataSetChanged()

        // Salve os itens no Firebase Firestore após a exclusão
        saveItemsToFirestore()
    }

    private fun saveItemsToFirestore() {
        val listaDeCompras = mutableListOf<Map<String, Any>>()
        for (item in items) {
            val itemData = mapOf(
                "name" to item.nome,
                "checked" to item.checked
            )
            listaDeCompras.add(itemData)
        }

        // Crie ou atualize o documento no Firestore
        db.collection("listasDeCompras")
            .document("minhaLista")
            .set(mapOf("items" to listaDeCompras))
            .addOnSuccessListener {
                // Sucesso
            }
            .addOnFailureListener { e ->
                // Tratar erro
            }
    }

    private fun loadItemsFromFirestore() {
        db.collection("listasDeCompras")
            .document("minhaLista")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val lista = document.data
                    val itemsData = lista?.get("items") as? List<Map<String, Any>>
                    if (itemsData != null) {
                        items.clear()
                        for (itemData in itemsData) {
                            val nome = itemData["name"] as? String
                            val checked = itemData["checked"] as? Boolean
                            if (nome != null && checked != null) {
                                items.add(Item(System.currentTimeMillis(), nome, checked))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { e ->
                // Tratar erro
            }
    }
}
