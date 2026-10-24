package com.example.dacs3

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RunWith(AndroidJUnit4::class)
class MapUiTest {
    @Test fun pickerValidatesCoordinatesAndPreservesZeroZeroAfterRecreation() {
        val intent=Intent(ApplicationProvider.getApplicationContext(),MapActivity::class.java).putExtra("PICK_LOCATION",true)
        ActivityScenario.launchActivityForResult<MapActivity>(intent).use { scenario ->
            onView(withId(R.id.mapAction)).check(matches(isNotEnabled()))
            onView(withId(R.id.mapCoordinates)).perform(click())
            onView(withId(R.id.mapLatitude)).perform(replaceText("91"),closeSoftKeyboard())
            onView(withId(R.id.mapLongitude)).perform(replaceText("0"),closeSoftKeyboard())
            onView(withText("Đặt ghim")).perform(click())
            onView(withId(R.id.mapLatitude)).check(matches(hasErrorText("Kiểm tra vĩ độ và kinh độ")))
            onView(withId(R.id.mapLatitude)).perform(replaceText("0"),closeSoftKeyboard())
            onView(withText("Đặt ghim")).perform(click())
            scenario.recreate()
            onView(withId(R.id.mapAction)).check(matches(isEnabled())).perform(click())
            assertEquals(Activity.RESULT_OK,scenario.result.resultCode)
            assertEquals(0.0,scenario.result.resultData!!.getDoubleExtra("LATITUDE",Double.NaN),0.0)
            assertEquals(0.0,scenario.result.resultData!!.getDoubleExtra("LONGITUDE",Double.NaN),0.0)
        }
    }
    @Test fun roomMapShowsItsActualMarkerAndKeepsDirectionsAvailable() {
        val property=Property(id="preview",title="Phòng bên sông",location="Hải Châu, Đà Nẵng",latitude=16.047,longitude=108.206)
        val intent=Intent(ApplicationProvider.getApplicationContext(),MapActivity::class.java).putExtra("CHOSEN_PROPERTY",property)
        ActivityScenario.launch<MapActivity>(intent).use { scenario ->
            onView(withId(R.id.mapAction)).check(matches(isEnabled()))
            scenario.onActivity { activity ->
                val markers=activity.findViewById<MapView>(R.id.roomMap).overlays.filterIsInstance<Marker>()
                assertEquals(1,markers.size)
                assertEquals(property.latitude!!,markers.single().position.latitude,0.000001)
                assertEquals(property.longitude!!,markers.single().position.longitude,0.000001)
                assertTrue(activity.findViewById<TextView>(R.id.mapStatus).text.contains("16.047"))
            }
        }
    }
}
