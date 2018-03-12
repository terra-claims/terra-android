package claims.terra.terradroid

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.fondesa.kpermissions.extension.listeners
import com.fondesa.kpermissions.extension.permissionsBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK
import org.osmdroid.util.GeoPoint


class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(Companion.MAP_PREFS, Context.MODE_PRIVATE) }

    private val mapListener = object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            val eventPosition = event?.source?.mapCenter as GeoPoint

            val centerPos = eventPosition.toDoubleString()
            prefs.edit()
                    .putString(CENTER_POS, centerPos)
                    .apply()
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            val zoom  = event?.zoomLevel ?: DEFAULT_ZOOM
            prefs.edit()
                    .putFloat(Companion.INITIAL_ZOOM, zoom.toFloat())
                    .apply()
            return true
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Creates the request with the permissions you would like to request.
        val request = permissionsBuilder(WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION).build()
        request.listeners {

            onAccepted { setupMap() }

            onDenied {
                map_view.visibility = View.GONE
                request.send()
            }

            onPermanentlyDenied {
                AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton(R.string.button_go_there_now) { _, _ -> goToPermissionSettings() }
                        .setNegativeButton(R.string.button_close_app) { _, _ ->
                            this@MainActivity.finish()
                        }
                        .setCancelable(false)
                        .setMessage(R.string.permission_permanently_denied_settings)
                        .show()
            }

            onShouldShowRationale { _, nonce ->
                AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton(R.string.button_allow) { _, _ -> nonce.use() }
                        .setNegativeButton(R.string.button_deny) { _, _ ->
                            Toast.makeText(this@MainActivity, "sorry", LENGTH_SHORT).show()
                        }
                        .setCancelable(false)
                        .setMessage(R.string.permission_rationale)
                        .show()
            }
        }
        request.send()
    }

    private fun setupMap() {

        val centerPos = prefs.getString(CENTER_POS, DEFAULT_START_LOCATION)
        val geo = GeoPoint.fromDoubleString(centerPos, ',')
        val zoom = prefs.getFloat(INITIAL_ZOOM, DEFAULT_ZOOM.toFloat())

        map_view.apply {
            tilesScaleFactor = 3.0f
            setTileSource(MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.apply {
                setZoom(zoom.toDouble())
                //start in Paris for debugging purposes
                setCenter(geo)
            }
            addMapListener(mapListener)
        }

        map_view.visibility = View.VISIBLE

    }

    override fun onStart() {
        super.onStart()
        map_view.onResume()
    }

    override fun onStop() {
        super.onStop()
        map_view.onPause()
    }

    private fun goToPermissionSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_NO_HISTORY)
        startActivity(intent)
    }

    companion object {
        private const val MAP_PREFS = "map_prefs"
        private const val CENTER_POS = "initial_pos"
        private const val INITIAL_ZOOM = "initial_zoom"
        private const val DEFAULT_ZOOM = 8.0
        private const val DEFAULT_START_LOCATION = "37.08732423665077,-8.21451187133789,0.0"

    }
}
