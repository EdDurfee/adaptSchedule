//
//  GanttChartView.swift
//  Schedule_App
//
//  Created by Drew Davis on 7/17/18.
//  Copyright © 2018 Drew. All rights reserved.
//
//  Much of this code was provided by Minh Nguyen on GitHub and medium.com
//  https://medium.com/@leonardnguyen/build-your-own-chart-in-ios-part-1-bar-chart-e1b7f4789d70
//  https://github.com/nhatminh12369/BarChart/blob/master/BarChart/BasicBarChart.swift


import UIKit

class GanttChartView: UIView {

    /// the width of each bar
    let barHeight: CGFloat = 40.0
    
    /// space between each bar
    let barSpace: CGFloat = 20.0
    
    /// space at the left of the bar to show the title
    private let leftSpace: CGFloat = 70.0
    
    /// space at the right of each bar to show the value
    private let rightSpace: CGFloat = 40.0
    
    
    private let mainLayer: CALayer = CALayer()
    private let scrollView: UIScrollView = UIScrollView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    convenience init() {
        self.init(frame: CGRect.zero)
        setupView()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setupView()
    }
    
    private func setupView() {
        scrollView.layer.addSublayer(mainLayer)
        self.addSubview(scrollView)
    }
    
    override func layoutSubviews() {
        scrollView.frame = CGRect(x: 0, y: 0, width: self.frame.size.width, height: self.frame.size.height)
    }
    
    
    // This is the data used to create the bars
    // the didSet function will be called when this variable is set
    var dataEntries: [BarEntry]? = nil {
        
        // clear out all previous bars
        // and create and show new bars with showEntry()
        // creates entire chart
        didSet {
            
            // remove old bars
            mainLayer.sublayers?.forEach({$0.removeFromSuperlayer()})
//            // remove drawn lines
//            self.layer.sublayers?.forEach({$0.removeFromSuperlayer()})
            
            if let dataEntries = dataEntries {
                scrollView.contentSize = CGSize(width: self.frame.size.width, height: (barHeight + barSpace)*CGFloat(dataEntries.count) + barSpace)
                mainLayer.frame = CGRect(x: 0, y: 0, width: scrollView.contentSize.width, height: scrollView.contentSize.height)
                
                drawVertLine(x: 0.0, color: UIColor.gray, lineType: "solid")
                drawVertLine(x: 1.0, color: UIColor.gray, lineType: "solid")
                drawVertLine(x: 0.5, color: UIColor.gray, lineType: "dashed")
                
                for i in 0..<dataEntries.count {
                    showEntry(index: i, entry: dataEntries[i])
                }
            }
        }
    }
    
    
    // Take a BarEntry object and create and display a visual bar representing it
    private func showEntry(index: Int, entry: BarEntry) {
        
        // actual left-to-right length of bar in pixels
        let barLength: CGFloat = CGFloat(entry.length) * (mainLayer.frame.width - leftSpace - rightSpace)
        
        // Starting x postion of the bar
        let xPos: CGFloat = leftSpace
        
        // Starting y postion of the bar
        let yPos: CGFloat = barSpace + CGFloat(index) * (barHeight + barSpace)
        
        drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: entry.color)
        
        // Draw label in the middle of the bar
        drawBarTextLabel(yPos_top: yPos + barHeight*3/10, barLength: barLength, textValue: entry.textValue)
        
        // Draw title to the left of the bar
        drawBarTextTitle(xPos_left: 10, yPos_top: yPos + barHeight*3/10, title: entry.title, color: entry.color)
    }
    
    
    // calculate frame shape and color of bar
    // and add bar to mainLayer (CALayer)
    // xPos: starting x position of bar (left)
    // yPos: starting y position of bar (top)
    // barLength: length of bar in pixels
    // color: UIColor of bar
    private func drawBar(xPos_left: CGFloat, yPos_top: CGFloat, barLength: CGFloat, color: UIColor) {
        
        let path: UIBezierPath = UIBezierPath()
        path.move(to: CGPoint(x: xPos_left + barHeight/2, y: yPos_top))
//
//        // draw 4 sides and 2 half-circles to fill
        path.addLine(to: CGPoint(x: xPos_left + barLength - barHeight/2,  y: yPos_top))
        path.addArc(withCenter: CGPoint(x: xPos_left + barLength - barHeight/2, y: yPos_top + barHeight/2), radius: barHeight/2, startAngle: CGFloat.pi/2.0, endAngle: 3.0*CGFloat.pi/2.0, clockwise: false) // addArc moves cursor to center of arc
        
        path.addLine(to: CGPoint(x: xPos_left + barLength - barHeight/2,  y: yPos_top + barHeight))
        path.addLine(to: CGPoint(x: xPos_left + barHeight/2,                            y: yPos_top + barHeight))
        path.addArc(withCenter: CGPoint(x: xPos_left + barHeight/2, y: yPos_top + barHeight/2), radius: barHeight/2, startAngle: 3.0*CGFloat.pi/2.0, endAngle: CGFloat.pi/2.0, clockwise: false)
        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 0.5
        pathLayer.fillColor = color.cgColor
        pathLayer.strokeColor = color.cgColor
        
        mainLayer.addSublayer(pathLayer)
        
    }
    
    
    // Draw the text above a bar in the chart
    private func drawBarTextLabel(yPos_top: CGFloat, barLength: CGFloat, textValue: String) {
        let textLayer = CATextLayer()
        textLayer.frame = CGRect(x: leftSpace + barLength/2-50, y: yPos_top, width: 100, height: 22)
        textLayer.foregroundColor = UIColor.gray.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentCenter
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = textValue
        mainLayer.addSublayer(textLayer)
    }
    
    
    // Draw the text below a bar in the chart
    private func drawBarTextTitle(xPos_left: CGFloat, yPos_top: CGFloat, title: String, color: UIColor) {
        let textLayer = CATextLayer()
        textLayer.frame = CGRect(x: xPos_left, y: yPos_top, width: leftSpace - xPos_left - 5, height: barHeight)
        textLayer.foregroundColor = color.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentRight
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = title
        mainLayer.addSublayer(textLayer)
    }
    
    
    // Convert height percent (0.0 - 1.0) to actual bar height relative to GanttChartView
    private func translateHeightValueToBarsYPosition(value: Float) -> CGFloat {
        let height: CGFloat = CGFloat(value) * (scrollView.contentSize.height)
        return scrollView.contentSize.height + height
    }
    
    // Convert width percent (0.0 - 1.0) to actual bar height relative to GanttChartView
    private func translateWidthValueToBarsXPosition(value: Float) -> CGFloat {
        let width: CGFloat = CGFloat(value) * (scrollView.contentSize.width - leftSpace - rightSpace)
        return width + leftSpace
    }
    
    
    // draw a horizontal line across the chart
    // will be at height yPos and will go all the way across the chart
    private func drawHorzLine(y: Float, color: UIColor, lineType: String) {
        
        let xPos = CGFloat(0.0)
        let yPos = translateHeightValueToBarsYPosition(value: y)
        
        let path = UIBezierPath()
        path.move(to: CGPoint(x: xPos, y: yPos))
        path.addLine(to: CGPoint(x: scrollView.frame.size.width, y: yPos))
        
        let lineLayer = CAShapeLayer()
        lineLayer.path = path.cgPath
        lineLayer.lineWidth = 0.5
        if lineType == "dashed" {
            lineLayer.lineDashPattern = [4, 4]
        }
        lineLayer.strokeColor = color.cgColor
        self.layer.insertSublayer(lineLayer, at: 0)
        
    }
    
    // draw a vertical line across the chart
    // will be at width xPos and will go all the way vertical across the chart
    private func drawVertLine(x: Float, color: UIColor, lineType: String) {
        
        let xPos = translateWidthValueToBarsXPosition(value: x)
        let yPos = CGFloat(0.0)
        
        let path = UIBezierPath()
        path.move(to: CGPoint(x: xPos, y: yPos))
        path.addLine(to: CGPoint(x: xPos, y: scrollView.frame.size.height))
        
        let lineLayer = CAShapeLayer()
        lineLayer.path = path.cgPath
        lineLayer.lineWidth = 0.5
        if lineType == "dashed" {
            lineLayer.lineDashPattern = [4, 4]
        }
        lineLayer.strokeColor = color.cgColor
        self.layer.insertSublayer(lineLayer, at: 0)
        
    }
    
    
    // The struct containing all the info represented by a visual bar
    struct BarEntry {
        let color: UIColor
        
        /// Ranged from 0.0 to 1.0
        let length: Float
        
        /// To be shown on top of the bar
        let textValue: String
        
        /// To be shown at the bottom of the bar
        let title: String
    }

}
