//
//  AddActSlideTemplate.swift
//  Schedule_App
//
//  Created by Drew Davis on 11/15/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class AddActSlideTemplate: UIView {

    let headerLabel: UILabel = UILabel(frame: CGRect(x: 0, y: 61, width: 664, height: 114))
    
    
    
    
    
    // Only override draw() if you perform custom drawing.
    // An empty implementation adversely affects performance during animation.
    override func draw(_ rect: CGRect) {
        // Drawing code
        super.draw(rect)
        
        // set up label properties
        headerLabel.font = UIFont.preferredFont(forTextStyle: .headline)
        headerLabel.textColor = .black
        headerLabel.center = CGPoint(x: self.frame.width/2, y: 118)
        headerLabel.textAlignment = .center
        
        headerLabel.text = "Header"
        
        self.addSubview(label)
    }
    

}
