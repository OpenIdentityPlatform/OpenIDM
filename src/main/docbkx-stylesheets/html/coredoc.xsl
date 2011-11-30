<?xml version="1.0" encoding="UTF-8"?>
<!--
  ! CCPL HEADER START
  !
  ! This work is licensed under the Creative Commons
  ! Attribution-NonCommercial-NoDerivs 3.0 Unported License.
  ! To view a copy of this license, visit
  ! http://creativecommons.org/licenses/by-nc-nd/3.0/
  ! or send a letter to Creative Commons, 444 Castro Street,
  ! Suite 900, Mountain View, California, 94041, USA.
  !
  ! You can also obtain a copy of the license at
  ! legal/CC-BY-NC-ND.txt.
  ! See the License for the specific language governing permissions
  ! and limitations under the License.
  !
  ! If applicable, add the following below this CCPL HEADER, with the fields
  ! enclosed by brackets "[]" replaced with your own identifying information:
  !      Portions Copyright [yyyy] [name of copyright owner]
  !
  ! CCPL HEADER END
  !
  !      Copyright 2011 ForgeRock AS
  !    
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:xslthl="http://xslthl.sf.net" exclude-result-prefixes="xslthl"
 version="1.0">
 <xsl:import href="urn:docbkx:stylesheet" />
 <xsl:import href="urn:docbkx:stylesheet/highlight.xsl" />

<!-- <xsl:param name="chunk.section.depth" select="0" />-->
<!-- <xsl:param name="chunker.output.encoding">UTF-8</xsl:param>-->
<!-- <xsl:param name="chunker.output.indent">yes</xsl:param>-->
 <xsl:param name="generate.legalnotice.link" select="1" />
<!-- <xsl:param name="generate.revhistory.link" select="1" />-->
 <xsl:param name="root.filename">index</xsl:param>
 <xsl:param name="use.id.as.filename" select="1" />	
<!-- <xsl:template name="user.footer.content">-->
<!--  <a>-->
<!--   <xsl:attribute name="href">-->
<!--    <xsl:apply-templates select="//legalnotice[1]" mode="chunk-filename" />-->
<!--   </xsl:attribute>-->
<!--   <xsl:apply-templates select="//copyright[1]" mode="titlepage.mode" />-->
<!--  </a>-->
<!-- </xsl:template>-->
  <xsl:param name="generate.toc">
   appendix  nop
   article/appendix  nop
   article   nop
   book      toc,title,figure,equation
   chapter   nop
   part      toc,title
   preface   nop
   qandadiv  nop
   qandaset  nop
   reference nop
   sect1     nop
   sect2     nop
   sect3     nop
   sect4     nop
   sect5     nop
   section   nop
   set       toc,title
  </xsl:param>
 <xsl:param name="toc.section.depth" select="0" />

 <xsl:template match="xslthl:keyword" mode="xslthl">
   <strong class="hl-keyword">
     <xsl:apply-templates mode="xslthl"/>
   </strong>
 </xsl:template>
 <xsl:template match="xslthl:string" mode="xslthl">
   <strong class="hl-string">
     <em style="color: #f58220">
       <xsl:apply-templates mode="xslthl"/>
     </em>
   </strong>
 </xsl:template>
 <xsl:template match="xslthl:comment" mode="xslthl">
   <em class="hl-comment" style="color: #868686">
     <xsl:apply-templates mode="xslthl"/>
   </em>
 </xsl:template>
 <xsl:template match="xslthl:directive" mode="xslthl">
   <span class="hl-directive" style="color: #868686">
     <xsl:apply-templates mode="xslthl"/>
   </span>
 </xsl:template>
 <xsl:template match="xslthl:tag" mode="xslthl">
   <strong class="hl-tag" style="color: #f58220">
     <xsl:apply-templates mode="xslthl"/>
   </strong>
 </xsl:template>
 <xsl:template match="xslthl:attribute" mode="xslthl">
   <span class="hl-attribute" style="color: #868686">
     <xsl:apply-templates mode="xslthl"/>
   </span>
 </xsl:template>
 <xsl:template match="xslthl:value" mode="xslthl">
   <span class="hl-value" style="color: #333">
     <xsl:apply-templates mode="xslthl"/>
   </span>
 </xsl:template>
 <xsl:template match="xslthl:html" mode="xslthl">
   <strong>
     <em style="color: #f58220">
       <xsl:apply-templates mode="xslthl"/>
     </em>
   </strong>
 </xsl:template>
 <xsl:template match="xslthl:xslt" mode="xslthl">
   <strong style="color: #868686">
     <xsl:apply-templates mode="xslthl"/>
   </strong>
 </xsl:template>
 <!-- Not emitted since XSLTHL 2.0 -->
 <xsl:template match="xslthl:section" mode="xslthl">
   <strong>
     <xsl:apply-templates mode="xslthl"/>
   </strong>
 </xsl:template>
 <xsl:template match="xslthl:number" mode="xslthl">
   <span class="hl-number">
     <xsl:apply-templates mode="xslthl"/>
   </span>
 </xsl:template>
 <xsl:template match="xslthl:annotation" mode="xslthl">
   <em>
     <span class="hl-annotation" style="color: #868686">
       <xsl:apply-templates mode="xslthl"/>
     </span>
   </em>
 </xsl:template>
 <!-- Not sure which element will be in final XSLTHL 2.0 -->
 <xsl:template match="xslthl:doccomment|xslthl:doctype" mode="xslthl">
   <strong class="hl-tag" style="color: #868686">
     <xsl:apply-templates mode="xslthl"/>
   </strong>
 </xsl:template>
</xsl:stylesheet>
