// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "SuperkassaCore",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SuperkassaCore",
            targets: ["SuperkassaCore"]
        ),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "SuperkassaCore",
            url: "https://github.com/texport/superkassa-core/releases/download/v1.0.0/SuperkassaCore.xcframework.zip",
            checksum: "9cf97043c7a621c0e602edcc6f8ffc890b496b3ac66f68f4c760117d6ff1df55"
        )
    ]
)
