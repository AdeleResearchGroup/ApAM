<map version="0.9.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1353065971613" ID="ID_856281372" MODIFIED="1353076828406" TEXT="APAM-Unit test coverture">
<node CREATED="1353065995318" ID="ID_866107605" MODIFIED="1353066274405" POSITION="right" TEXT="Core">
<node CREATED="1353066005446" ID="ID_776905123" MODIFIED="1353066634176" TEXT="Property">
<node CREATED="1353066218822" ID="ID_476104344" MODIFIED="1353071117012" TEXT="Inherited properties should not be updateable-01">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353066255334" ID="ID_95062722" MODIFIED="1353071120106" TEXT="Configured using api by initial parameter-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353066636390" ID="ID_1873589170" MODIFIED="1353071122834" TEXT="Configured using api by set property-03">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353066277318" ID="ID_42176654" MODIFIED="1353071125539" TEXT="Internal/Non internal properties should be visible-04">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353066428630" ID="ID_1680728427" MODIFIED="1353071139715" TEXT="Internal should be updateable only through the app and not from apam-04">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070152246" ID="ID_725900474" MODIFIED="1353071142514" TEXT="Non Internal should be updateable through the app and from apam-04">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353066561782" ID="ID_1302007357" MODIFIED="1353071145034" TEXT="Internal/Non internal properties declared in the XML should be injected in apam-05">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070279798" ID="ID_1459016122" MODIFIED="1353071147570" TEXT="Filtering applied in integer values should work properly-06">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070310598" ID="ID_1414356345" MODIFIED="1353071150218" TEXT="Filtering applied in boolean values should work properly-07">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070377590" ID="ID_382562301" MODIFIED="1353071152723" TEXT="Filtering applied in string values should work properly-08">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353066008853" ID="ID_82303023" MODIFIED="1353071077628" TEXT="Constraint">
<node CREATED="1353070505975" ID="ID_346843196" MODIFIED="1353071155770" TEXT="injected instance should respect constraint declared in XML-01">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070582118" ID="ID_1387289746" MODIFIED="1353071158482" TEXT="Injected instance should respect constraint in initial property through API-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070694278" ID="ID_515894246" MODIFIED="1353071160706" TEXT="Set of the dependent type must receive only instances that respect the constraint in the XML-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353070821991" ID="ID_1373095281" MODIFIED="1353071163282" TEXT="Set of the dependent type must receive all existing instances that respect the constraint  in the XML-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353071077628" ID="ID_1122141323" MODIFIED="1353071165778" TEXT="Injected instance should be filtered by the values configured in the api-03">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353066015126" ID="ID_1595772935" MODIFIED="1353316598237" TEXT="Dependency">
<node CREATED="1353075661607" ID="ID_916344715" MODIFIED="1353076800476" TEXT="set/array types">
<node CREATED="1353071915367" ID="ID_1456627929" MODIFIED="1353075217660" TEXT="check if the class instantiated is exactly of the same type as the requested-01">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353071220215" ID="ID_1175335295" MODIFIED="1353077373919" TEXT="A dependency of the type Set should be updated after creation of new instance-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353072339175" ID="ID_50755232" MODIFIED="1353075224467" TEXT="Injecting depency in a Set should be updated after removing the wires-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353072618247" ID="ID_1536441835" MODIFIED="1353075224471" TEXT="Injecting depency in an Array should be updated after removing the wires-03">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353075446919" ID="ID_1296868598" MODIFIED="1353075609854" TEXT="singleton+shared">
<node CREATED="1353074583452" ID="ID_777037208" MODIFIED="1353075467794" TEXT="singleton+!shared, should be raised on creating the second instance-04">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353074692503" ID="ID_658986551" MODIFIED="1353075467792" TEXT="singleton+shared, the instance there should be only one instance in apam-05">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353074767992" ID="ID_1258072892" MODIFIED="1353075467791" TEXT="!singleton+!shared, all injected instances are different-06">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353074879335" ID="ID_1750847459" MODIFIED="1353075467787" TEXT="!singleton+shared, instances can be recicled (reused)-07">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353075584486" ID="ID_1134906873" MODIFIED="1353316530836" TEXT="instantiable">
<node CREATED="1353075027047" ID="ID_1273627382" MODIFIED="1353075224469" TEXT="!instantiable, should not allow to create an instance manually-08">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353075061384" ID="ID_545711227" MODIFIED="1353075224468" TEXT="instantiable, should allow to create an instance manually-09">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353075698358" ID="ID_296603360" MODIFIED="1353075702293" TEXT="callback">
<node CREATED="1353075111719" ID="ID_968919845" MODIFIED="1353075224468" TEXT="init callback, should be called when the components starts-10">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353075141416" ID="ID_384580446" MODIFIED="1353085786890" TEXT="remove callback, should be called when the components is uninstalled-11">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353075257352" ID="ID_1845106728" MODIFIED="1353316551326" TEXT="preference">
<node CREATED="1353075782472" ID="ID_1933538834" MODIFIED="1353076608931" TEXT="injected instance should respect the preference (if its satisfiable)-12">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353076140408" ID="ID_1990480808" MODIFIED="1353076608931" TEXT="injected instance with different impl should respect the preference (if its satisfiable)-13">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353076476440" FOLDED="true" ID="ID_500157770" MODIFIED="1353313491297" TEXT="respect constraint where there is an empty preference tag in the xml-14">
<icon BUILTIN="button_ok"/>
<node CREATED="1353313460239" ID="ID_633668789" MODIFIED="1353313460239" TEXT="">
<node CREATED="1353313460264" ID="ID_1219775830" MODIFIED="1353313460264" TEXT=""/>
</node>
</node>
<node CREATED="1353076557752" ID="ID_533005585" MODIFIED="1353076608931" TEXT="should be possible find an impl by its name-15">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353316599105" ID="ID_209507505" MODIFIED="1353316659376" TEXT="error handling">
<node CREATED="1353316606452" ID="ID_1742264459" MODIFIED="1353316654377" TEXT="in &lt;dependency&gt; fail wait should cause thread to be halted">
<icon BUILTIN="button_cancel"/>
</node>
<node CREATED="1353316660323" ID="ID_929298761" MODIFIED="1353316747777" TEXT="in &lt;dependency&gt; tag, if fail is configured to &quot;exception&quot; the exception declared in the property should be thrown">
<icon BUILTIN="button_cancel"/>
</node>
</node>
</node>
</node>
<node CREATED="1353066000806" ID="ID_1984824160" MODIFIED="1353077178051" POSITION="left" TEXT="Composite">
<node CREATED="1353076999720" ID="ID_1637874218" MODIFIED="1353406251643" TEXT="instantiation">
<node CREATED="1353076702920" ID="ID_1910174465" MODIFIED="1353076973683" TEXT="should be possible to instantiate one composite-01">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353076657096" ID="ID_1884842516" MODIFIED="1353076901982" TEXT="should be possible to instantiate two composites, based on the same impl, sequentially-02">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353076955016" ID="ID_1396045592" MODIFIED="1353077032748" TEXT="should be possible to retrieve the service object of a composite-03">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353332025894" ID="ID_1509607474" MODIFIED="1353332074612" TEXT="enclosed dependency instantiation should instantiate automatically indirect dependency-04">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353406252448" ID="ID_868172689" MODIFIED="1353412015763" TEXT="if dependency is marked as eager, it should be instantiated as soon as the bundle is ready-05">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353077063784" ID="ID_158894754" MODIFIED="1353077097851" TEXT="in case meta-composites, the inner composite instantiation should instantiate the outers">
<icon BUILTIN="button_cancel"/>
</node>
<node CREATED="1353077111176" ID="ID_1509995545" MODIFIED="1353077140556" TEXT="declared composites should be loaded into apam right after the platform initialization">
<icon BUILTIN="button_cancel"/>
</node>
</node>
<node CREATED="1353077038120" ID="ID_1357881613" MODIFIED="1353077290758" TEXT=""/>
<node CREATED="1353077178936" ID="ID_817521693" MODIFIED="1353077182374" TEXT="start"/>
<node CREATED="1353077184248" ID="ID_914230240" MODIFIED="1353077186598" TEXT="borrow"/>
<node CREATED="1353077190840" ID="ID_1475045056" MODIFIED="1353077192166" TEXT="local"/>
<node CREATED="1353077196808" ID="ID_1525157524" MODIFIED="1353077213100" TEXT="friend">
<icon BUILTIN="info"/>
</node>
<node CREATED="1353077202712" ID="ID_1802016001" MODIFIED="1353077204742" TEXT="application"/>
<node CREATED="1353077208472" ID="ID_1719755842" MODIFIED="1353077210118" TEXT="own"/>
</node>
<node CREATED="1353075841416" ID="ID_1642355188" MODIFIED="1353085358873" POSITION="right" TEXT="OBRMan">
<node CREATED="1353085358875" ID="ID_1347548977" MODIFIED="1353085447680" TEXT="should raise an exception in case of a invalid configuration path is given">
<icon BUILTIN="button_ok"/>
</node>
<node CREATED="1353077625512" ID="ID_852943712" MODIFIED="1353085682813" TEXT="load and instantiate a component from repository">
<icon BUILTIN="button_ok"/>
</node>
</node>
<node CREATED="1353076837416" ID="ID_295652406" MODIFIED="1353076845686" POSITION="left" TEXT="ApamMAN"/>
</node>
</map>
