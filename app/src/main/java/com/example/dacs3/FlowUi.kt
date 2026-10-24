package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

fun HouseActivity.notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
fun HouseActivity.requireAccount(): Boolean {
    if (Backend.auth.currentUser != null) return true
    startActivity(Intent(this, LoginActivity::class.java).putExtra("RETURN_TO_CALLER", true))
    notice("Đăng nhập để tiếp tục")
    return false
}
fun HouseActivity.failure(error: Exception) {
    val message = when ((error as? com.google.firebase.firestore.FirebaseFirestoreException)?.code) {
        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Bạn chưa có quyền thực hiện thao tác này. Kiểm tra tài khoản và cấu hình Firebase."
        com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE -> "Chưa kết nối được máy chủ. Kiểm tra mạng và thử lại."
        else -> error.message?.takeIf { it.length < 200 } ?: "Không thể hoàn tất. Vui lòng thử lại."
    }
    notice(message)
}
abstract class FlowActivity : HouseActivity() {
    lateinit var body: LinearLayout
    lateinit var state: TextView
    private val listeners = mutableListOf<ListenerRegistration>()
    fun screen(title: String, subtitle: String = "") {
        val scroll = ScrollView(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.background)); isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(12),dp(20),dp(24)) }
        scroll.addView(body); setContentView(scroll)
        action("‹  Quay lại") { finish() }
        label(title, 26f)
        if (subtitle.isNotBlank()) label(subtitle, 14f)
        state = label("Đang tải…", 14f)
    }
    fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    fun label(text: String, size: Float = 16f, into: LinearLayout = body) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        setPadding(0,dp(8),0,dp(8)); into.addView(this)
    }
    fun action(text: String, into: LinearLayout = body, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text=text; isAllCaps=false; minHeight=dp(48)
        into.addView(this, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) })
        setOnClickListener { onClick() }
    }
    fun field(hint: String, value: String = "", type: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText = EditText(this).apply {
        this.hint=hint; setText(value); inputType=type; minHeight=dp(56)
        body.addView(this, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
    }
    fun card(into: LinearLayout = body): LinearLayout {
        val card = MaterialCardView(this).apply { radius=dp(18).toFloat(); cardElevation=0f
            setCardBackgroundColor(ContextCompat.getColor(context,R.color.surface)); strokeWidth=dp(1)
            strokeColor=ContextCompat.getColor(context,R.color.outline) }
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(12)) }
        card.addView(content); into.addView(card,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12) }); return content
    }
    fun observe(query: Query, render: (QuerySnapshot) -> Unit) {
        listeners.add(query.addSnapshotListener { data,error ->
            if (error != null) { state.text="Không tải được dữ liệu. Chạm để thử lại."; state.setOnClickListener { recreate() }; failure(error) }
            else if(data!=null) { state.text=if(data.isEmpty) "Chưa có dữ liệu." else "${data.size()} mục"; render(data) }
        })
    }
    override fun onDestroy() { listeners.forEach { it.remove() }; super.onDestroy() }
}
fun HouseActivity.chooseProperty(owned: Boolean, occupied: Boolean = false, selected: (Property) -> Unit) {
    val uid=Backend.auth.currentUser?.uid ?: run { requireAccount(); return }
    val query=if(owned) Backend.db.collection("properties").where(com.google.firebase.firestore.Filter.or(
        com.google.firebase.firestore.Filter.equalTo("hostId",uid),com.google.firebase.firestore.Filter.equalTo("hostUid",uid)))
        else Backend.db.collection("properties").whereEqualTo("tenantUid",uid)
    query.get().addOnSuccessListener { result ->
        val rooms=result.documents.map(Property::from).filter { !occupied || it.tenantUid.isNotBlank() }
        if (rooms.isEmpty()) { notice(if(owned) "Chưa có phòng phù hợp. Hãy đăng phòng hoặc chấp thuận yêu cầu thuê trước." else "Bạn chưa có phòng đang thuê."); return@addOnSuccessListener }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Chọn phòng")
            .setItems(rooms.map { "${it.title} • ${it.address}" }.toTypedArray()) { _,i -> selected(rooms[i]) }
            .setNegativeButton("Hủy",null).show()
    }.addOnFailureListener(::failure)
}
