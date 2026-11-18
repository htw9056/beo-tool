<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- ******************************************************************** -->
<!-- Neptan.xsl                                                           -->
<!-- Export eines Trimble Access Jobs (JobXML) als NEPTAN-                -->
<!-- Beobachtungsdatei (*.beo)                                            -->
<!--                                                                      -->
<!-- Satzaufbau (feste Spalten, rechtsbuendig):                           -->
<!--   Spalte   1-  5  Satzart   0. = Kopf, 10. = Standpunkt, 20. = Ziel  -->
<!--   Spalte   6- 15  Kennwert  (Kopf: Vorgabewert, sonst 0.0)           -->
<!--   Spalte  16- 29  Punktnummer                                        -->
<!--   Spalte  30- 49  Schraegstrecke [m], 4 Nachkommastellen             -->
<!--   Spalte  50- 65  Horizontalrichtung [gon], 5 Nachkommastellen       -->
<!--   Spalte  66- 80  Zenitwinkel [gon], 5 Nachkommastellen              -->
<!--   Spalte  81- 94  Instrumentenhoehe (10.) bzw. Zielhoehe (20.) [m]   -->
<!--   Spalte  95-102  Punktcode                                          -->
<!--   Spalte 103-143  Text: Prismenkonstante und atmosphaerische ppm     -->
<!--   ab Spalte 147   Kommentar (Instrument, Temperatur, Luftdruck, Job) -->
<!--                                                                      -->
<!-- Die Schraegstrecke ist wie im Stylesheet "GDM job" bereits um        -->
<!-- Prismenkonstante, atmosphaerische ppm und Stationsmassstab           -->
<!-- korrigiert (Stationsmassstab nicht bei Anschlussmessungen).          -->
<!-- Winkel werden unabhaengig von der Job-Einstellung in gon und         -->
<!-- Strecken in Metern ausgegeben.                                       -->
<!-- ******************************************************************** -->

<xsl:output method="text" omit-xml-declaration="yes" encoding="ISO-8859-1"/>

<xsl:decimal-format name="Standard"
                    decimal-separator="."
                    grouping-separator=","
                    infinity="Infinity"
                    minus-sign="-"
                    NaN="?"
                    percent="%"
                    zero-digit="0"
                    digit="#"
                    pattern-separator=";" />

<xsl:variable name="DecPl0" select="'#0'"/>
<xsl:variable name="DecPl1" select="'#0.0'"/>
<xsl:variable name="DecPl2" select="'#0.00'"/>
<xsl:variable name="DecPl3" select="'#0.000'"/>
<xsl:variable name="DecPl4" select="'#0.0000'"/>
<xsl:variable name="DecPl5" select="'#0.00000'"/>

<!-- Dateiendung der Exportdatei -->
<xsl:variable name="fileExt" select="'beo'"/>

<!-- Abfragen im Exportdialog von Trimble Access -->
<xsl:variable name="userField1" select="'headerValue|Kennwert Kopfzeile (Satz 0.)|Integer|0|9999'"/>
<xsl:variable name="headerValue" select="24"/>
<xsl:variable name="userField2" select="'writeComments|Kommentare ausgeben|StringMenu|2|Ja|Nein'"/>
<xsl:variable name="writeComments" select="'Ja'"/>

<!-- Spaltenbreiten -->
<xsl:variable name="wType"   select="5"/>
<xsl:variable name="wValue"  select="10"/>
<xsl:variable name="wName"   select="14"/>
<xsl:variable name="wDist"   select="20"/>
<xsl:variable name="wHz"     select="16"/>
<xsl:variable name="wVt"     select="15"/>
<xsl:variable name="wHeight" select="14"/>
<xsl:variable name="wCode"   select="8"/>
<xsl:variable name="wInfo"   select="41"/>  <!-- Breite des Prisma/ppm Textes -->

<xsl:variable name="Blanks" select="'                                                                                '"/>

<!-- Winkel stehen in der JobXML Datei immer in Dezimalgrad -->
<xsl:variable name="DegToGon" select="10 div 9"/>

<!-- Druckeinheit nur fuer den Kommentar -->
<xsl:variable name="PressUnit" select="/JOBFile/Environment/DisplaySettings/PressureUnits"/>
<xsl:variable name="PressConvFactor">
  <xsl:choose>
    <xsl:when test="$PressUnit='InchHg'">0.029529921</xsl:when>
    <xsl:when test="$PressUnit='mmHg'">0.75006</xsl:when>
    <xsl:otherwise>1.0</xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="PressUnitStr">
  <xsl:choose>
    <xsl:when test="$PressUnit='InchHg'">inHg</xsl:when>
    <xsl:when test="$PressUnit='mmHg'">mmHg</xsl:when>
    <xsl:otherwise>hPa</xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<!-- Schluessel fuer schnelle Suche -->
<xsl:key name="tgtID-search"   match="/JOBFile/FieldBook/TargetRecord"     use="@ID"/>
<xsl:key name="atmosID-search" match="/JOBFile/FieldBook/AtmosphereRecord" use="@ID"/>
<xsl:key name="reducedPoint-search" match="/JOBFile/Reductions/Point"      use="Name"/>
<xsl:key name="obsByStation"
         match="/JOBFile/FieldBook/PointRecord[Circle and not(Deleted = 'true')]"
         use="StationID"/>


<!-- **************************************************************** -->
<!-- ************************** Hauptteil *************************** -->
<!-- **************************************************************** -->
<xsl:template match="/">
  <!-- Kopfzeile -->
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="'0.'"/>
    <xsl:with-param name="width" select="$wType"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="concat($headerValue, '.')"/>
    <xsl:with-param name="width" select="$wValue"/>
  </xsl:call-template>
  <xsl:call-template name="NewLine"/>

  <!-- Alle Standpunkte in der gemessenen Reihenfolge -->
  <xsl:apply-templates select="JOBFile/FieldBook/StationRecord"/>
</xsl:template>


<!-- **************************************************************** -->
<!-- ******************** Standpunkt (Satz 10.) ********************* -->
<!-- **************************************************************** -->
<xsl:template match="StationRecord">
  <xsl:variable name="stnName" select="string(StationName)"/>

  <xsl:variable name="stnCode">
    <xsl:value-of select="key('reducedPoint-search', $stnName)[1]/Code"/>
  </xsl:variable>

  <xsl:variable name="instHt">
    <xsl:call-template name="NumStr">
      <xsl:with-param name="val" select="TheodoliteHeight"/>
      <xsl:with-param name="fmt" select="$DecPl3"/>
    </xsl:call-template>
  </xsl:variable>

  <!-- Atmosphaere des Standpunktes -->
  <xsl:variable name="atmos" select="key('atmosID-search', AtmosphereID)[1]"/>

  <xsl:variable name="ppm">
    <xsl:choose>
      <xsl:when test="string(number($atmos/PPM)) = 'NaN'">0</xsl:when>
      <xsl:otherwise><xsl:value-of select="$atmos/PPM"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="stnSF">
    <xsl:choose>
      <xsl:when test="string(number(ScaleFactor)) = 'NaN'">1</xsl:when>
      <xsl:otherwise><xsl:value-of select="ScaleFactor"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Kommentartexte, werden auf die ersten Zielzeilen verteilt -->
  <xsl:variable name="serial" select="string(preceding-sibling::InstrumentRecord[1]/Serial)"/>
  <xsl:variable name="cmt1">
    <xsl:if test="$serial != ''">
      <xsl:value-of select="concat('Instrumentennummer: ', $serial)"/>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="cmt2">
    <xsl:if test="string(number($atmos/Temperature)) != 'NaN'">
      <xsl:value-of select="concat('Temperatur (&#176;C): ', format-number($atmos/Temperature, $DecPl1, 'Standard'))"/>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="cmt3">
    <xsl:if test="string(number($atmos/Pressure)) != 'NaN'">
      <xsl:value-of select="concat('Luftdruck (', $PressUnitStr, '): ', format-number($atmos/Pressure * $PressConvFactor, $DecPl2, 'Standard'))"/>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="cmt4" select="concat('Messjob: ', /JOBFile/@jobName)"/>

  <!-- Standpunktzeile -->
  <xsl:call-template name="LineStart">
    <xsl:with-param name="type" select="'10.'"/>
    <xsl:with-param name="name" select="$stnName"/>
  </xsl:call-template>
  <xsl:value-of select="substring($Blanks, 1, $wDist + $wHz + $wVt)"/>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$instHt"/>
    <xsl:with-param name="width" select="$wHeight"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$stnCode"/>
    <xsl:with-param name="width" select="$wCode"/>
  </xsl:call-template>
  <xsl:call-template name="NewLine"/>

  <!-- Alle Beobachtungen dieses Standpunktes -->
  <xsl:for-each select="key('obsByStation', @ID)">
    <xsl:call-template name="Observation">
      <xsl:with-param name="ppm" select="$ppm"/>
      <xsl:with-param name="stnSF" select="$stnSF"/>
      <xsl:with-param name="comment">
        <xsl:if test="$writeComments = 'Ja'">
          <xsl:choose>
            <xsl:when test="position() = 1"><xsl:value-of select="$cmt1"/></xsl:when>
            <xsl:when test="position() = 2"><xsl:value-of select="$cmt2"/></xsl:when>
            <xsl:when test="position() = 3"><xsl:value-of select="$cmt3"/></xsl:when>
            <xsl:when test="position() = 4"><xsl:value-of select="$cmt4"/></xsl:when>
          </xsl:choose>
        </xsl:if>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:for-each>
</xsl:template>


<!-- **************************************************************** -->
<!-- ******************** Zielpunkt (Satz 20.) ********************** -->
<!-- **************************************************************** -->
<xsl:template name="Observation">
  <xsl:param name="ppm" select="0"/>
  <xsl:param name="stnSF" select="1"/>
  <xsl:param name="comment" select="''"/>

  <xsl:variable name="target" select="key('tgtID-search', TargetID)[1]"/>

  <xsl:variable name="prismConst">
    <xsl:choose>
      <xsl:when test="string(number($target/PrismConstant)) = 'NaN'">0</xsl:when>
      <xsl:otherwise><xsl:value-of select="$target/PrismConstant"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Stationsmassstab nicht an Anschlussmessungen anbringen -->
  <xsl:variable name="sf">
    <xsl:choose>
      <xsl:when test="Classification = 'BackSight'">1</xsl:when>
      <xsl:otherwise><xsl:value-of select="$stnSF"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Schraegstrecke inkl. Prismenkonstante, ppm und Massstab -->
  <xsl:variable name="sdStr">
    <xsl:variable name="sd" select="Circle/EDMDistance"/>
    <xsl:if test="string(number($sd)) != 'NaN'">
      <xsl:value-of select="format-number(($sd + $prismConst + ($ppm div 1000000.0 * $sd)) * $sf, $DecPl4, 'Standard')"/>
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="hzStr">
    <xsl:call-template name="GonStr">
      <xsl:with-param name="deg" select="Circle/HorizontalCircle"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="vtStr">
    <xsl:call-template name="GonStr">
      <xsl:with-param name="deg" select="Circle/VerticalCircle"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="tgtHt">
    <xsl:call-template name="NumStr">
      <xsl:with-param name="val" select="$target/TargetHeight"/>
      <xsl:with-param name="fmt" select="$DecPl3"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="info">
    <xsl:value-of select="concat('  Prisma ', format-number($prismConst, $DecPl4, 'Standard'))"/>
    <xsl:value-of select="'  athmosph&#228;rische ppm'"/>
    <xsl:call-template name="PadLeft">
      <xsl:with-param name="str" select="format-number($ppm, $DecPl0, 'Standard')"/>
      <xsl:with-param name="width" select="4"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="LineStart">
    <xsl:with-param name="type" select="'20.'"/>
    <xsl:with-param name="name" select="string(Name)"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$sdStr"/>
    <xsl:with-param name="width" select="$wDist"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$hzStr"/>
    <xsl:with-param name="width" select="$wHz"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$vtStr"/>
    <xsl:with-param name="width" select="$wVt"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$tgtHt"/>
    <xsl:with-param name="width" select="$wHeight"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="string(Code)"/>
    <xsl:with-param name="width" select="$wCode"/>
  </xsl:call-template>
  <xsl:value-of select="$info"/>
  <xsl:if test="string($comment) != ''">
    <xsl:value-of select="substring($Blanks, 1, $wInfo - string-length($info))"/>
    <xsl:value-of select="concat('   ', $comment)"/>
  </xsl:if>
  <xsl:call-template name="NewLine"/>
</xsl:template>


<!-- **************************************************************** -->
<!-- ************* Zeilenanfang: Satzart, 0.0, Punktnummer ********** -->
<!-- **************************************************************** -->
<xsl:template name="LineStart">
  <xsl:param name="type"/>
  <xsl:param name="name"/>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$type"/>
    <xsl:with-param name="width" select="$wType"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="'0.0'"/>
    <xsl:with-param name="width" select="$wValue"/>
  </xsl:call-template>
  <xsl:call-template name="PadLeft">
    <xsl:with-param name="str" select="$name"/>
    <xsl:with-param name="width" select="$wName"/>
  </xsl:call-template>
</xsl:template>


<!-- **************************************************************** -->
<!-- ********* Winkel Dezimalgrad -> gon mit 5 Nachkommastellen ***** -->
<!-- **************************************************************** -->
<xsl:template name="GonStr">
  <xsl:param name="deg"/>
  <xsl:if test="string(number($deg)) != 'NaN'">
    <xsl:variable name="gon" select="format-number($deg * $DegToGon, $DecPl5, 'Standard')"/>
    <xsl:choose>
      <xsl:when test="$gon = '400.00000'">0.00000</xsl:when>
      <xsl:otherwise><xsl:value-of select="$gon"/></xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>


<!-- **************************************************************** -->
<!-- ************ Zahl formatieren, leer wenn kein Wert ************* -->
<!-- **************************************************************** -->
<xsl:template name="NumStr">
  <xsl:param name="val"/>
  <xsl:param name="fmt"/>
  <xsl:if test="string(number($val)) != 'NaN'">
    <xsl:value-of select="format-number($val, $fmt, 'Standard')"/>
  </xsl:if>
</xsl:template>


<!-- **************************************************************** -->
<!-- ************* Text rechtsbuendig auf Breite auffuellen ********* -->
<!-- **************************************************************** -->
<xsl:template name="PadLeft">
  <xsl:param name="str"/>
  <xsl:param name="width"/>
  <xsl:choose>
    <xsl:when test="string-length($str) &lt; $width">
      <xsl:value-of select="concat(substring($Blanks, 1, $width - string-length($str)), $str)"/>
    </xsl:when>
    <xsl:otherwise>
      <!-- Wert ist breiter als die Spalte: mindestens ein Trennzeichen -->
      <xsl:value-of select="concat(' ', $str)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- **************************************************************** -->
<!-- ************************** Neue Zeile ************************** -->
<!-- **************************************************************** -->
<xsl:template name="NewLine">
<xsl:text>&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
