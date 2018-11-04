package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CharActivity"
        private const val COLLECTION_KEY = "Chat"
        private const val DOCUMENT_KEY = "Message"
        private const val NAME_FIELD = "Name"
        private const val TEXT_FIELD = "Text"
    }

    private var firestore: FirebaseFirestore? = null
    private var firestoreChat: DocumentReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        //setSupportActionBar(toolbar)
        //send_message_btn.setOnClickListener { _ -> sendMessage()}
        firestore = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp objects instead of java.util.Date objects
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreChat = firestore?.collection(COLLECTION_KEY)
                ?.document(DOCUMENT_KEY)
        //realtimeUpdateListener()
    }
}
