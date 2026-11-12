package com.revzz.sap.upgrade.support;

import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.Severity;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;

import java.util.List;

/**
 * Server-side SVG charts.
 *
 * <p>The charts are drawn on the server, as plain SVG markup, with no chart
 * library, no CDN and no client-side JavaScript. That is a deliberate choice
 * for a tool that has to run inside a corporate landscape where outbound
 * internet access is often blocked, and where an upgrade board has to be
 * able to save the page and still see the evidence.</p>
 */
public final class UpgradeChartRenderer {

    private UpgradeChartRenderer() {
    }

    /** The colour of a severity band. */
    public static String severityColour(Severity severity) {
        if (severity == null) {
            return "#63748a";
        }
        switch (severity) {
            case CRITICAL:
                return "#d7263d";
            case HIGH:
                return "#e8710a";
            case MEDIUM:
                return "#b8860b";
            case LOW:
                return "#1a7f5a";
            default:
                return "#2e9e6b";
        }
    }

    /** The colour of a component state. */
    public static String statusColour(String status) {
        if (ComponentStatus.STATUS_PATCHED.equals(status)) {
            return "#2e9e6b";
        }
        if (ComponentStatus.STATUS_NEW.equals(status)) {
            return "#0a6ed1";
        }
        if (ComponentStatus.STATUS_OBSOLETE.equals(status)) {
            return "#d7263d";
        }
        if (ComponentStatus.STATUS_RELEASED.equals(status)) {
            return "#8b1a2b";
        }
        return "#63748a";
    }

    /**
     * A horizontal bar chart of the upgrade readiness score of every system.
     *
     * @param assessments the systems, already sorted worst first
     */
    public static String readinessBars(List<UpgradeAssessment> assessments) {
        int width = 900;
        int labelWidth = 92;
        int valueWidth = 74;
        int rowHeight = 36;
        int top = 46;
        int height = top + Math.max(1, assessments.size()) * rowHeight + 16;

        int barWidth = width - labelWidth - valueWidth - 24;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"readiness\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" ")
                .append("aria-label=\"Upgrade readiness score by system\">");

        svg.append("<text x=\"0\" y=\"18\" class=\"c-head\">UPGRADE READINESS BY SYSTEM</text>");
        svg.append("<text x=\"").append(width).append("\" y=\"18\" class=\"c-scale\" ")
                .append("text-anchor=\"end\">0 &#8211; 100</text>");

        svg.append("<rect x=\"").append(labelWidth).append("\" y=\"").append(top - 14)
                .append("\" width=\"").append(barWidth)
                .append("\" height=\"").append(Math.max(1, assessments.size()) * rowHeight)
                .append("\" fill=\"#eef2f7\" rx=\"4\"/>");

        for (int i = 0; i < assessments.size(); i++) {
            UpgradeAssessment assessment = assessments.get(i);
            int y = top + i * rowHeight;
            int filled = Math.round(barWidth
                    * Math.max(0, Math.min(100, assessment.getScore())) / 100f);
            String colour = severityColour(assessment.getSeverity());

            svg.append("<text x=\"0\" y=\"").append(y + 5)
                    .append("\" class=\"c-label\">")
                    .append(esc(assessment.getSystem().getSystemId())).append("</text>");
            svg.append("<rect x=\"").append(labelWidth).append("\" y=\"").append(y - 11)
                    .append("\" width=\"").append(Math.max(2, filled))
                    .append("\" height=\"20\" fill=\"").append(colour).append("\" rx=\"3\"/>");
            svg.append("<text x=\"").append(labelWidth + barWidth + 8).append("\" y=\"").append(y + 4)
                    .append("\" class=\"c-value\" fill=\"").append(colour).append("\">")
                    .append(assessment.getScore()).append("</text>");
            svg.append("<text x=\"").append(labelWidth + barWidth + 40).append("\" y=\"").append(y + 4)
                    .append("\" class=\"c-sub\">").append(assessment.getSeverity().name())
                    .append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * The component matrix: every component against every system.
     *
     * <p>This is the picture an upgrade project actually needs - one row per
     * component, one column per system, and the cell shows the patch level
     * the component is on and the state it is in. The obsolete components
     * and the ones left behind are visible at a glance.</p>
     *
     * @param assessments the systems
     * @param components  the component catalogue
     */
    public static String componentMatrix(List<UpgradeAssessment> assessments,
                                         List<SoftwareComponent> components) {
        int labelWidth = 190;
        int cellWidth = 180;
        int cellHeight = 36;
        int top = 70;
        int width = labelWidth + Math.max(1, assessments.size()) * cellWidth + 16;
        int height = top + Math.max(1, components.size()) * cellHeight + 20;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"matrix\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" ")
                .append("aria-label=\"Component state per system\">");

        svg.append("<text x=\"0\" y=\"18\" class=\"c-head\">COMPONENT STATE MATRIX</text>");
        svg.append("<text x=\"").append(width).append("\" y=\"18\" class=\"c-scale\" ")
                .append("text-anchor=\"end\">patch level per cell</text>");

        // the system headers, with the level the system itself is on
        for (int c = 0; c < assessments.size(); c++) {
            UpgradeAssessment assessment = assessments.get(c);
            int x = labelWidth + c * cellWidth;
            svg.append("<text x=\"").append(x + cellWidth / 2).append("\" y=\"42\" ")
                    .append("class=\"c-head-center\">")
                    .append(esc(assessment.getSystem().getSystemId())).append("</text>");
            svg.append("<text x=\"").append(x + cellWidth / 2).append("\" y=\"58\" ")
                    .append("class=\"c-sub-center\">level ")
                    .append(assessment.getSystem().getPatchLevel()).append("</text>");
        }

        for (int r = 0; r < components.size(); r++) {
            SoftwareComponent component = components.get(r);
            int y = top + r * cellHeight;

            svg.append("<text x=\"0\" y=\"").append(y + 23)
                    .append("\" class=\"c-label\">")
                    .append(esc(component.getComponentId())).append("</text>");

            for (int c = 0; c < assessments.size(); c++) {
                UpgradeAssessment assessment = assessments.get(c);
                int x = labelWidth + c * cellWidth;

                ComponentStatus status = null;
                for (ComponentStatus candidate : assessment.getComponentStatuses()) {
                    if (candidate.getComponentId().equals(component.getComponentId())) {
                        status = candidate;
                    }
                }

                if (status == null) {
                    svg.append("<rect x=\"").append(x + 4).append("\" y=\"").append(y + 4)
                            .append("\" width=\"").append(cellWidth - 8).append("\" height=\"")
                            .append(cellHeight - 8).append("\" rx=\"5\" fill=\"#f1f4f9\" "
                                    + "stroke=\"#dfe5ee\"/>");
                    svg.append("<text x=\"").append(x + cellWidth / 2).append("\" y=\"")
                            .append(y + 23).append("\" class=\"c-cell-missing\">not installed")
                            .append("</text>");
                    continue;
                }

                String colour = statusColour(status.getStatus());
                svg.append("<rect x=\"").append(x + 4).append("\" y=\"").append(y + 4)
                        .append("\" width=\"").append(cellWidth - 8).append("\" height=\"")
                        .append(cellHeight - 8).append("\" rx=\"5\" fill=\"").append(colour)
                        .append("\"/>");
                svg.append("<text x=\"").append(x + cellWidth / 2).append("\" y=\"")
                        .append(y + 23).append("\" class=\"c-cell\">")
                        .append(status.getPatchLevel()).append("</text>");
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /** Escapes the five characters that matter in XML text and attributes. */
    public static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&apos;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
