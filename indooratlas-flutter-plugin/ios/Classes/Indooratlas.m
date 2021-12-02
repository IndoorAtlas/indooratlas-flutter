#import "Indooratlas.h"
#if __has_include(<indooratlas/indooratlas-Swift.h>)
#import <indooratlas/indooratlas-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "indooratlas-Swift.h"
#endif

@implementation IAFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftIAFlutterPlugin registerWithRegistrar:registrar];
}
@end
