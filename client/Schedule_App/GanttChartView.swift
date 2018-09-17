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

class GanttChartView: UIView, UITextViewDelegate {

    /// the width of each bar
    let barHeight: CGFloat = 30.0
    
    /// space between each bar
    let barSpace: CGFloat = 15.0
    
    /// space at the left of the bar to show the title
    let leftSpace: CGFloat = 10.0
    
    /// space at the right of each bar to show the value
    let rightSpace: CGFloat = 40.0
    
    /// space at the right of each bar to show the value
    let scaleSpace: CGFloat = 60.0
    
    private let minutesDisplayedInPlot: CGFloat = 1440.0
    
    // current time in minutes from start of day
    public var currTime: Int = 0
    
    
    private let barsDrawLayer: CALayer = CALayer()
    private let barsScrollView: UIScrollView = UIScrollView()
    private let scaleDrawLayer: CALayer = CALayer()
    private let scaleScrollView: UIScrollView = UIScrollView()
    
    let finishedColor: UIColor         = UIColor.init(red: 128.0/255.0, green: 064.0/255.0, blue: 000.0/255.0, alpha: 1.0)
    
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
        scaleScrollView.layer.addSublayer(scaleDrawLayer)
        self.addSubview(scaleScrollView)
        scaleScrollView.delegate = self
        
        barsScrollView.layer.addSublayer(barsDrawLayer)
        self.addSubview(barsScrollView)
        barsScrollView.delegate = self
    }
    
    // Force the two scroll views to keep x coordinates synchronized so that scale always lines up with bars
    // Also force the x and y coordinates to move together and match % offsets
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        
        // lock the x coordinates of the 2 scroll views
        if scrollView == scaleScrollView {
            barsScrollView.contentOffset.x = scrollView.contentOffset.x
        }else if scrollView == barsScrollView {
            scaleScrollView.contentOffset.x = barsScrollView.contentOffset.x
        }
        
        // synchrinize vertical and horizontal scrolling
        let barsXPerc = barsScrollView.contentOffset.x / (barsScrollView.contentSize.width - barsScrollView.frame.width) // what percent x offset
        
        // do not begin automatic vertical offset scrolling unless the content exceeds the vertical screen size
        if (barsScrollView.contentSize.height > barsScrollView.frame.height) {
            barsScrollView.contentOffset.y = barsXPerc * abs(barsScrollView.contentSize.height - barsScrollView.frame.height)
        }
        
    }
    
    override func layoutSubviews() {
        barsScrollView.frame  = CGRect(x: 0, y: 0, width: self.frame.size.width, height: self.frame.size.height - scaleSpace)
        
        scaleScrollView.frame = CGRect(x: 0, y: self.frame.size.height - scaleSpace, width: self.frame.size.width, height: scaleSpace)
    }
    
    
    // This is the data used to create the bars
    // the didSet function will be called when this variable is set
    var dataEntries: [BarEntry]? = nil {
        
        // clear out all previous bars
        // and create and show new bars with showEntry()
        // creates entire chart
        didSet {
            
            // remove old bars
            barsDrawLayer.sublayers?.forEach({$0.removeFromSuperlayer()})
            // remove old scale
            scaleDrawLayer.sublayers?.forEach({$0.removeFromSuperlayer()})
            
            if let dataEntries = dataEntries {
                barsScrollView.contentSize = CGSize(width: self.frame.size.width*2, height: (barHeight + barSpace)*CGFloat(dataEntries.count) + barSpace)
                scaleScrollView.contentSize = CGSize(width: self.frame.size.width*2, height: scaleSpace)
                barsDrawLayer.frame = CGRect(x: 0, y: 0, width: barsScrollView.contentSize.width, height: barsScrollView.contentSize.height)
                
                drawVertLine(x: leftSpace,                                               color: UIColor.gray,   lineType: "solid")
                drawVertLine(x: leftSpace + minsToX(mins: Int(minutesDisplayedInPlot)),  color: UIColor.gray,   lineType: "solid")
                drawVertLine(x: minsToX(mins: Int(leftSpace) + currTime),                color: UIColor.red,   lineType: "solid")
                
                drawScale()
                
                // sort the entries based on their ID (which was sorted in the server based on act time)
                let tempDataEntries = dataEntries.sorted(by: { $0.ID < $1.ID })
                
                // display the sorted items so that the earlier activities are at the top
                for i in 0..<dataEntries.count {
                    showEntry(index: i, entry: tempDataEntries[i], currentTime: currTime)
                }
            }
            
            // Automatically adjust scroll so that the left side of the screen is a few hours behind current time
            barsScrollView.contentOffset.x  = minsToX(mins: self.currTime ) - minsToX(mins: 180 )
            scaleScrollView.contentOffset.x = minsToX(mins: self.currTime ) - minsToX(mins: 180 )
        }
    }
    
    
    // Take a BarEntry object and create and display a visual bar representing it
    private func showEntry(index: Int, entry: BarEntry, currentTime: Int) {
        
        
        
        // Starting x postion of the bar
        let xPos: CGFloat = leftSpace + minsToX(mins: entry.EST)
        
        // Starting y postion of the bar
        let yPos: CGFloat = barSpace + CGFloat(index) * (barHeight + barSpace)
        
        // barLength is actual left-to-right length of bar in pixels
        var barLength: CGFloat
        
        // if this activity is already completed
        if (entry.LET <= currentTime) {
            // draw activity participation time
            barLength = minsToX ( mins: entry.LET - entry.EST )
            drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: finishedColor)
        }
        // if this activity is not yet completed, draw duration
        else {
            // draw availability
            barLength = minsToX ( mins: entry.LET - entry.EST )
            drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: entry.color)
            
            // draw max duration
            barLength = minsToX ( mins: entry.maxDuration )
            drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: UIColor.lightGray)
            
            // draw min duration
            barLength = minsToX ( mins: entry.minDuration )
            drawBar(xPos_left: xPos + minsToX(mins: entry.maxDuration/2 - entry.minDuration/2), yPos_top: yPos, barLength: barLength, color: UIColor.darkGray)
        }
        
        // Draw label in the middle of the bar
        drawBarTextLabel(yPos_top: yPos + barHeight/4, xPos: leftSpace + minsToX(mins: entry.EST), textValue: entry.activityName)
        
        // Draw title to the left of the bar
//        drawBarTextTitle(xPos_left: 10, yPos_top: yPos + barHeight*3/10, title: entry.title, color: entry.color)
    }
    
    
    // calculate frame shape and color of bar
    // and add bar to barsDrawLayer (CALayer)
    // xPos: starting x position of bar (left)
    // yPos: starting y position of bar (top)
    // barLength: length of bar in pixels
    // color: UIColor of bar
    private func drawBar(xPos_left: CGFloat, yPos_top: CGFloat, barLength: CGFloat, color: UIColor) {
        
        let barLength = max(barLength - 7.5*2, 0)
        
        let xPos_left = xPos_left + 7.5
        
        let path: UIBezierPath = UIBezierPath()
        
//        path.move(to: CGPoint(x: xPos_left + barHeight/4, y: yPos_top))
//
//
//        // TODO: Display of bar is wrong when barHeight > barLength
////        // draw 4 sides and 2 half-circles to fill
//        path.addLine(to: CGPoint(x: xPos_left + barLength - barHeight/4,  y: yPos_top))
////        path.addArc(withCenter: CGPoint(x: xPos_left + barLength - barHeight/4, y: yPos_top + barHeight/4), radius: barHeight/4, startAngle: CGFloat.pi/2.0, endAngle: 3.0*CGFloat.pi/2.0, clockwise: false) // addArc moves cursor to center of arc
//        path.addCurve(to: CGPoint(x: xPos_left + barLength - barHeight/4,  y: yPos_top + barHeight), controlPoint1: CGPoint(x: xPos_left + barLength, y: yPos_top), controlPoint2: CGPoint(x: xPos_left + barLength, y: yPos_top + barHeight))
//
//        path.addLine(to: CGPoint(x: xPos_left + barLength - barHeight/4,  y: yPos_top + barHeight))
//        path.addLine(to: CGPoint(x: xPos_left + barHeight/4,                            y: yPos_top + barHeight))
////        path.addArc(withCenter: CGPoint(x: xPos_left + barHeight/4, y: yPos_top + barHeight/4), radius: barHeight/4, startAngle: 3.0*CGFloat.pi/2.0, endAngle: CGFloat.pi/2.0, clockwise: false)

                path.move(to: CGPoint(x: xPos_left, y: yPos_top))
        
        
                // TODO: Display of bar is wrong when barHeight > barLength
        //        // draw 4 sides and 2 half-circles to fill
                path.addLine(to: CGPoint(x: xPos_left + barLength,  y: yPos_top))
        //        path.addArc(withCenter: CGPoint(x: xPos_left + barLength - barHeight/4, y: yPos_top + barHeight/4), radius: barHeight/4, startAngle: CGFloat.pi/2.0, endAngle: 3.0*CGFloat.pi/2.0, clockwise: false) // addArc moves cursor to center of arc
                path.addCurve(to: CGPoint(x: xPos_left + barLength,  y: yPos_top + barHeight), controlPoint1: CGPoint(x: xPos_left + barLength + 10, y: yPos_top), controlPoint2: CGPoint(x: xPos_left + barLength + 10, y: yPos_top + barHeight))
        
                path.addLine(to: CGPoint(x: xPos_left + barLength,  y: yPos_top + barHeight))
                path.addLine(to: CGPoint(x: xPos_left,                            y: yPos_top + barHeight))
                path.addCurve(to: CGPoint(x: xPos_left,  y: yPos_top), controlPoint1: CGPoint(x: xPos_left - 10, y: yPos_top + barHeight), controlPoint2: CGPoint(x: xPos_left - 10, y: yPos_top))

        //        path.addArc(withCenter: CGPoint(x: xPos_left + barHeight/4, y: yPos_top + barHeight/4), radius: barHeight/4, startAngle: 3.0*CGFloat.pi/2.0, endAngle: CGFloat.pi/2.0, clockwise: false)

        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 0.5
        pathLayer.fillColor = color.cgColor
        pathLayer.strokeColor = color.cgColor
        
        barsDrawLayer.addSublayer(pathLayer)
        
    }
    
    
    // Draw the text on a bar in the chart
    private func drawBarTextLabel(yPos_top: CGFloat, xPos: CGFloat, textValue: String) {
        let textLayer = CATextLayer()
        let textBoxWidth = CGFloat(100.0)
        textLayer.frame = CGRect(x: xPos - textBoxWidth - 10, y: yPos_top, width: textBoxWidth, height: barHeight)
        textLayer.foregroundColor = UIColor.black.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentRight
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = textValue
        barsDrawLayer.addSublayer(textLayer)
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
        barsDrawLayer.addSublayer(textLayer)
    }
    
    
    // Convert height percent (0.0 - 1.0) to actual bar height relative to GanttChartView
    private func translateHeightValueToBarsYPosition(value: Float) -> CGFloat {
        let height: CGFloat = CGFloat(value) * (barsScrollView.contentSize.height)
        return barsScrollView.contentSize.height + height
    }
    
    // Convert width percent (0.0 - 1.0) to actual bar height relative to GanttChartView
    private func translateWidthValueToBarsXPosition(value: Float) -> CGFloat {
        let width: CGFloat = CGFloat(value) * (barsScrollView.contentSize.width - leftSpace - rightSpace)
        return width + leftSpace
    }
    
    private func drawScale() {
        drawHorzLine(y: barsScrollView.frame.height, color: UIColor.gray, lineType: "solid" )
        // mark each hour in the day
        for i in 1..<Int(minutesDisplayedInPlot / 60) {
            drawVertLine(x: minsToX(mins: i*60) + leftSpace, color: UIColor.gray, lineType: "dashed")
            drawScaleLabel(mins: i*60)
        }
    }
    
    // draw a horizontal line across the chart
    // will be at height yPos and will go all the way across the chart
    private func drawHorzLine(y: CGFloat, color: UIColor, lineType: String) {
        
        let xPos = CGFloat(0.0)
        let yPos = y
        
        let path = UIBezierPath()
        path.move(to: CGPoint(x: xPos, y: yPos))
        path.addLine(to: CGPoint(x: barsScrollView.frame.size.width, y: yPos))
        
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
    private func drawVertLine(x: CGFloat, color: UIColor, lineType: String) {
        
        // draw line that will show in the bars drawing area
        let xPosBars = x
        let yPosBars = CGFloat(0.0)
        
        let pathBars = UIBezierPath()
        pathBars.move(to: CGPoint(x: xPosBars, y: yPosBars))
        pathBars.addLine(to: CGPoint( x: xPosBars, y: 2 * max(barsScrollView.frame.height, barsScrollView.contentSize.height) ))
        
        let lineLayerBars = CAShapeLayer()
        lineLayerBars.path = pathBars.cgPath
        lineLayerBars.lineWidth = 0.5
        if lineType == "dashed" {
            lineLayerBars.lineDashPattern = [4, 4]
        }
        lineLayerBars.strokeColor = color.cgColor
        barsDrawLayer.insertSublayer(lineLayerBars, at: 0)
        
        // draw line that will show in the scale drawing area
        let xPosScale = x
        let yPosScale = CGFloat(0.0)
        
        let pathScale = UIBezierPath()
        pathScale.move(to: CGPoint(x: xPosScale, y: yPosScale))
        pathScale.addLine(to: CGPoint( x: xPosScale, y: scaleScrollView.contentSize.height / 4 ))
        
        let lineLayerScale = CAShapeLayer()
        lineLayerScale.path = pathScale.cgPath
        lineLayerScale.lineWidth = 0.5
//        if lineType == "dashed" {
//            lineLayerScale.lineDashPattern = [4, 4]
//        }
        lineLayerScale.strokeColor = color.cgColor
        scaleDrawLayer.insertSublayer(lineLayerScale, at: 0)
    }
    
    // draw a label on the x scale (ex: 14:00)
    private func drawScaleLabel(mins: Int) {
        let xPos = minsToX(mins: mins-30) + leftSpace // left of text
        let yPos = scaleScrollView.contentSize.height / 4 // top of text
        
        let textLayer = CATextLayer()
        textLayer.alignmentMode = kCAAlignmentCenter
        textLayer.fontSize = 18
        textLayer.foregroundColor = UIColor.black.cgColor
        textLayer.frame = CGRect(x: xPos, y: yPos, width: minsToX(mins:60), height: scaleSpace )
        textLayer.string = String(mins / 60) + ":00"
        
        scaleDrawLayer.insertSublayer(textLayer, at: 0)
        
    }
    
    // Convert from minutes to the X pixel coordinate from left side of plot
    private func minsToX(mins: Int) -> CGFloat {
        // % of day  *  width of plot in pixels
        return CGFloat( CGFloat(mins) / minutesDisplayedInPlot  * (barsScrollView.contentSize.width - leftSpace - rightSpace) )
    }
    
    
    // The struct containing all the info represented by a visual bar
    struct BarEntry {
        let color: UIColor
        
        let ID: Int
        
        let EST: Int
        
        let LET: Int
        
        let minDuration: Int
        
        let maxDuration: Int
        
        let restrict: Double
        
        /// To be shown on top of the bar
        let activityName: String
        
        /// To be shown at the bottom of the bar
        let title: String
    }

}
