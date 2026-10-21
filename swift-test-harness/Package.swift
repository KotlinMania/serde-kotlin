// swift-tools-version: 6.0
import Foundation
import PackageDescription

let exportBuild = ProcessInfo.processInfo.environment["KOTLINMANIA_SWIFT_EXPORT_BUILD"] ?? "../build"

let package = Package(
    name: "SwiftTestHarness",
    platforms: [.macOS(.v15)],
    dependencies: [
        .package(name: "Serde", path: "\(exportBuild)/SPMPackage/macosArm64/Debug")
    ],
    targets: [
        .testTarget(
            name: "SwiftTestHarnessTests",
            dependencies: [
                .product(name: "SerdeLibrary", package: "Serde")
            ],
            linkerSettings: [
                .unsafeFlags([
                    "-L", "\(exportBuild)/swift-test",
                    "-lSerde",
                ]),
            ]
        ),
    ],
    swiftLanguageModes: [.v6]
)
