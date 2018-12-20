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
class MainViewController: UIViewController, UITextFieldDelegate, UITextViewDelegate {
    
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
//    @IBOutlet weak var GanttImageDisplay: UIImageView!
    
    @IBOutlet weak var advSysClockButton: UIButton!
    
    // Modify Activty View that appears when user selects sidemenu 'modify activity'
    var modActMenuDelegate = ModActTableController(style: .grouped)
    @IBOutlet weak var ModActMenu: UITableView!
    @IBOutlet weak var ModifyActView: ActivityEditView!
    @IBOutlet weak var ModAct_ESTField: AvailTextBox!
    @IBOutlet weak var ModAct_LSTField: AvailTextBox!
    @IBOutlet weak var ModAct_EETField: AvailTextBox!
    @IBOutlet weak var ModAct_LETField: AvailTextBox!
    @IBOutlet weak var ModAct_DurationTextBox: UITextView!
    @IBOutlet weak var ModAct_ConfirmButton: UIButton!
    
    
    // Add Activity View that appears when user selects sidemenu 'add activity'
    @IBOutlet weak var AddActView: ActivityEditView!
    @IBOutlet weak var AddAct_ActNameField: AvailTextBox!
    @IBOutlet weak var AddAct_ESTField: AvailTextBox!
    @IBOutlet weak var AddAct_LSTField: AvailTextBox!
    @IBOutlet weak var AddAct_EETField: AvailTextBox!
    @IBOutlet weak var AddAct_LETField: AvailTextBox!
    @IBOutlet weak var AddAct_MinDurField: AvailTextBox!
    @IBOutlet weak var AddAct_MaxDurField: AvailTextBox!
    @IBOutlet weak var AddAct_ConfirmButton: UIButton!
    
    // Delete activity table that appears when user selects "remove activity"
    var delActMenuDelegate = DelActTableController(style: .grouped)
    @IBOutlet weak var DelActMenu: UITableView!
    @IBOutlet weak var DeleteActButton: UIButton!
    var delRightBorderLine: UIView!
    
    // clickable image to warn user why their system has locked them from actions
    @IBOutlet weak var WarningImage: UIImageView!
    
    
    
    var origEST : String?
    var origLST : String?
    var origEET : String?
    var origLET : String?
    var origDur : String?
    var validDurationValue : Bool?
    var lastConfirmedBars : [GanttChartView.BarEntry]?
    
    let stronglyRestrictColor: UIColor = UIColor.init(red: 255.0/255.0, green: 204.0/255.0, blue: 204.0/255.0, alpha: 1.0)
    let weaklyRestrictedColor: UIColor = UIColor.init(red: 255.0/255.0, green: 247.0/255.0, blue: 204.0/255.0, alpha: 1.0)
    
    
    // This is the outlet for custom xCode generated gantt chart
    // It is not complete and more of a generic horizontal chart at this point
    @IBOutlet weak var GanttChart: GanttChartView!
    
    
    // Called whenever this view (main screen) opens to the user (including app startup)
    override func viewDidLoad() {
        super.viewDidLoad()
//        NotificationCenter.default.addObserver(self, selector: #selector(ViewControl ler.keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
//        NotificationCenter.default.addObserver(self, selector: #selector(ViewController.keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        
        
        // set up warning exclamation point image
        // This image will appear when there is a system notification of status for the user
        // The user can click the image to see the notification of status
        WarningImage.isHidden = true
        let warningTap = UITapGestureRecognizer(target: self, action: Selector("WarningImgTapDetected"))
        WarningImage.isUserInteractionEnabled = true
        WarningImage.addGestureRecognizer(warningTap)
        
        // Set the sideMenu tableView as a child view and Set the ModActMenu delegate and dataSource
//        addChildViewController(modActMenuDelegate)
        //        view.addSubview(modActMenuDelegate.view)
//        modActMenuDelegate.didMove(toParentViewController: self)
        ModActMenu.delegate = modActMenuDelegate
        ModActMenu.dataSource = modActMenuDelegate
        
        DelActMenu.delegate = delActMenuDelegate
        DelActMenu.dataSource = delActMenuDelegate
        DelActMenu.isHidden = true
        DeleteActButton.isHidden = true
        delRightBorderLine = drawDelActMenuRightLine()
        delRightBorderLine.isHidden = true
        
        ModifyActView.isHidden = true
        AddActView.isHidden = true
        
        // Set the sideMenu tableView as a child view and Set the sideMenu delegate and dataSource to MenuController (sublass of TableViewController)
        addChildViewController(menuDelegate)
//        view.addSubview(menuDelegate.view)
        menuDelegate.didMove(toParentViewController: self)
        SideMenu.delegate = menuDelegate
        SideMenu.dataSource = menuDelegate
        
        
        // testing to see how addTarget works and then see its feasibility for handeling actions inside of modifyAct view
//        menuDelegate.picker.addTarget(self, action: #selector(testTarg(sender:)), for: .valueChanged)
        
        ModAct_ESTField.availConstraint = "EST";
        ModAct_LSTField.availConstraint = "LST";
        ModAct_EETField.availConstraint = "EET";
        ModAct_LETField.availConstraint = "LET";
        
        self.ModAct_DurationTextBox.delegate = self
        
        drawMenuRightLine()
        drawModActMenuRightLine()
        ModifyActView.addBackgroundShape()
        AddActView.addBackgroundShape()

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
    
        // make sure the advSystemTimeButton is always above the scale
        
        
        advSysClockButton.frame = CGRect(x: SideMenu.frame.width + 120.0, y: self.view.frame.height - GanttChart.scaleSpace - advSysClockButton.frame.height - 20.0, width: 200, height: 72)
        
        // initialize client
        self.aClient = client(serverIP!, agentNumber!)
        
        
        //        let boxWidth: CGFloat = 200.0
        //        let boxHeight: CGFloat = 125.0
        //        let boxX: CGFloat = view.frame.width - boxWidth - 50.0
        //        let boxY: CGFloat = 50
        
        let impactHeight: CGFloat = 70.0
        let impactWidth: CGFloat =  140.0
        let impactY: CGFloat =  50
        
//        drawImpactBox(title: "Personal", weakCount: 0, StrongCount: 0, boxX: view.frame.width - impactWidth - 30.0,   boxY: impactY, boxWidth: impactWidth, boxHeight: impactHeight)
//        drawImpactBox(title: "Others",   weakCount: 0, StrongCount: 0, boxX: view.frame.width - impactWidth*2 - 50.0, boxY: impactY, boxWidth: impactWidth, boxHeight: impactHeight)
        
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
                if self.menuDelegate.otherSelectionChanged == true {
                    
                    
                    DispatchQueue.main.async() {
                        
                        // if 'add activity' selected, display modAct screen
                        if (self.SideMenu.indexPathForSelectedRow?.section == 1 && self.SideMenu.indexPathForSelectedRow?.row == 0) {
                            
                            // NEW SYSTEM: segue to full new add act view
                            self.performSegue(withIdentifier: "AddActSegue", sender: self)
                            
//                            self.AddActView.isHidden = false
                        }
                        
//                        // if 'add activity' screen is showing but not selected on side menu, hide it
//                        if ( self.AddActView.isHidden == false && ( self.SideMenu.indexPathForSelectedRow?.section != 1 || self.SideMenu.indexPathForSelectedRow?.row != 0 ) ) {
//                            self.AddActView.isHidden = true
//                            if self.ModActMenu.indexPathForSelectedRow != nil {
//                                self.ModActMenu.deselectRow(at: self.ModActMenu.indexPathForSelectedRow!, animated: false)
//                            }
//                        }
                        
                        
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
                        
                        // if 'delete activity' selected, .....
                        if (self.SideMenu.indexPathForSelectedRow?.section == 1 && self.SideMenu.indexPathForSelectedRow?.row == 2) {
                            self.delActMenuDelegate.allDeletableActivities.removeAll()
                            for a in self.viewInInfo.actNames! {
                                if !(self.confirmedActsList.contains(a)) {
                                    self.delActMenuDelegate.allDeletableActivities.append(a)
                                }
                            }
                            // reset selection
                            self.DelActMenu.deselectRow(at: IndexPath(row: self.delActMenuDelegate.currentCellRow, section: 0), animated: true)
                            self.delActMenuDelegate.currentCellRow = -1
                            
                            self.DelActMenu.reloadData()
                            
                            self.DelActMenu.isHidden = false
                            self.DeleteActButton.isHidden = false
                            self.delRightBorderLine.isHidden = false
                            
//                            self.deleteActivity()
                        }
                        
                        // if 'delete activity' menu is showing but not selected on side menu, hide it
                        if ( self.DelActMenu.isHidden == false && ( self.SideMenu.indexPathForSelectedRow?.section != 1 || self.SideMenu.indexPathForSelectedRow?.row != 2 ) ) {
                            self.DelActMenu.isHidden = true
                            self.DeleteActButton.isHidden = true
                            self.delRightBorderLine.isHidden = true
                        }
                        
                    }
                    
                    // reset sectChanged flag
                    self.menuDelegate.otherSelectionChanged = false
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
                            /*if self.viewInInfo.nextActivities!.count > 0 && self.viewInInfo.clearToConfirm == "true" {
                                self.ConfirmActButton.isEnabled = true
                                //self.ConfirmActButton.backgroundColor = UIColor(red: 76/255, green: 217/255, blue: 100/255, alpha: 1.0)
                            } else {
                                self.ConfirmActButton.isEnabled = false
                                //self.ConfirmActButton.backgroundColor = UIColor.white
                            }*/
                            self.lastConfirmedBars = self.GanttChart.dataEntries
                            
                            // if the user needs to wait for a higher priority user to make a decision first, display the
                            //  notification warning clickable image to explain the situation
                            //  only trigger 'confirm block' response when the user would typically be able to make a decision
                            if self.viewInInfo.clearToConfirm == "false" && self.menuDelegate.activityOptions.count > 0 {
                                self.menuDelegate.clearToConfirm = false
                                self.WarningImage.isHidden = false
                                self.WarningImage.alpha = 1.0
                            } else {
                                self.menuDelegate.clearToConfirm = true
                                self.WarningImage.isHidden = true
                            }
                        }
                    
                        // save the bars of the confirmed acts so that ratios can be compared when evaluating tentative choices
                        if (self.viewInInfo.infoType == "ganttImage") {
                            
                            
                            // temp info to test gantt with
                            // these traits will need to be derived from list of timepoits in server, not necesarily what is hardcoded into dtp definition file
                            var actNames = self.viewInInfo.actNames!
                            var actIDs = self.viewInInfo.actIDs!
                            var actESTs = self.viewInInfo.actESTs!
                            var actLETs = self.viewInInfo.actLETs!
                            var actMinDurs  = self.viewInInfo.actMinDurs! // use only the smallest min and the greatest max
                            var actMaxDurs = self.viewInInfo.actMaxDurs! // use only the smallest min and the greatest max
                            var actRestricts = self.viewInInfo.actRestricts!
                            var currentTime = self.viewInInfo.currentTime!
                            
                            var barColor : UIColor
                            
                            var gotBars : [GanttChartView.BarEntry] = []
                            
                            for actNum in 0..<actNames.count {
                                
                                barColor = UIColor.init(red: 204.0/255.0, green: 255.0/255.0, blue: 204.0/255.0, alpha: 1.0)
                                
                                // set the bar color (for tentative selections) based on how much the availability has
                                //  been restricted relative to the last confirmed act
                                // lower restrict values => more restricted
                                // if the side menu has something selected in section 0, this is a tent selection
//                                NOTE: THIS BAR COLOR SELECTION SYSTEM WAS REPLACED WITH A SIMPLER SYSTEM INSIDE OF GanttChartView
                                if (self.SideMenu.indexPathForSelectedRow?.section == 0) {
//                                    // if it has been restricted twice as much as previous:
//                                    // 50% threshold
//                                    if (self.lastConfirmedBars![actNum].restrict > 1.5 * Double(actRestricts[actNum])!) {
//                                        barColor = self.stronglyRestrictColor
//                                    } // if it has been restricted any more than previous but less than double:
//                                    else if (self.lastConfirmedBars![actNum].restrict > Double(actRestricts[actNum])!) {
//                                        barColor = self.weaklyRestrictedColor
//                                    }
                                    self.GanttChart.tentGantt = true
                                    self.ConfirmActButton.isEnabled = true
                                } else {
                                    self.GanttChart.tentGantt = false
                                    self.ConfirmActButton.isEnabled = false
                                }
                                
                                
                                // if this activity has already been completed
                                if (currentTime == "") {currentTime = "0"}
                                if (actLETs.count > 0 && Int(actLETs[actNum])! <= Int(currentTime)!) {
                                    gotBars.append( GanttChartView.BarEntry(
                                        color: barColor,
                                        ID:  Int(actIDs[actNum])!,
                                        isTentAct: false,
                                        EST: Int(actESTs[actNum])!,
                                        LET: Int(actLETs[actNum])!,
                                        minDuration: Int( actLETs[actNum] )! - Int( actESTs[actNum] )!,
                                        maxDuration: Int( actLETs[actNum] )! - Int( actESTs[actNum] )!,
                                        restrict: 0.0,
                                        activityName: actNames[actNum],
                                        title: "test" ) )
                                } else {
                                    var thisActTent = false
                                    if self.GanttChart.tentGantt {
                                        thisActTent = actNames[actNum] == self.SideMenu.cellForRow(at: self.SideMenu.indexPathForSelectedRow!)?.textLabel?.text!
                                    }
                                    gotBars.append( GanttChartView.BarEntry(
                                        color: barColor,
                                        ID:  Int(actIDs[actNum])!,
                                        isTentAct: thisActTent,
                                        EST: Int(actESTs[actNum])!,
                                        LET: Int(actLETs[actNum])!,
                                        minDuration: Int(actMinDurs[actNum])!,
                                        maxDuration: Int(actMaxDurs[actNum])!,
                                        restrict: 0.0,
                                        activityName: actNames[actNum],
                                        title: "test" ) )
                                }
                            }
                            
                            self.GanttChart.currTime = Int(currentTime)!
                            self.GanttChart.lastConfirmedBars = self.lastConfirmedBars
                            self.GanttChart.dataEntries = gotBars
                            
                            // if it has not yet been set, then this is the first gantt displayed after startup
                            // (AKA a gantt of confirmed actions)
                            if (self.lastConfirmedBars == nil) {
                                self.lastConfirmedBars = self.GanttChart.dataEntries
                            }
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
                                self.ModAct_ESTField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.EST! )
                                self.ModAct_LSTField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.LST! )
                                self.ModAct_EETField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.EET! )
                                self.ModAct_LETField.text = self.mmmmTOhhmm(mmmm: self.viewInInfo.actDetails!.LET! )
                                
                                self.ModAct_ESTField.validValue = true
                                self.ModAct_LSTField.validValue = true
                                self.ModAct_EETField.validValue = true
                                self.ModAct_LETField.validValue = true
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
                                self.ModAct_DurationTextBox.text = durStr
                                self.origDur = durStr
                            }
                        }

                        // Legacy code for old gantt image display system (generated on Java server)
//                        if (self.viewInInfo.strImg != "") {
//                            if let decodedData = Data(base64Encoded: self.viewInInfo.strImg!, options: .ignoreUnknownCharacters) {
//                                let image = UIImage(data: decodedData)
//                                self.GanttImageDisplay.image = image
//                            }
//                        }
                    
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
    
    @objc func WarningImgTapDetected() {
        
        self.WarningImage.alpha = 0.6
        
        let alert = UIAlertController(title: "User Status", message: "Higher priority users must confirm an activity before you can proceed.", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Cancel", style: .default, handler: nil))
        
        self.present(alert, animated: true)
        
    }

    override func viewDidAppear(_ animated: Bool) {
        
        
        
        
    }

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
            menuDelegate.currentCellSect = -1
            menuDelegate.currentCellRow  = -1
            
            
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
    
    // Clicking this button sends a delete act request to the server
    @IBAction func DeleteActButtonClick(_ sender: Any) {
        
        if DelActMenu.indexPathForSelectedRow == nil {
            return
        }
        let actNameToDel = delActMenuDelegate.allDeletableActivities[(DelActMenu.indexPathForSelectedRow!.row)]
        deleteActivity(actNameToDel)
        
        // hide remove activity menu and reset side menu
        DelActMenu.isHidden = true
        delRightBorderLine.isHidden = true
        DeleteActButton.isHidden = true
        menuDelegate.selectionSectNum = -1
        menuDelegate.selectionActNum = -1
    }
    
    
    
    @IBAction func AvailTextBox_EditingEnded(_ sender: AvailTextBox) {
        var tempHours : Int?
        var tempMins  : Int?

        sender.validValue = false;
        sender.textColor = UIColor.red
        
        // if empty
        if sender.text == nil || sender.text == "" {
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
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! > Int(hhmmTOmmmm(hhmm: ModAct_LSTField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origEST!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "LST") {
            // if constraint loosened instead of tightened or has illegal value or is smaller than EST
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! >  Int(self.origLST!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! < Int(hhmmTOmmmm(hhmm: ModAct_ESTField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origLST!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "EET") {
            // if constraint loosened instead of tightened or has illegal value or is larger than LET
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! <  Int(self.origEET!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! > Int(hhmmTOmmmm(hhmm: ModAct_LETField.text!))! ) {return}
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! == Int(self.origEET!)! ) {sender.textColor = UIColor.black}
            
        } else if (sender.availConstraint == "LET") {
            // if constraint loosened instead of tightened or has illegal value or is smaller than EET
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! >  Int(self.origLET!)! ) { return }
            if (tempHours! < 0 || tempHours! > 24 || tempMins! < 0 || tempMins! > 59) { return }
            if ( Int(hhmmTOmmmm(hhmm: sender.text!))! < Int(hhmmTOmmmm(hhmm: ModAct_EETField.text!))! ) {return}
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
        // only check this for mod activity
        var maxOrigDur = 0
        var minOrigDur = 999999999
        if textView == ModAct_DurationTextBox {
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
                // only check this for Mod Act
                if textView == ModAct_DurationTextBox {
                    if ( Int(hhmmTOmmmm(hhmm: splitLine[1]))! > maxOrigDur || minOrigDur > Int(hhmmTOmmmm(hhmm: splitLine[0]))! ) {
                        return
                    }
                
                    // if duration hasnt been changed
                    if (self.origDur == textView.text!) {
                        textView.textColor = UIColor.black
                    }
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
        textViewDidEndEditing(ModAct_DurationTextBox)
        AvailTextBox_EditingEnded(ModAct_ESTField)
        AvailTextBox_EditingEnded(ModAct_LSTField)
        AvailTextBox_EditingEnded(ModAct_EETField)
        AvailTextBox_EditingEnded(ModAct_LETField)
        
        // if all user entered values are legal
        if (ModAct_ESTField.validValue && ModAct_LSTField.validValue && ModAct_EETField.validValue && ModAct_LETField.validValue && validDurationValue!) {
            modifyActivity()
            return;
        } // else show a popup warning
        else {
            let alert = UIAlertController(title: "Error", message: "Some entered modifcation fields are illegal. New values must have hh:mm format and tighten constraints (no loosening).", preferredStyle: .alert)
            
            alert.addAction(UIAlertAction(title: "Okay", style: .default, handler: nil))
            
            self.present(alert, animated: true)
        }
        
        
    }
    
    @IBAction func ConfirmAddButtonClick(_ sender: UIButton) {
        
        
        // make sure all box values are valid - need this if user goes straight from editing to button
//        textViewDidEndEditing(AddAct_DurationTextBox)
        AvailTextBox_EditingEnded(AddAct_MinDurField)
        AvailTextBox_EditingEnded(AddAct_MaxDurField)
        AvailTextBox_EditingEnded(AddAct_ESTField)
        AvailTextBox_EditingEnded(AddAct_LSTField)
        AvailTextBox_EditingEnded(AddAct_EETField)
        AvailTextBox_EditingEnded(AddAct_LETField)

        // if all user entered values are legal
        if (AddAct_ESTField.validValue && AddAct_LSTField.validValue && AddAct_EETField.validValue && AddAct_LETField.validValue && AddAct_MinDurField.validValue && AddAct_MaxDurField.validValue && AddAct_ActNameField.text! != "") {
            addActivity()
            return;
        } // else show a popup warning
        else {
            let alert = UIAlertController(title: "Error", message: "Some entered activity addition fields are illegal. New values must have hh:mm format.", preferredStyle: .alert)

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
    
        let lineView : UIView = {
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
    
    // Draw a vertical line (and return it) on the right side of the del menu
    func drawDelActMenuRightLine() -> UIView {
        
        let lineView : UIView = {
            let view = UIView()
            view.backgroundColor = UIColor.black
            view.translatesAutoresizingMaskIntoConstraints = false
            return view
        }()
        
        // use VFL to define where to draw the line
        self.view.addSubview(lineView)
        self.view.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|-20-[view]|", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))
        self.view.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|-398-[view(1)]", options: NSLayoutFormatOptions(), metrics: nil, views: ["view": lineView]))
        
        return lineView
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
    
    
    // This will draw 3 different rectangles and the text to tell the user the impact of their tentative choice
    func drawImpactBox(title: String, weakCount: Int, StrongCount: Int, boxX: CGFloat, boxY: CGFloat, boxWidth: CGFloat, boxHeight: CGFloat) {
//        let boxWidth: CGFloat = 200.0
//        let boxHeight: CGFloat = 125.0
//        let boxX: CGFloat = view.frame.width - boxWidth - 50.0
//        let boxY: CGFloat = 50
        
        drawBox(leftX: boxX, topY: boxY, height: boxHeight, width: boxWidth, type: "interior")
        
        drawSubBox(leftX: boxX,                topY: boxY + boxHeight*0.4, height: boxHeight*0.6, width: boxWidth/2.0, pos: "l")
        drawSubBox(leftX: boxX + boxWidth/2.0, topY: boxY + boxHeight*0.4, height: boxHeight*0.6, width: boxWidth/2.0, pos: "r")
        
        drawBox(leftX: boxX, topY: boxY, height: boxHeight, width: boxWidth, type: "border")
        
        
        
    }
    
    // Draw a rectangle box with rounded corners
    func drawBox(leftX: CGFloat, topY: CGFloat, height: CGFloat, width: CGFloat, type: String) {
        let cornerRadius: CGFloat = 15.0
        
        let path: UIBezierPath = UIBezierPath()
        
        path.move(to: CGPoint(x: leftX + cornerRadius, y: topY))
        
        path.addLine(to: CGPoint(x: leftX + width - cornerRadius,  y: topY))
        
        path.addArc(withCenter: CGPoint(x: leftX + width - cornerRadius, y: topY + cornerRadius), radius: cornerRadius, startAngle: 3.0*CGFloat.pi/2.0, endAngle: 0.0, clockwise: true)
        
        path.addLine(to: CGPoint(x: leftX + width,  y: topY + height - cornerRadius))
        
        path.addArc(withCenter: CGPoint(x: leftX + width - cornerRadius, y: topY + height - cornerRadius), radius: cornerRadius, startAngle: 0.0, endAngle: CGFloat.pi/2.0, clockwise: true)
        
        path.addLine(to: CGPoint(x: leftX + cornerRadius,  y: topY + height))
        
        path.addArc(withCenter: CGPoint(x: leftX + cornerRadius, y: topY + height - cornerRadius), radius: cornerRadius, startAngle: CGFloat.pi/2.0, endAngle: CGFloat.pi, clockwise: true)
        
        path.addLine(to: CGPoint(x: leftX,  y: topY + cornerRadius))
        
        path.addArc(withCenter: CGPoint(x: leftX + cornerRadius, y: topY + cornerRadius), radius: cornerRadius, startAngle: CGFloat.pi, endAngle: 3.0*CGFloat.pi/2.0, clockwise: true)
        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 1
        if type == "border" {
            pathLayer.fillColor = UIColor.clear.cgColor
            pathLayer.strokeColor = UIColor.darkGray.cgColor
        } else {
            pathLayer.fillColor = UIColor.white.cgColor
            pathLayer.strokeColor = UIColor.clear.cgColor
        }
        
        view.layer.addSublayer(pathLayer)
    }
    
    // Draw a rectangle box with ONLY 1 rounded corner
    // This will be used for filling an inside corner of a normal rectangular box with 4 rounded corners
    func drawSubBox(leftX: CGFloat, topY: CGFloat, height: CGFloat, width: CGFloat, pos: String) {
        let cornerRadius: CGFloat = 15.0
        
        let path: UIBezierPath = UIBezierPath()
        
        // top left
        path.move(to: CGPoint(x: leftX, y: topY))
        
        // top right
        path.addLine(to: CGPoint(x: leftX + width,  y: topY))
        
        // bottom right
        if pos == "r" {
            path.addLine(to: CGPoint(x: leftX + width,  y: topY + height - cornerRadius))
            path.addArc(withCenter: CGPoint(x: leftX + width - cornerRadius, y: topY + height - cornerRadius), radius: cornerRadius, startAngle: 0.0, endAngle: CGFloat.pi/2.0, clockwise: true)
        } else {
            path.addLine(to: CGPoint(x: leftX + width,  y: topY + height))
        }
        
        // bottom left
        if pos == "l" {
            path.addLine(to: CGPoint(x: leftX + cornerRadius,  y: topY + height))
            path.addArc(withCenter: CGPoint(x: leftX + cornerRadius, y: topY + height - cornerRadius), radius: cornerRadius, startAngle: CGFloat.pi/2.0, endAngle: CGFloat.pi, clockwise: true)
        } else {
            path.addLine(to: CGPoint(x: leftX,  y: topY + height))
        }
        
        // top left
        path.addLine(to: CGPoint(x: leftX,  y: topY))
        
        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 4.1
        if (pos == "l") {
            pathLayer.fillColor = weaklyRestrictedColor.cgColor
            pathLayer.strokeColor = UIColor.clear.cgColor
        } else if (pos == "r") {
            pathLayer.fillColor = stronglyRestrictColor.cgColor
            pathLayer.strokeColor = UIColor.clear.cgColor
        } else {
            pathLayer.fillColor = UIColor.magenta.cgColor
            pathLayer.strokeColor = UIColor.magenta.cgColor
        }
        
        
        view.layer.addSublayer(pathLayer)
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
    
    // This is legacy addActivity(), which works with single screen activity description setup
    // Is now replaced with add activity for slide system that takes in parameters
    func addActivity() {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "addActivity"
        aPut.agentNum = agentNumber!
        aPut.activityName = AddAct_ActNameField.text!
        
        var newActDetails = activityDefinition()
        
        newActDetails.actName = AddAct_ActNameField.text!
        
        // clear the gantt chart until a new one is ready
        GanttChart.dataEntries = nil
        
        // clear the act list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
//        let durLines = AddAct_DurationTextBox.text!.components(separatedBy: CharacterSet.newlines)
//        for line in durLines {
//
//            // if not an empty line, append its durations to the newActDetails
//            if line != "" {
//                var splitLine = line.components(separatedBy: " - " )
//                // trim off white space and append to min/max duration lists
//                newActDetails.minDurs?.append( hhmmTOmmmm(hhmm: splitLine[0].trimmingCharacters(in: .whitespaces)) ) // format:  mmmm
//                newActDetails.maxDurs?.append( hhmmTOmmmm(hhmm: splitLine[1].trimmingCharacters(in: .whitespaces)) ) // format:  mmmm
//            }
//        }
        
        // minDurs and maxDurs are lists due to old implementation details
        // see communication protocol outline for details
        newActDetails.minDurs?.append(hhmmTOmmmm(hhmm: AddAct_MinDurField.text!))
        newActDetails.maxDurs?.append(hhmmTOmmmm(hhmm: AddAct_MaxDurField.text!))
        
        newActDetails.EST = hhmmTOmmmm(hhmm: AddAct_ESTField.text!) // format:  mmmm
        newActDetails.LST = hhmmTOmmmm(hhmm: AddAct_LSTField.text!) // format:  mmmm
        newActDetails.EET = hhmmTOmmmm(hhmm: AddAct_EETField.text!) // format:  mmmm
        newActDetails.LET = hhmmTOmmmm(hhmm: AddAct_LETField.text!) // format:  mmmm
        
        aPut.actDetails = newActDetails
        
        // send request to server
        self.aClient!.sendStructToServer(aPut)
        
        // clear the list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
        // deselect 'Add Activity' option in left menu
        self.AddActView.isHidden = true
    }
    
    
    func addActivity(actName: String, minDur: String, maxDur: String, EST: String, LET: String, precConstraints: [String], succConstraints: [String] ) {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "addActivity"
        aPut.agentNum = agentNumber!
        aPut.activityName = actName
        
        var newActDetails = activityDefinition()
        
        newActDetails.actName = actName
        
        // clear the gantt chart until a new one is ready
        GanttChart.dataEntries = nil
        
        // clear the act list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
        
        // minDurs and maxDurs are lists due to old implementation details
        // see communication protocol outline for details
        newActDetails.minDurs?.append(minDur)
        newActDetails.maxDurs?.append(maxDur)
        
        newActDetails.EST = EST // format:  mmmm
        newActDetails.LST = ""
        newActDetails.EET = ""
        newActDetails.LET = LET // format:  mmmm
        
        // add activity ordering constraints
        newActDetails.constraintSource = precConstraints  // preceeding constraints
        newActDetails.constraintDest = succConstraints    // succeeding constraints
        
        
        aPut.actDetails = newActDetails
        
        // send request to server
        self.aClient!.sendStructToServer(aPut)
        
        // deselect 'Add Activity' option in left menu
        // TODO
    }
    
    
    
    func modifyActivity() {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "modifyActivity"
        aPut.agentNum = agentNumber!
        aPut.activityName = modActMenuDelegate.allModifiableActivities![ ModActMenu.indexPathForSelectedRow!.row ]
        
        var newActDetails = activityDefinition()
        
        newActDetails.actName = modActMenuDelegate.allModifiableActivities![ ModActMenu.indexPathForSelectedRow!.row ]
        let durLines = ModAct_DurationTextBox.text!.components(separatedBy: CharacterSet.newlines)
        for line in durLines {
            
            // if not an empty line, append its durations to the newActDetails
            if line != "" {
                var splitLine = line.components(separatedBy: " - " )
                // trim off white space and append to min/max duration lists
                newActDetails.minDurs?.append( splitLine[0].trimmingCharacters(in: .whitespaces) )
                newActDetails.maxDurs?.append( splitLine[1].trimmingCharacters(in: .whitespaces) )
            }
        }
        newActDetails.EST = ModAct_ESTField.text! // format:  hh:mm
        newActDetails.LST = ModAct_LSTField.text! // format:  hh:mm
        newActDetails.EET = ModAct_EETField.text! // format:  hh:mm
        newActDetails.LET = ModAct_LETField.text! // format:  hh:mm
        
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
        
        ModAct_ESTField.text = ""
        ModAct_LSTField.text = ""
        ModAct_EETField.text = ""
        ModAct_LETField.text = ""
        ModAct_DurationTextBox.text = ""
    }
    
    
    // Use this function to delete an activity on the server that the client has not yet performed. For now it will only delete breakfast
    func deleteActivity(_ actNameToDel: String) {
        var aPut = putCMD()
        aPut.clientID = self.aClient!.ID
        aPut.infoType = "deleteActivity"
        aPut.agentNum = agentNumber!
        aPut.activityName = actNameToDel
        
        
        // send request to server
        self.aClient!.sendStructToServer(aPut)
        
        // clear the list until new activity options are provided
        self.menuDelegate.activityOptions = []
        self.menuDelegate.startTime = 0
        ConfirmActButton.isEnabled = false
        self.SideMenu.reloadData()
        
        // clear the gantt chart until a new one is ready
        GanttChart.dataEntries = nil
        
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
    
    
    
    
    
    
    // segue code for transitioning view controller
    
//    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // if segue to add activity screen, no info that needs to be passed in
//        if let destinationViewController = segue.destination as? ViewController {
//            destinationViewController.serverIP = IPaddress
//        }
//    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
        if segue.identifier == "AddActSegue" {
            let popoverViewController = segue.destination as! AddActivityViewController
            popoverViewController.didMove(toParentViewController: self)
            
            // set up popover
            popoverViewController.modalPresentationStyle = UIModalPresentationStyle.popover
            popoverViewController.popoverPresentationController!.delegate = self as? UIPopoverPresentationControllerDelegate
            
            // set the position of the popover
            popoverViewController.popoverPresentationController!.sourceView = self.view
            popoverViewController.popoverPresentationController!.sourceRect = CGRect(x: 500,y: self.view.bounds.midY-200,width:0,height:0)
            
            // do not use any popover arrows with this popover
            popoverViewController.popoverPresentationController!.permittedArrowDirections = []
            
            // set the size of the popover
            popoverViewController.preferredContentSize = CGSize(width: 704, height: 680)
            
            popoverViewController.popoverPresentationController!.backgroundColor = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 0.8)
        }
        
    }
    
//    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
//        return UIModalPresentationStyle.none
//    }

    
}


