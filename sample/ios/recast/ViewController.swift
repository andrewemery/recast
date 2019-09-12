import UIKit
import lib

class ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()       
        ClassExample().getUser(id: "14") { (result: LibResult<User>) -> Void in
            let user: User? = result.getOrNull()
            if (user != nil) { self.label.text = user!.id }
            else { NSLog("complete with error") }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    @IBOutlet weak var label: UILabel!
}
