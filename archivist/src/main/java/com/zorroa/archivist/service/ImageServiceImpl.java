package com.zorroa.archivist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by chambers on 7/8/16.
 */
@Service
public class ImageServiceImpl implements ImageService {

    @Value("${archivist.watermark.enabled:false}")
    private Boolean watermarkEnabled;

    @Value("${archivist.watermark.min-proxy-width:384}")
    private int watermarkMinProxyWidth;

    @Value("${archivist.watermark.template}")
    private String watermarkTemplate;

    @Value("${archivist.watermark.font-size:6}")
    private int watermarkFontSize;

    @Value("${archivist.watermark.font-name:Arial Black}")
    private String watermarkFontName;
    private Font watermarkFont;

    @PostConstruct
    public void init() {
        watermarkFont = new Font(watermarkFontName, Font.PLAIN, watermarkFontSize);
    }

    @Override
    public BufferedImage watermark(BufferedImage src) {
        if (!watermarkEnabled) {
            return src;
        }

        if (src.getWidth() <= watermarkMinProxyWidth) {
            return src;
        }

        // Replace variables in the watermarkTemplte once we have session & asset
        String text = watermarkTemplate;

        // Draw AA text composited on top of the image
        // FIXME: Wrap strings that are too long
        Graphics2D g2d = src.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g2d.setComposite(c);
        g2d.setPaint(Color.white);
        g2d.setFont(watermarkFont);
        float x = (src.getWidth() - g2d.getFontMetrics(watermarkFont).stringWidth(text)) / 2;
        float y = 1.1f * g2d.getFontMetrics(watermarkFont).getHeight();
        g2d.drawString(text, x, y);

        return src;
    }
}
