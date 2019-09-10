import UIKit
import lib

class ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        label.text = SampleClass().getUserSync(id: "12").id
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    @IBOutlet weak var label: UILabel!
}
