#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint indooratlas.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'indooratlas'
  s.version          = '0.0.1'
  s.summary          = 'IndoorAtlas Flutter Plugin'
  s.description      = <<-DESC
IndoorAtlas Flutter Plugin
                       DESC
  s.homepage         = 'https://indooratlas.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'IndoorAtlas' => 'support@indooratlas.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '8.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
