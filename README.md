Sakai Gatling Stress Test
=========================

This is a Simple Stress Test for Sakai 11+ instances.

To test it out, simply execute the following command (by default it will run against the Sakai nightly server on https://trunk-mysql.nightly.sakaiproject.org):

    $mvn test

Each time you run this you'll get a result in `target/gatling/nightly-mysql/sakaisimulation-xxxxxxx` folder.

Stress Test Use Case
====================

This test is really simple, I hope we can add more complex cases soon.

Use two different type of users "random" and "exahustive".

The random users follow these steps:
- Go to the portal
- Login
- Go to one random site
- Got to one random tool
- Logout 

The exahustive users follow these steps:
- Go to the portal
- Login
- Go to each site
- Got to each tool on each site
- Logout 

Both types of users are running at the same time, so during the test you've got the same number of users of both types.

Some screenshots
================

![](https://raw.githubusercontent.com/sakaiproject/sakai-stress-test/master/src/test/resources/request-bodies/global1.jpg)![](https://raw.githubusercontent.com/sakaiproject/sakai-stress-test/master/src/test/resources/request-bodies/global2.jpg)![](https://raw.githubusercontent.com/sakaiproject/sakai-stress-test/master/src/test/resources/request-bodies/detail1.jpg)

Setting Up Your Test
====================

There are several things you can tune in this test:

- **Test Name**: You can add a name to identify your test the report will include the name and also the configurable values for the test.

	```$mvn test -Dtestname=test-server-x```

- **Target URL**: You can change the URL for the test by typing

	```$mvn test -Dtesturl=https://my-sakai-instance```
	
- **Pause Between Requests**: You can change the pause between requests the process stops a random time between min and max values (in seconds)

	```$mvn test -Dminpause=N -Dmaxpause=M```
	
- **Concurrent Users and RampUp time**: You can change the number of concurrent users of each type and the time to rampup them by typing

	```$mvn test -DrandomUsers=<RandomUsers> -DexhausUsers=<ExhaustiveUsers> -DrampUpTime=<Seconds>```
	
- **Loops**: You are able to repeat the test in different ways
	
	```$mvn test -DuserLoop=U -DsiteLoop=S -DtoolLoop=T```

	* _userLoop_: Each user (random or exhaustive) repeat U times the test case (all the steps)
	* _siteLoop_: For random users only pick S random sites
	* _toolLoop_: For random users only pick T tools on each random site
	
- **User credentials**: There is one csv file _data/user_credentials.csv_ to add test users to your test case. 
	
- **Impersonate Users**: For production environments probably you won't be able to know user credentials. In that case you can login as admin and impersonate test users. In that case you need to provide admin credentials in _data/admin_credentials.csv_ file. 

	```$mvn test -DimpersonateUsers=true```

- **Private credentials**: You can use your own credential files, just using the *private_* prefix in the mentioned files and typing: 

	```$mvn test -DprivateCredentials=true```

- **Feed Strategy**: You can change the way gatling is consuming users from your credentials file (default is random), just type: 

	```$mvn test -DfeedStrategy=circular```
	
	to learn more see http://gatling.io/docs/2.2.2/session/feeder.html#feeder

- **Fixed Ids**: You could focus your test in just one site or tool type:

	```$mvn test -DsiteId=abc.*```
	
	the test will try to browse only sites with id starting with abc
	
	```$mvn test -DsiteTitle=.*SMPL202.*```
	
	the test will try to browse only sites with title contains SMPL202

	```$mvn test -DtoolId=sakai-announcements```
	
	the test will try to browse only announcements tool on each site
	
- **NOTE**: You can set all these properties in the _stresstest.properties_ file to avoid include a long list of -Dprop=vale list in your command line. Use your own properties file typing:
 
	```$mvn test -DpropertiesFile=<path-to-properties-file>```
	
	or use _private_stresstest.properties_ and private credentials files and type:

	```$mvn test -Pprivate```

Configure log level
====================

- **LogLevel**: You can set the log level to review later requests or responses made during test, by default only failed request are logged (debug level), but you can change to log all requests typing:

	```$mvn test -DlogLevel=trace```

**NOTE**: You'll find a file with name _${logLevel}.log_ inside the result folder at the end of the simulation.

Add plugins to do more things
=============================

- **allowPlugins**: You can enable plugins in your test to perform specific actions:

	```$mvn test -DallowPlugins=pluginName```

You can create a plugin inside plugins folder, extending _SakaiSimulationPlugin_ class and adding the steps you want to run inside an concrete tool.
For example you can add a plugin to create or remove a folder in resources each time a user reach this tool.

Exploring the results
=====================

Gatling return an HTML report that gives you lot of information about the stress test.

Please go to http://gatling.io/docs/2.2.2/general/reports.html to know more about it.

