package com.revzz.sap.cockpit.support;

import com.revzz.sap.cockpit.domain.HourlyBucket;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Renders the hourly traffic trend as a self-contained SVG.
 *
 * <p>Same approach as the impact graph in the other projects: no graph
 * library, no client-side JavaScript, no CDN. The markup is built on the
 * server and embedded straight into the page, so it works on an isolated
 * network and can be asserted on in a test.</p>
 */
@Component
public class TrendChartRenderer {

    /**
     * A stacked bar per hour: the total traffic, with the failed part in
     * red on top of it.
     */
    public String render(List<HourlyBucket> buckets, int width, int height) {
        if (buckets == null || buckets.isEmpty()) {
            return "";
        }

        int chartWidth = width - 120;
        int chartHeight = height - 70;
        int barWidth = Math.max(8, chartWidth / buckets.size() - 6);
        int max = 1;
        for (HourlyBucket bucket : buckets) {
            max = Math.max(max, bucket.getTotal());
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width)
                .append(" ").append(height).append("' class='trend' role='img' ")
                .append("aria-label='Hourly traffic trend'>");

        // Baseline and the maximum marker.
        svg.append("<line x1='50' y1='").append(chartHeight + 20).append("' x2='")
                .append(chartWidth + 40).append("' y2='").append(chartHeight + 20)
                .append("' class='t-axis'/>");
        svg.append("<text x='50' y='22' class='t-head'>MESSAGES PER HOUR</text>");
        svg.append("<text x='").append(chartWidth + 30).append("' y='22' class='t-max'>peak ")
                .append(max).append("</text>");

        for (int index = 0; index < buckets.size(); index++) {
            HourlyBucket bucket = buckets.get(index);
            int x = 50 + index * (barWidth + 6);
            int totalHeight = (int) Math.round((double) bucket.getTotal() / max * chartHeight);
            int errorHeight = bucket.getTotal() == 0
                    ? 0
                    : (int) Math.round((double) bucket.getErrors() / bucket.getTotal() * totalHeight);
            int y = chartHeight + 20 - totalHeight;

            svg.append("<rect x='").append(x).append("' y='").append(y)
                    .append("' width='").append(barWidth).append("' height='").append(totalHeight)
                    .append("' rx='2' class='t-bar'/>");
            if (errorHeight > 0) {
                svg.append("<rect x='").append(x).append("' y='").append(y)
                        .append("' width='").append(barWidth).append("' height='").append(errorHeight)
                        .append("' rx='2' class='t-bar-error'/>");
            }
            svg.append("<text x='").append(x + barWidth / 2).append("' y='")
                    .append(chartHeight + 36).append("' class='t-label' text-anchor='middle'>")
                    .append(bucket.getLabel()).append("</text>");
            if (bucket.getErrors() > 0) {
                svg.append("<text x='").append(x + barWidth / 2).append("' y='").append(y - 5)
                        .append("' class='t-errors' text-anchor='middle'>")
                        .append(bucket.getErrors()).append("</text>");
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
