
##How to run server
- Requires Java 1.8 JRE
- In Eclipse, run the AppServerGUI.java file as a java application
- Immediately after starting, follow the console prompts to select which example schedule you want to see

##How to run client
- First, start running the server and select which problem to run on the server
- If the app was open previously, reset it by fully closing the app. This is done by double-clicking the home button on the iPad and then swiping the app up off the screen
- On iPad home screen, click the app icon that looks like this: 
- Enter the IP address of the computer running the server
  - If using a commercial network (like U of M’s), enter the public IP address of the server system. This IP can be obtained by googling “what is my IP address”
  - If on a private network (like your home), enter the private IP address of the server system. This IP address can be found in the network settings of the computer
- Select which agent you want this iPad to act as
  - An iPad can switch agents by resetting/closing the iPad and re-entering the IP
  - If using a single-agent problem, select agent 0


##How to install app on iPad from TestFlight
- If the schedule app has been deleted, updated, or just needs to be reinstalled, it must be updated/installed through the testflight app. This app has already been installed on both iPads and has this icon: 
- Steps
  - Open TestFlight app on iPad
  - Select the app on the left menu
  - Click install on the details pane shown on the right 2/3s of the screen

##How to push to TestFlight from codebase
- If for any reason a new version of the app needs to be distributed onto TestFlight, it must be distributed using xCode on a Mac computer.
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

##How to install fresh instance of the server
- Due to the size of Yices and the java JAR files, the entire system cannot be stored on the git repo.
  - A zip file containing all versions of Yices and a zip file containing all necessary JAR files can be found in the directory containing this document.
  - There is also a zip file containing a full instance of the server and client system. This instance’s program files may be outdated, but can be updated by pulling from the git repo.
- Steps
  - Download the compressed file containing the entire system from the directory containing this document (adaptSchedule.zip)
  - Navigate to the top level of that directory ( adaptSchedule/ )
  - Pull latest changes from online github repo
    - (Mac / Linux) In the terminal run git pull origin master
    - (Windows) Use a git shell to run git pull origin master
    - The Eclipse project that contains the server will have automatically lost its connections to the JAR files. This can be fixed by opening the project in Eclipse, right clicking the top-level project folder in the Eclipse Package Explorer, and clicking refresh.
    - If the project still has errors, manually check the Java Build Path in 2 places:
      - In File -> Preferences
      - By right clicking the top-level project folder in the Eclipse Package Explorer -> Properties
    - Run the AppServerGUI.java file

##How to update server from repo codebase
- If changes have been made to the server and pushed to the repo, you can update your server instance without redownloading or transfering the entire system
- Steps
  - Navigate to the top level of that directory ( adaptSchedule/ )
  - Pull latest changes from online github repo
    - (Mac / Linux) In the terminal run git pull origin master
    - (Windows) Use a git shell to run git pull origin master
