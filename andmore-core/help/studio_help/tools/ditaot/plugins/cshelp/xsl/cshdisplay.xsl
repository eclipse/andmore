<?xml version="1.0"?>
<!-- (c) Copyright IBM Corp. 2005, 2006 All Rights Reserved.      -->
<!-- This file is part of the DITA Open Toolkit project hosted on -->
<!-- Sourceforge.net. See the accompanying license.txt file for   -->
<!-- applicable licenses.                                         -->

<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="*[contains(@class,' cshelp/cshelp ')]" name="cshelp">
  <xsl:choose>
    <xsl:when test="$DRAFT='yes'">
      <!-- review output -->
      <xsl:choose>
        <xsl:when test="not(parent::*)">
        <html><xsl:value-of select="$newline"/>
        <head /><xsl:value-of select="$newline"/>
        <body><xsl:value-of select="$newline"/>
	  <xsl:call-template name="csreviewoutput"/>
        <xsl:if test="*[contains(@class,' cshelp/cshelp ')]">
          <xsl:apply-templates select="*[contains(@class,' cshelp/cshelp ')]"/>
        </xsl:if>
        </body><xsl:value-of select="$newline"/>
        </html><xsl:value-of select="$newline"/>
	</xsl:when>
      <xsl:otherwise>
	  <xsl:call-template name="csreviewoutput"/>
        <xsl:if test="*[contains(@class,' cshelp/cshelp ')]">
          <xsl:apply-templates select="*[contains(@class,' cshelp/cshelp ')]"/>
        </xsl:if>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <!-- Otherwise, do standard processing -->
      <xsl:choose>
        <xsl:when test="not(parent::*)">
          <xsl:call-template name="chapter-setup"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-imports/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="csreviewoutput"> 
       <hr/><xsl:value-of select="$newline"/>
       <hr/><xsl:value-of select="$newline"/>
<strong><xsl:value-of select="@id"/></strong><br/><xsl:value-of select="$newline"/>
<xsl:value-of select="*[contains(@class,' cshelp/csprolog ')]/*[contains(@class,' cshelp/csmetadata ')]/*[contains(@class,' cshelp/cswindowtitle ')]"/><br/><xsl:value-of select="$newline"/>
<xsl:value-of select="*[contains(@class,' cshelp/csprolog ')]/*[contains(@class,' cshelp/csmetadata ')]/*[contains(@class,' cshelp/cswidgetlabel ')]"/><br/><xsl:value-of select="$newline"/>
<br/><xsl:value-of select="$newline"/>
          <p><xsl:apply-templates select="*[contains(@class,' topic/shortdesc ')]"/></p><xsl:value-of select="$newline"/>
          <xsl:apply-templates select="*[contains(@class,' cshelp/csbody ')]"/><xsl:value-of select="$newline"/>
          <xsl:if test="*[contains(@class,' topic/related-links ')]">
            <xsl:for-each select="*[contains(@class,' topic/related-links ')]/*[contains(@class,' topic/link ')]">
              <a>
                <xsl:attribute name="href">
                  <xsl:call-template name="href"/>
                </xsl:attribute>
                <xsl:value-of select="linktext" />
              </a>
              <br/><xsl:value-of select="$newline"/>
            </xsl:for-each>
          </xsl:if>
        <br/><xsl:value-of select="$newline"/><xsl:value-of select="$newline"/>
</xsl:template>

</xsl:stylesheet>
