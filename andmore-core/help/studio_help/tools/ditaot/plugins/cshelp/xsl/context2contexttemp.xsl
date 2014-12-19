<?xml version="1.0"?>
<!-- (c) Copyright IBM Corp. 2005, 2006 All Rights Reserved.      -->
<!-- This file is part of the DITA Open Toolkit project hosted on -->
<!-- Sourceforge.net. See the accompanying license.txt file for   -->
<!-- applicable licenses.                                         -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:variable name="cr"><xsl:text>
</xsl:text></xsl:variable>
<xsl:variable name="lt">&#60;</xsl:variable>
<xsl:variable name="gt">&#62;</xsl:variable>

    <xsl:template match="/">
	  <xsl:processing-instruction name="NLS">type="org.eclipse.help.contexts"</xsl:processing-instruction><xsl:value-of select="$cr"/>
	  <contexts><xsl:value-of select="$cr"/>
	  <xsl:apply-templates select="//context" />
	  </contexts><xsl:value-of select="$cr"/>
	</xsl:template>

     <xsl:template match="//comment()">
        <xsl:copy />		
     </xsl:template>

     <xsl:template match="context">
	   <context id="{@id}">		
		<xsl:value-of select="$cr"/>
		<xsl:apply-templates />
	   </context><xsl:value-of select="$cr"/>
     </xsl:template>

     <xsl:template match="description">
	   <description>
		<xsl:apply-templates />
	   </description><xsl:value-of select="$cr"/>
     </xsl:template>

     <xsl:template match="b">
	   <xsl:text>&lt;b&gt;</xsl:text>
		<xsl:apply-templates />
	   <xsl:text>&lt;/b&gt;</xsl:text>
	</xsl:template>

	<xsl:template match="topic">
		<xsl:copy-of select="." />
	</xsl:template>

</xsl:stylesheet>