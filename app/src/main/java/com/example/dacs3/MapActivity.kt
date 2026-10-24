package com.example.dacs3

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/** Address search uses the Android geocoder; the map and pins need no Google Maps API key. */
class MapActivity : HouseActivity() {
    private lateinit var map: MapView
    private lateinit var status: TextView
    private lateinit var apply: MaterialButton
    private lateinit var address: EditText
    private lateinit var search: MaterialButton
    private lateinit var tileWarning: TextView
    private val worker=Executors.newSingleThreadExecutor()
    private var selected: GeoPoint?=null
    private var pin: Marker?=null
    private var property: Property?=null
    private var pick=false
    private var searchGeneration=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue="House/${BuildConfig.VERSION_NAME} ($packageName; https://github.com/Yeye915/DACS3)"
            osmdroidBasePath=File(filesDir,"maps").apply { mkdirs() }
            osmdroidTileCache=File(cacheDir,"map-tiles").apply { mkdirs() }
        }
        pick=intent.getBooleanExtra("PICK_LOCATION",false)
        property=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        val landscape=resources.configuration.screenWidthDp>resources.configuration.screenHeightDp
        val root=LinearLayout(this).apply { orientation=if(landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL;setBackgroundColor(ContextCompat.getColor(context,R.color.background)) }
        val controls=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val bar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(4),dp(16),dp(4)) }
        if(landscape) root.addView(ScrollView(this).apply { addView(controls) },LinearLayout.LayoutParams(dp(240),-1))
        else root.addView(bar)
        if(landscape) controls.addView(bar)
        val heading=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(0,dp(4),0,dp(8)) };bar.addView(heading)
        ImageButton(this).apply { setImageResource(R.drawable.ic_ui_back);background=ContextCompat.getDrawable(context,R.drawable.bg_icon_button)
            imageTintList=android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context,R.color.primary));setPadding(dp(12),dp(12),dp(12),dp(12))
            contentDescription=getString(R.string.action_back);heading.addView(this,LinearLayout.LayoutParams(dp(48),dp(48)));setOnClickListener { finish() } }
        TextView(this).apply { text=if(pick) "Chọn vị trí phòng" else "Bản đồ phòng trọ";textSize=20f;setTypeface(typeface,android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context,R.color.text_primary));heading.addView(this,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(12) }) }
        val searchRow=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL };bar.addView(searchRow)
        address=EditText(this).apply { id=R.id.mapAddress;hint="Địa chỉ, tỉnh / thành";setSingleLine(true)
            setText(savedInstanceState?.getString("address") ?: property?.address ?: intent.getStringExtra("ADDRESS").orEmpty());minHeight=dp(48) }
        searchRow.addView(address,LinearLayout.LayoutParams(0,-2,1f))
        search=MaterialButton(this).apply { id=R.id.mapSearch;contentDescription="Tìm địa chỉ trên bản đồ";icon=ContextCompat.getDrawable(context,R.drawable.ic_ui_search)
            iconPadding=0;iconGravity=MaterialButton.ICON_GRAVITY_TEXT_START;minWidth=0;setPadding(dp(12),0,dp(12),0)
            searchRow.addView(this,LinearLayout.LayoutParams(dp(48),dp(48)));setOnClickListener { findAddress() } }
        status=TextView(this).apply { id=R.id.mapStatus;textSize=13f;setPadding(0,dp(4),0,dp(4));bar.addView(this) }
        val mapFrame=FrameLayout(this)
        root.addView(mapFrame,if(landscape) LinearLayout.LayoutParams(0,-1,1f) else LinearLayout.LayoutParams(-1,0,1f))
        map=MapView(this).apply { id=R.id.roomMap
            setTileSource(org.osmdroid.tileprovider.tilesource.XYTileSource("HouseOSM",0,19,256,".png",arrayOf("https://tile.openstreetmap.org/")))
            setMultiTouchControls(true);contentDescription="Bản đồ vị trí phòng. Dùng hai ngón tay để thu phóng." }
        mapFrame.addView(map,FrameLayout.LayoutParams(-1,-1))
        tileWarning=TextView(this).apply { text="Chưa tải được nền bản đồ. Kiểm tra mạng; vị trí ghim vẫn được giữ.";textSize=12f;setPadding(dp(12),dp(8),dp(12),dp(8))
            setBackgroundColor(ContextCompat.getColor(context,R.color.surface));visibility=android.view.View.GONE
            setOnClickListener { map.invalidate() } }
        mapFrame.addView(tileWarning,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP))
        // Keep osmdroid's own redraw handler and add a separate, visible failure indicator.
        map.tileProvider.tileRequestCompleteHandlers.add(Handler(Looper.getMainLooper()) { message ->
            if(!isDestroyed) tileWarning.visibility=if(message.what==1) android.view.View.VISIBLE else android.view.View.GONE
            false
        })
        TextView(this).apply { text="© OpenStreetMap contributors";textSize=11f;setPadding(dp(8),dp(4),dp(8),dp(4));minHeight=dp(32)
            setBackgroundColor(ContextCompat.getColor(context,R.color.surface));gravity=android.view.Gravity.CENTER_VERTICAL
            mapFrame.addView(this,FrameLayout.LayoutParams(-2,-2,android.view.Gravity.BOTTOM or android.view.Gravity.START))
            setOnClickListener { openUrl("https://www.openstreetmap.org/copyright") } }
        val footer=LinearLayout(this).apply { orientation=if(landscape) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL;setPadding(dp(16),dp(4),dp(16),dp(8)) }
        if(landscape) controls.addView(footer) else root.addView(footer)
        if(pick) {
            MaterialButton(this).apply { id=R.id.mapCoordinates;text="Nhập tọa độ";isAllCaps=false
                footer.addView(this,if(landscape) LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) })
                setOnClickListener { coordinateDialog() } }
            map.overlays.add(MapEventsOverlay(object: MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { select(p);return true }
                override fun longPressHelper(p: GeoPoint): Boolean { select(p);return true }
            }))
        }
        apply=MaterialButton(this).apply { id=R.id.mapAction;isAllCaps=false;text=if(pick) "Dùng vị trí này" else "Mở chỉ đường"
            footer.addView(this,if(landscape) LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,1f))
            setOnClickListener { if(pick) {
                val point=selected ?: return@setOnClickListener
                setResult(RESULT_OK,Intent().putExtra("LATITUDE",point.latitude).putExtra("LONGITUDE",point.longitude));finish()
            } else directions() } }
        setContentView(root)
        map.controller.setZoom(5.0);map.controller.setCenter(GeoPoint(16.0,107.0))
        val lat=savedInstanceState?.getDouble("lat",Double.NaN) ?: property?.latitude ?: intent.getDoubleExtra("LATITUDE",Double.NaN)
        val lon=savedInstanceState?.getDouble("lon",Double.NaN) ?: property?.longitude ?: intent.getDoubleExtra("LONGITUDE",Double.NaN)
        if(RentalValidation.coordinates(lat,lon)) select(GeoPoint(lat,lon))
        else if(!pick && property==null) loadRooms()
        else { status.text=if(pick) "Chạm bản đồ hoặc nhập tọa độ để ghim chính xác. Bản đồ cần kết nối mạng." else "Phòng chưa có ghim vị trí. Bạn có thể tìm địa chỉ hoặc mở chỉ đường bằng địa chỉ."; apply.isEnabled=!pick && address.text.isNotBlank() }
    }
    private fun dp(n: Int)=(n*resources.displayMetrics.density).toInt()
    private fun select(point: GeoPoint) {
        searchGeneration++;selected=point
        pin?.let { map.overlays.remove(it) }
        pin=Marker(map).apply { icon=ContextCompat.getDrawable(this@MapActivity,R.drawable.ic_map_pin);position=point; setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
            title=property?.title ?: "Vị trí đã chọn"; snippet=property?.address.orEmpty() }
        map.overlays.add(pin);map.controller.setZoom(16.0);map.controller.animateTo(point);map.invalidate()
        status.text=String.format(Locale.US,"Vị trí ghim: %.6f, %.6f • Bản đồ cần kết nối mạng",point.latitude,point.longitude)
        apply.isEnabled=true
    }
    private fun loadRooms() {
        status.text="Đang tải vị trí phòng…";apply.isEnabled=false
        Backend.db.collection("properties").get().addOnSuccessListener { result ->
            val filters=intent.getSerializableExtra("ROOM_FILTERS") as? RoomFilters ?: RoomFilters()
            val favorites=intent.getStringArrayListExtra("FAVORITE_IDS")
            val matches=filters.apply(result.documents.map(Property::from),Backend.auth.currentUser?.uid).filter { favorites==null || it.id in favorites }
            val rooms=matches.filter { it.hasCoordinates }
            rooms.forEach { room -> map.overlays.add(Marker(map).apply {
                icon=ContextCompat.getDrawable(this@MapActivity,R.drawable.ic_map_pin);position=GeoPoint(room.latitude!!,room.longitude!!);title=room.title;snippet="${room.price} • Chạm để xem phòng"
                setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _,_ -> startActivity(Intent(this@MapActivity,PropertyDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",room)); true }
            }) }
            if(rooms.isNotEmpty()) map.post {
                if(rooms.size==1) { map.controller.setZoom(16.0);map.controller.setCenter(GeoPoint(rooms[0].latitude!!,rooms[0].longitude!!)) }
                else map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(rooms.map { GeoPoint(it.latitude!!,it.longitude!!) }),false,dp(48),16.0,0L)
            }
            status.text="${rooms.size} / ${matches.size} phòng phù hợp có tọa độ. Chạm ghim để xem chi tiết."
            map.invalidate()
        }.addOnFailureListener { status.text="Không tải được danh sách phòng. Hãy quay lại và thử lại."; failure(it) }
    }
    private fun coordinateDialog() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,dp(20),0) }
        val latitude=EditText(this).apply { id=R.id.mapLatitude;hint="Vĩ độ (-90 đến 90)";inputType=12290;setText(selected?.latitude?.toString().orEmpty());minHeight=dp(48);box.addView(this) }
        val longitude=EditText(this).apply { id=R.id.mapLongitude;hint="Kinh độ (-180 đến 180)";inputType=12290;setText(selected?.longitude?.toString().orEmpty());minHeight=dp(48);box.addView(this) }
        val dialog=androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Ghim bằng tọa độ").setView(box).setPositiveButton("Đặt ghim",null).setNegativeButton("Hủy",null).create()
        dialog.setOnShowListener { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val lat=latitude.text.toString().replace(",",".").toDoubleOrNull();val lon=longitude.text.toString().replace(",",".").toDoubleOrNull()
            if(!RentalValidation.coordinates(lat,lon)) { latitude.error="Kiểm tra vĩ độ và kinh độ";return@setOnClickListener }
            select(GeoPoint(lat!!,lon!!));dialog.dismiss()
        } };dialog.show()
    }
    private fun findAddress() {
        val query=address.text.toString().trim(); if(query.isBlank()) { address.error="Nhập địa chỉ cần tìm";return }
        if(!Geocoder.isPresent()) { notice("Thiết bị chưa hỗ trợ tìm tọa độ. Hãy chạm bản đồ, nhập tọa độ hoặc mở chỉ đường bằng địa chỉ.");return }
        val generation=++searchGeneration;status.text="Đang tìm địa chỉ…";search.isEnabled=false
        worker.execute {
            val results=runCatching { @Suppress("DEPRECATION") Geocoder(this,Locale.forLanguageTag("vi-VN")).getFromLocationName(query,5).orEmpty() }
            runOnUiThread {
                if(isFinishing || isDestroyed) return@runOnUiThread
                search.isEnabled=true
                if(generation!=searchGeneration) return@runOnUiThread
                val locations=results.getOrNull().orEmpty()
                if(locations.isEmpty()) { status.text="Chưa tìm được địa chỉ. Thêm phường / xã và tỉnh / thành, hoặc ghim thủ công.";return@runOnUiThread }
                androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Kiểm tra vị trí trước khi chọn")
                    .setItems(locations.map { it.getAddressLine(0).orEmpty() }.toTypedArray()) { _,i ->
                        select(GeoPoint(locations[i].latitude,locations[i].longitude))
                    }.setNegativeButton("Hủy",null).show()
            }
        }
    }
    private fun directions() {
        val destination=selected?.let { "${it.latitude},${it.longitude}" } ?: address.text.toString().trim()
        if(destination.isBlank()) { notice("Chưa có vị trí để chỉ đường");return }
        openUrl("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
    }
    private fun openUrl(url: String) { try { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } catch(_: ActivityNotFoundException) { notice("Thiết bị chưa có ứng dụng mở bản đồ hoặc trình duyệt") } }
    override fun onSaveInstanceState(out: Bundle) { selected?.let { out.putDouble("lat",it.latitude);out.putDouble("lon",it.longitude) };out.putString("address",address.text.toString());super.onSaveInstanceState(out) }
    override fun onResume() { super.onResume();if(::map.isInitialized) map.onResume() }
    override fun onPause() { if(::map.isInitialized) map.onPause();super.onPause() }
    override fun onDestroy() { searchGeneration++;worker.shutdownNow();if(::map.isInitialized) map.onDetach();super.onDestroy() }
}
