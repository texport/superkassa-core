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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.0.2/SuperkassaCore.xcframework.zip",
            checksum: "9e887335253000fc53796ee819a1743ecf0d299b67d013b199fed7c207b52509"
        )
    ]
)
