//
//  DelActTableViewController.swift
//  Schedule_App
//
//  Created by Drew Davis on 11/1/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class DelActTableController: UITableViewController {
    
    var allDeletableActivities : [String] = []
    var actSelectionChanged = false
    var currentCellRow = -1
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    
    // this function sets the number of sections
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    // this function sets the number of rows in each section
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return allDeletableActivities.count
    }
    
    // this function sets the section names
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 {
            return "Activity to Delete"
        } else {
            return ""
        }
    }
    
    // this function populates the cell at indexPath with the cell returned
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "DelActCell", for: indexPath)
        
        if allDeletableActivities.count > indexPath.row {
            cell.textLabel?.text = allDeletableActivities[indexPath.row]
        }
        
        return cell
        
    }
    
    // This function called when user clicks an item in the tableview
    override public func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        
        // if this is the row already selected, deselect it
        if indexPath.row == currentCellRow {
            tableView.deselectRow(at: indexPath, animated: true)
            return nil
        }
        
        return indexPath
    }
    
    // This function is called after a cell has been selected
    override public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        currentCellRow = indexPath.row
    }
    
}
