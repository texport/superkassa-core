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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.0.1/SuperkassaCore.xcframework.zip",
            checksum: "bc25296318de377291a1643696af4163e32d0dee1ca97eed43bd4a9a0c812203"
        )
    ]
)
