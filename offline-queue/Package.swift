// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "SuperkassaOfflineQueue",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SuperkassaOfflineQueue",
            targets: ["SuperkassaOfflineQueue"]
        ),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "SuperkassaOfflineQueue",
            url: "https://github.com/texport/superkassa-offline-queue/releases/download/v1.0.3/SuperkassaOfflineQueue.xcframework.zip",
            checksum: "ad9a3f622b325aed4822677bf924b6e6a63930c08a1ab71be4c5850ef54ed409"
        )
    ]
)
