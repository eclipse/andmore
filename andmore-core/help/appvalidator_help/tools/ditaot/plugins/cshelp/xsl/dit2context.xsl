<?xml version="1.0"?>
<!-- (c) Copyright IBM Corp. 2005, 2007 All Rights Reserved.      -->
<!-- This file is part of the DITA Open Toolkit project hosted on -->
<!-- Sourceforge.net. See the accompanying license.txt file for   -->
<!-- applicable licenses.                                         -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:saxon="http://icl.com/saxon"
                xmlns:xt="http://www.jclark.com/xt"
                extension-element-prefixes="saxon xt">

<xsl:import href="../../../xsl/common/dita-utilities.xsl" />
<xsl:include href="../../../xsl/common/output-message.xsl"/>
<xsl:include href="GetCSHMeta.xsl"/>

<!-- /OUTEXT = default "output extension" processing parameter ('html')-->
<!-- Should be overridden by rexx command to be 'xml' -->
<xsl:param name="OUTEXT" select="'html'"/><!-- "htm" and "html" are valid values -->

<!-- /WORKDIR = the working directory that contains the document being transformed.
     Needed as a directory prefix for the @conref "document()" function calls.
     default is '../doc/')-->
<xsl:param name="WORKDIR" select="'./'"/>

<!-- /FILENAME = the file name (file name and extension only - no path) of the document being transformed.
     Needed to help with debugging.
     default is 'myfile.xml')-->
<xsl:param name="FILENAME"/>

<xsl:variable name="msgprefix">IDXS</xsl:variable>
<xsl:variable name="newline"><xsl:text>
</xsl:text></xsl:variable>

<xsl:output indent="no"/>

      <xsl:template match="/">
	  <xsl:value-of select="$newline"/>
        <xsl:processing-instruction name="NLS">type="org.eclipse.help.contexts"</xsl:processing-instruction>
 	  <xsl:value-of select="$newline"/>
	  <xsl:apply-templates select="//*[contains(@class,' cshelp/cshelp ')]"/> <!-- select is formatted to unnest nested cshelp elements -->
	  <xsl:text disable-output-escaping="yes">&lt;/contexts&gt;</xsl:text>
	  <xsl:value-of select="$newline"/>
      </xsl:template>

	<xsl:template match="//comment()">
		<xsl:copy />
	</xsl:template>

	<xsl:template match="*[contains(@class,' cshelp/cshelp ')]" name="cshelp">

		<xsl:if test="not(parent::*[contains(@class,' cshelp/cshelp ')])">
			<xsl:call-template name="getCSHMeta"/>  <!-- 5/31/2006 -->
		  <!--			<xsl:call-template name="ibmcopyright"/>-->  <!-- 5/30/2006 -->
			<xsl:text disable-output-escaping="yes">&lt;contexts&gt;</xsl:text>
			<xsl:value-of select="$newline"/>
		</xsl:if>
		
		<xsl:if test="parent::*[contains(@class,' cshelp/cshelp ')]">
			<xsl:element name="context">
			<xsl:attribute name="id">
			<xsl:value-of select="@id"/>
			</xsl:attribute>
			<xsl:if test="*[contains(@class,' topic/title ')]/text() | *[contains(@class,' topic/title ')]/*">
				<xsl:attribute name="title">
				<xsl:value-of select="*[contains(@class,' topic/title ')]" />
				</xsl:attribute>
			</xsl:if>
			<xsl:text>&#xA;</xsl:text>
			
			<xsl:element name="description">
			<xsl:apply-templates select="*[contains(@class,' topic/shortdesc ')]" />
			<xsl:apply-templates select="*[contains(@class,' cshelp/csbody ')]" />
			</xsl:element>
	     	   	<xsl:text>&#xA;</xsl:text>

			<xsl:if test="*[contains(@class,' topic/related-links ')]">
			<xsl:for-each select="*[contains(@class,' topic/related-links ')]/*[contains(@class,' topic/link ')]">
			<xsl:element name="topic">
           	             <xsl:attribute name="href">
           	             <xsl:call-template name="href"/>
           	             </xsl:attribute>
				<xsl:attribute name="label">
           	             <xsl:choose>
           	                <xsl:when test="*[contains(@class, ' topic/linktext ')]">
           	                   <xsl:apply-templates select="*[contains(@class, ' topic/linktext ')]"/>
           	                </xsl:when>
           	                <xsl:otherwise>
           	                   <!-- use href -->
           	                   <xsl:call-template name="href"/>
           	                </xsl:otherwise>
           	             </xsl:choose>
				</xsl:attribute>
			</xsl:element><xsl:text>&#xA;</xsl:text>
			</xsl:for-each>
			</xsl:if>

			</xsl:element><xsl:text>&#xA;</xsl:text>
		</xsl:if>
	</xsl:template>


	<xsl:template match="*[contains(@class,' topic/shortdesc ')]" name="shortdesc">
		<xsl:apply-templates />
	</xsl:template>

        <xsl:template match="*[contains(@class,' cshelp/csbody ')]" name="csbody">
            <xsl:if test="node()"><xsl:text>&#xA;&#xA;</xsl:text></xsl:if>
		<xsl:apply-templates />
	</xsl:template>

      <xsl:template match="*[contains(@class,' topic/boolean ')]" name="topic.boolean">
        <!-- below copied from dit2htm.xsl -->
        <xsl:value-of select="name()"/><xsl:text>: </xsl:text><xsl:value-of select="@state"/>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/cite ')]" name="topic.cite">
        <xsl:call-template name="checkPreceding" />
       	<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/dd ')]" name="topic.dd">
        <!-- jta 11/06/2006 -->
        <xsl:call-template name="indentDL"/>
	<xsl:text>  </xsl:text>
        <xsl:apply-templates />
	<!-- jta 10/16/2006 -->
        <!-- <xsl:if test="position()!=last()"> -->
        <xsl:if test="following-sibling::*[contains(@class,' topic/dd ')]">
          <xsl:text>&#xA;</xsl:text>
        </xsl:if>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/ddhd ')]" name="topic.ddhd">
        <!-- jta 11/06/2006 -->
        <xsl:call-template name="indentDL"/>
		<xsl:text>  </xsl:text><b><xsl:apply-templates /></b><xsl:text>&#xA;</xsl:text>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/desc ')]" name="topic.desc">
        <xsl:call-template name="checkPreceding" />
        <xsl:text>(</xsl:text><xsl:apply-templates /><xsl:text>)</xsl:text>
        <xsl:call-template name="checkFollowing" />
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/dl ')]" name="topic.dl">
        <xsl:if test="not(parent::*[contains(@class,' cshelp/csbody ')])">
          <xsl:choose>
             <xsl:when test="parent::*[contains(@class,' topic/p ')]">
               <xsl:text>&#xA;&#xA;</xsl:text>
             </xsl:when>
             <xsl:otherwise>
               <xsl:text>&#xA;</xsl:text>
             </xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:apply-templates />
        <xsl:call-template name="isNestedTextFollows" />
        <xsl:call-template name="isFirstChildOfCsbody" />
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/dlentry ')]" name="topic.dlentry">
        <xsl:apply-templates />
        <!-- jta 11/07/2006 -->
        <xsl:if test="following-sibling::*[contains(@class,' topic/dlentry ')]">
           <xsl:text>&#xA;</xsl:text>
        </xsl:if>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/dlhead ')]" name="topic.dlhead">
        <xsl:apply-templates />
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/draft-comment ')]" name="topic.draft-comment">
        <!-- no output -->
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/dt ')]" name="topic.dt">
        <!-- jta 11/06/2006 -->
        <xsl:call-template name="indentDL"/>
      	<b><xsl:apply-templates /></b><xsl:text>&#xA;</xsl:text>
      </xsl:template>
     
      <xsl:template match="*[contains(@class,' topic/dthd ')]" name="topic.dthd">   
        <!-- jta 11/06/2006 -->
        <xsl:call-template name="indentDL"/>
      	<b><xsl:apply-templates /></b><xsl:text>&#xA;</xsl:text>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/fig ')]" name="topic.fig">
        <xsl:call-template name="twoPrecedingCRs" />
        <xsl:apply-templates />
        <xsl:call-template name="isFirstChildOfCsbody" />
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/fn ')]" name="topic.fn">
        <!-- no output -->
        <xsl:call-template name="output-message">
           <xsl:with-param name="msg">Eclipse context-sensitive help files do not support footnotes.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>
      </xsl:template>

      <xsl:template match="*[contains(@class,' topic/image ')]" name="topic.image">
        <xsl:variable name="alttext">
          <xsl:choose>
            <xsl:when test="*[contains(@class,' topic/alt ')]"><xsl:apply-templates/></xsl:when>
            <xsl:otherwise><xsl:value-of select="@alt"/></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <!-- no output -->
        <xsl:if test="$alttext=''">
          <xsl:call-template name="output-message">
             <xsl:with-param name="msg">Eclipse context-sensitive help files do not support images.</xsl:with-param>
              <xsl:with-param name="msgnum">066</xsl:with-param>
             <xsl:with-param name="msgsev">I</xsl:with-param>
          </xsl:call-template>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="@placement='break'">
            <xsl:call-template name="twoPrecedingCRs" />
            <xsl:value-of select="$alttext"/>
            <xsl:call-template name="isFirstChildOfCsbody" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="checkPreceding" />
            <xsl:value-of select="$alttext"/>
            <xsl:call-template name="checkFollowing" />
          </xsl:otherwise>
        </xsl:choose>
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/indexterm ')]" name="topic.indexterm">
        <!-- no output -->
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/indextermref ')]" name="topic.indextermref">
        <!-- no output -->
	</xsl:template>

        <xsl:template match="*[contains(@class,' topic/itemgroup ')]" name="topic.itemgroup">
           <xsl:text>&#xA;   </xsl:text><xsl:apply-templates />
	   <!-- jta 10/16/2006 -->
	   <xsl:call-template name="isFirstChildOfCsbody" />
        </xsl:template>

	<xsl:template match="*[contains(@class,' topic/keyword ')]" name="topic.keyword">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/li ')]" name="topic.li">
            <xsl:variable name="olcount" select="count(ancestor-or-self::*[contains(@class,' topic/ol ')])"/>
 <!--       <xsl:variable name="ulcount" select="count(ancestor-or-self::*[contains(@class,' topic/ul ')])"/> -->
 <!--       <xsl:variable name="slcount" select="count(ancestor-or-self::*[contains(@class,' topic/sl ')])"/> -->
 <!--       <xsl:variable name="nestcount" select="number($olcount) + number($ulcount) + number($slcount)" /> -->
 <!--       <xsl:choose> -->
 <!--         <xsl:when test="number($nestcount)=1"> -->
 <!--            <xsl:text>  </xsl:text> -->
 <!--         </xsl:when> -->
 <!--         <xsl:when test="number($nestcount)=2"> -->
 <!--            <xsl:text>    </xsl:text> -->
 <!--         </xsl:when> -->
 <!--         <xsl:when test="number($nestcount)=3"> -->
 <!--            <xsl:text>      </xsl:text> -->
 <!--         </xsl:when> -->
 <!--         <xsl:otherwise> -->
 <!--            <xsl:text>        </xsl:text> -->
 <!--         </xsl:otherwise> -->
 <!--        </xsl:choose> -->
          <xsl:call-template name="indentLI"/>
          <xsl:if test="parent::*[contains(@class,' topic/ul ')]">
               <xsl:text>&#x2D; </xsl:text>
          </xsl:if>
          <xsl:apply-templates />
	  <!-- jta 11/06/2006 -->
         <xsl:if test="following-sibling::*[contains(@class,' topic/li ')]">
           <xsl:text>&#xA;</xsl:text>
         </xsl:if>
	</xsl:template>

    <xsl:template match="*[contains(@class,' topic/lines ')]" name="topic.lines">
	    <xsl:call-template name="spec-title-nospace"/>
	    <xsl:call-template name="br-replace">
	    <xsl:with-param name="brtext" select="."/>
        </xsl:call-template>
        <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

    <xsl:template name="spec-title-nospace">
        <!-- below adapted from dit2htm.xsl -->
        <xsl:if test="@spectitle"><b><xsl:value-of select="@spectitle"/></b><xsl:text>&#xA;</xsl:text></xsl:if>
    </xsl:template>

    <!-- Break replace - used for LINES -->
    <!-- this replaces newlines with the BR element. Forces breaks. -->
    <xsl:template name="br-replace">
	  <xsl:param name="brtext"/>
	  <!-- capture an actual newline within the xsl:text element -->
	  <xsl:variable name="cr"><xsl:text>
</xsl:text></xsl:variable>
	  <xsl:choose>
	    <xsl:when test="contains($brtext,$cr)"> <!-- is there a CR within the text? -->
	       <xsl:value-of select="substring-before($brtext,$cr)"/> <!-- yes - substring & add the BR & newline -->
		 <xsl:value-of select="$cr"/>
	       <xsl:call-template name="br-replace"> <!-- call again to get remaining CRs -->
	         <xsl:with-param name="brtext" select="substring-after($brtext,$cr)"/>
	       </xsl:call-template>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:value-of select="$brtext"/> <!-- No CRs, just output -->
	    </xsl:otherwise>
	  </xsl:choose>
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/linkinfo ')]" name="topic.linkinfo">
        <xsl:text>&#xA;</xsl:text><xsl:apply-templates /><xsl:text>&#xA;&#xA;</xsl:text>
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/linklist ')]" name="topic.linklist">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/linkpool ')]" name="topic.linkpool">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/linktext ')]" name="topic.linktext">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/lq ')]" name="topic.lq">
        <xsl:call-template name="twoPrecedingCRs" />
	  <xsl:call-template name="indentLQ" />
        <xsl:text>&#34;</xsl:text><xsl:apply-templates /><xsl:text>&#34;</xsl:text>
        <xsl:call-template name="isNestedTextFollows" />
        <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/msgblock ')]" name="topic.msgblock">
	  <xsl:call-template name="br-replace">
	  <xsl:with-param name="brtext" select="."/>
	  </xsl:call-template>
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/note ')]" name="topic.note">
	   <!-- jta 10/16/2006 -->
         <xsl:call-template name="twoPrecedingCRs"/>
         <xsl:call-template name="indentNote" />
         <!-- <xsl:text>&#xA;&#xA;</xsl:text> -->
         <xsl:call-template name="spec-title"/>
          <xsl:choose>
            <xsl:when test="@type='note'">
             <xsl:call-template name="note"/>
            </xsl:when>
            <xsl:when test="@type='tip'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Tip'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates /><xsl:text>&#xA;&#xA;</xsl:text>
            </xsl:when>
            <xsl:when test="@type='fastpath'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Fastpath'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates /><xsl:text>&#xA;&#xA;</xsl:text>
            </xsl:when>
            <xsl:when test="@type='important'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Important'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates /><xsl:text>&#xA;&#xA;</xsl:text>
            </xsl:when>
            <xsl:when test="@type='remember'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Remember'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates /><xsl:text>&#xA;&#xA;</xsl:text>
            </xsl:when>
            <xsl:when test="@type='restriction'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Restriction'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates />
            </xsl:when>
            <xsl:when test="@type='attention'">
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'Attention'"/>
               </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates />
            </xsl:when>
            <xsl:when test="@type='caution'">
              <xsl:call-template name="getString">
               <xsl:with-param name="stringName" select="'Caution'"/>
              </xsl:call-template>
               <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
               </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates />
            </xsl:when>
            <xsl:when test="@type='danger'">
              <xsl:call-template name="getString">
               <xsl:with-param name="stringName" select="'Danger'"/>
              </xsl:call-template>
              <xsl:call-template name="getString">
                <xsl:with-param name="stringName" select="'ColonSymbol'"/>
              </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:apply-templates />
            </xsl:when>
            <xsl:when test="@type='other'">
             <xsl:choose>
              <xsl:when test="@othertype"> <!-- is there a type title? Use that -->
                 <!-- TBD: this attr is a key that should look up external, translateable text.
                      For now, just output the othertype attr value. -->
                 <xsl:value-of select="@othertype"/>
                 <xsl:call-template name="getString">
                  <xsl:with-param name="stringName" select="'ColonSymbol'"/>
                 </xsl:call-template>
                <xsl:text> </xsl:text>
              </xsl:when>
              <xsl:otherwise>
               <xsl:call-template name="note"/> <!-- otherwise, give them the standard note -->
              </xsl:otherwise>
             </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
             <xsl:call-template name="note"/>
            </xsl:otherwise>
          </xsl:choose>
         <xsl:call-template name="isNestedTextFollows" />
         <xsl:call-template name="isFirstChildOfCsbody" />
        </xsl:template>

    <xsl:template name="note">
        <xsl:call-template name="getString">
        <xsl:with-param name="stringName" select="'Note'"/>
        </xsl:call-template>
        <xsl:call-template name="getString">
        <xsl:with-param name="stringName" select="'ColonSymbol'"/>
        </xsl:call-template>
        <xsl:text> </xsl:text>
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template name="spec-title">
        <xsl:if test="@spectitle"><b><xsl:value-of select="@spectitle"/></b><xsl:text>&#xA;</xsl:text></xsl:if>
    </xsl:template>

	<xsl:template match="*[contains(@class,' topic/object ')]" name="topic.object">
        <!-- no output -->
        <xsl:call-template name="output-message">
           <xsl:with-param name="msg">Eclipse context-sensitive help files do not support objects.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>
	</xsl:template>

    <xsl:template match="*[contains(@class,' topic/ol ')]" name="topic.ol">
        <xsl:variable name="olcount" select="count(ancestor-or-self::*[contains(@class,' topic/ol ')])"/>
<!--    <xsl:variable name="ulcount" select="count(ancestor-or-self::*[contains(@class,' topic/ul ')])"/> -->
<!--    <xsl:variable name="slcount" select="count(ancestor-or-self::*[contains(@class,' topic/sl ')])"/> -->
<!--    <xsl:variable name="ddcount" select="count(ancestor-or-self::*[contains(@class,' topic/dd ')])"/> -->
<!--    <xsl:variable name="nestcount" select="number($olcount) + number($ulcount) + number($slcount) + number($ddcount)" /> -->
        <xsl:call-template name="twoPrecedingCRs" />

<!--    <xsl:if test="parent::*[contains(@class,' topic/li ')] | parent::*[contains(@class,' topic/sli ')]"><xsl:text>&#xA;</xsl:text></xsl:if> -->
  	   <xsl:for-each select="*[contains(@class,' topic/li ')]">
<!--       <xsl:choose> -->
<!--              <xsl:when test="number($nestcount)=1"> -->
<!--                 <xsl:text>  </xsl:text> -->
<!--              </xsl:when> -->
<!--              <xsl:when test="number($nestcount)=2"> -->
<!--                 <xsl:text>    </xsl:text> -->
<!--              </xsl:when> -->
<!--              <xsl:when test="number($nestcount)=3"> -->
<!--                 <xsl:text>      </xsl:text> -->
<!--              </xsl:when> -->
<!--              <xsl:otherwise> -->
<!--                 <xsl:text>        </xsl:text> -->
<!--              </xsl:otherwise> -->
<!--            </xsl:choose> -->
           <xsl:call-template name="indentLI"/> <!-- jta 11/07/2006 -->
           <xsl:choose>
             <xsl:when test="$olcount mod 3 = 1">
               <xsl:number value="position()" format="1. "/>
             </xsl:when>
             <xsl:when test="$olcount mod 3 = 2">
               <xsl:number value="position()" format="a. "/>
             </xsl:when>
             <xsl:otherwise>
               <xsl:number value="position()" format="i. "/>
             </xsl:otherwise>
           </xsl:choose>
           <xsl:apply-templates /><!-- jta 11/07/2006 <xsl:text>&#xA;</xsl:text> -->
           <xsl:if test="following-sibling::*[contains(@class,' topic/li ')]">
             <xsl:text>&#xA;</xsl:text>
           </xsl:if>
         </xsl:for-each>
       <xsl:call-template name="isNestedTextFollows" />
       <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

	<xsl:template match="*[contains(@class,' topic/p ')]" name="topic.p">
		<!-- jta 10/16/2006 -->
		<xsl:call-template name="twoPrecedingCRs"/>
                <xsl:call-template name="indentP" />
		<xsl:apply-templates />
		<!-- <xsl:text>&#xA;&#xA;</xsl:text> -->
                <xsl:call-template name="isNestedTextFollows" />
		<xsl:call-template name="isFirstChildOfCsbody" />
	</xsl:template>

        <xsl:template match="*[contains(@class,' topic/ph ')]" name="topic.ph">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/pre ')]" name="topic.pre">
        <xsl:call-template name="checkPreceding" />
	        <xsl:call-template name="br-replace">
	        <xsl:with-param name="brtext" select="."/>
	        </xsl:call-template>
        <xsl:call-template name="checkFollowing" />
        <xsl:call-template name="isFirstChildOfCsbody" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/q ')]" name="topic.q">
        <xsl:call-template name="checkPreceding" />
		<xsl:text>&#34;</xsl:text><xsl:apply-templates /><xsl:text>&#34;</xsl:text> <!-- places quotation marks -->
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

 	<xsl:template match="*[contains(@class,' topic/required-cleanup ')]" name="topic.required-cleanup">
        <!-- no output -->
	</xsl:template>

    <xsl:template match="*[contains(@class,' topic/simpletable ')]" name="topic.simpletable">
        <!-- no output -->
        <xsl:call-template name="output-message">
           <xsl:with-param name="msg">Eclipse context-sensitive help files do not support tables.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/sl ')]" name="topic.sl">
        <xsl:call-template name="twoPrecedingCRs" />
<!--    <xsl:if test="parent::*[contains(@class,' topic/li ')] | parent::*[contains(@class,' topic/sli ')]"><xsl:text>&#xA;</xsl:text></xsl:if> -->
        <xsl:apply-templates />
        <xsl:call-template name="isNestedTextFollows" />
        <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/sli ')]" name="topic.sli">
<!--        <xsl:variable name="olcount" select="count(ancestor-or-self::*[contains(@class,' topic/ol ')])"/> -->
<!--        <xsl:variable name="ulcount" select="count(ancestor-or-self::*[contains(@class,' topic/ul ')])"/> -->
<!--        <xsl:variable name="slcount" select="count(ancestor-or-self::*[contains(@class,' topic/sl ')])"/> -->
<!--	  <xsl:variable name="ddcount" select="count(ancestor-or-self::*[contains(@class,' topic/dd ')])"/> -->
<!--        <xsl:variable name="nestcount" select="number($olcount) + number($ulcount) + number($slcount) + number($ddcount)" /> -->
<!--        <xsl:choose> -->
<!--          <xsl:when test="number($nestcount)=1"> -->
<!--             <xsl:text>  </xsl:text> -->
<!--          </xsl:when> -->
<!--          <xsl:when test="number($nestcount)=2"> -->
<!--             <xsl:text>    </xsl:text> -->
<!--          </xsl:when> -->
<!--          <xsl:when test="number($nestcount)=3"> -->
<!--             <xsl:text>      </xsl:text> -->
<!--          </xsl:when> -->
<!--          <xsl:otherwise> -->
<!--             <xsl:text>        </xsl:text> -->
<!--          </xsl:otherwise> -->
<!--         </xsl:choose> -->
         <xsl:call-template name="indentLI"/>
         <xsl:text>&#x2D; </xsl:text>
         <xsl:apply-templates />
         <xsl:if test="following-sibling::*[contains(@class,' topic/sli ')]">
           <xsl:text>&#xA;</xsl:text>
         </xsl:if>
	</xsl:template>


    <xsl:template match="*[contains(@class,' topic/state ')]" name="topic.state">
        <xsl:value-of select="name()"/><xsl:text>: </xsl:text><xsl:value-of select="@name"/><xsl:text>=</xsl:text><xsl:value-of select="@value"/>
    </xsl:template>

    <xsl:template match="*[contains(@class,' topic/table ')]" name="topic.table">
        <!-- no output -->
        <xsl:call-template name="output-message">
           <xsl:with-param name="msg">Eclipse context-sensitive help files do not support tables.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

	<xsl:template match="*[contains(@class,' topic/term ')]" name="topic.term">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' topic/title ')]" name="topic.title">
        <b><xsl:apply-templates /></b><xsl:text>&#xA;</xsl:text>	
      </xsl:template>

	<xsl:template match="*[contains(@class,' topic/tm ')]" name="topic.tm">
        <!-- output nothing -->
        <!--<xsl:call-template name="output-message">
           <xsl:with-param name="msg">Trademarks are not required in context-sensitive help.</xsl:with-param>
            <xsl:with-param name="msgnum">069</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>-->
        <xsl:call-template name="checkPreceding" />
        <xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
      </xsl:template>

    <xsl:template match="*[contains(@class,' topic/ul ')]" name="topic.ul">
        <xsl:call-template name="twoPrecedingCRs" />
        <!-- jta commenting out 11/03/2006 -->
        <!-- <xsl:if test="parent::*[contains(@class,' topic/li ')] | parent::*[contains(@class,' topic/sli ')]"><xsl:text>&#xA;</xsl:text></xsl:if> -->
           <xsl:apply-templates />
        <xsl:call-template name="isNestedTextFollows" />
        <xsl:call-template name="isFirstChildOfCsbody" />
	</xsl:template>


    <xsl:template match="*[contains(@class,' topic/xref ')]" name="topic.xref" priority="100">
        <xsl:call-template name="checkPreceding" />
        <xsl:choose>
            <xsl:when test="*|text()"><xsl:text>&#34;</xsl:text><xsl:apply-templates select="*|text()"/><xsl:text>&#34;</xsl:text></xsl:when>
            <xsl:otherwise><xsl:call-template name="href"/></xsl:otherwise>
        </xsl:choose>
        <xsl:call-template name="checkFollowing" />
    </xsl:template>

    <xsl:template name="href">
        <!-- below adapted from rel-links.xsl -->
          <xsl:choose>
            <!-- For non-DITA formats - use the href as is -->
            <xsl:when test="(not(@format) and (@type='external' or @scope='external')) or (@format and not(@format='dita' or @format='DITA'))">
                <xsl:value-of select="@href"/>
              </xsl:when>
              <!-- For DITA - process the internal href -->
              <xsl:when test="starts-with(@href,'#')">
                <xsl:call-template name="parsehref">
                  <xsl:with-param name="href" select="@href"/>
                </xsl:call-template>
              </xsl:when>
              <!-- It's to a DITA file - process the file name (adding the html extension)
              and process the rest of the href -->
              <xsl:when test="contains(@href,'.dita')">
                <xsl:value-of select="substring-before(@href,'.dita')"/>.html<xsl:call-template name="parsehref"><xsl:with-param name="href" select="substring-after(@href,'.dita')"/></xsl:call-template>
              </xsl:when>
              <xsl:when test="@href=''"/>
              <xsl:otherwise>
		    <xsl:call-template name="output-message">
                  <xsl:with-param name="msg">Unknown file extension in href: <xsl:value-of select="@href"/>
If this is a link to a non-DITA resource, set the format attribute to match the resource (for example, 'txt', 'pdf', or 'html').
If it's a link to a DITA resource, the file extension must be .dita .</xsl:with-param>
                    <xsl:with-param name="msgnum">015</xsl:with-param>
                  <xsl:with-param name="msgsev">E</xsl:with-param>
                </xsl:call-template>
                <xsl:value-of select="@href"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:template>

    <!-- "/" is not legal in IDs - need to swap it with two underscores -->
    <xsl:template name="parsehref">
         <xsl:param name="href"/>
          <xsl:choose>
           <xsl:when test="contains($href,'/')">
            <xsl:value-of select="substring-before($href,'/')"/>__<xsl:value-of select="substring-after($href,'/')"/>
           </xsl:when>
           <xsl:otherwise>
            <xsl:value-of select="$href"/>
           </xsl:otherwise>
          </xsl:choose>
        </xsl:template>




    <!-- Highlight domain overrides -->

	<xsl:template match="*[contains(@class,' hi-d/b ')]" name="topic.hi-d.b" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>
	
    <xsl:template match="*[contains(@class,' hi-d/i ')]" name="topic.hi-d.i" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

    <xsl:template match="*[contains(@class,' hi-d/sub ')]" name="topic.hi-d.sub" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' hi-d/sup ')]" name="topic.hi-d.sup" priority="100">
        <xsl:text>&#94;</xsl:text><xsl:apply-templates /> <!-- adds caret -->
    </xsl:template>

    <xsl:template match="*[contains(@class,' hi-d/tt ')]" name="topic.hi-d.tt" priority="100">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' hi-d/u ')]" name="topic.hi-d.u" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

    <!-- Programming domain overrides -->

    <xsl:template match="*[contains(@class,' pr-d/codeph ')]" name="topic.pr-d.codeph" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/kwd ')]" name="topic.pr-d.kwd" priority="100">
		<xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/var ')]" name="topic.pr-d.var" priority="100">
		<xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/synph ')]" name="topic.pr-d.synph" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/oper ')]" name="topic.pr-d.oper" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/delim ')]" name="topic.pr-d.delim" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/sep ')]" name="topic.pr-d.sep" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/repsep ')]" name="topic.pr-d.repsep" priority="100">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/option ')]" name="topic.pr-d.option" priority="100">
		<xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/parmname ')]" name="topic.pr-d.parmname" priority="100">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/apiname ')]" name="topic.pr-d.apiname" priority="100">
        <xsl:call-template name="checkPreceding" />
		<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
    </xsl:template>

    <!-- Programming overrides - not found in pr-d.xsl? -->

    <xsl:template match="*[contains(@class,' pr-d/codeblock ')]" name="topic.pr-d.codeblock" priority="100">
        <!-- same as topic.lines -->
	    <xsl:call-template name="br-replace">
	    <xsl:with-param name="brtext" select="."/>
        </xsl:call-template>
        <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/parml ')]" name="topic.pr-d.parml" priority="100">
        <xsl:apply-templates />
        <xsl:call-template name="isFirstChildOfCsbody" />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/plentry ')]" name="topic.pr-d.plentry" priority="100">
        <!-- same as topic.dlentry -->
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/pt ')]" name="topic.pr-d.pt" priority="100">
        <!-- same as topic.dt -->
		<b><xsl:apply-templates /></b><xsl:text>&#xA;</xsl:text>
    </xsl:template>

    <xsl:template match="*[contains(@class,' pr-d/pd ')]" name="topic.pr-d.pd" priority="100">
        <!-- same as topic.dd -->
		<xsl:text>   </xsl:text>
        <xsl:apply-templates />
        <xsl:if test="position()!=last()">
        <xsl:text>&#xA;</xsl:text>
        </xsl:if>
    </xsl:template>



    <!-- Syntax diagram (Programming domain) overrides -->

    <xsl:template match="*[contains(@class, ' pr-d/syntaxdiagram ')]" name="topic.pr-d.syntaxdiagram" priority="100">
        <xsl:call-template name="output-message">
           <xsl:with-param name="msg">Syntax diagrams are not supported in Eclipse context-sensitive help.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

<!-- above template modified; several more syntax diagram templates removed -->



    <!-- Software domain overrides -->

	<xsl:template match="*[contains(@class,' sw-d/cmdname ')]" name="topic.sw-d.cmdname" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>
	
    <xsl:template match="*[contains(@class,' sw-d/filepath ')]" name="topic.sw-d.filepath" priority="100">
        <xsl:call-template name="checkPreceding" />
	<xsl:apply-templates />
        <xsl:call-template name="checkFollowing" />
	</xsl:template>
	
    <xsl:template match="*[contains(@class,' sw-d/msgnum ')]" name="topic.sw-d.msgnum" priority="100">
        <b><xsl:apply-templates /></b>
    </xsl:template>

    <xsl:template match="*[contains(@class,' sw-d/msgph ')]" name="topic.sw-d.msgph" priority="100">
        <b><xsl:apply-templates /></b>
    </xsl:template>

	<xsl:template match="*[contains(@class,' sw-d/systemoutput ')]" name="topic.sw-d.systemoutput" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>
	
 	<xsl:template match="*[contains(@class,' sw-d/userinput ')]" name="topic.sw-d.userinput" priority="100">
        <xsl:call-template name="checkPreceding" />
		<b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' sw-d/varname ')]" name="topic.sw-d.varname" priority="100">
		<b><xsl:apply-templates /></b>
	</xsl:template>
	
	

    <!-- UI domain overrides -->

    <xsl:template match="*[contains(@class,' ui-d/menucascade ')]" name="topic.ui-d.menucascade" priority="100">
        <xsl:apply-templates />
    </xsl:template>

	<xsl:template match="*[contains(@class,' ui-d/screen ')]" name="topic.ui-d.screen" priority="100">
        <!-- no output -->
	</xsl:template>
	
    <xsl:template match="*[contains(@class,' ui-d/shortcut ')]" name="topic.ui-d.shortcut" priority="100">
        <b><xsl:apply-templates /></b>
    </xsl:template>

	<xsl:template match="*[contains(@class,' ui-d/uicontrol ')]" name="topic.ui-d.uicontrol" priority="100">
        <xsl:call-template name="checkPreceding" />
        <xsl:if test="parent::*[contains(@class,' ui-d/menucascade ')] and preceding-sibling::*[contains(@class,' ui-d/uicontrol ')]">
        <xsl:text> > </xsl:text>
        </xsl:if>
        <b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>

	<xsl:template match="*[contains(@class,' ui-d/wintitle ')]" name="topic.ui-d.wintitle" priority="100">
        <xsl:call-template name="checkPreceding" />
        <b><xsl:apply-templates /></b>
        <xsl:call-template name="checkFollowing" />
	</xsl:template>



    <!-- Utilities domain overrides -->

    <!-- imagemap -->
    <xsl:template match="*[contains(@class,' ut-d/imagemap ')]" name="topic.ut-d.imagemap">
        <!-- Process the image to get alternate text -->
        <!--<xsl:call-template name="output-message">
           <xsl:with-param name="msg">Eclipse context-sensitive help files do not support images.</xsl:with-param>
            <xsl:with-param name="msgnum">066</xsl:with-param>
           <xsl:with-param name="msgsev">I</xsl:with-param>
        </xsl:call-template>-->
      <xsl:apply-templates select="*[contains(@class,' topic/image ')]"/>
    </xsl:template>



	<!-- Miscellaneous templates -->
	
    <xsl:template match="text()">
		<xsl:value-of select="normalize-space(.)" />
    </xsl:template>

    <xsl:template name="commonattributes" />
    <xsl:template name="setscale" />
    <xsl:template name="flagit" />
    <xsl:template name="start-revflag" />
    <xsl:template name="end-revflag" />

    <xsl:template name="checkPreceding">
		<xsl:if test="(substring(preceding-sibling::text()[position()=string-length()],1,1)!=' ') and (substring(preceding-sibling::text()[position()=string-length()],1,1)!='(')"><xsl:text> </xsl:text></xsl:if>
	</xsl:template>

    <xsl:template name="checkFollowing">
       	<xsl:if test="(substring(following-sibling::text()[position()=1],1,1)!='.') and (substring(following-sibling::text()[position()=1],1,1)!=',') and (substring(following-sibling::text()[position()=1],1,1)!=';') and (substring(following-sibling::text()[position()=1],1,1)!=':')"><xsl:text> </xsl:text></xsl:if>
    </xsl:template>

    <xsl:template name="twoPrecedingCRs">
        <!-- <xsl:if test="parent::*[contains(@class,' topic/desc ')] | parent::*[contains(@class,' topic/p ')] | parent::*[contains(@class,' topic/note ')] | parent::*[contains(@class,' topic/lq ')] | parent::*[contains(@class,' topic/li ')] | parent::*[contains(@class,' topic/sli ')] | parent::*[contains(@class,' topic/itemgroup ')] | parent::*[contains(@class,' topic/dd ')]"> -->
        <xsl:choose>
          <xsl:when test="parent::*[contains(@class,'topic/li ')] | parent::*[contains(@class, 'topic/dd ')] ">
            <xsl:choose>
              <xsl:when test="self::*[contains(@class,' topic/ul ')] | self::*[contains(@class,' topic/sl ')] | self::*[contains(@class,' topic/ol ')]">
                <xsl:text>&#xA;</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>&#xA;&#xA;</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="parent::*[contains(@class,' topic/p ')]">
            <xsl:if test="preceding-sibling::text()"> <!-- only add 2 cr's if nesting p has text before nested tag -->
              <xsl:text>&#xA;&#xA;</xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:when test="not(parent::*[contains(@class,' cshelp/csbody ')])">
            <xsl:text>&#xA;&#xA;</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <!-- do nothing -->
          </xsl:otherwise>            
        </xsl:choose>
    </xsl:template>
    
<!-- jta: endOfP no longer called 11/03/2006 -->
    <xsl:template name="endOfP">
        <xsl:choose>
        <xsl:when test="parent::*[contains(@class,' topic/p ')] and position()=last()">
        <!-- do nothing -->
        </xsl:when>
        <xsl:when test="parent::*[contains(@class,' topic/p ')] and position()!=last()">
        <xsl:text>&#xA;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
        <xsl:text>&#xA;&#xA;</xsl:text>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!-- jta: listEndOfP no longer called 11/03/2006 -->
    <xsl:template name="listEndOfP">
        <xsl:choose>
	  <!-- jta 10/16/2006 -->
        <xsl:when test="parent::*[contains(@class,' topic/p ')] | parent::*[contains(@class,' topic/desc ')] | parent::*[contains(@class,' topic/note ')] | parent::*[contains(@class,' topic/lq ')] | parent::*[contains(@class,' topic/li ')] | parent::*[contains(@class,' topic/sli ')] | parent::*[contains(@class,' topic/itemgroup ')] | parent::*[contains(@class,' topic/dd ')]">
        <!-- do nothing -->
        </xsl:when>
        <xsl:otherwise>
        <xsl:text>&#xA;&#xA;</xsl:text>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!-- jta 10/16/2006 -->
    <xsl:template name="isFirstChildOfCsbody">
	  <xsl:choose>
           <xsl:when test="parent::*[contains(@class,' cshelp/csbody ')]">
             <xsl:if test="following-sibling::*">
               <xsl:text>&#xA;&#xA;</xsl:text>
             </xsl:if>
	   </xsl:when>
           <xsl:when test="parent::*[contains(@class,' topic/dd ')] | parent::*[contains(@class,' topic/li ')]">
             <xsl:choose>
               <xsl:when test="self::*[contains(@class,' topic/ol ')] | self::*[contains(@class,' topic/sl ')] | self::*[contains(@class,' topic/ul ')]">
                 <!-- do nothing -->
               </xsl:when>
               <xsl:otherwise>
                 <xsl:if test="not(following-sibling::*)"><xsl:text>&#xA;</xsl:text></xsl:if>
               </xsl:otherwise>
             </xsl:choose>
           </xsl:when>
           <xsl:otherwise>
             <!-- do nothing -->
	   </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!-- jta 10/19/2006 -->
    <xsl:template name="indentDL">
        <xsl:variable name="ddcount" select="count(ancestor::*[contains(@class,' topic/dd ')])"/> 
        <xsl:variable name="licount" select="count(ancestor::*[contains(@class,' topic/li ')])"/>
        <xsl:variable name="lqcount" select="count(ancestor::*[contains(@class,' topic/lq ')])"/>
	<xsl:variable name="notecnt" select="count(ancestor::*[contains(@class,' topic/note ')])"/>
	<xsl:variable name="dd_adj"><xsl:choose><xsl:when test="number($ddcount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
	<xsl:variable name="li_adj"><xsl:choose><xsl:when test="number($licount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="indent" select="number($ddcount) + number($dd_adj) + number($licount) + number($li_adj) + number($lqcount) + number($notecnt)" />
        <xsl:choose>
          <xsl:when test="number($indent)=1">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=2">
             <xsl:text>    </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=3">
             <xsl:text>      </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=4">
             <xsl:text>        </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=5">
             <xsl:text>          </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=6">
             <xsl:text>            </xsl:text>
          </xsl:when>
          <xsl:otherwise>
             <!-- no indent -->
          </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
            
    <xsl:template name="indentP">
        <xsl:variable name="ddcount" select="count(ancestor::*[contains(@class,' topic/dd ')])"/> 
        <xsl:variable name="licount" select="count(ancestor::*[contains(@class,' topic/li ')])"/>
        <xsl:variable name="lqcount" select="count(ancestor::*[contains(@class,' topic/lq ')])"/>
	<xsl:variable name="notecnt" select="count(ancestor::*[contains(@class,' topic/note ')])"/>
	<xsl:variable name="p_count" select="count(ancestor::*[contains(@class,' topic/p ')])"/>
	<!-- <xsl:variable name="dd_adj"><xsl:choose><xsl:when test="number($ddcount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable> -->
 	<xsl:variable name="li_adj"><xsl:choose><xsl:when test="number($licount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="indent" select="number($ddcount) + number($licount) + number($li_adj) + number($lqcount) + number($notecnt) + number($p_count)" />
        <xsl:choose>
          <xsl:when test="number($indent)=1">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=2">
             <xsl:text>    </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=3">
             <xsl:text>      </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=4">
             <xsl:text>        </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=5">
             <xsl:text>          </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=6">
             <xsl:text>            </xsl:text>
          </xsl:when>
          <xsl:otherwise>
             <!-- no indent -->
          </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
    
    <xsl:template name="indentNote">
        <xsl:variable name="ddcount" select="count(ancestor::*[contains(@class,' topic/dd ')])"/> 
        <xsl:variable name="licount" select="count(ancestor::*[contains(@class,' topic/li ')])"/>
        <xsl:variable name="lqcount" select="count(ancestor::*[contains(@class,' topic/lq ')])"/>
	<xsl:variable name="notecnt" select="count(ancestor::*[contains(@class,' topic/note ')])"/>
	<xsl:variable name="p_count" select="count(ancestor::*[contains(@class,' topic/p ')])"/>
	<xsl:variable name="dd_adj"><xsl:choose><xsl:when test="number($ddcount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
	<xsl:variable name="li_adj"><xsl:choose><xsl:when test="number($licount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="indent" select="number($ddcount) + number($dd_adj) + number($licount) + number($li_adj) + number($lqcount) + number($notecnt) + number($p_count)" />
        <xsl:choose>
          <xsl:when test="number($indent)=1">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=2">
             <xsl:text>    </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=3">
             <xsl:text>      </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=4">
             <xsl:text>        </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=5">
             <xsl:text>          </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=6">
             <xsl:text>            </xsl:text>
          </xsl:when>
          <xsl:otherwise>
             <!-- no indent -->
          </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
    
    <xsl:template name="indentLQ">
        <xsl:variable name="ddcount" select="count(ancestor::*[contains(@class,' topic/dd ')])"/> 
        <xsl:variable name="licount" select="count(ancestor::*[contains(@class,' topic/li ')])"/>
        <xsl:variable name="lqcount" select="count(ancestor::*[contains(@class,' topic/lq ')])"/>
	<xsl:variable name="notecnt" select="count(ancestor::*[contains(@class,' topic/note ')])"/>
	<xsl:variable name="p_count" select="count(ancestor::*[contains(@class,' topic/p ')])"/>
	<xsl:variable name="dd_adj"><xsl:choose><xsl:when test="number($ddcount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
	<xsl:variable name="li_adj"><xsl:choose><xsl:when test="number($licount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="indent" select="number($ddcount) + number($dd_adj) + number($licount) + number($li_adj) + number($lqcount) + number($notecnt) + number($p_count)" />
        <xsl:choose>
          <xsl:when test="number($indent)=0">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=1">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=2">
             <xsl:text>    </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=3">
             <xsl:text>      </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=4">
             <xsl:text>        </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=5">
             <xsl:text>          </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=6">
             <xsl:text>            </xsl:text>
          </xsl:when>
          <xsl:otherwise>
             <xsl:text>  </xsl:text>
          </xsl:otherwise>
         </xsl:choose>
    </xsl:template>


    <xsl:template name="indentLI">
        <xsl:variable name="ddcount" select="count(ancestor::*[contains(@class,' topic/dd ')])"/> 
        <xsl:variable name="licount" select="count(ancestor::*[contains(@class,' topic/li ')])"/>
        <xsl:variable name="lqcount" select="count(ancestor::*[contains(@class,' topic/lq ')])"/>
	<xsl:variable name="notecnt" select="count(ancestor::*[contains(@class,' topic/note ')])"/>
	<xsl:variable name="p_count" select="count(ancestor::*[contains(@class,' topic/p ')])"/>
	<xsl:variable name="dd_adj"><xsl:choose><xsl:when test="number($ddcount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
	<xsl:variable name="li_adj"><xsl:choose><xsl:when test="number($licount)>0">1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
	<xsl:variable name="p__adj"><xsl:choose><xsl:when test="number($p_count)>0">-1</xsl:when><xsl:otherwise>0</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="indent" select="number($ddcount) + number($dd_adj) + number($licount) + number($li_adj) + number($lqcount) + number($notecnt) + number($p_count) + number($p__adj)" />
        <xsl:choose>
          <xsl:when test="number($indent)=0">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=1">
             <xsl:text>  </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=2">
             <xsl:text>    </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=3">
             <xsl:text>      </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=4">
             <xsl:text>        </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=5">
             <xsl:text>          </xsl:text>
          </xsl:when>
          <xsl:when test="number($indent)=6">
             <xsl:text>            </xsl:text>
          </xsl:when>
          <xsl:otherwise>
             <xsl:text>  </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="isNestedTextFollows">
        <!-- <xsl:if test="parent::*[contains(@class,' topic/dd ')][following-sibling::text()] |
                parent::*[contains(@class,' topic/lq ')][following-sibling::text()] |
                parent::*[contains(@class,' topic/li ')][following-sibling::text()] |
                parent::*[contains(@class,' topic/note ')][following-sibling::text()] |
                parent::*[contains(@class,' topic/p ')][following-sibling::text()]"> -->
        <xsl:if test="not(parent::*[contains(@class,' cshelp/csbody ')]) and following-sibling::text()">
          <xsl:text>&#xA;&#xA;</xsl:text> <!-- jta 11/03/2006: condition when nesting tag has more text -->
        </xsl:if>
    </xsl:template>

    <!-- IBM Copyright - English only output, and only when copyright belongs to IBM -->

    <xsl:template name="ibmcopyright">
      <xsl:variable name="userCopyright">
        <xsl:value-of select="self::dita/*/*[contains(@class,' topic/prolog ')]/
                                       *[contains(@class,' topic/copyright ')]/
                                       *[contains(@class,' topic/copyrholder ')] |
                          *[contains(@class,' topic/prolog ')]/
                                       *[contains(@class,' topic/copyright ')]/
                                       *[contains(@class,' topic/copyrholder ')]"/>
      </xsl:variable>
      <xsl:variable name="copyYears">
        <xsl:value-of select="self::dita/*/*[contains(@class,' topic/prolog ')]/
                                       *[contains(@class,' topic/copyright ')]/
                                       *[contains(@class,' topic/copyryear ')]/@year |
                          *[contains(@class,' topic/prolog ')]/
                                       *[contains(@class,' topic/copyright ')]/
                                       *[contains(@class,' topic/copyryear ')]/@year"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="normalize-space($userCopyright)!='' and not(contains($userCopyright,'IBM'))">
          <!-- P018721: user copyright is specified, and it does not contain IBM; do not put out the comment -->
        </xsl:when>
        <xsl:when test="(self::dita/*/*[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')] |
                     *[contains(@class,' topic/prolog ')]/*[contains(@class,' topic/copyright ')]) and
                    normalize-space($userCopyright)='' and normalize-space($copyYears)=''">
          <!-- P018721: if user forces empty copyright, empty year, do not put out the copyright comment -->
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="childlang">
            <xsl:choose>
              <xsl:when test="self::dita">
                <xsl:for-each select="*[1]"><xsl:call-template name="getLowerCaseLang"/></xsl:for-each>
              </xsl:when>
              <xsl:otherwise><xsl:call-template name="getLowerCaseLang"/></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:choose>
            <xsl:when test="$childlang='en-us' or $childlang='en'">
              <xsl:comment> All rights reserved. Licensed Materials Property of IBM </xsl:comment><xsl:value-of select="$newline"/>
              <xsl:comment> US Government Users Restricted Rights </xsl:comment><xsl:value-of select="$newline"/>
              <xsl:comment> Use, duplication or disclosure restricted by </xsl:comment><xsl:value-of select="$newline"/>
              <xsl:comment> GSA ADP Schedule Contract with IBM Corp. </xsl:comment><xsl:value-of select="$newline"/>
            </xsl:when>
            <xsl:otherwise/>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
