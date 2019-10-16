Custom Vector Tool
===================================
Helper project for Auto-Auth-Server which allows for admin creation and utilization of specialized models for portals. See https://github.com/OkJaybird/auto-auth-server for more info.

Setup
===================================
To start creating vectors used for specialized models, enter valid MySQL login information in the database.properties file. The account used to connect must have table creation rights. 

The file portal.conf must also be altered to contain one portal object definition as explained in the Auto-Auth-Server README. This is the portal to collect data for.

Use
===================================
Note that Custom Vector Tool utilizes Java's Console, which typically has problems running from within Ecilpse. Running the program on the command line instead of using the Ecilpse terminal is perferred. 

On startup, Custom Vector Tool will ask for the credentials to use for attempted login. Using this username / password combo, the portal defined in portal.conf will be logged into and some feedback is provided to the user: 

1. The raw features scraped from the page will be printed out
2. An HTML snapshot will be taken and opened in the system's default browser. This will not look exactly like the real response page and can often be quite jumbled. Nevertheless, there are often still visual indicators of login success or failure.

Custom Vector Tool will then ask if it should save the vector as successful authentication being True, False, or don't save it at all.

It's best to generate as many distinct vector samples as possible. However, you must have collected at least one case of successful authentication and one case of unsuccessful authentication in order for valid use by Auto-Auth-Server.

