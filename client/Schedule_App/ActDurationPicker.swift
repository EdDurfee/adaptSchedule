//
//  ActDurationPicker.swift
//  Schedule_App
//
//  Created by Drew Davis on 7/9/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class ActDurationPicker: UIDatePicker {

    /*
    // Only override draw() if you perform custom drawing.
    // An empty implementation adversely affects performance during animation.
    override func draw(_ rect: CGRect) {
        // Drawing code
    }
    */
    
    func datePickedValueChanged (sender: UIDatePicker) {
        if (sender.countDownDuration > 5400) { //5400 seconds = 1h30min
            sender.countDownDuration = 60.0; //Defaults to 1 minute
        }
    }
    
    override func didChangeValue(forKey key: String) {
        print(key)
    }
    

}
