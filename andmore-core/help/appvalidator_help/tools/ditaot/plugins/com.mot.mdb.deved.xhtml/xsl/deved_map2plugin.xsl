<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="../../../xsl/map2pluginImpl.xsl"/>

  <!--  -->
  <!-- BEGIN OVERRIDE:  USE CRLF RATHER THAN LF IN MANIFEST.MF -->
  <!-- 2009.06.26 BG:  Appears that Eclipse 3.5 requires this, so
       I added explicit CRLF characters. Be careful not to change 
       the whitespace within this variable.-->
  <!--  -->
  <xsl:variable name="newline">
<xsl:text>&#13;&#10;</xsl:text></xsl:variable>
  <!-- END OVERRIDE:  USE CRLF RATHER THAN LF IN MANIFEST.MF -->

  <!--  -->
  <!-- BEGIN OVERRIDE:  PLUGIN.PROPERTIES  -->
  <!-- 2010.02.16 BG:  Engineering requested we change name property to pluginName -->
  <!--  -->
  <xsl:template match="*[contains(@class, ' map/map ')]" mode="eclipse.properties">
    
    <xsl:text># NLS_MESSAGEFORMAT_NONE</xsl:text><xsl:value-of select="$newline"/>
    <xsl:text># NLS_ENCODING=UTF-8</xsl:text><xsl:value-of select="$newline"/>
    <!--<xsl:value-of select="$newline"/>-->
    <xsl:choose>
      <xsl:when test="@title">
        <xsl:text>pluginName=</xsl:text><xsl:value-of select="@title"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>name=Sample Title</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="$newline"/>
    <xsl:text>providerName=</xsl:text><xsl:value-of select="$provider"/>
  </xsl:template>
  <!-- END OVERRIDE:  PLUGIN.PROPERTIES -->


  <!--  -->
  <!-- BEGIN OVERRIDE:  MANIFEST.MF  -->
  <!-- 2010.02.18 BG:  Engineering requested we change "name" property to "pluginName", 
           "Eclipse-LazyStart: true" to "Bundle-ActivationPolicy: lazy", and add 
           "Bundle-RequiredExecutionEnvironment: J2SE-1.5".-->
  <!--  -->
<xsl:template match="*[contains(@class, ' map/map ')]" mode="eclipse.manifest">
    
    <xsl:text>Bundle-Version: </xsl:text><xsl:value-of select="$version"/><xsl:value-of select="$newline"/>
    <xsl:text>Manifest-Version: 1.0</xsl:text><xsl:value-of select="$newline"/>
    <xsl:text>Bundle-ManifestVersion: 2</xsl:text><xsl:value-of select="$newline"/>
    <xsl:text>Bundle-Localization: plugin</xsl:text><xsl:value-of select="$newline"/>
    <xsl:text>Bundle-Name: %pluginName</xsl:text><xsl:value-of select="$newline"/>
    <xsl:text>Bundle-Vendor: %providerName</xsl:text><xsl:value-of select="$newline"/>
    
    <xsl:choose>
      <xsl:when test="$plugin='true'">
        <xsl:text>Bundle-ActivationPolicy: lazy</xsl:text><xsl:value-of select="$newline"/>
        <xsl:choose>
          <xsl:when test="@id">
            <xsl:text>Bundle-SymbolicName: </xsl:text><xsl:value-of select="@id"/>;<xsl:text> singleton:=true</xsl:text><xsl:value-of select="$newline"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>Bundle-SymbolicName: org.sample.help.doc; singleton:=true</xsl:text><xsl:value-of select="$newline"/>
            <xsl:call-template name="output-message">
              <xsl:with-param name="msgnum">050</xsl:with-param>
              <xsl:with-param name="msgsev">W</xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose> 
        <xsl:text>Bundle-RequiredExecutionEnvironment: J2SE-1.5</xsl:text><xsl:value-of select="$newline"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="@id">                        
            <xsl:if test="$fragment.lang!=''">
              <xsl:text>Fragment-Host: </xsl:text><xsl:value-of select="@id"/>;
              <xsl:text>Bundle-SymbolicName: </xsl:text>
              <xsl:choose>
                <xsl:when test="$fragment.country!=''">
                  <xsl:value-of select="@id"/>.<xsl:value-of select="$fragment.lang"/>.<xsl:value-of select="$fragment.country"/>;<xsl:text/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@id"/>.<xsl:value-of select="$fragment.lang"/>;<xsl:text/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if> 
            <xsl:if test="$fragment.lang=''">
              <xsl:text>Bundle-SymbolicName: </xsl:text><xsl:value-of select="@id"/><xsl:value-of select="$newline"/>
            </xsl:if>
          </xsl:when>
          <xsl:otherwise>
            
            <xsl:text>Bundle-SymbolicName: org.sample.help.doc.</xsl:text>
            <xsl:choose>
              <xsl:when test="$fragment.lang!=''">
                <xsl:choose>
                  <xsl:when test="$fragment.country!=''">
                    <xsl:value-of select="$fragment.lang"/>.<xsl:value-of select="$fragment.country"/>;
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="$fragment.lang"/>;
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:when>
              <!-- We shouldn' t be getting here, but just in case -->
              <xsl:otherwise>
                <xsl:text>lang; </xsl:text>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newline"/>
            <xsl:text>Fragment-Host: org.sample.help.doc;</xsl:text><xsl:value-of select="$newline"/>
            <xsl:call-template name="output-message">
              <xsl:with-param name="msgnum">050</xsl:with-param>
              <xsl:with-param name="msgsev">W</xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>                 
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- END OVERRIDE:  MANIFEST.MF -->

</xsl:stylesheet>
