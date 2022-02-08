import Flutter
import UIKit

public class IAFlutterEngine: NSObject {
}

public class SwiftIAFlutterPlugin: NSObject, FlutterPlugin {
  var _engine: IAFlutterEngine;

  init(engine: IAFlutterEngine) {
    _engine = engine;
  }

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.indooratlas.flutter", binaryMessenger: registrar.messenger())
    let instance = SwiftIAFlutterPlugin(engine: IAFlutterEngine())
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    print(call)
    result("iOS " + UIDevice.current.systemVersion)
  }
}
