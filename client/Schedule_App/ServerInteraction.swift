//
//  ServerInteraction.swift
//  Schedule_App
//
//  Created by Drew Davis on 5/22/18.
//  Copyright © 2018 Drew. All rights reserved.
//

import Foundation
import UIKit


class client {
    
    var requestPOST: URLRequest
    var requestGET: URLRequest
    
    // for now, use a random int as the client ID (to prevent randos from internet from accessing)
    let ID = String( arc4random_uniform(_:10000000) )
    var urlString: String
    let url: URL?
    
    var JSON_encoder: JSONEncoder
    
    // This object has the structure containing all info pieces that will be recieved on each reply from the server
    var currentInfo: fromServer = fromServer()
    
    var lastInfoType: String = ""
    
    var mostRecentGanttImg: UIImage? = nil


    /*
     * Constructor
    */
    init(_ servIP: String, _ agentNum: String) {
        do {
            
            JSON_encoder = JSONEncoder()
            JSON_encoder.outputFormatting = .prettyPrinted
            
            // URL to access server at, appended by client ID to validate access
            // hardcode in port 8080
            urlString = "http://" + servIP + ":8080/" + agentNum
            url = URL(string: urlString)
            
            print("\n\n\n\nCONNECTING TO " + urlString + "\n\n\n\n")
            
            requestPOST = URLRequest(url: url!)
            requestPOST.httpMethod = "POST"
            requestGET = URLRequest(url: url!)
            requestGET.httpMethod = "GET"
            
            // tell server you are starting a new client session
            var aPut = putCMD()
            aPut.infoType = "startup"
            aPut.clientID = ID
            aPut.agentNum = agentNum
            let someData = try JSON_encoder.encode( aPut )
            requestPOST.httpBody = someData
            makeAReq(req: requestPOST)
            
        } catch {
            print("Error: client init() failed")
        }
    }
    
    
    /*
     * Destructor
    */
    deinit {
    }
    
    
    /*
     * Send a JSON file to the server using internal methods
     * This should be called by GUI items upon user interaction
     * DEPRECATED: Not in use
    */
    func sendStructToServer(_ thePut: putCMD) {
        do {
            let json = try JSON_encoder.encode(thePut)
            
            var aReq: URLRequest
            aReq = URLRequest(url: self.url!)
            aReq.httpMethod = "POST"
            aReq.httpBody = json
            
            makeAReq(req: aReq)
            
            print("\nJSON sent to server:")
            print(thePut)
        } catch let error as NSError {
            print(error)
        }
    }
    
    
    /*
     * Make a Get or Put request to the server and get the response from the server
     * Server expected response JSON format defined by fromServer struct
     * This response format should match the JSON format seen in the server code
     */
    func makeAReq(req: URLRequest) {
        // make the specific request and then handle the reply (data, response, error)
        URLSession.shared.dataTask(with: req) { (data, response, error) in
            if error != nil {
                print("non-nil communication error:")
                print(error!.localizedDescription)
            }
            
            // get the data from inside the reply
            guard let data = data else { return }
            dataProcess: do {
                
                let servData = try JSONDecoder().decode(fromServer.self, from: data)

                // if the reply info type is not blank, parse the data into local object
                if (servData.infoType != "") {
                    print("\nJSON received from server:")
                    self.currentInfo.infoType = servData.infoType!;                     print("  infoType: ", terminator:"");          print(servData.infoType!)
                    self.currentInfo.startTime = servData.startTime!;                   print("  startTime: ", terminator:"");         print(servData.startTime!)
                    self.currentInfo.clearToConfirm = servData.clearToConfirm!;         print("  clearToConfirm: ", terminator:"");    print(servData.clearToConfirm!)
                    self.currentInfo.nextActivities = servData.nextActivities!;         print("  nextActivities: ", terminator:"");    print(servData.nextActivities!)
                    self.currentInfo.nextActsMinDur = servData.nextActsMinDur!;         print("  nextActsMinDur: ", terminator:"");    print(servData.nextActsMinDur!)
                    self.currentInfo.nextActsMaxDur = servData.nextActsMaxDur!;         print("  nextActsMaxDur: ", terminator:"");    print(servData.nextActsMaxDur!)
                    self.currentInfo.actNames =  servData.actNames!;           print("  actNames: ", terminator:"");     print(servData.actNames!)
                    self.currentInfo.actIDs =  servData.actIDs!;           print("  actIDs: ", terminator:"");     print(servData.actIDs!)
                    self.currentInfo.actMinDurs =     servData.actMinDurs!;              print("  actMinDurs: ", terminator:"");        print(servData.actMinDurs!)
                    self.currentInfo.actMaxDurs =     servData.actMaxDurs!;              print("  actMaxDurs: ", terminator:"");        print(servData.actMaxDurs!)
                    self.currentInfo.actESTs =   servData.actESTs!;            print("  actESTs: ", terminator:"");      print(servData.actESTs!)
                    self.currentInfo.actLETs =     servData.actLETs!;              print("  actLETs: ", terminator:"");        print(servData.actLETs!)
                    self.currentInfo.actRestricts =     servData.actRestricts!;              print("  actRestricts: ", terminator:"");        print(servData.actRestricts!)
                    self.currentInfo.otherWeakRestrictCount =   servData.otherWeakRestrictCount!;    print("  otherWeakRestrictCount: ", terminator:"");          print(servData.otherWeakRestrictCount!)
                    self.currentInfo.otherStrongRestrictCount = servData.otherStrongRestrictCount!;  print("  otherStrongRestrictCount: ", terminator:"");        print(servData.otherStrongRestrictCount!)
                    self.currentInfo.currentTime =     servData.currentTime!;              print("  currentTime: ", terminator:"");        print(servData.currentTime!)
                    self.currentInfo.strImg = servData.strImg!;                         print("  strImg length: ", terminator:"");     print(servData.strImg!.count)
                    self.currentInfo.actDetails = servData.actDetails;                  print("  actDetails: ", terminator:"");        print(servData.actDetails!)
                    self.currentInfo.debugInfo = servData.debugInfo!;                   print("  debugInfo: ", terminator:"");         print("~suppressed for readability~")//print(servData.debugInfo!)
                    
                    
                    self.lastInfoType = servData.infoType!
                }
                
            } catch let error as NSError {
                print("\nmakeAReq error:")
                print(error)
            }
            
            }.resume()
    }

    
    /*
     * Every 200 ms poll the server for any new requests using GET requests
     * This allows the server to push requests and essentially breaks the restful paradigm
     */
    func heartbeat() {
        while(true) {
            self.makeAReq(req: self.requestGET)
            usleep(200000) // microseconds
            if self.currentInfo.infoType == "startup" {return;}
        }
    }
    
    
    enum MyError: Error {
        case runtimeError(String)
    }
    
}



/*
 * Structure of JSON format sent to the server
 * Should be the same format as the JSONs passed into sendJSONtoServer()
 */
struct putCMD: Codable {
    var clientID = ""
    var infoType = ""  // options: startup / ganttRequest / confirmActivity / addActivity / removeActivity / editActivty
    var agentNum = ""
    var activityName = ""
    var activityDuration = ""
    var actDetails = activityDefinition()
    var debugInfo = [""]
}

// JSON received from server
// These struct variables need to match the names seen in the JSON object
// This structure format should match the reply format on the server side
struct fromServer: Codable {
    // do not make these optional, because we want to throw error if some of the data is missing
    // (unecesary components should be included as blank strings / empty vectors)
    var infoType         : String?
    var startTime        : String?
    var clearToConfirm   : String?
    var nextActivities   : [String]?
    var nextActsMinDur   : [String]?
    var nextActsMaxDur   : [String]?
    var actNames    : [String]?
    var actIDs    : [String]?
    var actESTs       : [String]?
    var actLETs       : [String]?
    var actMinDurs     : [String]?
    var actMaxDurs       : [String]?
    var actRestricts       : [String]?
    var otherWeakRestrictCount  : String?
    var otherStrongRestrictCount  : String?
    var currentTime       : String?
    var strImg           : String?
    var actDetails  : activityDefinition?
    var debugInfo        : [String]?
    
    init() {
        infoType       = ""
        startTime      = ""
        clearToConfirm = ""
        nextActivities = []
        nextActsMinDur = []
        nextActsMaxDur = []
        actNames  = []
        actIDs      = []
        actESTs     = []
        actLETs     = []
        actMinDurs   = []
        actMaxDurs     = []
        actRestricts = []
        otherWeakRestrictCount = ""
        otherStrongRestrictCount = ""
        currentTime = ""
        strImg         = ""
        actDetails     = activityDefinition()
        debugInfo      = []
    }
}


// Substructure of JSON sent/received to/from server
// These struct variables need to match the names seen in the JSON object
// This structure format should match the format on the server side
struct activityDefinition: Codable {
    // (unecesary components should be included as blank strings / empty vectors)
    var actName             : String?
    var modifiableDurActs   : [String]?
    var modifiableAvailActs : [String]?
    var minDurs             : [String]?
    var maxDurs             : [String]?
    var EST                 : String?
    var LST                 : String?
    var EET                 : String?
    var LET                 : String?
    var constraintTypes     : [String]?
    var constraintSource    : [String]?
    var constraintDest      : [String]?

    
    init() {
        actName             = ""
        minDurs             = []
        maxDurs             = []
        EST                 = ""
        LST                 = ""
        EET                 = ""
        LET                 = ""
        constraintTypes     = []
        constraintSource    = []
        constraintDest      = []
    }
}
