//
//  ModifyActView.swift
//  Schedule_App
//
//  Created by Drew Davis on 7/24/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class ModifyActivityView: UIView {

    /*
    // Only override draw() if you perform custom drawing.
    // An empty implementation adversely affects performance during animation.
    override func draw(_ rect: CGRect) {
        // Drawing code
    }
    */
    
    func addBackgroundShape() {
        
//        let radius : CGFloat = 40.0
//
//        let path: UIBezierPath = UIBezierPath()
//        path.move(to: CGPoint(x: radius, y: 0))
//
//        // draw 4 sides and 2 half-circles to fill //
//
//        // top right
//        path.addLine(to: CGPoint(x: frame.size.width - radius,  y: 0))
//        path.addArc(withCenter: CGPoint(x: frame.size.width - radius, y: 0 + radius), radius: radius, startAngle: CGFloat.pi/2.0, endAngle: 0.0, clockwise: true) // addArc moves cursor to center of arc
//
//        // bottom right
//        path.addLine(to: CGPoint(x: frame.size.width,  y: frame.size.height - radius))
//        path.addArc(withCenter: CGPoint(x: frame.size.width - radius, y: frame.size.height - radius), radius: radius, startAngle: 0, endAngle: CGFloat.pi/2.0, clockwise: true)
//
//        // bottom left
//        path.addLine(to: CGPoint(x: radius, y: frame.size.height))
//        path.addArc(withCenter: CGPoint(x: radius, y: frame.size.height - radius), radius: radius, startAngle: 3.0*CGFloat.pi/2.0, endAngle: CGFloat.pi, clockwise: true)
//
//        // top left
//        path.addLine(to: CGPoint(x: 0, y: radius))
//        path.addArc(withCenter: CGPoint( x: radius, y: radius), radius: radius, startAngle: CGFloat.pi, endAngle: 0, clockwise: true)
//
//        path.close()
        
        
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
