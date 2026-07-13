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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.1.4/SuperkassaCore.xcframework.zip",
            checksum: "8fe99ebc4e4ae396b0b6c40dd15e6ef00409bd46732d003801c90e6e75132009"
        )
    ]
)
