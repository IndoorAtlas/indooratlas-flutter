#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint indooratlas_flutter.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'indooratlas_flutter'
  s.version          = '0.0.1'
  s.summary          = 'IndoorAtlas Flutter Plugin'
  s.description      = 'IndoorAtlas Flutter Plugin'
  s.homepage         = 'https://indooratlas.com'
  s.author           = { 'IndoorAtlas' => 'support@indooratlas.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'IndoorAtlas', '3.4.12'
  s.platform = :ios, '9.0'
  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = {
     'DEFINES_MODULE' => 'YES',
     'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386',
     'ENABLE_BITCODE' => 'NO',
  }
  s.swift_version = '5.0'
end
