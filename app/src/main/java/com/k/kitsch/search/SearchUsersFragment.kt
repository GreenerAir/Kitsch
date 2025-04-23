package com.k.kitsch.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.search.OthersPovUtilities.ProfileOtherPovActivity

class SearchUsersFragment : Fragment() {

    // Declaración de las vistas
    private lateinit var usersRecyclerView: RecyclerView   // RecyclerView para mostrar los usuarios
    private lateinit var searchUserBar: TextInputEditText // Barra de búsqueda para ingresar el nombre de usuario
    private lateinit var userAdapter: UserAdapter        // Adaptador del RecyclerView
    private val allUsers =
        mutableListOf<UserItem>()    // Lista mutable de usuarios para almacenar todos los usuarios
    private val db = FirebaseFirestore.getInstance()   // Firebase Conector


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_users, container, false)

        usersRecyclerView = view.findViewById(R.id.usersRecyclerView)
        searchUserBar = view.findViewById(R.id.searchUserBar)

        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        userAdapter = UserAdapter(emptyList()) { userId ->

            val intent = Intent(requireContext(), ProfileOtherPovActivity::class.java).apply {
                putExtra("UsernameView", userId)
            }
            startActivity(intent)
        }
        usersRecyclerView.adapter = userAdapter

        retreiveUsers()
        setupSearch()

        return view
    }

    private fun retreiveUsers() {
        db.collection("Users")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("SearchFragment", "Firebase error", it)
                    addSampleUsers()
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val userData = mutableListOf<UserItem>()
                    for (document in querySnapshot.documents) {
                        try {
                            val username = document.getString("username") ?: ""
                            val userId = document.getString("id") ?: ""
                            val pfpId = (document.getLong("pfpIconId") ?: 1L).toInt()

                            userData.add(
                                UserItem(
                                    userId = userId,
                                    username = username,
                                    imageRes = pfpId
                                )
                            )

                            Log.d("SearchFragment", "Loaded user: $userId")
                        } catch (e: Exception) {
                            Log.e("SearchFragment", "Error parsing user", e)
                        }
                    }
                    if (userData.isEmpty()) {
                        Log.w("SearchFragment", "No users found in Firestore")
                        addSampleUsers()
                    } else {
                        updateUsersList(userData)
                    }
                }
            }
    }

    private fun updateUsersList(userList: List<UserItem>) {
        allUsers.clear()
        allUsers.addAll(userList)
        userAdapter.notifyDataSetChanged()
    }

    private fun addSampleUsers() {
        allUsers.apply {
            add(UserItem("@steelstrain", "", R.drawable.a5))
            add(UserItem("@spaghettishealthytoo", "", R.drawable.a6))
            add(UserItem("@sooyaaa__", "", R.drawable.a7))
            add(UserItem("@jennierubyjane", "", R.drawable.a8))
            add(UserItem("@rosesarerosie", "", R.drawable.a9))
            add(UserItem("@lalalalisa_m", "", R.drawable.a10))
        }
    }

    // Configura la búsqueda de usuarios
    private fun setupSearch() {
        searchUserBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Metodo que se llama mientras se está escribiendo
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            // Metodo que se llama después de que el texto ha sido cambiado
            override fun afterTextChanged(s: Editable?) {
                val query =
                    s.toString().trim() // Obtener el texto ingresado en la barra de búsqueda
                if (query.isEmpty()) {
                    // Si la búsqueda está vacía, ocultar el RecyclerView
                    usersRecyclerView.visibility = View.GONE
                } else {
                    // Si hay texto, mostrar el RecyclerView y filtrar los usuarios
                    usersRecyclerView.visibility = View.VISIBLE
                    filterUsers(query)
                }
            }
        })
    }

    // Filtra los usuarios basándose en la búsqueda
    private fun filterUsers(query: String) {
        // Filtrar usuarios que contengan el texto de búsqueda en el nombre
        val filteredUsers = allUsers.filter { user ->
            user.userId.startsWith(
                query,
                ignoreCase = true
            ) || // Empieza con el texto de búsqueda
                    user.userId.contains(
                        query,
                        ignoreCase = true
                    ) // O contiene el texto de búsqueda
        }.sortedWith(compareBy<UserItem> {
            !it.userId.startsWith(
                query,
                ignoreCase = true
            ) // Poner los que empiezan con la búsqueda al principio
        }.thenBy { it.userId }) // Ordenar alfabéticamente si no empieza con el texto de búsqueda

        userAdapter.updateUsers(filteredUsers) // Actualizar el adaptador con los usuarios filtrados
    }

}

