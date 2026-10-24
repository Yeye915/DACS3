package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
class ConversationListActivity : FlowActivity() {
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen("Tin nhắn","Mở một phòng và chọn Liên hệ để bắt đầu trò chuyện với chủ nhà.")
        if(!requireAccount()) { finish();return }
        observe(Backend.db.collection("conversations").whereArrayContains("participants",Backend.auth.currentUser!!.uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            result.documents.sortedByDescending { it.getLong("lastTime") ?: 0 }.forEach { doc ->
                val c=card(section!!);label(doc.getString("roomName").orEmpty(),20f,c);label(doc.getString("lastText").orEmpty().ifBlank { "Chưa có tin nhắn" },14f,c)
                action("Mở trò chuyện",c) { startActivity(Intent(this,ChatActivity::class.java).putExtra("CONVERSATION_ID",doc.id)) }
            }
        }
    }
}
