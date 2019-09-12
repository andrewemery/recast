import XCTest
import lib

class iosAppTests: XCTestCase {
    func testSync() {
        assert(ClassExample().getUserSync(id: "12").id == "12")
    }
    
    func testAsync() {
        let expectation = self.expectation(description: "user")
        ClassExample().getUser(id: "12") { (result: LibResult<User>) -> Void in
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        waitForExpectations(timeout: 15, handler: nil)
    }
    
    func testAsyncScoped() {
        let expectation = self.expectation(description: "user")
        let scope = CoroutinesKt.supervisorScope()
        _1Kt.getUserScoped(id: "12", scope: scope) { (result: LibResult<User>) -> Void in  // TODO: rename base file
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        waitForExpectations(timeout: 15, handler: nil)
    }
    
    func testAsyncDelayed() {
        let expectation = self.expectation(description: "user")
        let scope = CoroutinesKt.supervisorScope()
        _3Kt.getUserDelayed(id: "12", scope: scope) { (result: LibResult<User>) -> Void in  // TODO: rename base file
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        waitForExpectations(timeout: 15, handler: nil)
    }
    
    func testAsyncCancelScope() {
        let expectation = self.expectation(description: "user")
        expectation.isInverted = true
        let scope = CoroutinesKt.supervisorScope()
        _3Kt.getUserDelayed(id: "12", scope: scope) { (result: LibResult<User>) -> Void in
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        scope.cancel()
        waitForExpectations(timeout: 2, handler: nil)
    }
    
    func testAsyncCancelJob() {
        let expectation = self.expectation(description: "user")
        expectation.isInverted = true
        let job = _3Kt.getUserDelayed(id: "12", scope: CoroutinesKt.supervisorScope()) { (result: LibResult<User>) -> Void in
            XCTAssertTrue(result.isSuccess)
            let user: User = result.getOrNull()!
            XCTAssertEqual(user.id, "12")
            expectation.fulfill()
        }
        job.cancel()
        waitForExpectations(timeout: 2, handler: nil)
    }
}
