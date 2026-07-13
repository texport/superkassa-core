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
            url: "https://github.com/texport/superkassa-core/releases/download/v1.1.5/SuperkassaCore.xcframework.zip",
            checksum: "369854a8abe8329a5fbf6ab90ba23b8a687da2f15787746802b475ad58e184ee"
        )
    ]
)
