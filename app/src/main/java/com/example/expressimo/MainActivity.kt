package com.example.expressimo

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class User(
    val displayName: String= "",
    val emojis: String= ""
)

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
class MainActivity : AppCompatActivity() {

    private companion object{
        private const val TAG = "MainActivity"
    }
    private lateinit var auth: FirebaseAuth
    // Access a Cloud Firestore instance from your Activity
    val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth
        val query = db.collection("users")
        val options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java)
            .setLifecycleOwner(this).build()

        val adapter= object: FirestoreRecyclerAdapter<User, UserViewHolder> (options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                val view = LayoutInflater.from(this@MainActivity).inflate(android.R.layout.simple_list_item_2, parent, false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvEmojis: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.displayName
                tvEmojis.text = model.emojis
            }

        }
        findViewById<RecyclerView>(R.id.rvUsers).adapter = adapter
        findViewById<RecyclerView>(R.id.rvUsers).layoutManager = LinearLayoutManager(this)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.miLogout){
            Log.i(TAG, "Logout")

            // Logout the user
            auth.signOut()
            val logoutIntent = Intent(this, LoginActivity::class.java)

            //Clear the whole backstack
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
        }
        else if(item.itemId == R.id.miEdit){
            Log.i(TAG, "Show alert dialog to edit status")
            showAlertDialog()
        }


        return super.onOptionsItemSelected(item)
    }

    inner class EmojiFilter: InputFilter{
        override fun filter(
            source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence {
            // If the added text is valid, return Source
            if(source == null || source.isBlank()){
                return ""
            }
            Log.i(TAG, "Added text $source and it has ${source.length} characters.")
            val validCharTypes = listOf(Character.SURROGATE, Character.NON_SPACING_MARK, Character.OTHER_SYMBOL).map { it.toInt() }

            //if its invalid return "" i.e empty string.
            for (inputChar in source){
                val type = Character.getType(inputChar)
                Log.i(TAG,"Input Character is of type $type" )
                if (!validCharTypes.contains(type)){
                    Toast.makeText(this@MainActivity, "Enter a valid emoji",Toast.LENGTH_SHORT).show()
                    return ""
                }
            }
            return source
        }

    }

    private fun showAlertDialog() {
        val editText = EditText(this)
        // To only restrict the user to add only emojis status and no text, also restrict the length

        // 1. Emoji Filter
        val emojiFilter= EmojiFilter()



        // 2. Length Filter
        val lengthFilter= InputFilter.LengthFilter(16)
        editText.filters= arrayOf(lengthFilter,emojiFilter)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update your Emojis")
            .setView(editText)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener{
            Log.i(TAG, "clicked on positive button")

            val emojisEntered = editText.text.toString()
            if (emojisEntered.isBlank()){
                Toast.makeText(this, "Cannot leave the status blank!",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null){
                Toast.makeText(this, "User not Signed In",Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            //Update the firestore with the new emojis
            db.collection("users").document(currentUser.uid)
                .update("emojis", emojisEntered)
            dialog.dismiss()
        }
    }
}