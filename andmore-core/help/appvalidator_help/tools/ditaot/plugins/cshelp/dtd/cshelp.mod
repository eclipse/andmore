<!--
 |  (C) Copyright IBM Corporation 2005, 2006 All Rights Reserved.
 | This file is a specialization of DITA 3.0. See license.txt
 | for disclaimers and permissions.
 |
 | This file is part of the DITA Open Toolkit project hosted on
 | Sourceforge.net. See the accompanying license.txt file for 
 | applicable licenses.
 |
 | The Darwin Information Typing Architecture (DITA) was orginated by
 | IBM's XML Workgroup and ID Workbench tools team.
 |
 | Refer to this file by the following public identfier or an appropriate
 | system identifier:
 |
 |   PUBLIC "-//IBM//ELEMENTS DITA CSHelp//EN"
 |
 | Release history (vrm):
 |   1.0.0 Initial release, December 2005
 *-->

<!ENTITY DTDVersion 'V1.1.1' >


<!-- Specialization of declared elements -->

<!ENTITY % csprolog      "csprolog">
<!ENTITY % csmetadata    "csmetadata">
<!ENTITY % cswindowtitle "cswindowtitle">
<!ENTITY % cswidgetlabel "cswidgetlabel"> 
<!ENTITY % csbody        "csbody">
<!ENTITY % cshelp-info-types "%info-types;">

<!-- declared here, defined later -->
<!ENTITY included-domains "">

<!--doc:The <cshelp> element is the top-level element for a topic that corresponds to an Eclipse Help context (a brief description and link that appear after the user presses a Help button). To create one Eclipse context.xml file, create one DITA file with a root element of <cshelp> in which you next further <cshelp> topics, one for each context-sensitive help item. Only the content of the nested elements is output; the root element is a required, but empty container.
Category: CS Help plug-in elements-->
<!ELEMENT cshelp (%title;, (%titlealts;)?, (%shortdesc;), (%csprolog;)?, %csbody;, (%related-links;)?, (%cshelp-info-types;)* )>
<!ATTLIST cshelp  id ID #REQUIRED
                  conref CDATA #IMPLIED
                  %select-atts;
                  outputclass CDATA #IMPLIED
                  xml:lang NMTOKEN #IMPLIED
                  DTDVersion CDATA #FIXED "&DTDVersion;"
                  domains CDATA "&included-domains;"
>

<!--doc:The optional <csprolog> element contains <csmetadata> elements to describe the UI related to this context. 
Category: CS Help plug-in elements-->
<!ELEMENT csprolog ((%author;)*,(%source;)?,(%publisher;)?,(%copyright;)*,(%critdates;)?,(%permissions;)?,(%csmetadata;)*, (%resourceid;)*)>

<!--doc:The optional <csmetadata> element contains <cswindowtitle> and <cswidgetlabel> elements. 
Category: CS Help plug-in elements-->
<!ELEMENT csmetadata ((%audience;)*,(%cswindowtitle;)?,(%cswidgetlabel;)?,(%category;)*,(%keywords;)*,(%prodinfo;)*,(%othermeta;)*)>

<!-- define a custom block type with predefined topic content types -->

<!-- txt.incl minus footnotes and index entries -->

<!--doc:The <csbody> element contains the body of the CS Help topic.  All the elements allowed in <body> are allowed in <csbody>; however, some result in no output because of Eclipse Help limitations.  For example, table, simpletable, image, figure, xref, indexterm, indextermref, footnote, object are not output. Many in-line elements display using the <b> tag. Lists are supported, but nested lists are not recommended.
Category: CS Help plug-in elements-->
<!ELEMENT csbody  (%body.cnt;)*>
<!ATTLIST csbody  %id-atts;
                  translate (yes|no) #IMPLIED
                  xml:lang NMTOKEN #IMPLIED
                  outputclass CDATA #IMPLIED
>

<!--doc:The optional <cswindowtitle> element identifies the window associated with this context-sensitive help item. This content does not appear in the output context file. 
Category: CS Help plug-in elements-->
<!ELEMENT cswindowtitle (#PCDATA)>

<!--doc:The optional <cswidgetlabel> element identifies the UI widget within the window associated with this context-sensitive help item. This content does not appear in the output context file. 
Category: CS Help plug-in elements-->
<!ELEMENT cswidgetlabel (#PCDATA)>

<!--specialization attributes-->

<!ATTLIST cshelp        %global-atts; class  CDATA "- topic/topic cshelp/cshelp ">
<!ATTLIST csbody        %global-atts; class  CDATA "- topic/body cshelp/csbody ">
<!ATTLIST csprolog      %global-atts; class  CDATA "- topic/prolog cshelp/csprolog ">
<!ATTLIST csmetadata    %global-atts; class  CDATA "- topic/metadata cshelp/csmetadata ">
<!ATTLIST cswindowtitle %global-atts; class  CDATA "- topic/category cshelp/cswindowtitle ">
<!ATTLIST cswidgetlabel %global-atts; class  CDATA "- topic/category cshelp/cswidgetlabel ">
