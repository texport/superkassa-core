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
            checksum: "986adc73a9b1b76974f9e27a9bbf1951090b5c4d12f407fc2baabb166063ad6d"
        )
    ]
)
