package com.example.dacs3

import android.graphics.Bitmap
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageMetadata
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Runs only with -PuseFirebaseEmulators=true. No production account or data is touched. */
@RunWith(AndroidJUnit4::class)
class RentalFlowIntegrationTest {
    private fun <T> wait(task: Task<T>): T=Tasks.await(task,120,TimeUnit.SECONDS)
    private fun denied(block: () -> Unit) { try { block();fail("Operation should have failed") } catch(e: java.util.concurrent.ExecutionException) { assertNotNull(e.cause) } }
    private fun contains(view: View,text: String): Boolean = (view is TextView && view.text.contains(text)) ||
        (view is ViewGroup && (0 until view.childCount).any { contains(view.getChildAt(it),text) })
    private fun awaitScreen(scenario: ActivityScenario<out HouseActivity>,text: String) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60)
        while(System.nanoTime()<deadline) {
            var found=false;scenario.onActivity { found=contains(it.findViewById(android.R.id.content),text) }
            if(found) return
            android.os.SystemClock.sleep(100)
        }
        fail("Screen did not display expected data: $text")
    }
    private fun seeScreen(type: Class<out HouseActivity>,text: String,host: Boolean=false) {
        val intent=Intent(ApplicationProvider.getApplicationContext(),type).putExtra("HOST_MODE",host)
        ActivityScenario.launch<HouseActivity>(intent).use { awaitScreen(it,text) }
    }
    @Test(timeout=900000) fun ownerAndTwoTenantsCompleteOneRentalWithPrivateDataAndPaymentConfirmation() {
        assumeTrue("Requires the isolated demo-house emulator build",BuildConfig.USE_FIREBASE_EMULATORS)
        val suffix=System.currentTimeMillis().toString();val password="local-test-only-4829"
        fun account(name: String,role: String): Pair<String,String> {
            Backend.auth.signOut();val email="$name-$suffix@example.test"
            val user=wait(Backend.auth.createUserWithEmailAndPassword(email,password)).user!!
            wait(user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()))
            wait(Backend.db.collection("Users").document(user.uid).set(mapOf("fullName" to "Tài khoản $name","phone" to "0901234567","email" to email,"role" to role)))
            return user.uid to email
        }
        fun signIn(user: Pair<String,String>) { Backend.auth.signOut();wait(Backend.auth.signInWithEmailAndPassword(user.second,password)) }
        val host=account("host","host")
        val roomRef=Backend.db.collection("properties").document()
        wait(roomRef.set(mapOf("hostId" to host.first,"hostUid" to host.first,"tenantUid" to "","title" to "Phòng kiểm thử $suffix","location" to "Hải Châu, Đà Nẵng","rental" to "Hải Châu, Đà Nẵng","price" to "2.500.000 đ/tháng","monthlyPrice" to 2500000L,"area" to "25","bedroom" to 1,"bathroom" to 1,"imageUrl" to "","rating" to 0,"status" to "Trống","latitude" to 16.047,"longitude" to 108.206)))
        val room=Property.from(wait(roomRef.get(Source.SERVER)))
        assertTrue(room.hasCoordinates)
        val tenant=account("tenant","seeker")
        assertTrue(wait(Favorites.toggle(room.id)))
        wait(RentalStore.request(room));assertEquals("",wait(roomRef.get(Source.SERVER)).getString("tenantUid"))
        val other=account("other","seeker");wait(RentalStore.request(room))
        signIn(host)
        wait(RentalStore.decideRequest(RentalValidation.requestId(room.id,tenant.first),true))
        seeScreen(RentalRequestsActivity::class.java,"Người gửi: Tài khoản tenant",true)
        denied { wait(RentalStore.decideRequest(RentalValidation.requestId(room.id,other.first),true)) }
        assertEquals(tenant.first,wait(roomRef.get(Source.SERVER)).getString("tenantUid"))
        wait(RentalStore.createBill(room.id,"6/2026",2500000,100000,50000))
        denied { wait(RentalStore.createBill(room.id,"06/2026",2500000,100000,50000)) }
        val billId=RentalValidation.billId(room.id,tenant.first,"06/2026")
        val handover=Backend.db.collection("handovers").document(RentalValidation.requestId(room.id,tenant.first))
        denied { wait(handover.update(mapOf("completed" to true,"signedAt" to FieldValue.serverTimestamp()))) }
        signIn(other)
        denied { wait(Backend.db.collection("bills").document(billId).get(Source.SERVER)) }
        denied { wait(handover.get(Source.SERVER)) }
        signIn(tenant)
        assertTrue(wait(Backend.db.collection("Users").document(tenant.first).collection("favorites").document(room.id).get(Source.SERVER)).exists())
        seeScreen(RentedRoomsActivity::class.java,room.title)
        seeScreen(TenantHandoverActivity::class.java,"Chờ người thuê kiểm tra và ký nhận")
        seeScreen(TenantBillActivity::class.java,"2.650.000 đ")
        wait(handover.update(mapOf("completed" to true,"signedAt" to FieldValue.serverTimestamp())))
        wait(RentalStore.payment(billId,false))
        assertEquals("Chờ xác nhận",wait(Backend.db.collection("bills").document(billId).get(Source.SERVER)).getString("status"))
        val conversation=Backend.db.collection("conversations").document(RentalValidation.conversation(room.id,host.first,tenant.first))
        wait(conversation.set(mapOf("participants" to listOf(host.first,tenant.first),"propertyId" to room.id,"roomName" to room.title,"createdBy" to tenant.first,"lastText" to "","lastTime" to 0L)))
        val message=conversation.collection("messages").document()
        wait(message.set(mapOf("senderId" to tenant.first,"senderName" to "Người thuê","text" to "Tôi muốn hẹn xem phòng","time" to System.currentTimeMillis())))
        val bytes=ByteArrayOutputStream().use { out -> Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply { compress(Bitmap.CompressFormat.JPEG,80,out);recycle() };out.toByteArray() }
        val image=Backend.storage.reference.child("users/${tenant.first}/uploads/test-$suffix.jpg")
        wait(image.putBytes(bytes,StorageMetadata.Builder().setContentType("image/jpeg").build()))
        assertTrue(wait(image.downloadUrl).toString().contains("test-$suffix.jpg"))
        signIn(host);assertEquals("Tôi muốn hẹn xem phòng",wait(message.get(Source.SERVER)).toObject(Message::class.java)!!.text)
        val chatIntent=Intent(ApplicationProvider.getApplicationContext(),ChatActivity::class.java).putExtra("CONVERSATION_ID",conversation.id)
        ActivityScenario.launch<ChatActivity>(chatIntent).use { chat ->
            awaitScreen(chat,"Tôi muốn hẹn xem phòng")
            chat.onActivity {
                it.findViewById<EditText>(R.id.edtMessage).setText("Local UI message")
                it.findViewById<View>(R.id.btnSend).performClick()
            }
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60)
            var sent=false
            while(System.nanoTime()<deadline) {
                chat.onActivity { sent=it.findViewById<EditText>(R.id.edtMessage).text.isEmpty() && it.findViewById<View>(R.id.btnSend).isEnabled }
                if(sent) break
                android.os.SystemClock.sleep(100)
            }
            assertTrue("Chat should clear the draft after successful persistence",sent)
            assertTrue(wait(conversation.collection("messages").get(Source.SERVER)).documents.any { it.getString("text")=="Local UI message" && it.getString("senderId")==host.first })
        }
        wait(RentalStore.payment(billId,true))
        assertEquals("Đã thanh toán",wait(Backend.db.collection("bills").document(billId).get(Source.SERVER)).getString("status"))
        signIn(other);denied { wait(message.get(Source.SERVER)) }
        Backend.auth.signOut()
    }
}
