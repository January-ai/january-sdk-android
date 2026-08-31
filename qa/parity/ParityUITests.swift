import XCTest

final class ParityUITests: XCTestCase {
    let app = XCUIApplication(bundleIdentifier: "ai.january.partner.demo.parity")
    override func setUpWithError() throws {
        continueAfterFailure = false
        request("/__reset")
        app.launchEnvironment["PARITY_FIXTURES"] = "1"
        app.launch()
        XCTAssertTrue(app.tabBars.buttons["Search"].waitForExistence(timeout: 15))
    }
    func request(_ path: String) {
        let semaphore = DispatchSemaphore(value: 0)
        URLSession.shared.dataTask(with: URL(string: "http://127.0.0.1:18765" + path)!) { _,_,_ in semaphore.signal() }.resume()
        XCTAssertEqual(semaphore.wait(timeout: .now() + 10), .success)
    }
    func control(_ route: String, status: Int = 200, delay: Int = 0, empty: Bool = false) {
        request("/__control?route=\(route)&status=\(status)&delay=\(delay)&empty=\(empty)")
    }
    func element(_ label: String) -> XCUIElement {
        let button = app.buttons.matching(identifier: label).firstMatch
        return button.exists ? button : app.staticTexts.matching(identifier: label).firstMatch
    }
    @discardableResult func reveal(_ label: String) -> XCUIElement {
        for _ in 0..<8 {
            let e = element(label)
            if e.exists && e.isHittable { return e }
            app.swipeUp()
        }
        let e = element(label)
        XCTAssertTrue(e.exists && e.isHittable, "Missing visible element: \(label)\n\(app.debugDescription)")
        return e
    }
    func tap(_ label: String) { reveal(label).tap() }
    func tab(_ label: String) { app.tabBars.buttons[label].tap() }
    func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot());attachment.name = "ios-"+name;attachment.lifetime = .keepAlways;add(attachment)
    }
    func wait(_ label: String) { XCTAssertTrue(app.staticTexts[label].firstMatch.waitForExistence(timeout: 12), "Expected \(label)") }
    func searchFood() {
        let field = app.textFields["Food name"];XCTAssertTrue(field.waitForExistence(timeout: 5));field.tap();field.typeText("oatmeal")
        tap("Search foods")
        wait("Fixture oatmeal")
        tap("Fixture oatmeal")
    }
    func addFood() {
        let field = app.textFields["Search foods"];XCTAssertTrue(field.waitForExistence(timeout: 5));field.tap();field.typeText("oatmeal\n")
        wait("Fixture oatmeal");tap("Fixture oatmeal");wait("Choose serving");capture("serving");tap("Add to meal")
    }
    func testSearchAndDetails() {
        capture("search-initial")
        control("/v1.2/foods", status: 500, delay: 4)
        app.textFields["Food name"].tap();app.textFields["Food name"].typeText("oatmeal");capture("search-keyboard")
        tap("Search foods");capture("search-loading")
        wait("January couldn’t complete the request");reveal("Try again");capture("search-error")
        for (status,title) in [(401,"Couldn’t use the configured credentials"),(403,"Couldn’t use the configured credentials"),(404,"No matching result was found"),(422,"Check the information you entered"),(429,"Too many requests")] {
            control("/v1.2/foods",status:status);tap("Try again");wait(title);reveal("Try again");capture("search-error-\(status)")
        }
        control("/v1.2/foods", empty:true);tap("Try again");wait("No foods found");capture("search-empty")
        control("/v1.2/foods");tap("Search foods");wait("Fixture oatmeal");capture("search-results");tap("Fixture oatmeal");capture("food-detail")
        reveal("Check glucose");capture("food-nutrition")
        control("/v1.2/glucose/predictions",status:500,delay:4);tap("Check glucose");capture("food-glucose-loading");wait("January couldn’t complete the request");capture("food-glucose-error")
        control("/v1.2/glucose/predictions");tap("Try again");wait("Medium impact");capture("food-glucose-result");tap("Close glucose response")
        tap("Find alternatives");capture("alternatives-initial")
        control("/v1.2/foods/101/alternatives",status:500,delay:4);tap("Find alternatives");capture("alternatives-loading");wait("January couldn’t complete the request");reveal("Try again");capture("alternatives-error")
        control("/v1.2/foods/101/alternatives",empty:true);tap("Try again");wait("No suitable alternatives");capture("alternatives-empty")
        control("/v1.2/foods/101/alternatives");tap("Refresh alternatives");wait("Fixture lentils");capture("alternatives-results")
    }
    func testScan() {
        tab("Scan");capture("scan-initial");tap("Image URL");capture("image-url");tap("Close image URL entry")
        tap("Sample meal");capture("scan-preview")
        control("/v1.2/food-scans/photo",status:500,delay:4);tap("Analyze meal");capture("scan-loading");wait("January couldn’t complete the request");reveal("Try again");capture("scan-error")
        control("/v1.2/food-scans/photo");tap("Try again");wait("Fixture breakfast");capture("scan-result");tap("Correct result");capture("correction-initial")
    }
    func testLogs() {
        tab("Food Logs");capture("logs-initial");reveal("No food logs in this range");capture("logs-empty")
        control("/v1.2/food-logs",status:500,delay:4);tap("Refresh food logs");capture("logs-loading");wait("January couldn’t complete the request");reveal("Try again");capture("logs-error")
        control("/v1.2/food-logs");request("/__seed");tap("Try again");wait("Fixture breakfast");reveal("Fixture breakfast");capture("logs-results");tap("Fixture breakfast");capture("log-detail");tap("Edit");capture("log-edit");tap("Close food log editor")
    }
    func testGlucose() {
        tab("Glucose");capture("glucose-profile");tap("Health conditions");capture("conditions");tap("Prediabetes");app.navigationBars.buttons.element(boundBy:0).tap()
        tap("Add food to prediction");capture("food-picker-initial");addFood()
        control("/v1.2/glucose/predictions",status:500,delay:4);tap("Estimate glucose response");capture("glucose-loading");wait("January couldn’t complete the request");reveal("Try again");capture("glucose-error")
        control("/v1.2/glucose/predictions");tap("Try again");wait("Estimated response");capture("glucose-result");tap("Adjust meal");tap("Settings");capture("settings")
    }
    func testRestaurants() {
        tap("Restaurants");capture("restaurants-initial");tap("Search location");capture("restaurant-filters");tap("Apply filters")
        app.textFields["Restaurant name"].tap();app.textFields["Restaurant name"].typeText("Fixture Cafe")
        control("/v1.2/restaurants",status:500,delay:4);tap("Search nearby");capture("restaurants-loading");wait("January couldn’t complete the request");reveal("Try again");capture("restaurants-error")
        control("/v1.2/restaurants",empty:true);tap("Try again");capture("restaurants-empty")
        control("/v1.2/restaurants");tap("Search nearby");wait("Fixture Cafe");tap("Fixture Cafe");wait("Fixture bowl");capture("restaurant-detail");tap("Fixture bowl");capture("menu-detail")
    }
    func testLogMutationErrors() {
        tab("Food Logs");tap("Add food log");capture("log-new");tap("Add first food");addFood()
        control("/v1.2/food-logs",status:500,delay:4);tap("Save food log");capture("log-save-loading");wait("January couldn’t complete the request");reveal("Try again");capture("log-save-error")
        control("/v1.2/food-logs");tap("Try again");wait("Fixture breakfast");tap("Fixture breakfast");tap("Edit");tap("Update food log");wait("Food logs");tap("Fixture breakfast")
        tap("Delete food log");capture("log-delete-confirmation")
        control("/v1.2/food-logs/11111111-1111-4111-8111-111111111111",status:500)
        app.buttons.matching(identifier: "Delete food log").allElementsBoundByIndex.last!.tap();wait("January couldn’t complete the request");reveal("Try again");capture("log-delete-error")
        control("/v1.2/food-logs/11111111-1111-4111-8111-111111111111");tap("Try again");wait("Food logs");reveal("No food logs in this range");capture("log-delete-result")
    }
    func testCorrectionRecovery() {
        tab("Scan");tap("Sample meal");tap("Analyze meal");wait("Fixture breakfast");tap("Correct result")
        let field=app.textViews.firstMatch;XCTAssertTrue(field.waitForExistence(timeout:5));field.tap();field.typeText("This was lentils")
        control("/v1.2/food-scans/corrections",status:500,delay:4);tap("Submit correction");capture("correction-loading");wait("January couldn’t complete the request");reveal("Try again");capture("correction-error")
        control("/v1.2/food-scans/corrections");tap("Try again");wait("Corrected breakfast");capture("correction-result")
    }
    func testRestaurantSelectionUsesRestaurantID() {
        tap("Restaurants")
        app.textFields["Restaurant name"].tap()
        app.textFields["Restaurant name"].typeText("Fixture")
        tap("Search nearby");wait("Fixture Cafe");tap("Fixture Cafe")
        wait("Fixture bowl");capture("restaurant-id-regression")
    }

    func testMenuPresentation() {
        tap("Restaurants")
        app.textFields["Restaurant name"].tap()
        app.textFields["Restaurant name"].typeText("Fixture Cafe")
        tap("Search nearby");wait("Fixture Cafe")
        control("/v1.2/restaurants/cafe/menu-items",delay:4)
        tap("Fixture Cafe");wait("Loading menu");capture("menu-loading")
        wait("Fixture bowl");capture("restaurant-detail")
        tap("Fixture bowl");wait("Fixture bowl");capture("menu-detail")
    }

    func testMenuErrorAndEmpty() {
        tap("Restaurants");app.textFields["Restaurant name"].tap();app.textFields["Restaurant name"].typeText("Fixture Cafe");tap("Search nearby");wait("Fixture Cafe")
        control("/v1.2/restaurants/cafe/menu-items",status:500,delay:4);tap("Fixture Cafe");capture("menu-loading");wait("January couldn’t complete the request");capture("menu-error")
        control("/v1.2/restaurants/cafe/menu-items",empty:true);tap("Try again");wait("No menu items found");capture("menu-empty")
    }
    func testSetupScreen() {
        app.terminate();app.launchEnvironment.removeValue(forKey:"PARITY_FIXTURES");app.launch();capture("setup")
    }

    func testRemainingRequestFailures() {
        tap("Restaurants");app.textFields["Restaurant name"].tap();app.textFields["Restaurant name"].typeText("Fixture Cafe")
        control("/v1.2/restaurants",status:500,delay:4);tap("Search nearby");capture("restaurants-loading");wait("January couldn’t complete the request");reveal("Try again");capture("restaurants-error")
        tab("Scan");tap("Sample meal");control("/v1.2/food-scans/photo",status:500,delay:4);tap("Analyze meal");capture("scan-loading");wait("January couldn’t complete the request");reveal("Try again");capture("scan-error")
        tab("Food Logs");reveal("Refresh food logs");control("/v1.2/food-logs",status:500,delay:4);tap("Refresh food logs");capture("logs-loading");wait("January couldn’t complete the request");reveal("Try again");capture("logs-error")
    }

}
