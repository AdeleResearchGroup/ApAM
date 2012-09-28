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

		<xsl:variable name="groupid">
			<xsl:value-of select="../capability/p[@n='groupId']/@v" />
		</xsl:variable>

		<xsl:variable name="artifactid">
			<xsl:value-of
				select="../capability/p[@n='artifactId']/@v" />
		</xsl:variable>
		<xsl:variable name="version">
			<xsl:value-of select="../capability/p[@n='version']/@v" />
		</xsl:variable>

		<xsl:attribute name="uri">mvn:<xsl:value-of
			select="$groupid" />/<xsl:value-of select="$artifactid" />/<xsl:value-of
			select="'0.0.1-SNAPSHOT'" />
		</xsl:attribute>
	</xsl:template>

</xsl:stylesheet>