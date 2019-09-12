import XCTest
import lib

class iosAppTests: XCTestCase {
    func testSync() {
        assert(SampleClass().getUserSync(id: "12").id == "12")
    }
    
    func testAsync() {
        let expectation = self.expectation(description: "user")
        SampleClass().getUser(id: "12", scope: CoroutinesKt.GlobalScope) { (result: LibResult<User>) -> Void in
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        waitForExpectations(timeout: 15, handler: nil)
    }
}
