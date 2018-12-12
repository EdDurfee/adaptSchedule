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
    
    // how wide the names bar on the left should be
    let namesWidth: CGFloat = 120.0
    
    // how many minutes the plot repreents horizontally
    private let minutesDisplayedInPlot: CGFloat = 1440.0
    
    // this variable determines how wide the bars are scaled to
    let barsWidthScaleFactor: CGFloat = 1.75
    
    // current time in minutes from start of day
    public var currTime: Int = 0
    
    // set this as true if the upcoming set of bars if from a tentative user request
    var tentGantt: Bool = false
    
    // keep track of what the bars were before any tentative choice - set in ViewController
    var lastConfirmedBars: [BarEntry]? = []
    
    // the label that displays current system time to the user
    var timeLabel: UILabel = UILabel()
    
    // the coloring of the duration and availability bars
    let availDefault: UIColor = UIColor.init(red: 154/255.0, green: 196/255.0, blue: 248/255.0, alpha: 1.0)
//    let availGradStart: UIColor = UIColor.init(red: 255/255.0, green: 255/255.0, blue: 150/255.0, alpha: 1.0)
    let availGradReduced: UIColor = UIColor.init(red: 255/255.0, green: 255/255.0, blue: 110/255.0, alpha: 1.0)
    let durDefaultColor: UIColor = UIColor.init(red: 150.0/255.0, green: 150.0/255.0, blue: 150.0/255.0, alpha: 1.0)
    //let availDefaultColor: UIColor = UIColor.init(red: 200.0/255.0, green: 200.0/255.0, blue: 200.0/255.0, alpha: 1.0)
    let currentTentActColor: UIColor = UIColor(red: 190/255.0, green: 229/255.0, blue: 191/255.0, alpha: 1.0)
    let finishedColor: UIColor         = UIColor.init(red: 100.0/255.0,  green: 100.0/255.0, blue: 100.0/255.0, alpha: 1.0)
    
    // if the new availability is < threshold*oldAvailability then we can say it is strongly restricted by this choice
    let strongRestrictThreshold: Double = 0.75
    
    var sortedLastConfirmedBars: [BarEntry]? = []
    
    private let barsDrawLayer: CALayer = CALayer()
    private let barsScrollView: UIScrollView = UIScrollView()
    private let scaleDrawLayer: CALayer = CALayer()
    private let scaleScrollView: UIScrollView = UIScrollView()
    private let namesDrawLayer: CALayer = CALayer()
    private let namesScrollView: UIScrollView = UIScrollView()
    
    
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
        
//        namesScrollView.backgroundColor = UIColor.clear
//        namesDrawLayer.backgroundColor = UIColor.clear.cgColor
        namesScrollView.layer.addSublayer(namesDrawLayer)
        self.addSubview(namesScrollView)
        namesScrollView.delegate = self
        
        drawClock(xPos_left: self.frame.size.width - 160.0, yPos_top: 5.0, time: 0)
    }
    
    // Force the x coordinates synchronized so that scaleScroll always lines up with barsScroll
    // Force the y coordinates synchronized so that namesScroll always lines up with barsScroll
    // Also force the x and y coordinates to move together and match % offsets
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        
        // lock the x coordinates of 2 scroll views
        if scrollView == scaleScrollView {
            barsScrollView.contentOffset.x = scaleScrollView.contentOffset.x
        }else if scrollView == barsScrollView {
            scaleScrollView.contentOffset.x = barsScrollView.contentOffset.x
        }
        
        // lock the y coordinates of 2 scroll views
        if scrollView == namesScrollView {
            barsScrollView.contentOffset.y = namesScrollView.contentOffset.y
        }else if scrollView == barsScrollView {
            namesScrollView.contentOffset.y = barsScrollView.contentOffset.y
        }
        
        // synchrinize vertical and horizontal scrolling
        let barsXPerc = barsScrollView.contentOffset.x / (barsScrollView.contentSize.width - barsScrollView.frame.width) // what percent x offset
        
        // do not begin automatic vertical offset scrolling unless the content exceeds the vertical screen size
        if (barsScrollView.contentSize.height > barsScrollView.frame.height) {
            barsScrollView.contentOffset.y = barsXPerc * abs(barsScrollView.contentSize.height - barsScrollView.frame.height)
        }
        
    }
    
    override func layoutSubviews() {
        
        barsScrollView.frame  = CGRect(x: namesWidth, y: 0, width: self.frame.size.width - namesWidth, height: self.frame.size.height - scaleSpace)
        
        scaleScrollView.frame = CGRect(x: namesWidth, y: self.frame.size.height - scaleSpace, width: self.frame.size.width - namesWidth, height: scaleSpace)
        
        namesScrollView.frame = CGRect(x: 0, y: 0, width: namesWidth, height: self.frame.size.height - scaleSpace)
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
            // remove old names
            namesDrawLayer.sublayers?.forEach({$0.removeFromSuperlayer()})
            
            if let dataEntries = dataEntries {
                barsScrollView.contentSize = CGSize(width: self.frame.size.width*barsWidthScaleFactor, height: (barHeight + barSpace)*CGFloat(dataEntries.count) + barSpace)
                scaleScrollView.contentSize = CGSize(width: barsScrollView.contentSize.width, height: scaleSpace)
                namesScrollView.contentSize = CGSize(width: namesWidth, height: barsScrollView.contentSize.height)
                barsDrawLayer.frame = CGRect(x: 0, y: 0, width: barsScrollView.contentSize.width, height: barsScrollView.contentSize.height)
                
                drawVertLine(x: leftSpace,                                               color: UIColor.gray,   lineType: "solid")
                drawVertLine(x: leftSpace + minsToX(mins: Int(minutesDisplayedInPlot)),  color: UIColor.gray,   lineType: "solid")
                drawVertLine(x: minsToX(mins: Int(leftSpace) + currTime),                color: UIColor.red,   lineType: "solid")
                
                drawScale()
                
                
                // sort the entries based on their ID (which was sorted in the server based on act time)
                var tempDataEntries = dataEntries.sorted(by: { $0.EST < $1.EST })
                if tentGantt { sortedLastConfirmedBars = lastConfirmedBars!.sorted(by: { $0.ID < $1.ID }) }
                
                // display the sorted items so that the earlier activities are at the top
                var i = -1
                while i < tempDataEntries.count-1 {
                    i+=1
                    // if max duration is zero, this is an optinal acitvity that cannot be performed, so skip it:
                    /*if (tempDataEntries[i].maxDuration == 0) {
                        tempDataEntries.remove(at: i)
                        sortedLastConfirmedBars!.remove(at: i)
                        i -= 1
                        continue
                    }*/
                    
                    if (tentGantt && sortedLastConfirmedBars![i].minDuration == 0) {
                        sortedLastConfirmedBars![i].minDuration = sortedLastConfirmedBars![i].maxDuration
                    }
                    
                    showEntry(index: i, entry: tempDataEntries[i], currentTime: currTime)
                }
            }
            
            // Automatically adjust scroll so that the left side of the screen is a few hours behind current time
            barsScrollView.contentOffset.x  = minsToX(mins: self.currTime ) - minsToX(mins: 180 )
            scaleScrollView.contentOffset.x = minsToX(mins: self.currTime ) - minsToX(mins: 180 )
            
            // update the user displayed clock time
            updateClock(self.currTime)
        }
    }
    
    
    // Take a BarEntry object and create and display a visual bar representing it
    private func showEntry(index: Int, entry: BarEntry, currentTime: Int) {
        var entry = entry
        
        // Starting x postion of the bar
        let xPos: CGFloat = leftSpace + minsToX(mins: entry.EST)
        
        // Starting y postion of the bar
        let yPos: CGFloat = barSpace + CGFloat(index) * (barHeight + barSpace)
        
        // barLength is actual left-to-right length of bar in pixels
        var barLength: CGFloat
        
        // color that will appear behind the activity names
        var nameColor: UIColor = UIColor.clear
        
        
        // if this activity is already completed
        if (entry.LET <= currentTime) {
            // draw activity participation time
            barLength = minsToX ( mins: entry.LET - entry.EST )
            // if this activity was not skipped, draw its bar
            //if (entry.maxDuration != 0) {
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: finishedColor, type: "default")
            //}
            if tentGantt { nameColor = UIColor(red: 238.0/255, green: 238.0/255, blue: 238/255.0, alpha: 1.0) }
            else { nameColor = UIColor.clear}
        }
        // if this activity is not yet completed, draw duration & availibility
        else {
            
            // if this activity is optional (min duration = 0), display using the max duration
            if (entry.minDuration == 0) {
                entry.minDuration = entry.maxDuration
            }
            
            
            // if this is a tentative action, show old availability/duration with hollow bars
            if tentGantt {
                let oldEntry = sortedLastConfirmedBars![index]
                
                // if this activity has zero duration, do not show the bar. But do color the name accordingly
                /*if (entry.maxDuration == 0) {
                    if oldEntry.maxDuration != 0 { nameColor = UIColor.red }
                    else { nameColor = UIColor.clear }
                }*/
                
                // draw availability
                // if availability has shrunk, color it darker
                let avail: Int
                let oldAvail: Int
                if entry.maxDuration == 0 { avail = 0; oldAvail = 0; }
                else { avail = entry.LET - entry.EST; oldAvail = oldEntry.LET - oldEntry.EST; }
                barLength = minsToX ( mins: avail )
                
//                var startRed: CGFloat = 0
//                var startGreen: CGFloat = 0
//                var startBlue: CGFloat = 0
//                var startAlpha: CGFloat = 0
//                availGradStart.getRed(&startRed, green: &startGreen, blue: &startBlue, alpha: &startAlpha)
//
//                var endRed: CGFloat = 0
//                var endGreen: CGFloat = 0
//                var endBlue: CGFloat = 0
//                var endAlpha: CGFloat = 0
//                availGradEnd.getRed(&endRed, green: &endGreen, blue: &endBlue, alpha: &endAlpha)
                
                let barColor: UIColor
                
                let restrictRatio: CGFloat = CGFloat(avail) / CGFloat(oldAvail) // ratio (0 < r <= 1) gets smaller as new avail shrinks
                var colorChangeRatio: CGFloat = 0.0
                if restrictRatio == 1.0 {
                    barColor = availDefault
                } else {
//                    colorChangeRatio = min(1.0,1-restrictRatio*2)
//                    let red = startRed + (startRed - endRed) * colorChangeRatio
//                    let blue = startBlue + (startBlue - endBlue) * colorChangeRatio
//                    let green = startGreen + (startGreen - endGreen) * colorChangeRatio
                    barColor = availGradReduced // UIColor(red: red, green: green, blue: blue, alpha: 1.0)
                } // gets larger as avail shrinks
//                let greenRangeToMax: CGFloat = 255-(startGreen*255) // how much can the green color chang before max green level (255)
//                green = startGreen + ((1-restrictRatio) * greenRangeToMax)/255.0 // increase green level up to max of 255
//                startGreen = startGreen - (80 * (1-restrictRatio))/255
//                startBlue = startBlue - (80 * (1-restrictRatio))/255
//                let red = startRed + (endRed - startRed)*colorChangeRatio
//                let green = startGreen + (endGreen-startGreen)*colorChangeRatio
//                let blue = startBlue + (endBlue-startBlue)*colorChangeRatio
//                let red = startRed + (startRed - endRed) * colorChangeRatio
//                let blue = startBlue + (startBlue - endBlue) * colorChangeRatio
//                let green = startGreen + (startGreen - endGreen) * colorChangeRatio
                
                
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: barColor, type: "default")
                
                // draw min duration
                // if the duration has changed, paint the bar red
                barLength = minsToX ( mins: entry.minDuration )
                var durColor: UIColor
                if (entry.isTentAct) { durColor = currentTentActColor
                } else if (entry.minDuration == oldEntry.minDuration || entry.EST == currentTime) { durColor = durDefaultColor
                } else { durColor = UIColor.darkGray }
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: durColor, type: "default")
                
                
                // Starting x postion of the old bar
                let oldxPos: CGFloat = leftSpace + minsToX(mins: oldEntry.EST)
                
                // draw old availability
                barLength = minsToX ( mins: oldAvail )
                drawBar(xPos_left: oldxPos, yPos_top: yPos, barLength: barLength, color: UIColor.darkGray, type: "hollow")
                
                // draw old min duration
                barLength = minsToX ( mins: oldEntry.minDuration )
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: UIColor.darkGray, type: "hollow")
            
                // adjust the coloring behind the name to match the bar coloring level
                if (entry.isTentAct) { // if this is the tentative activity being investigated
                    nameColor = currentTentActColor
                } else if (avail == oldAvail && entry.minDuration == oldEntry.minDuration) {
                    nameColor = UIColor(red: 238.0/255, green: 238.0/255, blue: 238/255.0, alpha: 1.0) //UIColor.clear
                } else {
                    nameColor = availGradReduced //UIColor(red: red, green: green, blue: blue, alpha: 1.0)
                }
            
            } // else if not a tentative gantt, do not color things relative to previous entries
            else {
                
                // draw availability w/ outline
                // if availability has shrunk, paint it red
                var avail: Int
                if entry.maxDuration == 0 { avail = 0 }
                else { avail = entry.LET - entry.EST }
                barLength = minsToX ( mins: avail )
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: availDefault, type: "default")
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: UIColor.darkGray, type: "hollow")
                
                // draw min duration w/ outline
                // if the duration has changed, paint the bar red
                barLength = minsToX ( mins: entry.minDuration )
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: durDefaultColor, type: "default")
                drawBar(xPos_left: xPos, yPos_top: yPos, barLength: barLength, color: UIColor.darkGray, type: "hollow")
                

                
                nameColor = UIColor.clear
            }
            
        }
        
        
        // Draw label on left for bar
        drawActName(yPos_top: yPos + barHeight/4, xPos: leftSpace + minsToX(mins: entry.EST), textValue: entry.activityName, backColor: nameColor)
        
        // Draw title to the left of the bar
//        drawBarTextTitle(xPos_left: 10, yPos_top: yPos + barHeight*3/10, title: entry.title, color: entry.color)
    }
    
    
    // calculate frame shape and color of bar
    // and add bar to barsDrawLayer (CALayer)
    // xPos: starting x position of bar (left)
    // yPos: starting y position of bar (top)
    // barLength: length of bar in pixels
    // color: UIColor of bar
    private func drawBar(xPos_left: CGFloat, yPos_top: CGFloat, barLength: CGFloat, color: UIColor, type: String) {
        
        // if the duration is 0, then this is an optinal activity that was not performed. Keep the chart row, but dont show a bar
        if barLength == 0 { return; }
        
        let barLength = max(barLength, 0)
        
        let xPos_left = xPos_left
        
        let path: UIBezierPath = UIBezierPath()
        
        let cornerRadius = CGFloat(5)
        
        
        // draw side lines and corner curves
        path.move(to: CGPoint(x: xPos_left + cornerRadius, y: yPos_top))

        path.addLine(to: CGPoint(x: xPos_left + barLength - cornerRadius,  y: yPos_top))

        path.addCurve(to: CGPoint(x: xPos_left + barLength,  y: yPos_top + cornerRadius),
                      controlPoint1: CGPoint(x: xPos_left + barLength, y: yPos_top),
                      controlPoint2: CGPoint(x: xPos_left + barLength, y: yPos_top))

        path.addLine(to: CGPoint(x: xPos_left + barLength,  y: yPos_top + barHeight - cornerRadius))

        path.addCurve(to: CGPoint(x: xPos_left + barLength - cornerRadius,  y: yPos_top + barHeight),
              controlPoint1: CGPoint(x: xPos_left + barLength, y: yPos_top + barHeight),
              controlPoint2: CGPoint(x: xPos_left + barLength, y: yPos_top + barHeight))

        path.addLine(to: CGPoint(x: xPos_left + cornerRadius,  y: yPos_top + barHeight))

        path.addCurve(to: CGPoint(x: xPos_left,  y: yPos_top + barHeight - cornerRadius),
              controlPoint1: CGPoint(x: xPos_left, y: yPos_top + barHeight),
              controlPoint2: CGPoint(x: xPos_left, y: yPos_top + barHeight))

        path.addLine(to: CGPoint(x: xPos_left, y: yPos_top + cornerRadius))

        path.addCurve(to: CGPoint(x: xPos_left + cornerRadius,  y: yPos_top),
                      controlPoint1: CGPoint(x: xPos_left, y: yPos_top),
                      controlPoint2: CGPoint(x: xPos_left, y: yPos_top))
        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 0.5
        pathLayer.strokeColor = color.cgColor
        
        if type == "hollow" {
            pathLayer.fillColor = UIColor.clear.cgColor
        } else {
            pathLayer.fillColor = color.cgColor
        }
        
        barsDrawLayer.addSublayer(pathLayer)
        
    }
    
    
    // Draw the text on left for a bar in the chart
    private func drawActName(yPos_top: CGFloat, xPos: CGFloat, textValue: String, backColor: UIColor) {
        let w = namesWidth - 10.0
        drawNameRect(topY: yPos_top - barSpace, color: backColor)
        let textLayer = CATextLayer()
//        textLayer.frame = CGRect(x: xPos - textBoxWidth - 10, y: yPos_top, width: textBoxWidth, height: barHeight)
        textLayer.frame = CGRect(x: 0, y: yPos_top, width: w, height: barHeight)
        textLayer.foregroundColor = UIColor.black.cgColor
        textLayer.backgroundColor = UIColor.clear.cgColor
        textLayer.alignmentMode = kCAAlignmentRight
        textLayer.contentsScale = UIScreen.main.scale
        textLayer.font = CTFontCreateWithName(UIFont.systemFont(ofSize: 0).fontName as CFString, 0, nil)
        textLayer.fontSize = 14
        textLayer.string = textValue
        namesDrawLayer.addSublayer(textLayer)
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
        drawHorzLine(y: barsScrollView.frame.height, color: UIColor.black, lineType: "solid" )
        // mark each hour in the day
        for i in 1..<Int(minutesDisplayedInPlot / 60) {
            drawVertLine(x: minsToX(mins: i*60) + leftSpace, color: UIColor.gray, lineType: "dashed")
            drawScaleLabel(mins: i*60)
        }
        
        // draw line that will show on the right side of the names
        let namesPath = UIBezierPath()
        namesPath.move(to: CGPoint(x: namesWidth-1, y: 0.0))
        namesPath.addLine(to: CGPoint( x: namesWidth-1, y: namesScrollView.frame.height ))
        
        let lineLayerNames = CAShapeLayer()
        lineLayerNames.path = namesPath.cgPath
        lineLayerNames.lineWidth = 0.5
        lineLayerNames.strokeColor = UIColor.black.cgColor
        namesDrawLayer.insertSublayer(lineLayerNames, at: 0)
    }
    
    // draw a horizontal line across the chart
    // will be at height yPos and will go all the way across the chart
    private func drawHorzLine(y: CGFloat, color: UIColor, lineType: String) {
        
        let xPos = namesWidth
        let yPos = y
        
        let path = UIBezierPath()
        path.move(to: CGPoint(x: xPos, y: yPos))
        path.addLine(to: CGPoint(x: self.layer.frame.size.width, y: yPos))
        
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
    
    
    // Draw a rectangle box behind activity names
    func drawNameRect(topY: CGFloat, color: UIColor) {
        let width = namesWidth
        let height = barHeight + barSpace + 1
        
        let path: UIBezierPath = UIBezierPath()
        
        path.move(to: CGPoint(x: 0.0-1, y: topY))
        
        path.addLine(to: CGPoint(x: width-1,  y: topY))
        
        path.addLine(to: CGPoint(x: width-1,  y: topY + height))
        
        path.addLine(to: CGPoint(x: 0.0-1,  y: topY + height))
        
        path.close()
        
        // make shape from lines and fill rectangle
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 1
        pathLayer.fillColor = color.cgColor
        pathLayer.strokeColor = UIColor.clear.cgColor
        
        namesDrawLayer.addSublayer(pathLayer)
    }
    
    
    // draw a clock display as a box with a simple text display of time inside
    private func drawClock(xPos_left: CGFloat, yPos_top: CGFloat, time: Int) {
        
        let boxWidth: CGFloat = 140
        let boxHeight: CGFloat = 50
        let xPos_left = xPos_left
        let cornerRadius = CGFloat(5)
        
        let path: UIBezierPath = UIBezierPath()
        
        // draw side lines and corner curves
        path.move(to: CGPoint(x: xPos_left + cornerRadius, y: yPos_top))
        
        path.addLine(to: CGPoint(x: xPos_left + boxWidth - cornerRadius,  y: yPos_top))
        
        path.addCurve(to: CGPoint(x: xPos_left + boxWidth,  y: yPos_top + cornerRadius),
                      controlPoint1: CGPoint(x: xPos_left + boxWidth, y: yPos_top),
                      controlPoint2: CGPoint(x: xPos_left + boxWidth, y: yPos_top))
        
        path.addLine(to: CGPoint(x: xPos_left + boxWidth,  y: yPos_top + boxHeight - cornerRadius))
        
        path.addCurve(to: CGPoint(x: xPos_left + boxWidth - cornerRadius,  y: yPos_top + boxHeight),
                      controlPoint1: CGPoint(x: xPos_left + boxWidth, y: yPos_top + boxHeight),
                      controlPoint2: CGPoint(x: xPos_left + boxWidth, y: yPos_top + boxHeight))
        
        path.addLine(to: CGPoint(x: xPos_left + cornerRadius,  y: yPos_top + boxHeight))
        
        path.addCurve(to: CGPoint(x: xPos_left,  y: yPos_top + boxHeight - cornerRadius),
                      controlPoint1: CGPoint(x: xPos_left, y: yPos_top + boxHeight),
                      controlPoint2: CGPoint(x: xPos_left, y: yPos_top + boxHeight))
        
        path.addLine(to: CGPoint(x: xPos_left, y: yPos_top + cornerRadius))
        
        path.addCurve(to: CGPoint(x: xPos_left + cornerRadius,  y: yPos_top),
                      controlPoint1: CGPoint(x: xPos_left, y: yPos_top),
                      controlPoint2: CGPoint(x: xPos_left, y: yPos_top))
        
        path.close()
        
        let pathLayer = CAShapeLayer()
        pathLayer.path = path.cgPath
        pathLayer.lineWidth = 1
        pathLayer.fillColor = UIColor.white.cgColor
        pathLayer.strokeColor = UIColor.black.cgColor
        
        self.layer.addSublayer(pathLayer)
        
        
        let clockHeadTextHeight: CGFloat = 16
        let clockHeadTextWidth: CGFloat = boxWidth - 20
        
        let headerLabel = UILabel(frame: CGRect(x: 0, y: 0, width: clockHeadTextWidth, height: clockHeadTextHeight))
        headerLabel.center = CGPoint(x: xPos_left + boxWidth/2.0, y: yPos_top + clockHeadTextHeight/2.0 + 5.0)
        headerLabel.textAlignment = .center
        headerLabel.text = "Current Time"
        headerLabel.font = UIFont(name: "ArialHebrew-Light", size: 14.0)!
        self.addSubview(headerLabel)
        
        let clockTimeTextHeight: CGFloat = 25
        let clockTimeTextWidth: CGFloat = boxWidth - 20
        
        timeLabel = UILabel(frame: CGRect(x: 0, y: 0, width: clockTimeTextWidth, height: clockTimeTextHeight+clockHeadTextHeight+5))
        timeLabel.center = CGPoint(x: xPos_left + boxWidth/2.0, y: yPos_top + clockHeadTextHeight + 5 + clockTimeTextHeight/2.0)
        timeLabel.textAlignment = .center
        timeLabel.text = minsToStrTime(time)
        timeLabel.font = UIFont(name: "ArialRoundedMTBold", size: 20.0)!
        self.addSubview(timeLabel)
    }
    
    // update the displayed clock time
    // input: time in minutes since start of day
    private func updateClock(_ newTime: Int) {
        timeLabel.text = minsToStrTime(newTime)
        
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
    
    // Convert from minutes to "hh:mm pm/am" format
    private func minsToStrTime(_ mins: Int) -> String {
        var dayHalf = "AM"
        var adjMins = mins
        if (adjMins > 780) { // if it is the afternoon
            adjMins = mins - 780
            dayHalf = "PM"
        }
        let strHours = String(adjMins / 60) // interger division
        let strMins = String(format: "%02d", adjMins % 60)
        return strHours + ":" + strMins + " " + dayHalf
    }
    
    // Convert from minutes to the X pixel coordinate from left side of plot
    private func minsToX(mins: Int) -> CGFloat {
        // % of day  *  width of plot in pixels
        return CGFloat( CGFloat(mins) / minutesDisplayedInPlot  * (barsScrollView.contentSize.width - leftSpace - rightSpace) )
    }
    
    
    // The struct containing all the info represented by a visual bar
    struct BarEntry {
        var color: UIColor
        
        var ID: Int
        
        var isTentAct: Bool
        
        var EST: Int
        
        var LET: Int
        
        var minDuration: Int
        
        var maxDuration: Int
        
        var restrict: Double
        
        /// To be shown on top of the bar
        var activityName: String
        
        /// To be shown at the bottom of the bar
        var title: String
    }

}
