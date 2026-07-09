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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.0.4/SuperkassaCore.xcframework.zip",
            checksum: "dd4643875adbf2160b28767e56da8dfc8d201edb97384ad13c4f8504e0c582f8"
        )
    ]
)
