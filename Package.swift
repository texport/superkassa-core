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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.0.3/SuperkassaCore.xcframework.zip",
            checksum: "5303d7c6b66a35e6fdbb7e09ec165ec71944bbc4c14dafdd69e66ebe2caf6981"
        )
    ]
)
