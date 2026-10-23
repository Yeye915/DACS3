package com.example.dacs3

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TenantBillAdapter(
    private val billList: List<Bill>
) : RecyclerView.Adapter<TenantBillAdapter.BillViewHolder>() {

    class BillViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtBillTitle)
        val status: TextView = view.findViewById(R.id.txtBillStatus)
        val roomFee: TextView = view.findViewById(R.id.txtRoomFee)
        val serviceFee: TextView = view.findViewById(R.id.txtServiceFee)
        val totalAmount: TextView = view.findViewById(R.id.txtTotalAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tenant_bill, parent, false)
        return BillViewHolder(v)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = billList[position]
        holder.title.text = "${bill.roomName} - Tháng ${bill.month}"
        holder.roomFee.text = "• Tiền phòng: ${String.format("%,d", bill.roomFee)} đ"
        holder.serviceFee.text = "• Điện nước: ${String.format("%,d", bill.electricityFee + bill.waterFee)} đ"
        holder.totalAmount.text = "${String.format("%,d", bill.totalAmount)} đ"

        holder.status.text = bill.status
        if (bill.status == "Đã thanh toán") {
            holder.status.setTextColor(Color.parseColor("#4CAF50"))
            holder.status.setBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            holder.status.setTextColor(Color.parseColor("#FF4B4B"))
            holder.status.setBackgroundColor(Color.parseColor("#FFEBEE"))
        }
    }

    override fun getItemCount(): Int = billList.size
}