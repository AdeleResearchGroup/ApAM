<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	<xsl:output method="xml" encoding="UTF-8" />

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="@uri[parent::resource]">
		<xsl:attribute name="uri">
			<xsl:value-of select="'mvn:fr.imag.adele.apam'" />/<xsl:value-of
			select="../@symbolicname" />/<xsl:value-of select="'0.0.1-SNAPSHOT'" />
		</xsl:attribute>
	</xsl:template>


</xsl:stylesheet>