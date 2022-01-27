import 'package:flutter/widgets.dart';
import 'indooratlas.dart';

class _IndoorAtlasListenerState extends State<IndoorAtlasListener> {
  void _enable(IAListener? old) {
    if (widget.enabled) {
      if (widget.configuration != null) {
        // NOTE: Overrides any other configuration!
        //       IndoorAtlas configurations are global!
        IndoorAtlas.configure(widget.configuration!);
      }
      if (old != null) {
        IndoorAtlas.resubscribe(old, widget.listener);
      } else {
        IndoorAtlas.subscribe(widget.listener);
      }
    } else if (old != null) {
      IndoorAtlas.unsubscribe(old);
    }
  }

  @override
  void initState() {
    super.initState();
    _enable(null);
  }

  @override
  void didUpdateWidget(IndoorAtlasListener old) {
    super.didUpdateWidget(old);
    _enable(old.listener);
  }

  @override
  void dispose() {
    IndoorAtlas.unsubscribe(widget.listener);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return widget.child;
  }
}

typedef IAOnStatusCb = void Function(IAStatus status, String message);
typedef IAOnVenueCb = void Function(bool enter, IAVenue venue);
typedef IAOnFloorplanCb = void Function(bool enter, IAFloorplan floorplan);
typedef IAOnWayfindingRouteCb = void Function(
    IARoute route, IAWayfindingRequest request);
typedef IAOnOrientationCb = void Function(
    double x, double y, double z, double w);

class IACallbackListener extends IAListener {
  final IAOnStatusCb? onStatusCb;
  final ValueSetter<IALocation>? onLocationCb;
  final IAOnVenueCb? onVenueCb;
  final IAOnFloorplanCb? onFloorplanCb;
  final IAOnWayfindingRouteCb? onWayfindingRouteCb;
  final IAOnOrientationCb? onOrientationCb;
  final ValueSetter<double>? onHeadingCb;
  const IACallbackListener({
    required String name,
    required this.onStatusCb,
    required this.onLocationCb,
    required this.onVenueCb,
    required this.onFloorplanCb,
    required this.onWayfindingRouteCb,
    required this.onOrientationCb,
    required this.onHeadingCb,
  }) : super(name);

  @override
  void onStatus(IAStatus status, String message) {
    onStatusCb?.call(status, message);
  }

  @override
  void onLocation(IALocation position) {
    onLocationCb?.call(position);
  }

  @override
  void onVenue(bool enter, IAVenue venue) {
    onVenueCb?.call(enter, venue);
  }

  @override
  void onFloorplan(bool enter, IAFloorplan floorplan) {
    onFloorplanCb?.call(enter, floorplan);
  }

  @override
  void onWayfindingRoute(IARoute route, IAWayfindingRequest request) {
    onWayfindingRouteCb?.call(route, request);
  }

  @override
  void onOrientation(double x, double y, double z, double w) {
    onOrientationCb?.call(x, y, z, w);
  }

  @override
  void onHeading(double heading) {
    onHeadingCb?.call(heading);
  }
}

class IndoorAtlasListener extends StatefulWidget {
  final Widget child;
  final IACallbackListener listener;
  final IAConfiguration? configuration;
  final bool enabled;
  IndoorAtlasListener({
    Key? key,
    required String name,
    this.enabled = true,
    this.child = const SizedBox.shrink(),
    this.configuration = null,
    IAOnStatusCb? onStatus,
    ValueSetter<IALocation>? onLocation,
    IAOnVenueCb? onVenue,
    IAOnFloorplanCb? onFloorplan,
    IAOnWayfindingRouteCb? onWayfindingRoute,
    IAOnOrientationCb? onOrientation,
    ValueSetter<double>? onHeading,
  })  : listener = IACallbackListener(
          name: name,
          onStatusCb: onStatus,
          onLocationCb: onLocation,
          onVenueCb: onVenue,
          onFloorplanCb: onFloorplan,
          onWayfindingRouteCb: onWayfindingRoute,
          onOrientationCb: onOrientation,
          onHeadingCb: onHeading,
        ),
        super(key: key);
  @override
  State<StatefulWidget> createState() => _IndoorAtlasListenerState();
}
