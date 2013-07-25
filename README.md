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
### Automated Test reports 

[Check Report in Cloudbees](https://apam.ci.cloudbees.com/job/ApamRelations/fr.imag.adele.apam.tests.services$apam-pax-test/lastBuild/testReport/)

## Apam Metadata Validators

### Lastest released version

```xml
<apam xmlns="fr.imag.adele.apam" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="fr.imag.adele.apam http://repository-apam.forge.cloudbees.com/release/schema/ApamCore-0.0.3.xsd">

...

</apam>
```

### Snapshot version

```xml
<apam xmlns="fr.imag.adele.apam" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="fr.imag.adele.apam http://raw.github.com/AdeleResearchGroup/ApAM/master/runtime/core/src/main/resources/xsd/ApamCore.xsd">

...

</apam>
```

# Release

## Changelog

* ApAM 0.0.4 [details](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=3&page=1&state=closed) (currently under development) 
	* Yet to define
* ApAM 0.0.3 [bugfix](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=1&page=1&state=closed) [download](http://repository-apam.forge.cloudbees.com/release/repository/fr/imag/adele/apam/apam-basic-distribution/0.0.3/apam-basic-distribution-0.0.3.zip)  
	* Relations
	* Meta substitution
* ApAM 0.0.2
	* Previous implementantation without relation optimization
* ApAM 0.0.1
	* Not anylonger supported version


