package com.example.mondrain.report

import com.example.mondrain.data.DrainageCrossingEntity
import com.example.mondrain.data.ProjectEntity
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings

object ReportEngine {

    fun generateTextReport(
        project: ProjectEntity,
        crossings: List<DrainageCrossingEntity>,
        language: AppLanguage = MonStrings.currentLanguage
    ): String {
        val isMn = language == AppLanguage.MONGOLIAN
        val sb = StringBuilder()

        val sep = "=".repeat(72)
        val subSep = "-".repeat(72)

        sb.appendLine(sep)
        sb.appendLine(if (isMn) "  АВТО ЗАМЫН УС ЗАЙЛУУЛАХ БАЙГУУЛАМЖИЙН ИНЖЕНЕРЧЛЭЛИЙН ТООЦООНЫ ТАЙЛАН" else "  ROAD DRAINAGE & HYDRAULIC ENGINEERING CALCULATION REPORT")
        sb.appendLine(if (isMn) "  МОН-ДРАЙН Инженер Программ (MON-DRAIN Engineer Native Android)" else "  MON-DRAIN Engineer Native Android Application")
        sb.appendLine(sep)
        sb.appendLine()

        // Section 1: Project Metadata
        sb.appendLine(if (isMn) "1. ТӨСЛИЙН ЕРӨНХИЙ МЭДЭЭЛЭЛ / GENERAL PROJECT INFO" else "1. GENERAL PROJECT INFORMATION")
        sb.appendLine(subSep)
        sb.appendLine(if (isMn) "• Төслийн нэр:       ${project.projectName}" else "• Project Name:     ${project.projectName}")
        sb.appendLine(if (isMn) "• Төслийн дугаар:    ${project.projectNumber}" else "• Project Number:   ${project.projectNumber}")
        sb.appendLine(if (isMn) "• Захиалагч:         ${project.client}" else "• Client:           ${project.client}")
        sb.appendLine(if (isMn) "• Зураг төсөлч:      ${project.designer}" else "• Designer:         ${project.designer}")
        sb.appendLine(if (isMn) "• Байршил:           ${project.location}, ${project.province}, ${project.district}" else "• Location:         ${project.location}, ${project.province}, ${project.district}")
        sb.appendLine(if (isMn) "• Замын нэр, хэсэг:  ${project.roadName} (${project.roadSection})" else "• Road & Section:   ${project.roadName} (${project.roadSection})")
        sb.appendLine(if (isMn) "• Трассын урт:       ${String.format("%.2f", project.roadLengthMeters / 1000.0)} км" else "• Road Length:      ${String.format("%.2f", project.roadLengthMeters / 1000.0)} km")
        sb.appendLine(if (isMn) "• Огноо:             ${project.dateCreated}" else "• Date:             ${project.dateCreated}")
        sb.appendLine(if (isMn) "• Координатын систем: ${project.coordinateSystem}" else "• Coordinate CRS:   ${project.coordinateSystem}")
        sb.appendLine(if (isMn) "• Тооцооны хувилбар: ${project.calculationVersion}" else "• Calc Version:     ${project.calculationVersion}")
        sb.appendLine()

        // Section 2: DEM & Terrain Model
        sb.appendLine(if (isMn) "2. ДИЖИТАЛ ӨНДРИЙН ЗАГВАР (DEM) / TERRAIN DATA" else "2. DIGITAL ELEVATION MODEL (DEM)")
        sb.appendLine(subSep)
        sb.appendLine(if (isMn) "• DEM Эх сурвалж:    ${project.demSourceInfo}" else "• DEM Source:       ${project.demSourceInfo}")
        sb.appendLine(if (isMn) "• Горим:             ${project.demMode}" else "• Mode:             ${project.demMode}")
        sb.appendLine(if (isMn) "• Нарийвчлал:        1 arc-second (~30 метр)" else "• Resolution:       1 arc-second (~30 m)")
        sb.appendLine(if (isMn) "• Гадаргын шинж:     ${project.fieldObservations}" else "• Terrain Notes:    ${project.fieldObservations}")
        sb.appendLine()

        // Section 3: Hydrology & Norms
        sb.appendLine(if (isMn) "3. ГИДРОЛОГИ БА ХУР БОРООНЫ ТООЦОО / HYDROLOGY" else "3. HYDROLOGICAL ANALYSIS & CRITERIA")
        sb.appendLine(subSep)
        sb.appendLine(if (isMn) "• Тооцооны стандарт: ${project.hydrologyMethod}" else "• Governing Code:   ${project.hydrologyMethod}")
        sb.appendLine(if (isMn) "• Үерийн магадлал:   P = ${project.returnPeriodPercent}% (Авто замын II зэрэг)" else "• Return Period:    P = ${project.returnPeriodPercent}% (Road Category II)")
        sb.appendLine(if (isMn) "• Борооны үзүүлэлт:  ${project.rainfallInfo}" else "• Rainfall Param:   ${project.rainfallInfo}")
        sb.appendLine()

        // Section 4: Culvert Schedule Table
        sb.appendLine(if (isMn) "4. УС ЗАЙЛУУЛАХ ХООЛОЙН ХУВААРЬ БА ХҮЧИН ЧАДАЛ / CULVERT SCHEDULE" else "4. CULVERT SIZING SCHEDULE & HYDRAULIC CAPACITY")
        sb.appendLine(subSep)
        sb.appendLine(
            String.format(
                "%-10s | %-12s | %-7s | %-7s | %-18s | %-7s | %-6s | %-6s | %-10s",
                if (isMn) "Пикет" else "Station",
                if (isMn) "Координат" else "Coord",
                if (isMn) "F (км²)" else "Area km²",
                if (isMn) "Q тооц" else "Q req",
                if (isMn) "Хоолойн төрөл, хэмжээ" else "Culvert Type & Size",
                if (isMn) "Q чадал" else "Q cap",
                if (isMn) "HW (м)" else "HW (m)",
                if (isMn) "V (м/с)" else "V (m/s)",
                if (isMn) "Төлөв" else "Status"
            )
        )
        sb.appendLine(subSep)

        var totalAdequate = 0
        for (c in crossings) {
            if (c.isAdequate) totalAdequate++
            val typeStr = if (c.culvertType.contains("BOX")) {
                "${c.barrels}x(${c.culvertSpanOrDiameterM}x${c.culvertHeightM}м)"
            } else {
                "${c.barrels}xØ${c.culvertSpanOrDiameterM}м"
            }
            sb.appendLine(
                String.format(
                    "%-10s | %-12s | %-7.2f | %-7.2f | %-18s | %-7.2f | %-6.2f | %-6.2f | %-10s",
                    c.stationLabel,
                    "${String.format("%.3f", c.latitude)},${String.format("%.3f", c.longitude)}",
                    c.catchmentAreaKm2,
                    c.designDischargeM3s,
                    typeStr,
                    c.capacityDischargeM3s,
                    c.headwaterM,
                    c.flowVelocityMs,
                    if (c.isAdequate) (if (isMn) "ХАНГАСАН" else "PASS") else (if (isMn) "ХҮРЭЛЦЭХГҮЙ" else "FAIL")
                )
            )
        }
        sb.appendLine(subSep)
        sb.appendLine()

        // Section 5: Engineering Recommendations
        sb.appendLine(if (isMn) "5. ИНЖЕНЕРИЙН ДҮГНЭЛТ БА ЗӨВЛӨМЖ / RECOMMENDATIONS" else "5. ENGINEERING CONCLUSIONS & RECOMMENDATIONS")
        sb.appendLine(subSep)
        sb.appendLine(if (isMn) "• Нийт ус зайлуулах огтлолын тоо: ${crossings.size}" else "• Total Drainage Crossings: ${crossings.size}")
        sb.appendLine(if (isMn) "• Шаардлага хангасан байгууламж:    $totalAdequate / ${crossings.size}" else "• Adequate Structures:         $totalAdequate / ${crossings.size}")
        sb.appendLine(if (isMn) "• Угаагдлын хамгаалалт:" else "• Scour Protection:")
        for (c in crossings) {
            if (c.scourProtectionRequired) {
                sb.appendLine(if (isMn) "  - ${c.stationLabel}: Урсгалын хурд ${c.flowVelocityMs} м/с > 2.0 м/с тул гаралт дээр 6-8 метр чулуун өнгөлгөө (Rip-rap) шаардлагатай."
                else "  - ${c.stationLabel}: Velocity ${c.flowVelocityMs} m/s > 2.0 m/s; 6-8 m stone rip-rap apron required.")
            }
        }
        sb.appendLine()
        sb.appendLine(if (isMn) "Баталгаажуулсан инженер: _____________________ (${project.designer})" else "Certified Engineer: _____________________ (${project.designer})")
        sb.appendLine(sep)

        return sb.toString()
    }

    fun generateHtmlReport(
        project: ProjectEntity,
        crossings: List<DrainageCrossingEntity>,
        language: AppLanguage = MonStrings.currentLanguage
    ): String {
        val isMn = language == AppLanguage.MONGOLIAN
        val title = if (isMn) "Авто замын ус зайлуулах байгууламжийн тооцооны тайлан" else "Road Drainage & Culvert Engineering Report"

        val rows = StringBuilder()
        for (c in crossings) {
            val typeStr = if (c.culvertType.contains("BOX")) {
                "${c.barrels} × (${c.culvertSpanOrDiameterM} × ${c.culvertHeightM} м)"
            } else {
                "${c.barrels} × Ø${c.culvertSpanOrDiameterM} м"
            }
            val statusColor = if (c.isAdequate) "#2E7D32" else "#C62828"
            val statusText = if (c.isAdequate) (if (isMn) "ХАНГАСАН" else "ADEQUATE") else (if (isMn) "ХҮРЭЛЦЭХГҮЙ" else "INADEQUATE")

            rows.append("""
                <tr>
                    <td style="font-weight:bold;">${c.stationLabel}</td>
                    <td>${String.format("%.4f", c.latitude)}, ${String.format("%.4f", c.longitude)}</td>
                    <td>${String.format("%.2f", c.catchmentAreaKm2)}</td>
                    <td>${String.format("%.2f", c.slopePercent)}%</td>
                    <td style="font-weight:bold;">${String.format("%.2f", c.designDischargeM3s)}</td>
                    <td>$typeStr</td>
                    <td>${String.format("%.2f", c.capacityDischargeM3s)}</td>
                    <td>${String.format("%.2f", c.headwaterM)} (HW/D: ${String.format("%.2f", c.headwaterRatio)})</td>
                    <td>${String.format("%.2f", c.flowVelocityMs)}</td>
                    <td style="color:${statusColor}; font-weight:bold;">$statusText</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>$title</title>
                <style>
                    body { font-family: -apple-system, Roboto, sans-serif; margin: 24px; color: #1E293B; background: #FFF; }
                    .header { border-bottom: 3px solid #002244; padding-bottom: 12px; margin-bottom: 20px; }
                    h1 { color: #002244; margin: 0 0 6px 0; font-size: 22px; }
                    h2 { color: #1E3E62; font-size: 16px; margin: 18px 0 8px 0; border-left: 4px solid #00E5FF; padding-left: 8px; }
                    .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; background: #F8FAFC; padding: 14px; border-radius: 6px; font-size: 13px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
                    th { background: #0B192C; color: #FFF; padding: 8px; text-align: left; }
                    td { padding: 8px; border-bottom: 1px solid #E2E8F0; }
                    tr:nth-child(even) { background: #F8FAFC; }
                    .signature { margin-top: 36px; display: flex; justify-content: space-between; font-size: 13px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$title</h1>
                    <div style="color: #64748B; font-size: 13px;">MON-DRAIN Engineer Native Android Engineering System</div>
                </div>

                <h2>${if (isMn) "Төслийн мэдээлэл" else "Project Information"}</h2>
                <div class="meta-grid">
                    <div><b>${if (isMn) "Төслийн нэр:" else "Project Name:"}</b> ${project.projectName}</div>
                    <div><b>${if (isMn) "Төслийн дугаар:" else "Project Number:"}</b> ${project.projectNumber}</div>
                    <div><b>${if (isMn) "Захиалагч:" else "Client:"}</b> ${project.client}</div>
                    <div><b>${if (isMn) "Зураг төсөлч:" else "Designer:"}</b> ${project.designer}</div>
                    <div><b>${if (isMn) "Байршил:" else "Location:"}</b> ${project.location}, ${project.province}, ${project.district}</div>
                    <div><b>${if (isMn) "Замын хэсэг:" else "Road Section:"}</b> ${project.roadName} (${project.roadSection})</div>
                    <div><b>${if (isMn) "Координатын систем:" else "Coordinate CRS:"}</b> ${project.coordinateSystem}</div>
                    <div><b>${if (isMn) "DEM Эх сурвалж:" else "DEM Source:"}</b> ${project.demSourceInfo} (${project.demMode})</div>
                    <div><b>${if (isMn) "Үерийн магадлал:" else "Return Period:"}</b> P = ${project.returnPeriodPercent}% (${project.hydrologyMethod})</div>
                    <div><b>${if (isMn) "Огноо:" else "Date:"}</b> ${project.dateCreated}</div>
                </div>

                <h2>${if (isMn) "Ус зайлуулах хоолойн гидравлик тооцооны хуудас" else "Culvert Hydraulic Calculation Schedule"}</h2>
                <table>
                    <thead>
                        <tr>
                            <th>${if (isMn) "Пикет" else "Station"}</th>
                            <th>${if (isMn) "Координат (Lat, Lon)" else "Coordinates"}</th>
                            <th>${if (isMn) "Талбай F (км²)" else "Area (km²)"}</th>
                            <th>${if (isMn) "Налуу J (%)" else "Slope (%)"}</th>
                            <th>${if (isMn) "Q тооц (м³/с)" else "Q req (m³/s)"}</th>
                            <th>${if (isMn) "Хоолойн төрөл" else "Culvert"}</th>
                            <th>${if (isMn) "Q чадал (м³/с)" else "Q cap (m³/s)"}</th>
                            <th>${if (isMn) "HW (м)" else "HW (m)"}</th>
                            <th>${if (isMn) "Хурд V (м/с)" else "Velocity"}</th>
                            <th>${if (isMn) "Төлөв" else "Status"}</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <div class="signature">
                    <div><b>${if (isMn) "Зураг төсөл зохиосон:" else "Engineered by:"}</b> ${project.designer}</div>
                    <div><b>${if (isMn) "Хянасан Ерөнхий Инженер:" else "Chief Engineer:"}</b> _______________________</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
