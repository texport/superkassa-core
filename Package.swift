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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.1.7/SuperkassaCore.xcframework.zip",
            checksum: "fa2498a3e003cfcdd6e79f15c4cfb84afede3048b1fab0c3b814341623b9f835"
        )
    ]
)
