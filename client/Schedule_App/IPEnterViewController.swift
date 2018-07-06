//
//  IPEnterViewController.swift
//  Schedule_App
//
//  Created by Drew Davis on 6/22/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class IPEnterViewController: UIViewController {

    @IBOutlet var IPEnterViewControllerOutRef: UIView!
    
    @IBOutlet weak var IP_inTextField: UITextField!
    @IBOutlet weak var IP_enterButton: UIButton!
    
    var IPaddress : String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        IP_enterButton.isEnabled = false

        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    
    @IBAction func IP_textFiel_doneEditing(_ sender: UITextField) {
        IP_enterButton.isEnabled = true
    }
    
    @IBAction func IP_enterButton_click(_ sender: UIButton) {
        IPaddress = IP_inTextField.text
        
        self.performSegue(withIdentifier: "IPEnteredSegue", sender: sender)
    }
    
    @IBAction func IP_textField_EditingChanged(_ sender: UITextField) {
        IP_enterButton.isEnabled = true
    }
    
    
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let destinationViewController = segue.destination as? ViewController {
            destinationViewController.serverIP = IPaddress
        }
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
