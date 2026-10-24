package com.example.dacs3

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue

/** Every operation rechecks current server data; names are display values, never identity. */
object RentalStore {
    private val db get()=Backend.db
    private fun uid()=checkNotNull(Backend.auth.currentUser?.uid) { "Vui lòng đăng nhập" }
    fun request(property: Property): Task<Void> {
        val me=uid(); val ref=db.collection("rental_requests").document(RentalValidation.requestId(property.id,me))
        return db.runTransaction { tx ->
            val room=Property.from(tx.get(db.collection("properties").document(property.id)))
            val previous=tx.get(ref)
            val profile=tx.get(db.collection("Users").document(me))
            require(room.hostId.isNotBlank() && room.hostId!=me) { "Phòng chưa có thông tin chủ nhà hợp lệ" }
            require(room.status=="Trống" && room.tenantUid.isBlank()) { "Phòng hiện không còn trống" }
            require(!previous.exists() || previous.getString("status") in listOf("rejected","cancelled")) { "Bạn đã gửi yêu cầu cho phòng này" }
            tx.set(ref,mapOf("propertyId" to room.id,"roomName" to room.title,"hostId" to room.hostId,"tenantUid" to me,
                "tenantName" to (profile.getString("fullName") ?: "Người thuê"),"status" to "pending","createdAt" to FieldValue.serverTimestamp()))
            null
        }
    }
    fun decideRequest(id: String, accept: Boolean): Task<Void> {
        val me=uid(); val ref=db.collection("rental_requests").document(id)
        return db.runTransaction { tx ->
            val request=tx.get(ref); require(request.getString("hostId")==me && request.getString("status")=="pending") { "Yêu cầu đã thay đổi" }
            val roomRef=db.collection("properties").document(request.getString("propertyId")!!)
            val room=Property.from(tx.get(roomRef))
            require(room.hostId==me) { "Bạn không quản lý phòng này" }
            if(accept) {
                require(room.tenantUid.isBlank() && room.status=="Trống") { "Phòng đã có người thuê hoặc đang bảo trì" }
                val tenant=request.getString("tenantUid")!!
                tx.update(roomRef,mapOf("tenantUid" to tenant,"status" to "Đã thuê","rentalRequestId" to id))
                tx.set(db.collection("handovers").document(id),mapOf("propertyId" to room.id,"requestId" to id,
                    "roomName" to room.title,"hostId" to me,"tenantUid" to tenant,"tenantName" to (request.getString("tenantName") ?: "Người thuê"),
                    "assetCheck" to "Kiểm tra phòng và tài sản thực tế trước khi ký nhận.","completed" to false,"createdAt" to FieldValue.serverTimestamp()))
            }
            tx.update(ref,mapOf("status" to if(accept) "accepted" else "rejected","updatedAt" to FieldValue.serverTimestamp()))
            null
        }
    }
    fun createBill(propertyId: String, month: String, rent: Long, electricity: Long, water: Long): Task<Void> {
        val me=uid(); val period=requireNotNull(RentalValidation.month(month)) { "Tháng có dạng MM/YYYY" }
        val total=requireNotNull(RentalValidation.total(rent,electricity,water)) { "Số tiền không hợp lệ" }
        return db.runTransaction { tx ->
            val p=Property.from(tx.get(db.collection("properties").document(propertyId)))
            require(p.hostId==me && p.tenantUid.isNotBlank()) { "Chỉ tạo hóa đơn cho phòng của bạn đang có người thuê" }
            val ref=db.collection("bills").document(RentalValidation.billId(p.id,p.tenantUid,period))
            require(!tx.get(ref).exists()) { "Phòng này đã có hóa đơn cho tháng $period" }
            tx.set(ref,mapOf("propertyId" to p.id,"roomName" to p.title,"hostId" to me,"tenantUid" to p.tenantUid,
                "month" to period,"monthKey" to "${period.substring(3)}-${period.substring(0,2)}","roomFee" to rent,"electricityFee" to electricity,"waterFee" to water,
                "totalAmount" to total,"status" to "Chưa thanh toán","createdAt" to FieldValue.serverTimestamp()))
            null
        }
    }
    fun payment(id: String, confirm: Boolean): Task<Void> {
        val me=uid();val ref=db.collection("bills").document(id)
        return db.runTransaction { tx ->
            val bill=tx.get(ref); val old=bill.getString("status")
            if(confirm) require(bill.getString("hostId")==me && old=="Chờ xác nhận") { "Hóa đơn chưa được báo thanh toán" }
            else require(bill.getString("tenantUid")==me && old=="Chưa thanh toán") { "Hóa đơn đã thay đổi" }
            tx.update(ref,mapOf("status" to if(confirm) "Đã thanh toán" else "Chờ xác nhận","updatedAt" to FieldValue.serverTimestamp()))
            null
        }
    }
}
