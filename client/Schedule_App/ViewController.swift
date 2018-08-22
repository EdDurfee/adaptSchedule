//
//  ViewController.swift
//  Schedule_App
//
//  Created by Drew Davis on 5/21/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit
import QuartzCore

// All elements in the main screen are contained in this view
class ViewController: UIViewController, UITextFieldDelegate, UITextViewDelegate {
    
    var serverIP : String?
    
    // client class handles all client-server interaction
    var aClient: client?
    
    // holds the info recieved from client from server
    var viewInInfo: fromServer = fromServer()
    
    // menuDelegate is tableViewController to control left side menu of main screen
    var menuDelegate = MenuController(style: .grouped)
    
    // this is the agent number of the client. Set at startup and never mutated
    var agentNumber : String?
    
    // keep track of all activities that this agent has confirmed
    var confirmedActsList : [String] = []
    
    
    //MARK: Properties
    @IBOutlet weak var SideMenu: UITableView!
    @IBOutlet weak var ConfirmActButton: UIButton!
    @IBOutlet weak var GanttImageDisplay: UIImageView!
    
    @IBOutlet weak var advSysClockButton: UIButton!
    
    // Modify Activty View that appears when user selects sidemenu 'modify activity'
    var modActMenuDelegate = ModActTableController(style: .grouped)
    @IBOutlet weak var ModActMenu: UITableView!
    
    @IBOutlet weak var ModifyActView: ModifyActivityView!
    @IBOutlet weak var ESTField: AvailTextBox!
    @IBOutlet weak var LSTField: AvailTextBox!
    @IBOutlet weak var EETField: AvailTextBox!
    @IBOutlet weak var LETField: AvailTextBox!
    @IBOutlet weak var DurationTextBox: UITextView!
    @IBOutlet weak var ConfirmModificationButton: UIButton!
    
    
    var origEST : String?
    var origLST : String?
    var origEET : String?
    var origLET : String?
    var origDur : String?
    var validDurationValue : Bool?
    
    
    // This is the outlet for custom xCode generated gantt chart
    // It is not complete and more of a generic horizontal chart at this point
    @IBOutlet weak var GanttChart: GanttChartView!
    
    
    // Called whenever this view (main screen) opens to the user (including app startup)
    override func viewDidLoad() {
        super.viewDidLoad()
//        NotificationCenter.default.addObserver(self, selector: #selector(ViewControl ler.keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
//        NotificationCenter.default.addObserver(self, selector: #selector(ViewController.keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        
        
        // Set the sideMenu tableView as a child view and Set the ModActMenu delegate and dataSource
//        addChildViewController(modActMenuDelegate)
        //        view.addSubview(modActMenuDelegate.view)
//        modActMenuDelegate.didMove(toParentViewController: self)
        ModActMenu.delegate = modActMenuDelegate
        ModActMenu.dataSource = modActMenuDelegate
        
        ModifyActView.isHidden = true
        
        // Set the sideMenu tableView as a child view and Set the sideMenu delegate and dataSource to MenuController (sublass of TableViewController)
        addChildViewController(menuDelegate)
//        view.addSubview(menuDelegate.view)
        menuDelegate.didMove(toParentViewController: self)
        SideMenu.delegate = menuDelegate
        SideMenu.dataSource = menuDelegate
        
        
        // testing to see how addTarget works and then see its feasibility for handeling actions inside of modifyAct view
//        menuDelegate.picker.addTarget(self, action: #selector(testTarg(sender:)), for: .valueChanged)
        
        ESTField.availConstraint = "EST";
        LSTField.availConstraint = "LST";
        EETField.availConstraint = "EET";
        LETField.availConstraint = "LET";
        
        self.DurationTextBox.delegate = self
        
        drawMenuRightLine()
        drawModActMenuRightLine()
        ModifyActView.addBackgroundShape()

//        // create a pop-up requesting the IP of the server
//        var serverIP: String? = nil
//        let alert = UIAlertController(title: "Setup", message: "Enter the IP address of the server machine.", preferredStyle: .alert)
//        //adding textfields to our dialog box
//        alert.addTextField { (textField) in
//            textField.placeholder = "IP address"
//        }
//        alert.addAction(UIAlertAction(title: "Enter", style: .default) { (_) in
//
//            //getting the input values from user
//            serverIP = alert.textFields?[0].text
//            alert.dismiss(animated: true)
//
//            })
//        self.present(alert, animated: true)
//
//        while serverIP == nil {
//            sleep(1)
//        }
    
        
        // initialize client
        self.aClient = client(serverIP!, agentNumber!)
        
        // In the background, the client should continuously send heartbeat GET messages to the server
        // Heartbeat function will update the member variables of aClient in background whenever the server replies with new info
        DispatchQueue.global(qos: .background).async {
            self.aClient!.heartbeat()
        }
        
        // In the background, changes to the sidemenu selection should be monitered and gantt request should be sent
        DispatchQueue.global(qos: .background).async {
            while (true) {
                usleep(200000) // wait microseconds
                
                
                // if the side menu activity selected has changed
                if ( self.menuDelegate.actSelectionChanged == true ) {
                    
                    DispatchQueue.main.async() {
                        // if an activity option was selected
                        if (self.SideMenu.indexPathForSelectedRow?.section == 0) {
                            
                            
                            // request new gantt chart
                            self.requestTentActGantt()
                            
                        }
                    }
                    
                    // reset actChanged flag
                    self.menuDelegate.actSelectionChanged = false
                }
                
                if self.modActMenuDelegate.actSelectionChanged == true {
                    
                    DispatchQueue.main.async() {
                        if (self.ModActMenu.indexPathForSelectedRow !=  nil) {
                            
                            self.requestActDetails()
                        }
                    }
                    
                    self.modActMenuDelegate.actSelectionChanged = false

                }
                
                // if the section selected has changed
                if self.menuDelegate.sectSelectionChanged == true {
                    
                    
                    DispatchQueue.main.async() {
                        
                        // if 'modify activity' selected, display modAct screen
                        if (self.SideMenu.indexPathForSelectedRow?.section == 1 && self.SideMenu.indexPathForSelectedRow?.row == 1) {
                            self.requestActDetails() // get the lists of modifiable activities
                            self.ModifyActView.isHidden = false
                        }
                    
                        // if 'modify activity' screen is showing but not selected on side menu, hide it
                        if ( self.ModifyActView.isHidden == false && ( self.SideMenu.indexPathForSelectedRow?.section != 1 || self.SideMenu.indexPathForSelectedRow?.row != 1 ) ) {
                            self.ModifyActView.isHidden = true
                            if self.ModActMenu.indexPathForSelectedRow != nil {
                                self.ModActMenu.deselectRow(at: self.ModActMenu.indexPathForSelectedRow!, animated: false)
                            }
                        }
                        
                    }
                    
                    // reset sectChanged flag
                    self.menuDelegate.sectSelectionChanged = false
                }
                
                
                if self.modActMenuDelegate.actSelectionChanged == true {
                    var aPut = putCMD()
                    aPut.clientID = self.aClient!.ID
                    aPut.infoType = "requestActDetails"
                    aPut.agentNum = self.agentNumber!
                    self.aClient!.sendStructToServer(aPut)
                    self.modActMenuDelegate.actSelectionChanged = false
                }
            }
        }
        
        // In the background, changes to the client should be pulled up here into the controller
        // TODO: Set up queue system to hold multiple messsages from server at once
        DispatchQueue.global(qos: .background).async {
            while (true) {
                usleep(200000) // wait microseconds
                
                // UI must be updated on the main queue (thread)
                DispatchQueue.main.async() {
                
                    // infoType will be non-empty if client has received message of significance
                    // wait for counts to be same to avoid race condition towards seg fault
                    if (self.aClient!.currentInfo.infoType != "" &&
                                self.aClient!.currentInfo.nextActivities!.count == self.aClient!.currentInfo.nextActsMaxDur!.count) {
//                        self.viewInInfo.infoType = self.aClient!.currentInfo.infoType!; self.aClient!.currentInfo.infoType = ""
//                        self.viewInInfo.startTime = self.aClient!.currentInfo.startTime!; self.aClient!.currentInfo.startTime? = ""
//                        self.viewInInfo.nextActivities = self.aClient!.currentInfo.nextActivities!; self.aClient!.currentInfo.nextActivities = []
//                        self.viewInInfo.nextActsMinDur = self.aClient!.currentInfo.nextActsMinDur!; self.aClient!.currentInfo.nextActsMinDur = []
//                        self.viewInInfo.nextActsMaxDur = self.aClient!.currentInfo.nextActsMaxDur!; self.aClient!.currentInfo.nextActsMaxDur = []
//                        self.viewInInfo.strImg = self.aClient!.currentInfo.strImg!; self.aClient!.currentInfo.strImg = ""
//                        self.viewInInfo.debugInfo = self.aClient!.currentInfo.debugInfo!; self.aClient!.currentInfo.debugInfo?.removeAll()
                        self.viewInInfo = self.aClient!.currentInfo
                        

                        // if this effects current choices, update list of possible activities and trigger a sideMenu refresh
                        if (self.viewInInfo.infoType == "currentChoices") {
                            self.menuDelegate.activityOptions = self.viewInInfo.nextActivities!
                            self.menuDelegate.minDurs = self.viewInInfo.nextActsMinDur!
                            self.menuDelegate.maxDurs = self.viewInInfo.nextActsMaxDur!
                            self.SideMenu.reloadData()
                            if self.viewInInfo.nextActivities!.count > 0 {
                                self.ConfirmActButton.isEnabled = true
                            } else {
                                self.ConfirmActButton.isEnabled = false
                            }
                        }
                    
                        if (self.viewInInfo.infoType == "ganttImage") {
                            
                        }
                        
                        if (self.viewInInfo.infoType == "activityDetails") {
                            
                            // if not already populated, fill table with modifiable activities
                            if (self.modActMenuDelegate.allModifiableActivities == nil) {
                                self.modActMenuDelegate.modifiableDurActs    = self.viewInInfo.actDetails?.modifiableDurActs
                                self.modActMenuDelegate.modifiableAvailActs  = self.viewInInfo.actDetails?.modifiableAvailActs
                                
                                if self.modActMenuDelegate.modifiableDurActs == nil && self.modActMenuDelegate.modifiableAvailActs == nil {
                                    self.modActMenuDelegate.allModifiableActivities = []
                                } else if self.modActMenuDelegate.modifiableDurActs == nil {
                                    self.modActMenuDelegate.allModifiableActivities = Array(Set( self.modActMenuDelegate.modifiableAvailActs! ))
                                } else if self.modActMenuDelegate.modifiableAvailActs == nil {
                                    self.modActMenuDelegate.allModifiableActivities = Array(Set( self.modActMenuDelegate.modifiableDurActs! ))
                                } else {
                                    self.modActMenuDelegate.allModifiableActivities = Array(Set( self.modActMenuDelegate.modifiableDurActs! + self.modActMenuDelegate.modifiableAvailActs! ))
                                }
                                
                                self.ModActMenu.reloadData()
                            }
                            
                            if (self.viewInInfo.actDetails!.actName != "") {
                                self.ESTField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.EST! )
                                self.LSTField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.LST! )
                                self.EETField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.EET! )
                                self.LETField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.LET! )
                                
                                self.ESTField.validValue = true
                                self.LSTField.validValue = true
                                self.EETField.validValue = true
                                self.LETField.validValue = true
                                self.validDurationValue = true
                                
                                self.origEST = String(Int(Float(self.viewInInfo.actDetails!.EST!)!))
                                self.origLST = String(Int(Float(self.viewInInfo.actDetails!.LST!)!))
                                self.origEET = String(Int(Float(self.viewInInfo.actDetails!.EET!)!))
                                self.origLET = String(Int(Float(self.viewInInfo.actDetails!.LET!)!))
                                
                                var durStr : String = ""
                                for i in 0..<self.viewInInfo.actDetails!.minDurs!.count {
                                    durStr = durStr + self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.minDurs![i]) + " - "
                                        + self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.maxDurs![i]) + "\n"
                                }
                                self.DurationTextBox.text = durStr
                                self.origDur = durStr
                            }
                        }
                        
                        if (self.viewInInfo.strImg != "") {
                            if let decodedData = Data(base64Encoded: self.viewInInfo.strImg!, options: .ignoreUnknownCharacters) {
                                let image = UIImage(data: decodedData)
                                self.GanttImageDisplay.image = image
                            }
                        }
                    
                    self.aClient!.currentInfo = fromServer()
                    }
                }
                
                
            }
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillShow, object: self.view.window)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillHide, object: self.view.window)
    }
    
//    @objc func keyboardWillShow(notification: NSNotification) {
//        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.cgRectValue {
//            if self.view.frame.origin.y == 0{
//                self.view.frame.origin.y -= keyboardSize.height
//            }
//        }
//    }
//
//    @objc func keyboardWillHide(notification: NSNotification) {
//        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.cgRectValue {
//            if self.view.frame.origin.y != 0{
//                self.view.frame.origin.y += keyboardSize.height
//            }
//        }
//    }

//    override func viewDidAppear(_ animated: Bool) {
//        var gotBars : [GanttChartView.BarEntry] = []
//        gotBars.append( GanttChartView.BarEntry(color: UIColor.blue,     length: 0.2,   textValue: "20",  title: "First") )
//        gotBars.append( GanttChartView.BarEntry(color: UIColor.orange,   length: 0.80,  textValue: "80",  title: "Second") )
//        gotBars.append( GanttChartView.BarEntry(color: UIColor.magenta,  length: 0.9,   textValue: "90",  title: "Third") )
//        gotBars.append( GanttChartView.BarEntry(color: UIColor.brown,    length: 0.4,   textValue: "40",  title: "Fourth") )
//
//        gotBars.append(contentsOf: gotBars)
//        gotBars.append(contentsOf: gotBars)
//        gotBars.append(contentsOf: gotBars)
//        gotBars.append(contentsOf: gotBars)
//        gotBars.append(contentsOf: gotBars)
//
//        GanttChart.dataEntries = gotBars
//    }

    // Auto-generated function to handle memory overuse
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    //MARK: Actions
    
    // This function is called when the 'confirm activity' button is clicked
    // Will take the label from the currently selected activity and send it to the server as the activity selection
    @IBAction func confirmActButtonClick(_ sender: UIButton) {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "confirmActivity"
        aPut.agentNum = self.agentNumber!
        
        // if some activity has been selected
        if SideMenu.indexPathForSelectedRow != nil {
            
            // send the label and duration of the currently selected activity
            if menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row] != "other" { // this is necessary to display 'other' rather than 'idle'
                aPut.activityName = menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row]
                confirmedActsList.append(aPut.activityName)
            } else {
                aPut.activityName = "idle"
            }
            
            aPut.activityDuration = "00:"+String(menuDelegate.selectionDuration)
            
            // unselect the selection from the list
            SideMenu.deselectRow(at: SideMenu.indexPathForSelectedRow!, animated:false)
            
            // clear the list until new activity options are provided
            self.menuDelegate.activityOptions = []
            self.menuDelegate.startTime = 0
            ConfirmActButton.isEnabled = false
            self.SideMenu.reloadData()
            
            // send request to server
            self.aClient!.sendStructToServer(aPut)
            
        } else { // if no activity was selected
            
            // create a pop-up informing user no activity was selected
            let alert = UIAlertController(title: "Error", message: "Select an activity before confirming.", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Resume", style: .default, handler: nil))
            self.present(alert, animated: true)
        }
        
    }
    
    @IBAction func AvailTextBox_EditingEnded(_ sender: AvailTextBox) {
        var tempHours : Int?
        var tempMins  : Int?

        sender.validValue = false;
        sender.textColor = UIColor.red
        
        // if empty
        if sender.text == nil {
            sender.validValue = true
            sender.textColor = UIColor.green
            return
        }
        
        // check format
        // first 2 characters should be a number
        tempHours = Int( substr(str: sender.text!, i1: 0, i2: 2) ) // returns nil if not valid int
        if ( tempHours == nil ) {
            return;
        }
        
        // third character should be a colon
        if ( substr(str: sender.text!, i1: 2, i2: 3) != ":" ) {
            return;
        }
        
        // last 2 characters should be a number
        tempMins = Int( substr(str: sender.text!, i1: 3, i2: 5) )
        if ( tempMins == nil) { // returns nil if not valid int
            return;
        }
        
        if (sender.availConstraint == "EST") {
            // if constraint loosened instead of tightened or has illegal value or is larger than LST
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! <  Int(self.origEST!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! > Int(hhmmTOmmmm(hhmm: LSTField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origEST!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "LST") {
            // if constraint loosened instead of tightened or has illegal value or is smaller than EST
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! >  Int(self.origLST!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! < Int(hhmmTOmmmm(hhmm: ESTField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origLST!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "EET") {
            // if constraint loosened instead of tightened or has illegal value or is larger than LET
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! <  Int(self.origEET!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! > Int(hhmmTOmmmm(hhmm: LETField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origEET!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "LET") {
            // if constraint loosened instead of tightened or has illegal value or is smaller than EET
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! >  Int(self.origLET!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! < Int(hhmmTOmmmm(hhmm: EETField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origLET!)! ) {sender.textColor = UIColor.black}
            
        }
        
        // if non of these illegal conditions happen, it is a valid entry
        sender.validValue = true
        if sender.textColor == UIColor.red {
            sender.textColor = UIColor.green
        } // else do not change color
        return;
    }
    
    // called when user finishes editing the duration text view
    func textViewDidEndEditing(_ textView: UITextView) {
        
        validDurationValue = false;
        textView.textColor = UIColor.red
        
        if textView.text! == "" {return}
        
        // if new duration is longer than old (loosened constraint), so here get orig max dur
        var maxOrigDur = 0
        var minOrigDur = 999999999
        let origLines = self.origDur!.components(separatedBy: CharacterSet.newlines)
        for oLine in origLines {
            if oLine != "" {
                var oSplitLine = oLine.components(separatedBy: " - " )
                let tempOmins1 = hhmmTOmmmm(hhmm: oSplitLine[0])
                let tempOmins2 = hhmmTOmmmm(hhmm: oSplitLine[1])
                if Int(tempOmins1)! < minOrigDur { minOrigDur = Int(tempOmins1)! }
                if Int(tempOmins2)! > maxOrigDur { maxOrigDur = Int(tempOmins2)! }
            }
        }
        
        let durLines = textView.text!.components(separatedBy: CharacterSet.newlines)
        for line in durLines {
            
            // if not an empty line, check if valid
            if line != "" {
                var splitLine = line.components(separatedBy: " - " )
                if splitLine.count != 2 {return}
                
                
                // check format
                // first 2 characters of each should be a number
                let tempHours1 = Int( substr(str: splitLine[0], i1: 0, i2: 2) ) // returns nil if not valid int
                let tempHours2 = Int( substr(str: splitLine[1], i1: 0, i2: 2) ) // returns nil if not valid int
                if ( tempHours1 == nil || tempHours2 == nil ) {
                    return;
                }
                
                // third character of each should be a colon
                if ( substr(str: splitLine[0], i1: 2, i2: 3) != ":" || substr(str: splitLine[1], i1: 2, i2: 3) != ":" ) {
                    return;
                }
                
                // last 2 characters or each should be a number
                let tempMins1 = Int( substr(str: splitLine[0], i1: 3, i2: 5) )
                let tempMins2 = Int( substr(str: splitLine[1], i1: 3, i2: 5) )
                if ( tempMins1 == nil || tempMins2 == nil ) { // returns nil if not valid int
                    return;
                }
                
                // if duration range is backwards
                if ( hhmmTOmmmm(hhmm: splitLine[0]) > hhmmTOmmmm(hhmm: splitLine[1]) ) {
                    return;
                }
                
                // if max duration greater than max max orig duration or min less than
                if ( Int(hhmmTOmmmm(hhmm: splitLine[1]))! > maxOrigDur || minOrigDur > Int(hhmmTOmmmm(hhmm: splitLine[0]))! ) {
                    return
                }
                
                // if duration hasnt been changed
                if (self.origDur == textView.text!) {
                    textView.textColor = UIColor.black
                }
                
                validDurationValue = true;
                if textView.textColor == UIColor.red {
                    textView.textColor = UIColor.green
                }
                return;
            }
        }
    }
    
    
    @IBAction func ConfirmModButtonClick(_ sender: UIButton) {
        
        // if no act selected
        if ModActMenu.indexPathForSelectedRow == nil {
            return
        }
        
        // make sure all box values are valid - need this if user goes straight from editing to button
        textViewDidEndEditing(DurationTextBox)
        AvailTextBox_EditingEnded(ESTField)
        AvailTextBox_EditingEnded(LSTField)
        AvailTextBox_EditingEnded(EETField)
        AvailTextBox_EditingEnded(LETField)
        
        // if all user entered values are legal
        if (ESTField.validValue && LSTField.validValue && EETField.validValue && LETField.validValue && validDurationValue!) {
            modifyActivity()
            return;
        } // else show a popup warning
        else {
            let alert = UIAlertController(title: "Error", message: "Some entered modifcation fields are illegal. New values must have hh:mm format and tighten constraints (no loosening).", preferredStyle: .alert)
            
            alert.addAction(UIAlertAction(title: "Okay", style: .default, handler: nil))
            
            self.present(alert, animated: true)
        }
        
        
    }
    
    
    @IBAction func ClickAdvSysClock(_ sender: Any) {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "advSysTime"
        aPut.agentNum = self.agentNumber!
        
        // clear the list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
        self.aClient!.sendStructToServer(aPut)
    }
    
    
    func drawMenuRightLine() {
    
        var lineView : UIView = {
            let view = UIView()
            view.backgroundColor = UIColor.black
            view.translatesAutoresizingMaskIntoConstraints = false
            return view
        }()
        
        // use VFL to define where to draw the line
        self.view.addSubview(lineView)
        self.view.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|-20-[view]|", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))
        self.view.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|-198-[view(1)]", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))
    
    }
    
    // Draw a vertical line on the right side of the activity selection table in the mod activity view
    func drawModActMenuRightLine() {

        let lineView : UIView = {
            let view = UIView()
            view.backgroundColor = UIColor.lightGray
            view.translatesAutoresizingMaskIntoConstraints = false
            return view
        }()

        // use VFL to define where to draw the line
        ModifyActView.addSubview(lineView)
        let bottOfLine = String(Double(ModifyActView.frame.size.height - 100))
        ModifyActView.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|-100-[view]-100-|", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))
        ModifyActView.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|-230-[view(1)]", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))

    }
    
    
    // this function is called to request a gantt image from the server that includes the tentative selection of the user.
    // should automatically be called after the user clicks an activity in hte side menu
    func requestTentActGantt() {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "tentativeActivity"
        aPut.agentNum = agentNumber!
        
        // if no activity has been selected or if something besides an activity is selected, do nothing
        if (SideMenu.indexPathForSelectedRow == nil || SideMenu.indexPathForSelectedRow!.section != 0) {
            return
        } else {
            
            // send the label and duration of the currently selected activity
            if menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row] != "other" { // this is necessary to display 'other' rather than 'idle'
                aPut.activityName = menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row]
            } else {
                aPut.activityName = "idle"
            }
            
            aPut.activityDuration = "00:"+String(menuDelegate.selectionDuration)
            
            // send request to server
            self.aClient!.sendStructToServer(aPut)
        }
    }
    
    // this function is called to request the details of an activity from the server.
    // should automatically be called after the user clicks an activity in the mod act side menu
    // No internal checks - AKA assumes this is called only after a legal selection
    func requestActDetails() {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "requestActDetails"
        aPut.agentNum = agentNumber!
        
        if ModActMenu.indexPathForSelectedRow == nil {
            aPut.activityName = ""
        } else {
            aPut.activityName = modActMenuDelegate.allModifiableActivities![ ModActMenu.indexPathForSelectedRow!.row ]
        }
        
        // send request to server
        self.aClient!.sendStructToServer(aPut)
    }
    
    func modifyActivity() {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "modifyActivity"
        aPut.agentNum = agentNumber!
        aPut.activityName = modActMenuDelegate.allModifiableActivities![ ModActMenu.indexPathForSelectedRow!.row ]
        
        var newActDetails = activityDefinition()
        
        newActDetails.actName = modActMenuDelegate.allModifiableActivities![ ModActMenu.indexPathForSelectedRow!.row ]
        let durLines = DurationTextBox.text!.components(separatedBy: CharacterSet.newlines)
        for line in durLines {
            
            // if not an empty line, append its durations to the newActDetails
            if line != "" {
                var splitLine = line.components(separatedBy: " - " )
                // trim off white space and append to min/max duration lists
                newActDetails.minDurs?.append( splitLine[0].trimmingCharacters(in: .whitespaces) )
                newActDetails.maxDurs?.append( splitLine[1].trimmingCharacters(in: .whitespaces) )
            }
        }
        newActDetails.EST = ESTField.text! // format:  hh:mm
        newActDetails.LST = LSTField.text! // format:  hh:mm
        newActDetails.EET = EETField.text! // format:  hh:mm
        newActDetails.LET = LETField.text! // format:  hh:mm
        
        aPut.actDetails = newActDetails
        
        
        // send request to server
        self.aClient!.sendStructToServer(aPut)
        
        // clear the list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
//        self.SideMenu.deselectRow(at: SideMenu.indexPathForSelectedRow!, animated: false)
        self.ModifyActView.isHidden = true
        self.ModActMenu.deselectRow(at: self.ModActMenu.indexPathForSelectedRow!, animated: false)
    }
    
    
    
    // return a STRING of the string from i1..<i2 of str
    func substr(str: String, i1: Int, i2: Int) -> String {
        if str.count < i2 {return ""}
        
        let idx1 = str.index(str.startIndex, offsetBy: i1)
        let idx2 = str.index(str.startIndex, offsetBy: i2)
        return String(str[idx1..<idx2])
    }
    
    // convert time format of mmmm to hh:mm
    func mmmmTOhhmm(mmmm: String) -> String {
        if mmmm == "" {return ""}
        var hh = String( Int(Float(mmmm)!) / 60 )
        var mm = String( Int(Float(mmmm)!) % 60 )
        if Int(hh)! < 10 {
            hh = "0" + hh
        }
        if Int(mm)! < 10 {
            mm = "0" + mm
        }
        return hh + ":" + mm
    }
    
    // convert time format of hh:mm to mmmm
    func hhmmTOmmmm(hhmm: String) -> String {
        if hhmm == "" {return ""}
        let hh = Int( substr(str: hhmm, i1: 0, i2: 2) )!
        let mm = Int( substr(str: hhmm, i1: 3, i2: 5) )!
        var mmmm = String( hh * 60 + mm )
        if Int(mmmm)! < 10 {mmmm = "000" + mmmm}
        else if Int(mmmm)! < 100 {mmmm = "00" + mmmm}
        else if Int(mmmm)! < 1000 {mmmm = "0" + mmmm}
        
        return String( mmmm )
    }
    
}


