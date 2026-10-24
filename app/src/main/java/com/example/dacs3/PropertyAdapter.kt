package com.example.dacs3

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ItemPropertyBinding

class PropertyAdapter(
    private val propertyList: List<Property>
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(
        val binding: ItemPropertyBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PropertyViewHolder,
        position: Int
    ) {
        val property = propertyList[position]

        // Gán dữ liệu lên giao diện item
        holder.binding.txtPrice.text = property.price
        holder.binding.txtTitle.text = property.title
        holder.binding.txtLocation.text = property.location
        holder.binding.txtBedroom.text = "${property.bedroom} Phòng ngủ"
        holder.binding.txtBathroom.text = "${property.bathroom} Phòng tắm"
        holder.binding.txtArea.text = property.area
        holder.binding.ratingBar.rating = property.rating

        // Load hình ảnh nhà/phòng trọ
        Glide.with(holder.itemView.context)
            .load(property.imageUrl)
            .placeholder(R.drawable.illustration_home)
            .error(R.drawable.illustration_home)
            .into(holder.binding.imgProperty)

        // 1. SỰ KIỆN: Xem chi tiết phòng trọ (Đã sửa lỗi đưa startActivity xuống cuối)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PropertyDetailActivity::class.java)

            // Dùng tuyệt chiêu truyền cả Object siêu gọn nhờ Serializable
            intent.putExtra("CHOSEN_PROPERTY", property)

            // Hoặc nếu code cũ trang PropertyDetailActivity vẫn gọi lẻ từng chuỗi, Nhi dùng cụm dưới này:
            intent.putExtra("title", property.title)
            intent.putExtra("price", property.price)
            intent.putExtra("location", property.location)
            intent.putExtra("area", property.area)
            intent.putExtra("image", property.imageUrl)
            intent.putExtra("rating", property.rating)
            intent.putExtra("propertyId", property.id)
            intent.putExtra("hostId", property.hostId)

            // LUÔN LUÔN GỌI dòng này ở cuối cùng sau khi đã nạp đủ dữ liệu!
            holder.itemView.context.startActivity(intent)
        }

        holder.binding.btnChat.setOnClickListener { holder.itemView.performClick() }

        // 2. SỰ KIỆN: Nhấn nút Tạo hóa đơn tiền phòng (Nhi có thể bổ sung nút này vào giao diện nếu là role Chủ nhà)
        // Ví dụ nếu Nhi có nút đặt tên là btnCreateBill trong item_property.xml:
        /*
        holder.binding.btnCreateBill?.setOnClickListener {
            val intent = Intent(holder.itemView.context, AddBillActivity::class.java)
            intent.putExtra("CHOSEN_PROPERTY", property) // Gửi toàn bộ thông tin sang AddBillActivity
            holder.itemView.context.startActivity(intent)
        }
        */

        // Yêu thích
        holder.binding.imgFavorite.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "Thêm vào mục yêu thích ❤️",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Nhắn tin với chủ nhà
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PropertyDetailActivity::class.java)
            intent.putExtra("CHOSEN_PROPERTY", property) // Truyền dữ liệu phòng sang trang chi tiết
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return propertyList.size
    }
}