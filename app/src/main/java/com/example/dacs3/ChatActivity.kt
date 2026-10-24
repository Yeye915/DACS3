package com.example.dacs3

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityChatBinding
import com.google.firebase.firestore.ListenerRegistration

class ChatActivity : HouseActivity() {
    private lateinit var binding: ActivityChatBinding
    private val messages=mutableListOf<Message>()
    private val adapter=MessageAdapter(messages)
    private var listener: ListenerRegistration?=null
    private var id=""
    private var name="Người dùng"
    private lateinit var state: TextView
    private var ready=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityChatBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        binding.recyclerMessage.layoutManager=LinearLayoutManager(this);binding.recyclerMessage.adapter=adapter
        val root=binding.root as LinearLayout
        state=TextView(this).apply { text="Đang mở cuộc trò chuyện…";textSize=14f;setPadding(24,8,24,8) };root.addView(state,1)
        binding.btnSend.isEnabled=false
        binding.btnSend.setOnClickListener { send() }
        state.setOnClickListener { if(!ready) open() }
        open()
    }
    private fun open() {
        val user=Backend.auth.currentUser ?: return;name=user.displayName.orEmpty().ifBlank { "Người dùng" }
        Backend.db.collection("Users").document(user.uid).get().addOnSuccessListener { name=it.getString("fullName").orEmpty().ifBlank { name } }
        val existing=intent.getStringExtra("CONVERSATION_ID")
        if(existing!=null) { id=existing;listen();return }
        val room=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        val peer=intent.getStringExtra("PEER_UID").orEmpty()
        if(room==null || room.id.isBlank() || peer.isBlank() || peer==user.uid || room.hostId !in listOf(user.uid,peer)) { notice("Không tìm thấy người liên hệ hợp lệ");finish();return }
        id=RentalValidation.conversation(room.id,user.uid,peer)
        val ref=Backend.db.collection("conversations").document(id)
        Backend.db.runTransaction { tx ->
            val doc=tx.get(ref)
            if(!doc.exists()) tx.set(ref,mapOf("participants" to listOf(user.uid,peer).sorted(),"propertyId" to room.id,
                "roomName" to room.title,"createdBy" to user.uid,"lastText" to "","lastTime" to 0L))
            else require((doc.get("participants") as? List<*>)?.contains(user.uid)==true)
            null
        }.addOnSuccessListener { listen() }.addOnFailureListener { state.text="Chưa mở được cuộc trò chuyện. Chạm để thử lại.";failure(it) }
    }
    private fun listen() {
        Backend.db.collection("conversations").document(id).get().addOnSuccessListener { doc ->
            val uid=Backend.auth.currentUser?.uid
            if(!doc.exists() || (doc.get("participants") as? List<*>)?.contains(uid)!=true) { notice("Bạn không tham gia cuộc trò chuyện này");finish();return@addOnSuccessListener }
            state.text="${doc.getString("roomName").orEmpty()} • 200 tin nhắn gần nhất";ready=true;binding.btnSend.isEnabled=true
            listener?.remove()
            listener=doc.reference.collection("messages").orderBy("time").limitToLast(200).addSnapshotListener { results,error ->
                if(error!=null) { failure(error);return@addSnapshotListener }
                messages.clear();results?.documents?.forEach { messages.add(it.toObject(Message::class.java)!!) };adapter.notifyDataSetChanged()
                if(messages.isNotEmpty()) binding.recyclerMessage.scrollToPosition(messages.lastIndex)
            }
        }.addOnFailureListener { state.text="Chạm để thử lại";failure(it) }
    }
    private fun send() {
        val text=binding.edtMessage.text.toString().trim();if(text.isBlank() || !ready) return
        if(text.length>2000) { binding.edtMessage.error="Tối đa 2.000 ký tự";return }
        val uid=Backend.auth.currentUser?.uid ?: return
        binding.btnSend.isEnabled=false
        val conversation=Backend.db.collection("conversations").document(id)
        val time=System.currentTimeMillis();val batch=Backend.db.batch()
        batch.set(conversation.collection("messages").document(),mapOf("senderId" to uid,"senderName" to name,"text" to text,"time" to time))
        batch.update(conversation,mapOf("lastText" to text,"lastTime" to time))
        batch.commit().addOnSuccessListener { if(binding.edtMessage.text.toString().trim()==text) binding.edtMessage.text.clear() }
            .addOnFailureListener(::failure).addOnCompleteListener { binding.btnSend.isEnabled=true }
    }
    override fun onDestroy() { listener?.remove();super.onDestroy() }
}
