package com.example.greetingcard.data

import android.net.Uri
import com.example.greetingcard.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Auth
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            db.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Firestore - Needs
    fun getNeeds(): Flow<List<Need>> = callbackFlow {
        val subscription = db.collection("needs")
            .orderBy("urgent", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val needs = snapshot?.toObjects(Need::class.java) ?: emptyList()
                trySend(needs)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addNeed(need: Need, imageUri: Uri?): Boolean {
        return try {
            val imageUrl = imageUri?.let { uploadImage(it, "needs/${UUID.randomUUID()}") }
            val docRef = db.collection("needs").document()
            docRef.set(need.copy(id = docRef.id, imageUrl = imageUrl)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateNeedDonation(needId: String, amount: Int): Boolean {
        return try {
            val docRef = db.collection("needs").document(needId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val newCollected = (snapshot.getLong("collected") ?: 0) + amount
                transaction.update(docRef, "collected", newCollected)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Firestore - Impact
    fun getImpactItems(): Flow<List<ImpactItem>> = callbackFlow {
        val subscription = db.collection("impact")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(ImpactItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addImpactItem(item: ImpactItem, beforeUri: Uri?, afterUri: Uri?): Boolean {
        return try {
            val beforeUrl = beforeUri?.let { uploadImage(it, "impact/before_${UUID.randomUUID()}") }
            val afterUrl = afterUri?.let { uploadImage(it, "impact/after_${UUID.randomUUID()}") }
            val docRef = db.collection("impact").document()
            docRef.set(item.copy(id = docRef.id, beforeUrl = beforeUrl, afterUrl = afterUrl)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Storage
    private suspend fun uploadImage(uri: Uri, path: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun logout() {
        auth.signOut()
    }
}
