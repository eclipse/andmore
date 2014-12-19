<?xml version="1.0"?>
<!-- (c) Copyright IBM Corp. 2005, 2006 All Rights Reserved.      -->
<!-- This file is part of the DITA Open Toolkit project hosted on -->
<!-- Sourceforge.net. See the accompanying license.txt file for   -->
<!-- applicable licenses.                                         -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml"
            encoding="utf-8"
            indent="no"
            doctype-system="..\dtd\cshelp.dtd"
            doctype-public="-//IBM//DTD DITA CSHelp//EN"
/>

<xsl:variable name="cr"><xsl:text>
</xsl:text></xsl:variable>
<xsl:variable name="lt">&#60;</xsl:variable>
<xsl:variable name="gt">&#62;</xsl:variable>

    <xsl:template match="//contexts">
	  <xsl:value-of select="$cr"/>
	  <cshelp id="csh_outer_container" xml:lang="en-us"><xsl:value-of select="$cr"/>
		<title></title><xsl:value-of select="$cr"/>
		<shortdesc></shortdesc><xsl:value-of select="$cr"/>
		<csbody></csbody><xsl:value-of select="$cr"/>
            <xsl:apply-templates />
	  </cshelp><xsl:value-of select="$cr"/>
    </xsl:template>

	<xsl:template match="//comment()">
		<xsl:copy />		
	</xsl:template>

	<xsl:template match="//context">

	   <cshelp id="{@id}">
	   <xsl:value-of select="$cr"/>

		<title><xsl:value-of select="@title"/></title><xsl:value-of select="$cr"/>

		<shortdesc>
               <xsl:choose>
                 <xsl:when test="contains(description,$cr)">
			<xsl:call-template name="br-replace-1">
			<xsl:with-param name="brtext" select="substring-before(description,$cr)"/>
			</xsl:call-template>
                 </xsl:when>
                 <xsl:otherwise>
		        <xsl:value-of select="description" disable-output-escaping="yes" />
                 </xsl:otherwise>
               </xsl:choose>
		</shortdesc><xsl:value-of select="$cr"/>

		<csbody><xsl:value-of select="$cr"/>
                <xsl:if test="contains(description,$cr)">
			  <p>
			  <xsl:call-template name="br-replace-2">
			  <xsl:with-param name="brtext2" select="substring-after(description,$cr)"/>
			  </xsl:call-template>
			  </p>
                </xsl:if>
		</csbody><xsl:value-of select="$cr" />		

		<xsl:if test="topic">
			<related-links><xsl:value-of select="$cr"/>
			<xsl:for-each select="topic">
			<link href="{@href}">
				<xsl:attribute name="format">
					<xsl:call-template name="find-file-ext">
					<xsl:with-param name="path" select="@href"/>
					</xsl:call-template>
				</xsl:attribute>
				<linktext>
					<xsl:value-of select="@label"/>
				</linktext>
			</link><xsl:value-of select="$cr"/>
			</xsl:for-each>	
			</related-links><xsl:value-of select="$cr"/>
		</xsl:if>
	   </cshelp><xsl:value-of select="$cr"/>
	</xsl:template>

	<xsl:template name="br-replace-1">
	  <xsl:param name="brtext"/>
	  <xsl:choose>
	    <xsl:when test="contains($brtext,$cr)"> <!-- is there a CR within the text? -->
		 <xsl:value-of select="$brtext" disable-output-escaping="yes" />
		 <xsl:value-of select="$cr"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:value-of select="$brtext" disable-output-escaping="yes" /> <!-- No CRs, just output -->
	    </xsl:otherwise>
	  </xsl:choose>
      </xsl:template>

      <xsl:template name="br-replace-2">
	  <xsl:param name="brtext2"/>
	  <xsl:choose>
	    <xsl:when test="contains($brtext2,$cr)"> <!-- is there a CR within the text? -->
		<xsl:if test="string-length(substring-before($brtext2,$cr)) &gt; 0">
	       <xsl:value-of select="substring-before($brtext2,$cr)" disable-output-escaping="yes" /> <!-- yes - substring & add the BR & newline -->
		 <xsl:text disable-output-escaping="yes">&#60;</xsl:text>/p<xsl:text disable-output-escaping="yes">&#62;</xsl:text><xsl:value-of select="$cr"/>
		 <xsl:text disable-output-escaping="yes">&#60;</xsl:text>p<xsl:text disable-output-escaping="yes">&#62;</xsl:text>
		</xsl:if>
		<xsl:call-template name="br-replace-2">
		   <xsl:with-param name="brtext2" select="substring-after($brtext2,$cr)"/>
		</xsl:call-template>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:value-of select="$brtext2" disable-output-escaping="yes" /> <!-- No CRs, just output -->
	    </xsl:otherwise>
	  </xsl:choose>
      </xsl:template>

	<xsl:template name="find-file-ext">
		<xsl:param name="path"/>
		<xsl:choose>
	 	<xsl:when test="contains($path,'.')">
			<xsl:call-template name="find-file-ext">
			<xsl:with-param name="path" select="substring-after($path,'.')" />
			</xsl:call-template>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$path" />
		</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>