package com.irionna.eternalreturninfo.retrofit

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class FBRef {
    companion object {
        private val database = FirebaseDatabase.getInstance()
        val auth = FirebaseAuth.getInstance()

        val postRef = database.getReference("post")
        val userRef = database.getReference("user")
        val seasonRef = database.getReference("seasonID")
    }
}