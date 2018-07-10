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
    @IBOutlet weak var Agent0_enterButton: UIButton!
    @IBOutlet weak var Agent1_enterButton: UIButton!
    
    
    var IPaddress : String?
    var agentNum  : String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        Agent0_enterButton.isEnabled = false
        Agent1_enterButton.isEnabled = false

        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    
    @IBAction func IP_textFiel_doneEditing(_ sender: UITextField) {
        Agent0_enterButton.isEnabled = true
        Agent1_enterButton.isEnabled = true
    }
    
    @IBAction func IP_enterButton_click(_ sender: UIButton) {
        IPaddress = IP_inTextField.text
        
        if (sender.titleLabel!.text! == "Login as Agent 0") {agentNum = "0"}
        else if (sender.titleLabel!.text == "Login as Agent 1") {agentNum = "1"}
        
        self.performSegue(withIdentifier: "IPEnteredSegue", sender: sender)
    }
    
    
    @IBAction func IP_textField_EditingChanged(_ sender: UITextField) {
        Agent0_enterButton.isEnabled = true
        Agent1_enterButton.isEnabled = true
    }
    
    
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let destinationViewController = segue.destination as? ViewController {
            destinationViewController.serverIP = IPaddress
            destinationViewController.agentNumber = agentNum
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
