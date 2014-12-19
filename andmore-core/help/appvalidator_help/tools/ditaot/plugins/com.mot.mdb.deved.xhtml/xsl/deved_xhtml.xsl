<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!--<xsl:import href="../../../xsl/xslhtml/dita2htmlImpl.xsl"/>-->
  
  <!--  -->
  <!-- BEGIN OVERRIDE:  DON'T REQUIRE @TMCLASS VALUE TO RENDER TRADEMARK -->
  <!-- Change trademark logic to remove test for specific values of @tmclass, so symbol appears regardless of @tmclass. -->
  <!--  -->
  <xsl:template match="*[contains(@class,' topic/tm ')]" name="topic.tm">

    <xsl:apply-templates/>
    <!-- output the TM content -->

    <xsl:variable name="Ltmclass">
      <xsl:call-template name="convert-to-lower">
        <!-- ensure lowercase for comparisons -->
        <xsl:with-param name="inputval" select="@tmclass"/>
      </xsl:call-template>
    </xsl:variable>
    <!-- If this is a good class, continue... -->
    <!--  Commented out <xsl:if> test for @tmclass value, so processing continues even if no value is specified. BG 2008.11.25. -->
    <!--    <xsl:if test="$Ltmclass='ibm' or $Ltmclass='ibmsub' or $Ltmclass='special'">-->
    <!-- Test for TM area's language -->
    <xsl:variable name="tmtest">
      <xsl:call-template name="tm-area"/>
    </xsl:variable>

    <!-- If this language should get trademark markers, continue... -->
    <xsl:if test="$tmtest='tm'">
      <xsl:variable name="tmvalue">
        <xsl:value-of select="@trademark"/>
      </xsl:variable>

      <!-- Determine if this is in a title, and should be marked -->
      <xsl:variable name="usetitle">
        <xsl:if
          test="ancestor::*[contains(@class,' topic/title ')]/parent::*[contains(@class,' topic/topic ')]">
          <xsl:choose>
            <!-- Not the first one in a title -->
            <xsl:when test="generate-id(.)!=generate-id(key('tm',.)[1])">skip</xsl:when>
            <!-- First one in the topic, BUT it appears in a shortdesc or body -->
            <xsl:when
              test="//*[contains(@class,' topic/shortdesc ') or contains(@class,' topic/body ')]//*[contains(@class,' topic/tm ')][@trademark=$tmvalue]"
              >skip</xsl:when>
            <xsl:otherwise>use</xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </xsl:variable>

      <!-- Determine if this is in a body, and should be marked -->
      <xsl:variable name="usebody">
        <xsl:choose>
          <!-- If in a title or prolog, skip -->
          <xsl:when
            test="ancestor::*[contains(@class,' topic/title ') or contains(@class,' topic/prolog ')]/parent::*[contains(@class,' topic/topic ')]"
            >skip</xsl:when>
          <!-- If first in the document, use it -->
          <xsl:when test="generate-id(.)=generate-id(key('tm',.)[1])">use</xsl:when>
          <!-- If there is another before this that is in the body or shortdesc, skip -->
          <xsl:when
            test="preceding::*[contains(@class,' topic/tm ')][@trademark=$tmvalue][ancestor::*[contains(@class,' topic/body ') or contains(@class,' topic/shortdesc ')]]"
            >skip</xsl:when>
          <!-- Otherwise, any before this must be in a title or ignored section -->
          <xsl:otherwise>use</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <!-- If it should be used in a title or used in the body, output your favorite TM marker based on the attributes -->
      <xsl:if test="$usetitle='use' or $usebody='use'">
        <xsl:choose>
          <!-- ignore @tmtype=service or anything else -->
          <xsl:when test="@tmtype='tm'">&#x2122;</xsl:when>
          <!-- Removed superscript from TM symbol.  2008.11.25 BG. -->
          <xsl:when test="@tmtype='reg'">&#xAE;</xsl:when>
          <xsl:otherwise/>
        </xsl:choose>
      </xsl:if>
    </xsl:if>
   <!-- </xsl:if>-->
  </xsl:template>
  <!-- END OVERRIDE:  DON'T REQUIRE @TMCLASS VALUE TO RENDER TRADEMARK -->



<!--  -->
<!-- BEGIN OVERRIDE:  REMOVE BR TAG BEFORE/AFTER IMAGES  -->
<!--2009.03.17 bg:  Removed br tags before and after images when placement="break". 
                                   Instead, added a div with class="imageleft" around such images.--> 
<!--  -->
<!-- =========== IMAGE/OBJECT =========== -->
<xsl:template match="*[contains(@class,' topic/image ')]" name="topic.image">
  <xsl:variable name="flagrules">
    <xsl:call-template name="getrules"/>
  </xsl:variable>
  <!-- build any pre break indicated by style -->
  <xsl:choose>
    <xsl:when test="parent::fig[contains(@frame,'top ')]">
      <!-- NOP if there is already a break implied by a parent property -->
    </xsl:when>
    <xsl:otherwise>
     <xsl:choose>
      <!-- 2009.03.17 bg:  Removed br tag in next line. --> 
      <xsl:when test="(@placement='break')">
        <xsl:call-template name="start-flagit">
          <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param>     
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
       <xsl:call-template name="flagcheck"/>
      </xsl:otherwise>
     </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="start-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
  <xsl:call-template name="setaname"/>
  <xsl:choose>
   <xsl:when test="@placement='break'"><!--Align only works for break-->
    <xsl:choose>
     <xsl:when test="@align='left'">
      <div class="imageleft">
       <xsl:call-template name="topic-image"/>
      </div>
     </xsl:when>
     <xsl:when test="@align='right'">
      <div class="imageright">
       <xsl:call-template name="topic-image"/>
      </div>
     </xsl:when>
     <xsl:when test="@align='center'">
      <div class="imagecenter">
       <xsl:call-template name="topic-image"/>
      </div>
     </xsl:when>
     <xsl:otherwise>
      <!--2009.03.17 bg: If @placement=break and @align isn't set, then style the same as when align is set to left.-->
      <div class="imageleft">
       <xsl:call-template name="topic-image"/>
      </div>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:when>
   <xsl:otherwise>
    <xsl:call-template name="topic-image"/>
   </xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="end-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
  <xsl:call-template name="end-flagit">
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param> 
  </xsl:call-template>
  <!-- build any post break indicated by style -->
  <!-- 2009.03.17 bg:  Removed br tag in next line. --> 
  <xsl:if test="not(@placement='inline')"></xsl:if>
  <!-- image name for review -->
  <xsl:if test="$ARTLBL='yes'">
    [<xsl:value-of select="@href"/>]
  </xsl:if>
</xsl:template>
<!-- END OVERRIDE:  REMOVE BR TAG BEFORE/AFTER IMAGES  -->



<!--  -->
<!-- BEGIN OVERRIDE:  GLOSSENTRY TOPIC CSS STYLE -->
<!-- 2009.03.17 bg:  Added class="glossentry" to div enclosing glossentry--> 
<!--  -->
<!-- child topics get a div wrapper and fall through -->
<xsl:template match="*[contains(@class,' glossentry/glossentry ')]" name="child.topic">
  <xsl:param name="nestlevel">
      <xsl:choose>
          <!-- Limit depth for historical reasons, could allow any depth. Previously limit was 5. -->
          <xsl:when test="count(ancestor::*[contains(@class,' topic/topic ')]) > 9">9</xsl:when>
          <xsl:otherwise><xsl:value-of select="count(ancestor::*[contains(@class,' topic/topic ')])"/></xsl:otherwise>
      </xsl:choose>
  </xsl:param>
<div class="glossentry nested{$nestlevel}">
 <xsl:call-template name="gen-topic"/>
</div><xsl:value-of select="$newline"/>
</xsl:template>
<!-- END OVERRIDE:  GLOSSENTRY TOPIC CSS STYLE -->
  
  
<!--  -->
<!-- BEGIN OVERRIDE:  REMOVED BR BEFORE UNORDERED LISTS -->
<!-- 2009.03.17 bg:  Removed br element before ul--> 
<!--  -->
<xsl:template match="*[contains(@class,' topic/ul ')]" mode="ul-fmt">
  <xsl:variable name="flagrules">
    <xsl:call-template name="getrules"/>
  </xsl:variable>
  <xsl:variable name="conflictexist">
    <xsl:call-template name="conflict-check">
      <xsl:with-param name="flagrules" select="$flagrules"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="start-flagit">
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param>     
  </xsl:call-template>
  <xsl:call-template name="start-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
 <xsl:call-template name="setaname"/>
 <ul>
   <xsl:call-template name="commonattributes"/>
   <xsl:call-template name="gen-style">
     <xsl:with-param name="conflictexist" select="$conflictexist"></xsl:with-param> 
     <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param>
   </xsl:call-template>
   <xsl:apply-templates select="@compact"/>
   <xsl:call-template name="setid"/>
   <xsl:apply-templates/>
 </ul>
  <xsl:call-template name="end-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
  <xsl:call-template name="end-flagit">
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param> 
  </xsl:call-template>
 <xsl:value-of select="$newline"/>
</xsl:template>
  <!-- END OVERRIDE:  REMOVED BR BEFORE UNORDERED LISTS -->
  
<!--  -->
<!-- BEGIN OVERRIDE:  REMOVED BR BEFORE ORDERED LISTS -->
<!-- 2009.03.17 bg:  Removed br element before ol--> 
<!--  -->
<xsl:template match="*[contains(@class,' topic/ol ')]" name="topic.ol">
  <xsl:variable name="flagrules">
    <xsl:call-template name="getrules"/>
  </xsl:variable>
  <xsl:variable name="conflictexist">
    <xsl:call-template name="conflict-check">
      <xsl:with-param name="flagrules" select="$flagrules"/>
    </xsl:call-template>
  </xsl:variable>
<xsl:variable name="olcount" select="count(ancestor-or-self::*[contains(@class,' topic/ol ')])"/>
  <xsl:call-template name="start-flagit">
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param>     
  </xsl:call-template>
  <xsl:call-template name="start-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
<xsl:call-template name="setaname"/>
<ol>
  <xsl:call-template name="commonattributes"/>
  <xsl:call-template name="gen-style">
    <xsl:with-param name="conflictexist" select="$conflictexist"></xsl:with-param> 
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param>
  </xsl:call-template>
  <xsl:apply-templates select="@compact"/>
  <xsl:choose>
    <xsl:when test="$olcount mod 3 = 1"/>
    <xsl:when test="$olcount mod 3 = 2"><xsl:attribute name="type">a</xsl:attribute></xsl:when>
    <xsl:otherwise><xsl:attribute name="type">i</xsl:attribute></xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="setid"/>
  <xsl:apply-templates/>
</ol>
  <xsl:call-template name="end-revflag">
    <xsl:with-param name="flagrules" select="$flagrules"/>
  </xsl:call-template>
  <xsl:call-template name="end-flagit">
    <xsl:with-param name="flagrules" select="$flagrules"></xsl:with-param> 
  </xsl:call-template>
<xsl:value-of select="$newline"/>
</xsl:template>
<!-- END OVERRIDE:  REMOVED BR BEFORE ORDERED LISTS -->

</xsl:stylesheet>
