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
class ViewController: UIViewController, UITextFieldDelegate {
    
    var serverIP : String?
    
    // client class handles all client-server interaction
    var aClient: client?
    
    // holds the info recieved from client from server
    var viewInInfo: fromServer = fromServer()
    
    // menuDelegate is tableViewController to control left side menu of main screen
    var menuDelegate = MenuController(style: .grouped)
    
    // this is the agent number of the client. Set at startup and never mutated
    var agentNumber : String?
    
    
    //MARK: Properties
    @IBOutlet var SideMenu: UITableView!
    @IBOutlet weak var ConfirmActButton: UIButton!
    @IBOutlet weak var GanttImageDisplay: UIImageView!
    @IBOutlet weak var GanttChart: GanttChartView!
    
    
    // Called whenever this view (main screen) opens to the user (including app startup)
    override func viewDidLoad() {
        super.viewDidLoad()
        
        
        // Set the sideMenu tableView as a child view
        addChildViewController(menuDelegate)
        //        view.addSubview(menuDelegate.view)
        menuDelegate.didMove(toParentViewController: self)
        
        
        // Set the sideMenu delegate and dataSource to MenuController (sublass of TableViewController)
        SideMenu.delegate = menuDelegate
        SideMenu.dataSource = menuDelegate
        
        drawMenuRightLine()

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
                if self.menuDelegate.actSelectionChanged == true {
                    self.requestTentActGantt()
                    self.menuDelegate.actSelectionChanged = false
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
                        self.viewInInfo.infoType = self.aClient!.currentInfo.infoType!; self.aClient!.currentInfo.infoType = ""
                        self.viewInInfo.startTime = self.aClient!.currentInfo.startTime!; self.aClient!.currentInfo.startTime? = ""
                        self.viewInInfo.nextActivities = self.aClient!.currentInfo.nextActivities!; self.aClient!.currentInfo.nextActivities = []
                        self.viewInInfo.nextActsMinDur = self.aClient!.currentInfo.nextActsMinDur!; self.aClient!.currentInfo.nextActsMinDur = []
                        self.viewInInfo.nextActsMaxDur = self.aClient!.currentInfo.nextActsMaxDur!; self.aClient!.currentInfo.nextActsMaxDur = []
                        self.viewInInfo.strImg = self.aClient!.currentInfo.strImg!; self.aClient!.currentInfo.strImg = ""
                        self.viewInInfo.debugInfo = self.aClient!.currentInfo.debugInfo!; self.aClient!.currentInfo.debugInfo?.removeAll()
                        

                        // if this effects current choices, update list of possible activities and trigger a sideMenu refresh
                        if (self.viewInInfo.infoType == "currentChoices") {
                            self.menuDelegate.activityOptions = self.viewInInfo.nextActivities!
                            self.menuDelegate.minDurs = self.viewInInfo.nextActsMinDur!
                            self.menuDelegate.maxDurs = self.viewInInfo.nextActsMaxDur!
                            self.SideMenu.reloadData()
                            if self.viewInInfo.nextActivities!.count > 0 {
                                self.ConfirmActButton.isEnabled = true
                            }else {
                                self.ConfirmActButton.isEnabled = false
                            }
                        }
                    
                        if (self.viewInInfo.strImg != "") {
                            if let decodedData = Data(base64Encoded: self.viewInInfo.strImg!, options: .ignoreUnknownCharacters) {
                                let image = UIImage(data: decodedData)
                                self.GanttImageDisplay.image = image
                            }
                        }
                    }
                }
                
                
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        var gotBars : [GanttChartView.BarEntry] = []
        gotBars.append( GanttChartView.BarEntry(color: UIColor.blue, height: 0.2, textValue: "20", title: "First") )
        gotBars.append( GanttChartView.BarEntry(color: UIColor.orange, height: 0.80, textValue: "80", title: "Second") )
        gotBars.append( GanttChartView.BarEntry(color: UIColor.magenta, height: 0.9, textValue: "90", title: "Third") )
        gotBars.append( GanttChartView.BarEntry(color: UIColor.brown, height: 0.4, textValue: "40", title: "Fourth") )
        
        GanttChart.dataEntries = gotBars
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
        aPut.agentNum = agentNumber!
        
        // if some activity has been selected
        if SideMenu.indexPathForSelectedRow != nil {
            
            // send the label and duration of the currently selected activity
            if menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row] != "other" { // this is necessary to display 'other' rather than 'idle'
                aPut.activityName = menuDelegate.activityOptions[SideMenu.indexPathForSelectedRow!.row]
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
}


