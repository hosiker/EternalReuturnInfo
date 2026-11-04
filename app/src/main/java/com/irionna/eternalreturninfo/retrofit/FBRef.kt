package com.irionna.eternalreturninfo.retrofit

import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.Firebase

class FBRef {
    companion object {
        private val database = Firebase.database
        val auth = Firebase.auth

        val postRef = database.getReference("post")
        val userRef = database.getReference("user")
        val seasonRef = database.getReference("seasonID")
    }
}