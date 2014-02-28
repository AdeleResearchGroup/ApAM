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

[Check Report in Cloudbees](https://apam.ci.cloudbees.com/job/APAMUnitTest/)

## Apam Metadata Validators

### Lastest released version

```xml
<apam xmlns="fr.imag.adele.apam" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="fr.imag.adele.apam http://repository-apam.forge.cloudbees.com/release/schema/ApamCore.xsd">

...

</apam>
```

### Snapshot or older versions
Please suffix ApamCore with version number (using previous release number or latest release number plus "-SNAPSHOT"). For instance to use ApAM release 0.0.3 :
```xml
<apam xmlns="fr.imag.adele.apam" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="fr.imag.adele.apam http://raw.github.com/AdeleResearchGroup/ApAM/master/runtime/core/src/main/resources/xsd/ApamCore-0.0.3.xsd">

...

</apam>
```

# Release

## Changelog

* ApAM 0.0.7 [details](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=6&page=1&state=closed) (currently under development)
	* To be defined

* ApAM 0.0.6 [details](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=5&page=1&state=closed) (currently under development)
	* Added new Property Type : float (a floating point value, mapped to java Float Class)
	* Added new Property Type : version (defining an OSGi version, mapped to org.osgi.framework.Version)
	* Contextual Relation : "ContextDependencyType" are back again for composite contextual behavior
	* Refactoring of OBRMan manager for better compliance with OBR specifications
	* Refactoring of Managers (startup, order, declaration of config files)
	* ApAM is now building properly on maven 3.1.x (and still working for maven 3.0.x)
	* Bufixes with composite behavior (now main implementation are working as expected, an apam-instance can refer to a composite in another bundle)
	* A lot of bugfixes to improve startup behavior and asynchronous declaration processing (ApAM should be now waiting for the bundles to start correctly)
	* ConflictMan Manager is now properly defined for grant behavior(and works accordingly)
	* Bugfixes with promotions
	* Added new feature to add properties to ApAM components during build, such as maven properties of the project, apam version that have made the built (to be considered as experimental, can change drastically with next releases)

* ApAM 0.0.5 [bugfix](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=4&page=1&state=closed) [download](http://repository-apam.forge.cloudbees.com/release/repository/fr/imag/adele/apam/apam-basic-distribution/0.0.5/apam-basic-distribution-0.0.5.zip)
	* WARNING : Several changes to apam.xsd (apam components descriptor schema) ARE NOT compatible with previous releases (0.0.4)
		* removed ambiguous and unused types as "instance" (ambiguous with same ipojo concept), "ContextDependencyType", "ResourceDependencyType"
		* "dependency" element deprecated and fully replaced by "relation"
		* for relations, resolve="exist" is now the default behavior for relation
		* for properties, removed deprecated internal="true/false" -> replaced by injected="internal/external/both"
		* replaced "value" attribute by "default" in property definition
		* added "deny" element as the explicit reverse property of grant for a component
		* added "implementation" element for granted components
	* New specs for property definition and inheritance
	* New specs for metasubstitution
	* New specs for promotion
	* Clear separation between ApAM core (runtime and core managers such as ApAMan, OSGiMan, FailureMan, DynaMan) and optional managers (ObrMan, ConflictMan, DistriMan, Histman)
	* ApAM now works upon iPojo 1.11
	* Updated libraries version (felix 4.2+) and such
	* ApAM core can now run on top of Android (accordingly to how felix is embedded)
	
* ApAM 0.0.4 [bugfix](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=3&page=1&state=closed) [download](http://repository-apam.forge.cloudbees.com/release/repository/fr/imag/adele/apam/apam-basic-distribution/0.0.4/apam-basic-distribution-0.0.4.zip)
	* Bugfix
* ApAM 0.0.3 [bugfix](https://github.com/AdeleResearchGroup/ApAM/issues?milestone=1&page=1&state=closed) [download](http://repository-apam.forge.cloudbees.com/release/repository/fr/imag/adele/apam/apam-basic-distribution/0.0.3/apam-basic-distribution-0.0.3.zip)  
	* Relations
	* Meta substitution
* ApAM 0.0.2
	* Previous implementantation without relation optimization
* ApAM 0.0.1
	* Not anylonger supported version


