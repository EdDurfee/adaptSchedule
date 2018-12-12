//
//  AddActivityViewController.swift
//  Schedule_App
//
//  Created by Drew Davis on 11/15/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

/*
 * This class will prevent a series of 'slides' to the user that will allow
 *  them to input the details of the activity they want to add in an intuitive
 *  way.
*/
class AddActivityViewController: UIViewController {

    // All variables that can be used to describe a new activity
    var actName: String!
    var minDur: String! = "5"
    var maxDur: String! = "5"
    var EST: String!
    var LET: String!
    var precConstraints: [String]! = []
    var succConstraints: [String]! = []
    
    //// define object references for visual items used in the addition slides ////
    
    // perpetual cancel button
    @IBOutlet weak var allSlides_cancel: UIButton!
    
    // name slide
    @IBOutlet weak var nameSlide: UIView!
    @IBOutlet weak var nameSlide_nameField: UITextField!
    @IBOutlet weak var nameSlide_next: UIButton!
    
    // duration slide (min & max)
    @IBOutlet weak var DurSlide: UIView!
    @IBOutlet weak var DurSlide_next: UIButton!
    @IBOutlet weak var DurSlide_back: UIButton!
    @IBOutlet weak var DurSlide_minTimePicker: UIDatePicker!
    @IBOutlet weak var DurSlide_maxTimePicker: UIDatePicker!
    
    // EST slide
    @IBOutlet weak var ESTSlide: UIView!
    @IBOutlet weak var ESTSlide_butt_anytime: UIButton!
    @IBOutlet weak var ESTSlide_butt_constraint: UIButton!
    @IBOutlet weak var ESTSlide_butt_specTime: UIButton!
    @IBOutlet weak var ESTSlide_back: UIButton!
    
    // EST time slide
    @IBOutlet weak var ESTTimeSlide: UIView!
    @IBOutlet weak var ESTTimeSlide_next: UIButton!
    @IBOutlet weak var ESTTimeSlide_back: UIButton!
    @IBOutlet weak var ESTTimeSlide_timePicker: UIDatePicker!
    
    // EST constraints slide
    @IBOutlet weak var ESTConstraintsSlide: UIView!
    @IBOutlet weak var ESTConstraintsSlide_next: UIButton!
    @IBOutlet weak var ESTConstraintsSlide_back: UIButton!
    @IBOutlet weak var ESTConstraintsSlide_actList: UITableView!
    
    // LET slide
    @IBOutlet weak var LETSlide: UIView!
    @IBOutlet weak var LETSlide_butt_anytime: UIButton!
    @IBOutlet weak var LETSlide_butt_constraint: UIButton!
    @IBOutlet weak var LETSlide_butt_specTime: UIButton!
    @IBOutlet weak var LETSlide_back: UIButton!
    
    // LET time slide
    @IBOutlet weak var LETTimeSlide: UIView!
    @IBOutlet weak var LETTimeSlide_next: UIButton!
    @IBOutlet weak var LETTimeSlide_back: UIButton!
    @IBOutlet weak var LETTimeSlide_timePicker: UIDatePicker!
    
    // LET constraints slide
    @IBOutlet weak var LETConstraintsSlide: UIView!
    @IBOutlet weak var LETConstraintsSlide_next: UIButton!
    @IBOutlet weak var LETConstraintsSlide_back: UIButton!
    @IBOutlet weak var LETConstraintsSlide_actList: UITableView!
    
    // finished slide
    @IBOutlet weak var finishedSlide: UIView!
    @IBOutlet weak var finishedSlide_close: UIButton!
    
    //// end outlet reference definitions ////
    
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // hide all slides initially
        nameSlide           .isHidden = true
        DurSlide            .isHidden = true
        ESTSlide            .isHidden = true
        ESTTimeSlide        .isHidden = true
        ESTConstraintsSlide .isHidden = true
        LETSlide            .isHidden = true
        LETTimeSlide        .isHidden = true
        LETConstraintsSlide .isHidden = true
        finishedSlide       .isHidden = true
        
        
        // start on the nameSlide
        switchToSlide(nameSlide)
        
        // link the time pickers to the function that will update associated class variables and keep them in legal ranges
        DurSlide_minTimePicker.addTarget(self, action: #selector(datePickerChanged(sender:)), for: .valueChanged)
        DurSlide_maxTimePicker.addTarget(self, action: #selector(datePickerChanged(sender:)), for: .valueChanged)
        LETTimeSlide_timePicker.addTarget(self, action: #selector(datePickerChanged(sender:)), for: .valueChanged)
        
    }

    
    // this function switches to a certain slide by hiding all other slides
    // it also initializes / resets all varaibles associated with the given slide
    func switchToSlide(_ slide: UIView) {
        
        // hide all slides
        nameSlide           .isHidden = true
        DurSlide            .isHidden = true
        ESTSlide            .isHidden = true
        ESTTimeSlide        .isHidden = true
        ESTConstraintsSlide .isHidden = true
        LETSlide            .isHidden = true
        LETTimeSlide        .isHidden = true
        LETConstraintsSlide .isHidden = true
        finishedSlide       .isHidden = true
        
        // then unhide only the desired slide
        slide.isHidden = false
        
        // and reset any variables that are controlled by the desired slide
        switch slide {
        case nameSlide:
            actName = ""
            
        case DurSlide:
//            minDur = ""
//            maxDur = ""
            // choose where the picker values are initialized to
            DurSlide_minTimePicker.countDownDuration = Double(minDur)! * 60 // in seconds
            DurSlide_maxTimePicker.countDownDuration = Double(maxDur)! * 60 // in seconds
       
        case ESTSlide:
            EST = ""
            precConstraints = []
        
        case ESTTimeSlide:
            EST = ""
            // choose what the picker value is initialized to
            ESTTimeSlide_timePicker.countDownDuration = 5 * 60 * 60 // in seconds // 05:00 am
        
        case ESTConstraintsSlide:
            precConstraints = []
        
        case LETSlide:
            LET = ""
            succConstraints = []
        
        case LETTimeSlide:
            LET = ""
            // choose what the picker value is initialized to
            LETTimeSlide_timePicker.countDownDuration = 24 * 60 * 60 - 5 * 60 // in seconds // 11:55 pm
        
        case LETConstraintsSlide:
            succConstraints = []
        
        case finishedSlide:
            // if you've reached this slide, you should have a full act description
            // send off request for activity addition to server
            let mainViewCont = self.presentingViewController as? MainViewController
            mainViewCont!.addActivity(actName: actName, minDur: minDur, maxDur: maxDur, EST: EST, LET: LET, precConstraints: precConstraints, succConstraints: succConstraints)
            
        
        default:
            print("Error: Attempted to switch to a non-existing slide")
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    
    
    //// Every button on every slide needs its action defined ////
    
    // This function handles all navigation buttons that control slide progression
    @IBAction func allSlides_navigationButtons_click(_ sender: UIButton) {
        
        // depending on which button was clicked, switch to apporpriate slide
        switch sender {
        
            
        //// ALL next buttons  -  set associated variable and switch to 'next' slide
        
        case nameSlide_next:
            actName = nameSlide_nameField.text
            switchToSlide(DurSlide)
        
        case DurSlide_next:
//            minDur = String(Int(DurSlide_minTimePicker.countDownDuration / 60)) // in minutes
//            maxDur = String(Int(DurSlide_maxTimePicker.countDownDuration / 60)) // in minutes
            switchToSlide(ESTSlide)
        
        case ESTTimeSlide_next:
            // convert from a date to the number of minutes since 00:00 on this day
            let sod = Calendar.current.startOfDay(for: ESTTimeSlide_timePicker.date)
            let TI = ESTTimeSlide_timePicker.date.timeIntervalSince(sod)
            EST = String(Int(TI/60))
            switchToSlide(LETSlide)
        
        case ESTConstraintsSlide_next:
            precConstraints = [] // TODO: all selected preceeding constraints
            switchToSlide(LETSlide)
        
        case LETTimeSlide_next:
            // convert from a date to the number of minutes since 00:00 on this day
            let sod = Calendar.current.startOfDay(for: LETTimeSlide_timePicker.date)
            let TI = LETTimeSlide_timePicker.date.timeIntervalSince(sod)
            LET = String(Int(TI/60))
            switchToSlide(finishedSlide)
        
        case LETConstraintsSlide_next:
            succConstraints = [] // TODO: all selected succeeding constraints
            switchToSlide(finishedSlide)
        
        
        ////  Buttons on EST and LET slides  -  switch to appropriate slection slide
            
        case ESTSlide_butt_anytime:
            switchToSlide(LETSlide)
            
        case ESTSlide_butt_specTime:
            switchToSlide(ESTTimeSlide)
            
        case ESTSlide_butt_constraint:
            switchToSlide(ESTConstraintsSlide)
            
        case LETSlide_butt_anytime:
            switchToSlide(finishedSlide)
            
        case LETSlide_butt_specTime:
            switchToSlide(LETTimeSlide)
            
        case LETSlide_butt_constraint:
            switchToSlide(LETConstraintsSlide)
        
            
        //// ALL back buttons  -  switch to 'previous' slide in deck
            
        case DurSlide_back:
            switchToSlide(nameSlide)
            
        case ESTSlide_back:
            switchToSlide(DurSlide)
            
        case ESTTimeSlide_back:
            switchToSlide(ESTSlide)
            
        case ESTConstraintsSlide_back:
            switchToSlide(ESTSlide)
            
        case LETSlide_back:
            switchToSlide(ESTSlide)
            
        case LETTimeSlide_back:
            switchToSlide(LETSlide)
            
        case LETConstraintsSlide_back:
            switchToSlide(LETSlide)
            
        default:
            print("Error: Attempted to switch from an unexpected button")
        }
    }
    

    
    // perpetual cancel button AND finishedSlide close button
    // close the popover when button clicked
    @IBAction func allSlides_exitButtons_click(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
    
    
    //// End button action definitions ////
    
    
    // force the min and max duration pickers to stay in a legal time range
    @objc func datePickerChanged(sender: UIDatePicker){
        
        // if the picker being changed is the min duration picker
        if (sender == DurSlide_minTimePicker) {
            // if picker value is greater than max duration
            if ( (60 * Int( maxDur )!) < Int(sender.countDownDuration) ) {
                let calendar = Calendar(identifier: .gregorian)
                let date = DateComponents(calendar: calendar, hour: 0, minute: Int( maxDur )!).date!
                sender.setDate(date, animated: true)
            }
            minDur = String(Int(sender.countDownDuration / 60)) // in minutes
        }
        
        // if the picker being changed is the max duration picker
        if (sender == DurSlide_maxTimePicker) {
            // if picker value is less than min duration
            if ( Int(sender.countDownDuration) < (60 * Int( minDur )!) ) {
                let calendar = Calendar(identifier: .gregorian)
                let date = DateComponents(calendar: calendar, hour: 0, minute: Int( minDur )!).date!
                sender.setDate(date, animated: true)
            }
            maxDur = String(Int(sender.countDownDuration / 60)) // in minutes
        }
        
        // if the picker being changed is the LET time picker
        if (sender == LETTimeSlide_timePicker) {
            
            // convert from a date to the number of minutes since 00:00 on this day
            let sod = Calendar.current.startOfDay(for: LETTimeSlide_timePicker.date)
            let TI = LETTimeSlide_timePicker.date.timeIntervalSince(sod) // in seconds
            
            // if the picker value is less than EST + minDur
            if ( Int(TI) < (60 * (Int(EST)! + Int(minDur)!)) ) {
                let calendar = Calendar(identifier: .gregorian)
                let date = DateComponents(calendar: calendar, hour: 0, minute: Int(EST)! + Int(minDur)!).date!
                sender.setDate(date, animated: true)
            }
        }
    }
    
    
    // when presenting this popover, dim the background view
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presentingViewController?.view.alpha = 0.5
    }
    
    // when closing this popover, reset the background view dimness
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.presentingViewController?.view.alpha = 1
    }
    
    
}
