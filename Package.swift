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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.1.0/SuperkassaCore.xcframework.zip",
            checksum: "af74e3390e250a682ae79d7a15bc61f5018b47b1ff67e7d889e16a41a6c68251"
        )
    ]
)
