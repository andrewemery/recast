import XCTest
import lib

class iosAppTests: XCTestCase {
    func testExample() {
        assert(SampleClass().getUserSync(id: "12").id == "12")
    }
}
