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
    let barWidth: CGFloat = 40.0
    
    /// space between each bar
    let space: CGFloat = 20.0
    
    /// space at the bottom of the bar to show the title
    private let bottomSpace: CGFloat = 40.0
    
    /// space at the top of each bar to show the value
    private let topSpace: CGFloat = 40.0
    
    
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
            // remove drawn lines
            //self.layer.sublayers?.forEach({$0.removeFromSuperlayer()})
            
            if let dataEntries = dataEntries {
                scrollView.contentSize = CGSize(width: (barWidth + space)*CGFloat(dataEntries.count), height: self.frame.size.height)
                mainLayer.frame = CGRect(x: 0, y: 0, width: scrollView.contentSize.width, height: scrollView.contentSize.height)
                
                drawHorzLine(height: 0.0, color: UIColor.gray, lineType: "solid")
                drawHorzLine(height: 1.0, color: UIColor.gray, lineType: "solid")
                drawHorzLine(height: 0.2, color: UIColor.gray, lineType: "dashed")
                
                for i in 0..<dataEntries.count {
                    showEntry(index: i, entry: dataEntries[i])
                }
            }
        }
    }
    
    
    // calculate frame shape and color of bar
    // and add bar to mainLayer (CALayer)
    private func drawBar(xPos: CGFloat, yPos: CGFloat, color: UIColor) {
        let barLayer = CALayer()
        barLayer.frame = CGRect(x: xPos, y: yPos, width: barWidth, height: mainLayer.frame.height - bottomSpace - yPos)
        barLayer.backgroundColor = color.cgColor
        mainLayer.addSublayer(barLayer)
    }
    
    
    // Draw the text above a bar in the chart
    private func drawTextValue(xPos: CGFloat, yPos: CGFloat, textValue: String, color: UIColor) {
        let textLayer = CATextLayer()
        textLayer.frame = CGRect(x: xPos, y: yPos, width: barWidth+space, height: 22)
        textLayer.foregroundColor = color.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentCenter
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = textValue
        mainLayer.addSublayer(textLayer)
    }
    
    
    // Draw the text below a bar in the chart
    private func drawTitle(xPos: CGFloat, yPos: CGFloat, title: String, color: UIColor) {
        let textLayer = CATextLayer()
        textLayer.frame = CGRect(x: xPos, y: yPos, width: barWidth + space, height: 22)
        textLayer.foregroundColor = color.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentCenter
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = title
        mainLayer.addSublayer(textLayer)
    }
    
    
    // Convert height percent (0.0 - 1.0) to actual bar height
    private func translateHeightValueToYPosition(value: Float) -> CGFloat {
        let height: CGFloat = CGFloat(value) * (mainLayer.frame.height - bottomSpace - topSpace)
        return mainLayer.frame.height - bottomSpace - height
    }
    
    
    // Take a BarEntry object and create and display a visual bar representing it
    private func showEntry(index: Int, entry: BarEntry) {
        /// Starting x postion of the bar
        let xPos: CGFloat = space + CGFloat(index) * (barWidth + space)
        
        /// Starting y postion of the bar
        let yPos: CGFloat = translateHeightValueToYPosition(value: entry.height)
        
        drawBar(xPos: xPos, yPos: yPos, color: entry.color)
        
        /// Draw text above the bar
        drawTextValue(xPos: xPos - space/2, yPos: yPos - 30, textValue: entry.textValue, color: entry.color)
        
        /// Draw text below the bar
        drawTitle(xPos: xPos - space/2, yPos: mainLayer.frame.height - bottomSpace + 10, title: entry.title, color: entry.color)
    }
    
    
    // draw a horizontal line across the chart
    // will be at height yPos and will go all the way across the chart
    private func drawHorzLine(height: Float, color: UIColor, lineType: String) {
        
        let xPos = CGFloat(0.0)
        let yPos = translateHeightValueToYPosition(value: height)
        
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
    
    
    // The struct containing all the info represented by a visual bar
    struct BarEntry {
        let color: UIColor
        
        /// Ranged from 0.0 to 1.0
        let height: Float
        
        /// To be shown on top of the bar
        let textValue: String
        
        /// To be shown at the bottom of the bar
        let title: String
    }

}
