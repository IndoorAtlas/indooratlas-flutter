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
    "id" to poi.getId(),
    "properties" to mapOf(
      "name" to poi.getName(),
      "floor" to poi.getFloor(),
      "payload" to poi.getPayload().toString()
    ),
    "geometry" to mapOf(
      "type" to "Point",
      "coordinates" to listOf(poi.getLocation().longitude, poi.getLocation().latitude)
    )
  )
}

private fun IAGeofence2Map(geofence: IAGeofence): Map<String, Any> {
  var vertices: List<Double> = listOf()
  for (edge in geofence.getEdges()) {
    vertices += listOf(edge[1], edge[0])
  }
  return mapOf(
    "type" to "Feature",
    "id" to geofence.getId(),
    "properties" to mapOf(
      "name" to geofence.getName(),
      "floor" to geofence.getFloor(),
      "payload" to geofence.getPayload().toString()
    ),
    "geometry" to mapOf(
      "type" to "Polygon",
      "coordinates" to listOf(vertices)
    )
  )
}

private fun IAFloorplan2Map(floorplan: IAFloorPlan): Map<String, Any> {
  return mapOf(
    "id" to floorplan.getId(),
    "name" to floorplan.getName(),
    "url" to floorplan.getUrl(),
    "floorLevel" to floorplan.getFloorLevel(),
    "bearing" to floorplan.getBearing(),
    "bitmapWidth" to floorplan.getBitmapWidth(),
    "bitmapHeight" to floorplan.getBitmapHeight(),
    "widthMeters" to floorplan.getWidthMeters(),
    "heightMeters" to floorplan.getHeightMeters(),
    "metersToPixels" to floorplan.getMetersToPixels(),
    "pixelsToMeters" to floorplan.getPixelsToMeters(),
    "bottomLeft" to listOf(floorplan.getBottomLeft().longitude, floorplan.getBottomLeft().latitude),
    "center" to listOf(floorplan.getCenter().longitude, floorplan.getCenter().latitude),
    "topLeft" to listOf(floorplan.getTopLeft().longitude, floorplan.getTopLeft().latitude),
    "topRight" to listOf(floorplan.getTopRight().longitude, floorplan.getTopRight().latitude)
  )
}

private fun IAVenue2Map(venue: IAVenue): Map<String, Any> {
  var map = mutableMapOf<String, Any>(
    "id" to venue.getId(),
    "name" to venue.getName()
  )
  var plans: List<Map<String, Any>> = listOf()
  for (plan in venue.getFloorPlans()) {
    plans += IAFloorplan2Map(plan)
  }
  if (plans.size > 0) map["floorPlans"] = plans
  var fences: List<Map<String, Any>> = listOf()
  for (fence in venue.getGeofences()) {
    fences += IAGeofence2Map(fence)
  }
  if (fences.size > 0) map["geofences"] = fences
  var pois: List<Map<String, Any>> = listOf()
  for (poi in venue.getPOIs()) {
    pois += IAPOI2Map(poi)
  }
  if (pois.size > 0) map["pois"] = pois
  return map
}

private fun IARegion2Map(region: IARegion): Map<String, Any> {
  var map = mutableMapOf<String, Any>(
    "regionId" to region.getId(),
    "timestamp" to region.getTimestamp(),
    "regionType" to region.getType()
  )
  if (region.getFloorPlan() != null) {
    map["floorPlan"] = IAFloorplan2Map(region.getFloorPlan());
  }
  if (region.getVenue() != null) {
    map["venue"] = IAVenue2Map(region.getVenue());
  }
  return map
}

private fun IALocation2Map(location: IALocation): Map<String, Any> {
  var map = mutableMapOf<String, Any>(
    "latitude" to location.getLatitude(),
    "longitude" to location.getLongitude(),
    "accuracy" to location.getAccuracy(),
    "altitude" to location.getAltitude(),
    "heading" to location.getBearing(),
    "floorCertainty" to location.getFloorCertainty(),
    "flr" to location.getFloorLevel(),
    "velocity" to location.toLocation().getSpeed(),
    "timestamp" to location.getTime()
  )
  if (location.getRegion() != null) {
    map["region"] = IARegion2Map(location.getRegion());
  }
  return map
}

private fun IARoutePoint2Map(rp: IARoute.Point): Map<String, Any> {
   return mapOf<String, Any>(
      "latitude" to rp.getLatitude(),
      "longitude" to rp.getLongitude(),
      "floor" to rp.getFloor()
   )
}

private fun IARoute2Map(route: IARoute): Map<String, Any> {
   var legs: List<Map<String, Any>> = listOf()
   for (leg in route.getLegs()) {
      legs += mapOf<String, Any>(
         "begin" to IARoutePoint2Map(leg.getBegin()),
         "end" to IARoutePoint2Map(leg.getEnd()),
         "length" to leg.getLength(),
         "direction" to leg.getDirection(),
         "edgeIndex" to (leg.getEdgeIndex() ?: -1)
      )
   }
   return mapOf<String, Any>(
      "legs" to legs,
      "error" to route.getError().name
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
