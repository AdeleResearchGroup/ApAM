ApAM
=====

APAM (APplication Abstract Machine) is a runtime platform to support execution of adaptable applications. 

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/1fa31448de45acc1ccce5592df85df53 "githalytics.com")](http://githalytics.com/AdeleResearchGroup/ApAM)


## Infrastructure

### Maven Repository 
		
```xml
...
		<repository>
			<id>cloudbees-ApAM-release</id>
			<name>Cloudbees Private Repository - ApAM - Release </name>
			<url>https://repository-apam.forge.cloudbees.com/release/repository/</url>
		</repository>
		<repository>
			<id>cloudbees-ApAM-snapshot</id>
			<name>Cloudbees Private Repository - ApAM - Snapshot</name>
			<url>https://repository-apam.forge.cloudbees.com/snapshot/repository/</url>
		</repository>
...
```


### Nighly build test results

Unit test launched: 

* Relations [report](https://apam.ci.cloudbees.com/job/ApamRelations/fr.imag.adele.apam.tests.services$apam-pax-test/lastBuild/testReport/) 
* Master branch [report](https://apam.ci.cloudbees.com/job/APAMUnitTest/fr.imag.adele.apam.tests.services$apam-pax-test/lastBuild/testReport/) 

# Release

## Changelog

* ApAM 0.0.3 [details](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=1&page=1&state=closed) (currently under development) 
	* Relations
	* Meta substitution
* ApAM 0.0.2
	* Previous implementantation without relation optimization
* ApAM 0.0.1
	* Not anylonger supported version


