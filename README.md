# AdaptSchedule
This system was designed to assist adolescents, particularly adolescents with disabilities, transition towards independent management of their own schedules. It allows them to set up a daily schedule with all activities that must be performed and any constraints between those activities. The system then offers the user the ability to dynamically select which activity they want to perform at the time of selection. If a user selects from the choices offered throughout the day, they are guaranteed to complete all necessary activities during a day while still having the freedom to choose the order of their activities, and how/when to spend their free time.

When the client scheduling interface is run on an iPad, the user is presented with not only with a listing of activities that could be done next, and with the ability to alter the schedule by adding, modifying, and/or removing schedules, but also with a visual timeline of the windows in which activities can be done.  This allows the user to see the consequences of different tentative choices of activities to perform next.  The schedule server can serve multiple (current implementation is hardwired for up to 2) clients, whose schedules can be connected (e.g., a parent and adolescent might need to commute to school/work together, or eat dinner together), and as one person makes choices that constrain such joint activities, those constraints are automatically propagated to the other person's schedule.

## Status of software
- The intent of this repository is to make available the source code for a prototype distributed schedule managment tool. This is not intended to represent a finished product for potential immediate use.  It is intended to provide developers of commercial products with the software realization of the tool that has been developed, to inform incorporation of ideas from this prototype into commercial products.

## Outline of the set up
- The main engine of the scheduling system, the server, runs in Java, in Eclipse.  It uses the Yices SMT solver, so the version of Yices it calls depends on the system (Apple vs Windows).
- Besides a built-in interface that simulates a distributed system on the server, the distributed interface has been developed to run on iPads.  The client software runs on an iPad, communicating with the server through the internet.  The client largely does rendering, with the scheduling reasoning done on the server.
- As currently implemented, the server can support up to 2 clients, whose schedules can interact.


## How to run server
- Requires Java 1.8 JRE
- In Eclipse, run the AppServerGUI.java file as a java application
- Immediately after starting, follow the console prompts to select which example scheduling problem you want to execute.  (New scheduling problems can be created by following the XML format.)

## How to run client
- First, start running the server and select which problem to run on the server
- If the app was open previously, reset it by fully closing the app. This is done by double-clicking the home button on the iPad and then swiping the app up off the screen
- On iPad home screen, open the app label "Day Plan"
- Enter the IP address of the computer running the server
  - If using a commercial network (like U of M’s), enter the public IP address of the server system. This IP can be obtained by googling “what is my IP address”
  - If on a private network (like your home), enter the private IP address of the server system. This IP address can be found in the network settings of the computer
- Select which agent you want this iPad to act as
  - An iPad can switch agents by resetting/closing the iPad and re-entering the IP
  - If using a single-agent problem, select agent 0


## How to install app on iPad from TestFlight
- If the schedule app has been deleted, updated, or just needs to be reinstalled, it can be updated/installed through the Apple testflight app, assuming the developer has a testflight account.
- Steps
  - Open TestFlight app on iPad
  - Select the app on the left menu
  - Click install on the details pane shown on the right 2/3s of the screen

## How to push to TestFlight from codebase
- If a new version of the app needs to be distributed onto TestFlight, it must be distributed using xCode on a Mac computer.
- Steps
  - Open the client project in Xcode (client folder in repo)
  - Sign into Xcode -> Preferences -> Accounts with developer account
  - Select the top-level project object in the navigation pane on the left side of the main Xcode window. Increment either the version number or the build number (the combo must be unique in each upload)
  - Set the ‘Active Scheme’ to be ‘Generic iOS Device’ in the top left of the main Xcode window
  - ‘Archive’ the project with Product -> Archive
  - When archive is finished open the Organizer window with Window -> Organizer (it may open automatically after archiving)
  - Select the Archive that you just created and click ‘Distribute App’ on the right side
  - Proceed through the option slides leaving all values at default
  - After the upload is complete, the project will be automatically process in Apple servers, which will take some time. Wait for an email on the iPads from Apple saying the distribution succeeded/failed. The email usually arrives within 5/10 minutes.

## How to install fresh instance of the server
- A zip file containing all versions of Yices (https://yices.csl.sri.com/) is in the git repository.  Depending on what OS you are running, you will need to modify which version of yices is being accessed in `server/src/dtp/SimpleDTP.java`.
- Steps
  - Pull latest changes from online github repo
    - (Mac / Linux) In the terminal run git pull origin master
    - (Windows) Use a git shell to run git pull origin master
    - The Eclipse project that contains the server will have automatically lost its connections to the JAR files. This can be fixed by opening the project in Eclipse, right clicking the top-level project folder in the Eclipse Package Explorer, and clicking refresh.
    - If the project still has errors, manually check the Java Build Path in 2 places:
      - In File -> Preferences
      - By right clicking the top-level project folder in the Eclipse Package Explorer -> Properties
    - Run the AppServerGUI.java file in the utils subdirectory

## How to update server from repo codebase
- If changes have been made to the server and pushed to the repo, you can update your server instance without redownloading or transfering the entire system
- Steps
  - Navigate to the top level of that directory ( adaptSchedule/ )
  - Pull latest changes from online github repo
    - (Mac / Linux) In the terminal run git pull origin master
    - (Windows) Use a git shell to run git pull origin master

## Acknowledgements
- The software in this repository is the result of the research efforts of a series of graduate students at the University of Michigan. The initial codebase for multiagent temporal reasoning was developed by James Boerkoel, Jr. Jason Sleight used that codebase to create the initial schedule management tool.  Lynn Garrett extended this tool particularly in ways to support dynamic adaptation of a schedule.  Drew Bennett split the software base into the client and server components, and developed the user interface in part informed by an earlier design by Jorday MacKay.  The principal investigators of the project were Professors Edmund Durfee and Abigail Johnson.
