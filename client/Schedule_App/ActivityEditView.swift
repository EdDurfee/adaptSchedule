//
//  ModifyActView.swift
//  Schedule_App
//
//  Created by Drew Davis on 7/24/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class ActivityEditView: UIView {

    /*
    // Only override draw() if you perform custom drawing.
    // An empty implementation adversely affects performance during animation.
    override func draw(_ rect: CGRect) {
        // Drawing code
    }
    */
    
    func addBackgroundShape() {
        
        
        let path: UIBezierPath = UIBezierPath()
        let vertSpace : CGFloat = 100.0
        let horzSpace : CGFloat = 50.0
        
        path.move(to: CGPoint(x: horzSpace, y: vertSpace))
        path.addLine(to: CGPoint(x: frame.size.width - horzSpace,  y: vertSpace))
        path.addLine(to: CGPoint(x: frame.size.width - horzSpace,  y: frame.size.height - vertSpace))
        path.addLine(to: CGPoint(x: horzSpace,  y: frame.size.height - vertSpace))
        path.close()
        
        
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 0.5
        pathLayer.fillColor = UIColor.white.cgColor
        pathLayer.strokeColor = UIColor.white.cgColor
        
        
        layer.insertSublayer(pathLayer, at: UInt32(0.0))
        
        
    }

}
