Sakai Gatling Stress Test
=========================

This is a Simple Stress Test for Sakai 11+ instances.

To test it out, simply execute the following command:

    $mvn gatling:execute

Each time you run this you'll get a result in target/sakaisimulation-xxxxxxx folder.

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

Setting Up Your Test
====================

There are several things you can tune in this test:

- **Test Name**: You can add a name to identify your test the report will include the name and also the configurable values for the test.

	```$mvn gatling:execute -Dtestname=test-server-x```

- **Target URL**: You can change the URL for the test by typing

	```$mvn gatling:execute -Dtesturl=https://my-sakai-instance```
	
- **Concurrent Users and RampUp time**: You can change the number of concurrent users of each type and the time to rampup them by typing

	```$mvn gatling:execute -DrandomUsers=<RandomUsers> -DexhausUsers=<ExhaustiveUsers> -DrampUpTime=<Seconds>```
	
- **Loops**: You are able to repeat the test in different ways
	
	```$mvn gatling:execute -DuserLoop=U -DsiteLoop=S -DtoolLoop=T```

	* _userLoop_: Each user (random or exhaustive) repeat U times the test case (all the steps)
	* _siteLoop_: For random users only pick S random sites
	* _toolLoop_: For random users only pick T tools on each random site
	
- **User credentials**: There is one csv file _data/user_credentials.csv_ to add test users to your test case. 
	
	
Exploring the results
=====================

Gatling return an HTML report that gives you lot of information about the stress test.

Please go to http://gatling.io/docs/2.2.2/general/reports.html to know more about it.

