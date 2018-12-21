//
//  ModActTableViewController.swift
//  Schedule_App
//
//  Created by Drew Davis on 7/24/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import UIKit

class ModActTableController: UITableViewController {
    
    var modifiableDurActs : [String]?
    var modifiableAvailActs : [String]?
    var allModifiableActivities : [String]?
    var actSelectionChanged = false

    override func viewDidLoad() {
        super.viewDidLoad()

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    
    // MARK: - Table view data source
    
    
    // this function sets the number of sections
    override func numberOfSections(in tableView: UITableView) -> Int {
        // #warning Incomplete implementation, return the number of sections
        return 1
    }
    

    // this function sets the number of rows in each section
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if (allModifiableActivities == nil) {return 1}
        return allModifiableActivities!.count
    }

    // this function populates the cell at indexPath with the cell returned
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "ModActCell", for: indexPath)
        
        if allModifiableActivities != nil {
            cell.textLabel?.text = allModifiableActivities![indexPath.row]
        }
        
        
        return cell
        
    }
    
    // This function called when user clicks an item in the tableview
    override public func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        actSelectionChanged = true
        
        return indexPath
    }


}
