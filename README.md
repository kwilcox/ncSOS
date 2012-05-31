# This is an <h1> tag
## This is an <h2> tag
###### This is an <h6> tag

*This text will be italic*
_This will also be italic_
**This text will be bold**
__This will also be bold__
*You **can** combine them*

* Item 1
* Item 2
  * Item 2a
  * Item 2b
1. Item 1
2. Item 2
3. Item 3
   * Item 3a
   * Item 3b



*** We hold no responsibility for THREDDS issues/damage/lost data caused from following the instructions below***

--------------------------------------------------------------------------------------------------
Feature Type ncSOS

    Once thredds has been deployed on to the Apache Web Server (see below for 4.3 info)

   1. Extract ncSOS zip into a local directory
   2. Copy the ncSOS.jar into the "thredds/WEB-INF/lib" directory.
   3. Copy the "sos-servlet.xml" configuration file into the "thredds/WEB-INF/" directory.
   4. Add new servlet mappings to thredds/WEB-INF/web.xml with the following:

              <!-- sos services -->
              <servlet>
                <servlet-name>sos</servlet-name>
                <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
                <load-on-startup>1</load-on-startup>
              </servlet>

              <servlet-mapping>
                <servlet-name>sos</servlet-name>
                <url-pattern>/sos/*</url-pattern>
              </servlet-mapping>              

   5. Add the following service definition to enable SOS in your THREDDS catalog XML files:

              <service name="sos" serviceType="SOS" base="/thredds/sos/" />
      		
   6. Restart the web application server.

--------------------------------------------------------------------------------------------------
Updating THREDDS to 4.3

    1. Shutdown Apache Server if in use
    2. Backup webapps/thredds Directory outside of Apache Directory (precaution!)
    3. Start Apache Server
    4. Undeploy /thredds in tomcat manager
    5. Deploy new version of thredds using the "war to deploy section in the tomcat manager.
    6. Browse to webapps/thredds directory

    #. Once update is complete and successful remove backup webapps/thredds