package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherapp.core.utils.NetworkUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {

    // Launcher to request multiple permissions and handle the result
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // FusedLocationProviderClient to request location updates
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Register launcher to request multiple permissions at once
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
                val fineLocationGranted =
                    permission[Manifest.permission.ACCESS_FINE_LOCATION] == true
                val coarseLocationGranted =
                    permission[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                if (fineLocationGranted || coarseLocationGranted) {
                    checkLocationServices()
                } else {
                    showPermissionDialog()
                }
            }
        // Check location permission onCreate activity.
        requestPermission()
    }

    // Check whether GPS or Network location is enabled, returns true if enabled.
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // Check if location services are enabled, and if not, open location settings
    // Return true if location is enabled else false.
    private fun checkLocationServices(): Boolean {
        return if (!isLocationEnabled()) {
            Toast.makeText(this, "Location service is disabled", Toast.LENGTH_SHORT).show()
            // Opens device location settings
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            false
        } else {
            Toast.makeText(this, "Location service is enabled", Toast.LENGTH_SHORT).show()
            true
        }
    }

    // Request location permission if not granted, otherwise check location services
    private fun requestPermission() {
        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION

        if (hasLocationPermissionEnabled()) {
            if (checkLocationServices()) {
                // If location service is enabled, then get location data.
                getLocationData()
            }
        } else {
            // Request for location permission
            permissionLauncher.launch(arrayOf(fineLocation, coarseLocation))
        }
    }

    // Show dialog on permission denied and open app settings
    private fun showPermissionDialog() {
        AlertDialog.Builder(this).setTitle("Location Permission Required")
            .setMessage("This permission is required to access the location. Please enable it in App Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    // Called when activity is restarted and re-requests/checks permission
    override fun onRestart() {
        super.onRestart()
        requestPermission()
    }

    // Check and return true if either fine or coarse location permission is granted
    private fun hasLocationPermissionEnabled(): Boolean {
        val permissionGranted = PackageManager.PERMISSION_GRANTED

        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineGranted = ContextCompat.checkSelfPermission(this, fineLocation) == permissionGranted
        val coarseGranted =
            ContextCompat.checkSelfPermission(this, coarseLocation) == permissionGranted

        return fineGranted || coarseGranted
    }

    // Get latitude and longitude coordinates of the location
    // Request location request and handle them using LocationCallBack
    @SuppressLint("MissingPermission")
    private fun getLocationData() {
        // Create location request with HighAccuracy and update time of 5 secs.
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        // Listen for location updates
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Latitude: ${location.latitude} \nLongitude: ${location.longitude}",
                            Toast.LENGTH_SHORT
                        ).show()
                        getLocationWeatherDetails()
                    } else {
                        Toast.makeText(
                            this@MainActivity, "Location Unavailable", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }, Looper.myLooper() // Run callback on main UI thread
        )
    }

    // Check if the internet is available
    private fun getLocationWeatherDetails() {
        if (NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Internet available", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show()
        }
    }
}