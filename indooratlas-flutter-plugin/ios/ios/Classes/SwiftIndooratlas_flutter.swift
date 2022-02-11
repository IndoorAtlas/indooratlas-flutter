import Flutter
import UIKit
import IndoorAtlas

let jsonEncoder = JSONEncoder()

private func IAPOI2Map(poi: IAPOI) -> [String: Any] {
    var properties: [String:Any] = [
        "name": poi.name ?? "",
        "floor": poi.floor.level
    ]
    
    if let payload = poi.payload {
        if let jsonData = try? JSONSerialization.data(withJSONObject: payload, options: .fragmentsAllowed) {
            properties["payload"] = String(data: jsonData, encoding: .utf8)
        }
    }
    
    return [
        "type": "Feature",
        "id": poi.identifier,
        "properties": properties,
        "geometry": [
            "type": "Point",
            "coordinates": [poi.latLngFloor.longitude, poi.latLngFloor.latitude]
        ]
    ]
}

private func IAGeofence2Map(geofence: IAGeofence) -> [String:Any] {
    var properties: [String:Any] = [
        "name": geofence.name ?? "",
        "floor": geofence.floor?.level ?? 0,
    ]
    
    if let payload = geofence.payload {
        if let jsonData = try? JSONSerialization.data(withJSONObject: payload, options: .fragmentsAllowed) {
            properties["payload"] = String(data: jsonData, encoding: .utf8)
        }
    }
    
    return [
        "type": "Feature",
        "id": geofence.identifier,
        "properties": properties,
        "geometry": [
            "type": "Polygon",
            "coordinates": geofence.points
        ]
    ]
}

private func IAFloorplan2Map(floorplan: IAFloorPlan) -> [String: Any] {
    return [
        "id": floorplan.floorPlanId ?? "",
        "name": floorplan.name ?? "",
        "url": floorplan.imageUrl?.absoluteString ?? "",
        "floorLevel": floorplan.floor?.level ?? 0,
        "bearing": floorplan.bearing,
        "bitmapWidth": floorplan.width,
        "bitmapHeight": floorplan.height,
        "widthMeters": floorplan.widthMeters,
        "heightMeters": floorplan.heightMeters,
        "metersToPixels": floorplan.meterToPixelConversion,
        "pixelsToMeters": floorplan.pixelToMeterConversion,
        "bottomLeft": [floorplan.bottomLeft.longitude, floorplan.bottomLeft.latitude],
        "center": [floorplan.center.longitude, floorplan.center.latitude],
        "topLeft": [floorplan.topLeft.longitude, floorplan.topLeft.latitude],
        "topRight": [floorplan.topRight.longitude, floorplan.topRight.longitude]
    ]
}

private func IAVenue2Map(venue: IAVenue) -> [String: Any] {
    var map:[String:Any] = [
        "id": venue.id,
        "name": venue.name
    ]
    let plans = venue.floorplans.map { fp in
        return IAFloorplan2Map(floorplan: fp as! IAFloorPlan)
    }
    if plans.count > 0 {
        map["floorPlans"] = plans
    }

    if let geofences = venue.geofences {
        let fences = geofences.map { fene in
            return IAGeofence2Map(geofence: fene)
        }
        if fences.count > 0 {
            map["geofences"] = fences
        }
    }

    if let venuePois = venue.pois {
        let pois = venuePois.map { poi in
            return IAPOI2Map(poi: poi)
        }
        if pois.count > 0 {
            map["pois"] = pois
        }
    }
    
    return map
}

private func IARegion2Map(region: IARegion) -> [String: Any] {
    var map:[String:Any] = [
        "regionId": region.identifier,
        "timestamp": Int64((region.timestamp?.timeIntervalSince1970 ?? 0) * 1000),
        "regionType": region.type.rawValue
    ]
    if let fp = region.floorplan {
        map["floorPlan"] = IAFloorplan2Map(floorplan:fp)
    }
    if let venue = region.venue {
        map["venue"] = IAVenue2Map(venue:venue)
    }
    return map
}

private func IALocation2Map(location: IALocation) -> [String:Any] {
    let clLoc = location.location!
    var map:[String:Any] = [
        "latitude": location.latLngFloor.latitude,
        "longitude": location.latLngFloor.longitude,
        "accuracy": clLoc.horizontalAccuracy,
        "altitude": clLoc.altitude,
        "heading": clLoc.course,
        "floorCertainty": location.floor?.certainty ?? 0,
        "flr": location.floor?.level ?? 0,
        "velocity": clLoc.speed,
        "timestamp": Int64(clLoc.timestamp.timeIntervalSince1970 * 1000)
    ]
    if let region = location.region {
        map["region"] = IARegion2Map(region:region)
    }
    return map
}

private func IARoutePoint2Map(rp: IARoutePoint) -> [String: Any] {
    return [
        "latitude": rp.latLngFloor.latitude,
        "longitude": rp.latLngFloor.longitude,
        "floor": rp.floor
    ]
}

private func IARoute2Map(route: IARoute) -> [String:Any] {
    let legs = route.legs.map { leg in
        return [
            "begin": IARoutePoint2Map(rp: leg.begin),
            "end": IARoutePoint2Map(rp: leg.end),
            "direction": leg.direction,
            "edgeIndex": leg.edgeIndex
        ]
    }
    var errName: String = "NO_ERROR"
    switch(route.error) {
    case .iaRouteErrorNoError:
        break
    case .iaRouteErrorGraphNotAvailable:
        errName = "GRAPH_NOT_AVAILABLE"
    case .iaRouteErrorRoutingFailed:
        errName = "ROUTING_FAILED"
    @unknown default:
        fatalError()
    }
    return [
        "legs": legs,
        "error": errName
    ]
}

public class SwiftIAFlutterPlugin: NSObject, FlutterPlugin, IALocationManagerDelegate {
    private var _locationManager = IALocationManager.sharedInstance()
    private var _locationServiceRunning = false
    private static var CHANNEL:FlutterMethodChannel? = nil
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.indooratlas.flutter", binaryMessenger: registrar.messenger())
        let instance = SwiftIAFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        SwiftIAFlutterPlugin.CHANNEL = channel
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        // TODO: use reflection way to call function
        let args = call.arguments as? [Any]
        var ret:Any? = nil
        switch(call.method) {
        case "initialize":
            initialize(pluginVersion: args?.first as! String, apiKey: args?[1] as! String, endpoint: args?[2] as! String)
        case "getTraceId":
            ret = getTraceId()
        case "lockIndoors":
            lockIndoors(locked: args?.first as! Bool)
        case "lockFloor":
            lockFloor(floor: args?.first as! Int32)
        case "unlockFloor":
            unlockFloor()
        case "setPositioningMode":
            setPositioningMode(mode: args?.first as! Int)
        case "setOutputThresholds":
            setOutputThresholds(distance: args?.first as! Double, interval: args?[1] as! Double)
        case "startPositioning":
            startPositioning()
        case "stopPositioning":
            stopPositioning()
        case "setSensitivities":
            setSensitivities(orientationSensitivity: args?.first as! Double, headingSensitivity: args?[1] as! Double)
        case "startWayfinding":
            startWayfinding(lat: args?.first as! Double, lon: args?[1] as! Double, floor: args?[2] as! Int)
        case "stopWayfinding":
            stopWayfinding()
        default:
            break
        }
        result(ret)
    }
    
    func initialize(pluginVersion: String, apiKey: String, endpoint: String) {
        assert(!pluginVersion.isEmpty)
        _locationManager.delegate = self
        _locationManager.setObject([
            "name": "flutter",
            "version": pluginVersion
        ], forKey: "IAWrapper")
        if !endpoint.isEmpty {
            _locationManager.setObject(endpoint, forKey: "IACustomEndpoint")
        }
        // setApiKey must be called last
        _locationManager.setApiKey(apiKey, andSecret: "not-used-in-the-flutter-plugin")
        _locationServiceRunning = false
    }
    
    func getTraceId() -> String? {
        return _locationManager.extraInfo?[kIATraceId] as? String
    }
    
    func lockIndoors(locked: Bool) {
        _locationManager.lockIndoors(locked)
    }
    
    func lockFloor(floor: Int32) {
        _locationManager.lockFloor(floor)
    }
    
    func unlockFloor() {
        _locationManager.unlockFloor()
    }
    
    func setPositioningMode(mode: Int) {
        var accuracy = ia_location_accuracy.iaLocationAccuracyBest
        switch(mode) {
        case 0:
            accuracy = .iaLocationAccuracyBest
        case 1:
            accuracy = .iaLocationAccuracyLow
        case 2:
            accuracy = .iaLocationAccuracyBestForCart
        default:
            break
        }
        _locationManager.desiredAccuracy = accuracy
    }
    
    func setOutputThresholds(distance: Double, interval: Double) {
        if (distance < 0 || interval < 0) {
          // figure out how to return error
          return
        }
        let wasRunning = _locationServiceRunning
        if wasRunning {
            stopPositioning()
        }
        if distance >= 0 {
            _locationManager.distanceFilter = distance
        }
        if interval >= 0 {
            _locationManager.timeFilter = interval
        }
        if wasRunning {
            startPositioning()
        }
        
    }
    
    func startPositioning() {
        _locationManager.startUpdatingLocation()
        _locationServiceRunning = true
    }
    
    func stopPositioning() {
        _locationManager.stopUpdatingLocation()
        _locationServiceRunning = false
    }
    
    func setSensitivities(orientationSensitivity: Double, headingSensitivity: Double) {
        _locationManager.attitudeFilter = orientationSensitivity
        _locationManager.headingFilter = headingSensitivity
    }

    func startWayfinding(lat: Double, lon: Double, floor: Int) {
        let target = IALatLngFloor(latitude: lat, andLongitude: lon, andFloor: floor)
        _locationManager.startMonitoring(forWayfinding: target)
    }
    
    func stopWayfinding() {
        _locationManager.stopMonitoringForWayfinding()
    }
        
    // MARK: IALocationManagerDelegate
    public func indoorLocationManager(_ manager: IALocationManager, statusChanged status: IAStatus) {
        var mappedStatus:Int = 0
        switch(status.type) {
        case .iaStatusServiceOutOfService:
            mappedStatus = 0
        case .iaStatusServiceUnavailable:
            mappedStatus = 1
        case .iaStatusServiceAvailable:
            mappedStatus = 2
        case .iaStatusServiceLimited:
            mappedStatus = 3
        @unknown default:
            fatalError()
        }
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onStatusChanged", arguments: [mappedStatus])
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didUpdateLocations locations: [Any]) {
        let locs = (locations as! [IALocation]).map { loc in
            return IALocation2Map(location: loc)
        }
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onLocationChanged", arguments: locs)
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didEnter region: IARegion) {
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onEnterRegion", arguments: [IARegion2Map(region:region)])
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didExitRegion region: IARegion) {
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onExitRegion", arguments: [IARegion2Map(region:region)])
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didUpdate newAttitude: IAAttitude) {
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onOrientationChanged", arguments: [Int64((newAttitude.timestamp?.timeIntervalSince1970 ?? 0) * 1000), newAttitude.quaternion.w, newAttitude.quaternion.x, newAttitude.quaternion.y, newAttitude.quaternion.z ])
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didUpdate newHeading: IAHeading) {
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onHeadingChanged", arguments: [Int64((newHeading.timestamp?.timeIntervalSince1970 ?? 0) * 1000), newHeading.trueHeading])
    }
    
    public func indoorLocationManager(_ manager: IALocationManager, didUpdate route: IARoute) {
        SwiftIAFlutterPlugin.CHANNEL?.invokeMethod("onWayfindingUpdate", arguments: [IARoute2Map(route:route)])
    }
    
}
