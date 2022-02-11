package com.indooratlas.flutter

import androidx.annotation.NonNull
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.Manifest
import android.util.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener

import com.indooratlas.android.sdk.IALocation
import com.indooratlas.android.sdk.IALocationRequest
import com.indooratlas.android.sdk.IAOrientationRequest
import com.indooratlas.android.sdk.IALocationListener
import com.indooratlas.android.sdk.IALocationManager
import com.indooratlas.android.sdk.IARegion
import com.indooratlas.android.sdk.IARoute
import com.indooratlas.android.sdk.IAOrientationListener
import com.indooratlas.android.sdk.IAWayfindingListener
import com.indooratlas.android.sdk.IAWayfindingRequest
import com.indooratlas.android.sdk.IAGeofence
import com.indooratlas.android.sdk.IAGeofenceEvent
import com.indooratlas.android.sdk.IAGeofenceListener
import com.indooratlas.android.sdk.IAPOI
import com.indooratlas.android.sdk.resources.IAFloorPlan
import com.indooratlas.android.sdk.resources.IALatLng
import com.indooratlas.android.sdk.resources.IAVenue

class IAFlutterResult {
}

private fun IAPOI2Map(poi: IAPOI): Map<String, Any> {
  return mapOf(
    "type" to "Feature",
    "id" to poi.id,
    "properties" to mapOf(
      "name" to poi.name,
      "floor" to poi.floor,
      "payload" to poi.payload.toString()
    ),
    "geometry" to mapOf(
      "type" to "Point",
      "coordinates" to listOf(poi.location.longitude, poi.location.latitude)
    )
  )
}

private fun IAGeofence2Map(geofence: IAGeofence): Map<String, Any> {
  var vertices: List<Double> = geofence.edges.flatMap { listOf(it[1], it[0]) }
  return mapOf(
    "type" to "Feature",
    "id" to geofence.id,
    "properties" to mapOf(
      "name" to geofence.name,
      "floor" to geofence.floor,
      "payload" to geofence.payload.toString()
    ),
    "geometry" to mapOf(
      "type" to "Polygon",
      "coordinates" to listOf(vertices)
    )
  )
}

private fun IAFloorplan2Map(floorplan: IAFloorPlan): Map<String, Any> {
  return mapOf(
    "id" to floorplan.id,
    "name" to floorplan.name,
    "url" to floorplan.url,
    "floorLevel" to floorplan.floorLevel,
    "bearing" to floorplan.bearing,
    "bitmapWidth" to floorplan.bitmapWidth,
    "bitmapHeight" to floorplan.bitmapHeight,
    "widthMeters" to floorplan.widthMeters,
    "heightMeters" to floorplan.heightMeters,
    "metersToPixels" to floorplan.metersToPixels,
    "pixelsToMeters" to floorplan.pixelsToMeters,
    "bottomLeft" to listOf(floorplan.bottomLeft.longitude, floorplan.bottomLeft.latitude),
    "center" to listOf(floorplan.center.longitude, floorplan.center.latitude),
    "topLeft" to listOf(floorplan.topLeft.longitude, floorplan.topLeft.latitude),
    "topRight" to listOf(floorplan.topRight.longitude, floorplan.topRight.latitude)
  )
}

private fun IAVenue2Map(venue: IAVenue): Map<String, Any> {
  var map = mutableMapOf<String, Any>(
    "id" to venue.id,
    "name" to venue.name
  )
  var plans: List<Map<String, Any>> = venue.floorPlans.map { IAFloorplan2Map(it) }
  if (plans.isNotEmpty()) map["floorPlans"] = plans
  var fences: List<Map<String, Any>> = venue.geofences.map { IAGeofence2Map(it) }
  if (fences.isNotEmpty()) map["geofences"] = fences
  var pois: List<Map<String, Any>> = venue.poIs.map { IAPOI2Map(it) }
  if (pois.isNotEmpty()) map["pois"] = pois
  return map
}

private fun IARegion2Map(region: IARegion): Map<String, Any> {
  var map = mutableMapOf<String, Any>(
    "regionId" to region.id,
    "timestamp" to region.timestamp,
    "regionType" to region.type
  )
  if (region.getFloorPlan() != null) {
    map["floorPlan"] = IAFloorplan2Map(region.floorPlan);
  }
  if (region.getVenue() != null) {
    map["venue"] = IAVenue2Map(region.venue);
  }
  return map
}

private fun IALocation2Map(location: IALocation): Map<String, Any> {
  var map = mutableMapOf(
    "latitude" to location.latitude,
    "longitude" to location.longitude,
    "accuracy" to location.accuracy,
    "altitude" to location.altitude,
    "heading" to location.bearing,
    "floorCertainty" to location.floorCertainty,
    "flr" to location.floorLevel,
    "velocity" to location.toLocation().speed,
    "timestamp" to location.time
  )
  if (location.region != null) {
    map["region"] = IARegion2Map(location.region);
  }
  return map
}

private fun IARoutePoint2Map(rp: IARoute.Point): Map<String, Any> {
   return mapOf(
      "latitude" to rp.latitude,
      "longitude" to rp.longitude,
      "floor" to rp.floor
   )
}

private fun IARoute2Map(route: IARoute): Map<String, Any> {
   var legs: List<Map<String, Any>> = route.legs.map { leg ->
     mapOf(
         "begin" to IARoutePoint2Map(leg.begin),
         "end" to IARoutePoint2Map(leg.end),
         "length" to leg.length,
         "direction" to leg.direction,
         "edgeIndex" to (leg.edgeIndex ?: -1))
   }
   return mapOf(
      "legs" to legs,
      "error" to route.error.name
   )
}

// Separated from IAFlutterPlugin to avoid requiring java dependency in the app that uses this plugin
// (Causes compiler failure)
class IAFlutterEngine (context: Context, channel: MethodChannel): IALocationListener, IARegion.Listener, IAOrientationListener, IAWayfindingListener, IAGeofenceListener, RequestPermissionsResultListener {
  var activityBinding: ActivityPluginBinding? = null
    get() = field
    set(value) {
      if (field != null) {
        val old = field as ActivityPluginBinding
        old.removeRequestPermissionsResultListener(this)
      }
      if (value != null) {
        value.addRequestPermissionsResultListener(this)
      }
      field = value
    }

  private val _handler = Handler(Looper.getMainLooper())
  private val _context: Context = context
  private val _channel: MethodChannel = channel
  private var _locationManager: IALocationManager? = null
  private var _locationRequest = IALocationRequest.create()
  private var _orientationRequest = IAOrientationRequest(1.0, 1.0)
  private var _locationServiceRunning = false

  private val PERMISSION_REQUEST_CODE = 444444

  // TODO: Handle BLUETOOTH_SCAN (android 12) somehow
  //       https://github.com/IndoorAtlas/android-sdk/pull/609
  private val PERMISSIONS = arrayOf(
    Manifest.permission.CHANGE_WIFI_STATE,
    Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.INTERNET)

  override fun onStatusChanged(@NonNull provider: String, status: Int, bundle: Bundle?) {
    var mappedStatus = 0;
    if (status == IALocationManager.STATUS_OUT_OF_SERVICE) {
       mappedStatus = 0;
    } else if (status == IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE) {
       mappedStatus = 1;
    } else if (status == IALocationManager.STATUS_AVAILABLE) {
       mappedStatus = 2;
    } else if (status == IALocationManager.STATUS_LIMITED) {
       mappedStatus = 3;
    } else {
      // figure out how to return error
    }
    _channel.invokeMethod("onStatusChanged", listOf(mappedStatus))
  }

  override fun onLocationChanged(@NonNull location: IALocation) {
    _channel.invokeMethod("onLocationChanged", listOf(IALocation2Map(location)))
  }

  override fun onEnterRegion(@NonNull region: IARegion) {
    _channel.invokeMethod("onEnterRegion", listOf(IARegion2Map(region)))
  }

  override fun onExitRegion(@NonNull region: IARegion) {
    _channel.invokeMethod("onExitRegion", listOf(IARegion2Map(region)))
  }

  override fun onOrientationChange(timestamp: Long, @NonNull quaternion: DoubleArray) {
    _channel.invokeMethod("onOrientationChanged", listOf(timestamp, quaternion[0], quaternion[1], quaternion[2], quaternion[3]))
  }

  override fun onHeadingChanged(timestamp: Long, heading: Double) {
    _channel.invokeMethod("onHeadingChanged", listOf(timestamp, heading))
  }

  override fun onWayfindingUpdate(route: IARoute) {
    _channel.invokeMethod("onWayfindingUpdate", listOf(IARoute2Map(route)))
  }

  override fun onGeofencesTriggered(event: IAGeofenceEvent) {
    // _channel.invokeMethod("onGeofencesTriggered", listOf(IAGeofenceEvent2Map(event)))
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
    if (requestCode != PERMISSION_REQUEST_CODE) {
      _channel.invokeMethod("onPermissionsGranted", listOf(false))
      return false;
    }
    // TODO: actually check the permissions
    _channel.invokeMethod("onPermissionsGranted", listOf(true))
    return true
  }

  fun detach() {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.destroy()
        _locationManager = null;
      }
    }
    _channel.setMethodCallHandler(null)
  }

  fun requestPermissions() {
    if (activityBinding == null) return
    activityBinding?.getActivity()?.requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE)
  }

  fun initialize(pluginVersion: String, @NonNull apiKey: String, endpoint: String) {
    _handler.post {
      val bundle = Bundle(2)
      bundle.putString(IALocationManager.EXTRA_API_KEY, apiKey)
      bundle.putString(IALocationManager.EXTRA_API_SECRET, "not-used-in-the-flutter-plugin")
      bundle.putString("com.indooratlas.android.sdk.intent.extras.wrapperName", "flutter")
      if (endpoint != null && endpoint.length > 0) {
        bundle.putString("com.indooratlas.android.sdk.intent.extras.restEndpoint", endpoint);
      }
      if (pluginVersion != null && pluginVersion.length > 0) {
        bundle.putString("com.indooratlas.android.sdk.intent.extras.wrapperVersion", pluginVersion)
      }
      if (_locationManager != null) {
        _locationManager?.destroy()
      }
      requestPermissions()
      _locationServiceRunning = false
      _locationManager = IALocationManager.create(_context, bundle)
    }
  }

  fun getTraceId(): String {
    if (_locationManager == null) return ""
    // FIXME: store the traceid somewhere else and return that?
    //        (we aren't calling this on main thread here maybe)
    return _locationManager?.getExtraInfo()?.traceId ?: ""
  }

  fun lockIndoors(locked: Boolean?) {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.lockIndoors(locked!!)
      }
    }
  }

  fun lockFloor(floor: Int?) {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.lockFloor(floor!!)
      }
    }
  }

  fun unlockFloor() {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.unlockFloor()
      }
    }
  }

  fun setPositioningMode(mode: Int?) {
    var prio = 0
    when (mode) {
      0 -> prio = IALocationRequest.PRIORITY_HIGH_ACCURACY
      1 -> prio = IALocationRequest.PRIORITY_LOW_POWER
      2 -> prio = IALocationRequest.PRIORITY_CART_MODE
    }
    _locationRequest.setPriority(prio)
  }

  fun setOutputThresholds(distance: Double?, interval: Double?) {
    if (distance!! < 0 || interval!! < 0) {
      // figure out how to return error
      return
    }
    val wasRunning = _locationServiceRunning
    if (wasRunning) stopPositioning()
    if (distance!! >= 0) _locationRequest.setSmallestDisplacement(distance!!.toFloat())
    if (interval!! >= 0) _locationRequest.setFastestInterval((interval!! * 1000 /* s -> ms */).toLong())
    if (wasRunning) startPositioning()
  }

  fun setSensitivities(orientationSensitivity: Double?, headingSensitivity: Double?) {
    _orientationRequest = IAOrientationRequest(headingSensitivity!!, orientationSensitivity!!)
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.unregisterOrientationListener(this)
        _locationManager?.registerOrientationListener(_orientationRequest, this)
      }
    }
  }

  fun startPositioning() {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.registerRegionListener(this)
        _locationManager?.registerOrientationListener(_orientationRequest, this)
        _locationManager?.requestLocationUpdates(_locationRequest, this)
        _locationServiceRunning = true
      }
    }
  }

  fun stopPositioning() {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.removeLocationUpdates(this)
        _locationManager?.unregisterOrientationListener(this)
        _locationManager?.unregisterRegionListener(this)
        _locationServiceRunning = false
      }
    }
  }

  fun startWayfinding(lat: Double?, lon: Double?, floor: Int?) {
    _handler.post {
      if (_locationManager != null) {
        val request = IAWayfindingRequest.Builder().withLatitude(lat!!).withLongitude(lon!!).withFloor(floor!!).build()
        _locationManager?.requestWayfindingUpdates(request, this)
      }
    }
  }

  fun stopWayfinding() {
    _handler.post {
      if (_locationManager != null) {
        _locationManager?.removeWayfindingUpdates()
      }
    }
  }
}

class IAFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var _engine: IAFlutterEngine

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val context = flutterPluginBinding.applicationContext
    val channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.indooratlas.flutter")
    channel.setMethodCallHandler(this)
    _engine = IAFlutterEngine(context, channel)
  }

  override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
    _engine.activityBinding = activityPluginBinding
  }

  override fun onDetachedFromActivityForConfigChanges() {
    _engine.activityBinding = null
  }

  override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
    _engine.activityBinding = activityPluginBinding
  }

  override fun onDetachedFromActivity() {
    _engine.activityBinding = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    val types = mutableListOf<java.lang.Class<*>>()
    if (call.arguments != null) {
      if (call.arguments is List<*>) {
        for (arg in call.arguments as List<java.lang.Object>) {
          types.add(arg.getClass())
        }
      } else {
        types.add((call.arguments as java.lang.Object).getClass())
      }
    }

    val method = _engine.javaClass.getDeclaredMethod(call.method, *types.toTypedArray())
    if (method == null) {
      result.notImplemented()
      return
    }

    try {
      var ret: Any? = null
      method.setAccessible(true)
      if (call.arguments is List<*>) {
        ret = method.invoke(_engine, *(call.arguments as List<*>).toTypedArray())
      } else if (call.arguments != null) {
        ret = method.invoke(_engine, call.arguments)
      } else {
        ret = method.invoke(_engine)
      }
      if (ret is IAFlutterResult) {
        // ((IAFlutterResult)ret).excute(result)
      } else {
        result.success(ret)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      result.error(e.message, e.getStackTrace().toString(), null)
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    _engine.detach();
    _engine.activityBinding = null
  }
}
