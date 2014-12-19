<?xml version="1.0" encoding="UTF-8" ?>
<!-- (c) Copyright IBM Corp. 2005, 2006 All Rights Reserved.      -->
<!-- This file is part of the DITA Open Toolkit project hosted on -->
<!-- Sourceforge.net. See the accompanying license.txt file for   -->
<!-- applicable licenses.                                         -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- Metadata conversions from DITA to Eclipse context file XML -->

<xsl:key name="meta-keywords" match="*[ancestor::*[contains(@class,' topic/keywords ')]]" use="text()"/>

<xsl:template name="getCSHMeta">

  <!-- = = = = = = = = = = = CONTENT = = = = = = = = = = = -->

  <!-- CONTENT: Type -->
  <xsl:apply-templates select="." mode="gen-type-metadata"/>

  <!-- CONTENT: Title - title -->
  <xsl:apply-templates select="*[contains(@class,' topic/title ')] |
                               self::dita/*[1]/*[contains(@class,' topic/title ')]" mode="gen-metadata"/>

  <!-- CONTENT: Description - shortdesc -->
  <xsl:apply-templates select="*[contains(@class,' topic/shortdesc ')] |
                               self::dita/*[1]/*[contains(@class,' topic/shortdesc ')]" mode="gen-metadata"/>

  <!-- CONTENT: Source - prolog/source/@href -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/source ')]/@href |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/source ')]/@href" mode="gen-metadata"/>

  <!-- CONTENT: Coverage prolog/metadata/category -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/category ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/category ')]" mode="gen-metadata"/>

  <!-- CONTENT: Subject - prolog/metadata/keywords -->
  <xsl:apply-templates select="." mode="gen-keywords-metadata"/>

  <!-- CONTENT: Relation - related-links -->
  <xsl:apply-templates select="*[contains(@class,' topic/related-links ')]/descendant::*/@href |
                               self::dita/*/*[contains(@class,' topic/related-links ')]/descendant::*/@href" mode="gen-metadata"/>

  <!-- = = = = = = = = = = = Product - Audience = = = = = = = = = = = -->
  <!-- Audience -->
  <!-- prolog/metadata/audience/@experiencelevel and other attributes -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@experiencelevel |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@experiencelevel" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@importance |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@importance" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@job |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@job" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@name |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@name" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@type |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/audience ')]/@type" mode="gen-metadata"/>


  <!-- <prodname> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/prodname ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/prodname ')]" mode="gen-metadata"/>

  <!-- <vrmlist><vrm modification="3" release="2" version="5"/></vrmlist> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@version |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@version" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@release |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@release" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@modification |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/vrmlist ')]/*[contains(@class,' topic/vrm ')]/@modification" mode="gen-metadata"/>

  <!-- <brand> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/brand ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/brand ')]" mode="gen-metadata"/>
  <!-- <component> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/component ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/component ')]" mode="gen-metadata"/>
  <!-- <featnum> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/featnum ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/featnum ')]" mode="gen-metadata"/>
  <!-- <prognum> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/prognum ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/prognum ')]" mode="gen-metadata"/>
  <!-- <platform> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/platform ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/platform ')]" mode="gen-metadata"/>
  <!-- <series> -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/series ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/prodinfo ')]/*[contains(@class,' topic/series ')]" mode="gen-metadata"/>

  <!-- = = = = = = = = = = = INTELLECTUAL PROPERTY = = = = = = = = = = = -->

  <!-- INTELLECTUAL PROPERTY: Contributor - prolog/author -->
  <!-- INTELLECTUAL PROPERTY: Creator -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/author ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/author ')]" mode="gen-metadata"/>

  <!-- INTELLECTUAL PROPERTY: Publisher - prolog/publisher -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/publisher ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/publisher ')]" mode="gen-metadata"/>

  <!-- INTELLECTUAL PROPERTY: Rights - prolog/copyright -->
  <!-- Put primary first, then secondary, then remainder -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][@type='primary'] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][@type='primary']" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][@type='secondary'] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][@type='seconday']" mode="gen-metadata"/>
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][not(@type)] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')][not(@type)]" mode="gen-metadata"/>

  <!-- Usage Rights - prolog/permissions -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/permissions ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/permissions ')]" mode="gen-metadata"/>

  <!-- = = = = = = = = = = = INSTANTIATION = = = = = = = = = = = -->

  <!-- INSTANTIATION: Date - prolog/critdates/created -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/created ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/created ')]" mode="gen-metadata"/>

  <!-- prolog/critdates/revised/@modified -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@modified |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@modified" mode="gen-metadata"/>

  <!-- prolog/critdates/revised/@golive -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@golive |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@golive" mode="gen-metadata"/>

  <!-- prolog/critdates/revised/@expiry -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@expiry |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@expiry" mode="gen-metadata"/>

  <!-- prolog/metadata/othermeta -->
  <xsl:apply-templates select="*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/othermeta ')] |
                               self::dita/*[1]/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/othermeta ')]" mode="gen-metadata"/>

  <!-- INSTANTIATION: Format -->
  <xsl:apply-templates select="." mode="gen-format-metadata"/>

  <!-- INSTANTIATION: Identifier --> <!-- id is an attribute on Topic -->
  <xsl:apply-templates select="@id | self::dita/*[1]/@id" mode="gen-metadata"/>

  <!-- INSTANTIATION: Language -->
  <xsl:apply-templates select="@xml:lang | self::dita/*[1]/@xml:lang" mode="gen-metadata"/>

</xsl:template>


<!-- CONTENT: Type -->
<xsl:template match="dita" mode="gen-type-metadata">
  <xsl:apply-templates select="*[1]" mode="gen-type-metadata"/>
</xsl:template>
<xsl:template match="*" mode="gen-type-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Type" content="</xsl:text>
	<xsl:value-of select="name(.)"/>
  <xsl:text disable-output-escaping="yes"> --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- CONTENT: Title - title -->
<xsl:template match="*[contains(@class,' topic/title ')]" mode="gen-metadata">
  <xsl:variable name="titlemeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Title" content="</xsl:text>
	<xsl:value-of select="normalize-space($titlemeta)" />
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- CONTENT: Description - shortdesc -->
<xsl:template match="*[contains(@class,' topic/shortdesc ')]" mode="gen-metadata">
  <xsl:variable name="shortmeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="abstract" content="</xsl:text>
     <xsl:value-of select="normalize-space($shortmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="description" content="</xsl:text>
     <xsl:value-of select="normalize-space($shortmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- CONTENT: Source - prolog/source/@href -->
<xsl:template match="*[contains(@class,' topic/source ')]/@href" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Source" content="</xsl:text>
     <xsl:value-of select="normalize-space(.)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- CONTENT: Coverage prolog/metadata/category -->
<xsl:template match="*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/category ')]" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Coverage" content="</xsl:text>
     <xsl:value-of select="normalize-space(.)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- CONTENT: Subject - prolog/metadata/keywords -->
<xsl:template match="*" mode="gen-keywords-metadata">
  <xsl:variable name="keywords-content">
    <!-- for each item inside keywords (including nested index terms) -->
    <xsl:for-each select="descendant::*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/metadata ')]/*[contains(@class,' topic/keywords ')]/descendant-or-self::*">
      <!-- If this is the first term or keyword with this value -->
      <xsl:if test="generate-id(key('meta-keywords',text())[1])=generate-id(.)">
        <xsl:if test="position()>2"><xsl:text>, </xsl:text></xsl:if>
        <xsl:value-of select="normalize-space(text())"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:if test="string-length($keywords-content)>0">
    <!-- <meta name="DC.subject" content="{$keywords-content}"/> -->
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.subject" content="</xsl:text>
       <xsl:value-of select="$keywords-content"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- <meta name="keywords" content="{$keywords-content}"/> -->
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.subject" content="</xsl:text>
       <xsl:value-of select="$keywords-content"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:if>
</xsl:template>

<!-- CONTENT: Relation - related-links -->
<xsl:template match="*[contains(@class,' topic/link ')]/@href" mode="gen-metadata">
 <xsl:variable name="linkmeta">
  <xsl:value-of select="normalize-space(.)"/>
 </xsl:variable>
 <xsl:choose>
  <xsl:when test="substring($linkmeta,1,1)='#'" />  <!-- ignore internal file links -->
  <xsl:otherwise>
    <xsl:variable name="linkmeta_ext">
     <xsl:choose>
      <xsl:when test="contains($linkmeta,'.dita')">
       <xsl:value-of select="substring-before($linkmeta,'.dita')"/>.<xsl:value-of select="$OUTEXT"/><xsl:value-of select="substring-after($linkmeta,'.dita')"/>
      </xsl:when>
      <xsl:otherwise>
       <xsl:value-of select="$linkmeta"/>
      </xsl:otherwise>
     </xsl:choose>
    </xsl:variable>
    <!-- <meta name="DC.Relation" scheme="URI">
      <xsl:attribute name="content"><xsl:value-of select="$linkmeta_ext"/></xsl:attribute>
    </meta> -->
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.relation" scheme="URI" content="</xsl:text>
       <xsl:value-of select="$linkmeta_ext"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
    <xsl:value-of select="$newline"/>
   </xsl:otherwise>
 </xsl:choose>
</xsl:template>

<!-- Do not let any other @href's inside related-links generate metadata -->
<xsl:template match="*/@href" mode="gen-metadata" priority="0"/>

<!-- INTELLECTUAL PROPERTY: Contributor - prolog/author -->
<!-- INTELLECTUAL PROPERTY: Creator -->
<!-- Default is type='creator' -->
<xsl:template match="*[contains(@class,' topic/author ')]" mode="gen-metadata">
  <xsl:choose>
    <xsl:when test="@type= 'contributor'">
      <!-- <meta name="DC.Contributor" content="{normalize-space(.)}"/> -->
      <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Contributor" content="</xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
      <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:when>
    <xsl:otherwise>
      <!-- <meta name="DC.Creator" content="{normalize-space(.)}"/> -->
      <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Creator" content="</xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
      <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- INTELLECTUAL PROPERTY: Publisher - prolog/publisher -->
<xsl:template match="*[contains(@class,' topic/publisher ')]" mode="gen-metadata">
  <!-- <meta name="DC.Publisher" content="{normalize-space(.)}"/> -->
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Publisher" content="</xsl:text>
    <xsl:value-of select="normalize-space(.)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- Place copyrights in keys for quick lookup. A copyright should only appear once; if it is primary
     it should appear first with other primaries; if secondary, after primaries; otherwise, after
     any that had @type. -->
<xsl:key name="primary-meta-copyright" match="*[contains(@class,' topic/copyright ')][@type='primary']"
                               use="concat(*[contains(@class,' topic/copyryear ')]/@year,
                                           *[contains(@class,' topic/copyrholder ')])"/>
<xsl:key name="secondary-meta-copyright" match="*[contains(@class,' topic/copyright ')][@type='secondary']"
                               use="concat(*[contains(@class,' topic/copyryear ')]/@year,
                                           *[contains(@class,' topic/copyrholder ')])"/>
<xsl:key name="meta-copyright" match="*[contains(@class,' topic/copyright ')][not(@type)]"
                               use="concat(*[contains(@class,' topic/copyryear ')]/@year,
                                           *[contains(@class,' topic/copyrholder ')])"/>

<xsl:template name="generate-copyright-attributes">
   <xsl:choose>
     <xsl:when test="*[contains(@class,' topic/copyrholder ')]/text() | *[contains(@class,' topic/copyrholder ')]/*">
       <xsl:value-of select="normalize-space(*[contains(@class,' topic/copyrholder ')])"/>
     </xsl:when>
     <xsl:otherwise>
       <xsl:text>(C) </xsl:text>
       <xsl:call-template name="getString">
        <xsl:with-param name="stringName" select="'Copyright IBM'"/>
       </xsl:call-template>
     </xsl:otherwise>
   </xsl:choose>
   <xsl:for-each select="*[contains(@class,' topic/copyryear ')]">
    <xsl:text> </xsl:text><xsl:value-of select="@year"/>
   </xsl:for-each>
</xsl:template>

<xsl:template match="*" mode="valid-copyright">
  <!--P018721: if year and holder are both empty, do not generate anything -->
  <xsl:variable name="copyrInfo">
    <!-- Check for any text in the copyrholder or the year; if both are empty, no
         copyright will be used -->
    <xsl:value-of select="*[contains(@class,' topic/copyrholder ')] |
                          *[contains(@class,' topic/copyryear ')]/@year"/>
  </xsl:variable>
  <xsl:if test="normalize-space($copyrInfo)!=''">
    <!-- <meta name="copyright"><xsl:call-template name="generate-copyright-attributes"/></meta> -->
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="copyright" content="</xsl:text>
      <xsl:call-template name="generate-copyright-attributes"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- <meta name="DC.Rights.Owner"><xsl:call-template name="generate-copyright-attributes"/></meta> -->
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Rights.Owner" content="</xsl:text>
      <xsl:call-template name="generate-copyright-attributes"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:if>
</xsl:template>

<!-- INTELLECTUAL PROPERTY: Rights - prolog/copyright -->
<xsl:template match="*[contains(@class,' topic/copyright ')]" mode="gen-metadata">
  <xsl:variable name="keylookup"><xsl:value-of select="concat(*[contains(@class,' topic/copyryear ')]/@year,
                                           *[contains(@class,' topic/copyrholder ')])"/></xsl:variable>
  <xsl:choose>
    <!-- If primary, ensure this is the first time it was used as primary -->
    <xsl:when test="@type='primary'">
      <xsl:if test="generate-id(.)=generate-id(key('primary-meta-copyright',$keylookup)[1])">
        <xsl:apply-templates select="." mode="valid-copyright"/>
      </xsl:if>
    </xsl:when>
    <!-- If secondary, this should be the first time it was used as secondary, AND it should not be primary -->
    <xsl:when test="@type='secondary'">
      <xsl:if test="not(key('primary-meta-copyright',$keylookup)) and
                    generate-id(.)=generate-id(key('secondary-meta-copyright',$keylookup)[1])">
        <xsl:apply-templates select="." mode="valid-copyright"/>
      </xsl:if>
    </xsl:when>
    <!-- No type: should not be used as primary or secondary, and this should be the first time it is used -->
    <xsl:otherwise>
      <xsl:if test="not(key('primary-meta-copyright',$keylookup)) and
                    not(key('secondary-meta-copyright',$keylookup)) and
                    generate-id(.)=generate-id(key('meta-copyright',$keylookup)[1])">
        <xsl:apply-templates select="." mode="valid-copyright"/>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Usage Rights - prolog/permissions -->
<xsl:template match="*[contains(@class,' topic/permissions ')]" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Rights.Usage" content="</xsl:text>
    <xsl:value-of select="@view"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- = = = = = = = = = = = Product - Audience = = = = = = = = = = = -->
<!-- Audience -->
<xsl:template match="*[contains(@class,' topic/audience ')]/@experiencelevel" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Experiencelevel" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/audience ')]/@importance" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Importance" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/audience ')]/@name" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Name" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/audience ')]/@job" mode="gen-metadata">
 <xsl:choose>
  <xsl:when test=".='other'">
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Job" content="</xsl:text>
      <xsl:value-of select="normalize-space(../@otherjob)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  </xsl:when>
  <xsl:otherwise>
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Job" content="</xsl:text>
      <xsl:value-of select="."/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  </xsl:otherwise>
 </xsl:choose>
 <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/audience ')]/@type" mode="gen-metadata">
 <xsl:choose>
  <xsl:when test=".='other'">
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Type" content="</xsl:text>
      <xsl:value-of select="normalize-space(../@othertype)"/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  </xsl:when>
  <xsl:otherwise>
    <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Audience.Type" content="</xsl:text>
      <xsl:value-of select="."/>
    <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  </xsl:otherwise>
 </xsl:choose>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/prodname ')]" mode="gen-metadata">
  <xsl:variable name="prodnamemeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <meta name="prodname">
    <xsl:attribute name="content"><xsl:value-of select="normalize-space($prodnamemeta)"/></xsl:attribute>
  </meta>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/vrm ')]/@version" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="version" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/vrm ')]/@release" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="release" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>
<xsl:template match="*[contains(@class,' topic/vrm ')]/@modification" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="modification" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/brand ')]" mode="gen-metadata">
  <xsl:variable name="brandmeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="brand" content="</xsl:text>
    <xsl:value-of select="normalize-space($brandmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/component ')]" mode="gen-metadata">
  <xsl:variable name="componentmeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="component" content="</xsl:text>
    <xsl:value-of select="normalize-space($componentmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/featnum ')]" mode="gen-metadata">
  <xsl:variable name="featnummeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="featnum" content="</xsl:text>
    <xsl:value-of select="normalize-space($featnummeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/prognum ')]" mode="gen-metadata">
  <xsl:variable name="prognummeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="prognum" content="</xsl:text>
    <xsl:value-of select="normalize-space($prognummeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/platform ')]" mode="gen-metadata">
  <xsl:variable name="platformmeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="platform" content="</xsl:text>
    <xsl:value-of select="normalize-space($platformmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template match="*[contains(@class,' topic/series ')]" mode="gen-metadata">
  <xsl:variable name="seriesmeta">
    <xsl:apply-templates select="*|text()" mode="text-only"/>
  </xsl:variable>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="series" content="</xsl:text>
    <xsl:value-of select="normalize-space($seriesmeta)"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- INSTANTIATION: Date - prolog/critdates/created -->
<xsl:template match="*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/created ')]" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Date.Created" content="</xsl:text>
    <xsl:value-of select="@date"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- prolog/critdates/revised/@modified -->
<xsl:template match="*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@modified" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Date.Modified" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- prolog/critdates/revised/@golive -->
<xsl:template match="*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@golive" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Date.Issued" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Date.Available" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- prolog/critdates/revised/@expiry -->
<xsl:template match="*[contains(@class,' topic/critdates ')]/*[contains(@class,' topic/revised ')]/@expiry" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Date.Expiry" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- prolog/metadata/othermeta -->
<xsl:template match="*[contains(@class,' topic/othermeta ')]" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="</xsl:text>
    <xsl:value-of select="@name"/>   
  <xsl:text>" content="</xsl:text>
    <xsl:value-of select="@content"/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- INSTANTIATION: Format -->
<!-- this value is based on output format used for DC indexing, not source.
     Put in this odd template for easy overriding, if creating another output format. -->
<xsl:template match="*" mode="gen-format-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Format" content="XML" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- INSTANTIATION: Identifier --> <!-- id is an attribute on Topic -->
<xsl:template match="@id" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Identifier" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

<!-- INSTANTIATION: Language -->
<!-- ideally, take the first token of the language attribute value -->
<xsl:template match="@xml:lang" mode="gen-metadata">
  <xsl:text disable-output-escaping="yes">&lt;!-- meta name="DC.Language" content="</xsl:text>
    <xsl:value-of select="."/>
  <xsl:text disable-output-escaping="yes">" --&gt;</xsl:text>
  <xsl:value-of select="$newline"/>
</xsl:template>

</xsl:stylesheet>
