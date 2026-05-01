package Utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * GreenLedger Front Office Design System
 * Complete color palette, typography, shadows, gradients and component builders.
 * Exact replica of the GreenLedger web application design.
 */
public class GreenLedgerTheme {

    // =========================================================
    // SECTION 1: COLOR PALETTE
    // =========================================================

    // Emerald (Primary Green)
    public static final Color EMERALD_50  = Color.web("#f0fdf4");
    public static final Color EMERALD_100 = Color.web("#dcfce7");
    public static final Color EMERALD_500 = Color.web("#10b981");
    public static final Color EMERALD_600 = Color.web("#059669");
    public static final Color EMERALD_700 = Color.web("#047857");
    public static final Color EMERALD_800 = Color.web("#065f46");

    // Teal
    public static final Color TEAL_400 = Color.web("#2dd4bf");
    public static final Color TEAL_500 = Color.web("#14b8a6");

    // Sky / Blue
    public static final Color SKY_400  = Color.web("#38bdf8");
    public static final Color BLUE_50  = Color.web("#eff6ff");
    public static final Color BLUE_100 = Color.web("#dbeafe");
    public static final Color BLUE_500 = Color.web("#3b82f6");
    public static final Color BLUE_600 = Color.web("#2563eb");

    // Amber / Orange
    public static final Color AMBER_50  = Color.web("#fffbeb");
    public static final Color AMBER_100 = Color.web("#fef3c7");
    public static final Color AMBER_500 = Color.web("#f59e0b");
    public static final Color AMBER_600 = Color.web("#d97706");
    public static final Color AMBER_700 = Color.web("#b45309");
    public static final Color AMBER_800 = Color.web("#92400e");

    // Rose / Red
    public static final Color ROSE_500 = Color.web("#f43f5e");
    public static final Color ROSE_600 = Color.web("#e11d48");
    public static final Color RED_100  = Color.web("#fee2e2");
    public static final Color RED_200  = Color.web("#fecdd3");
    public static final Color RED_800  = Color.web("#991b1b");

    // Indigo / Purple
    public static final Color INDIGO_400  = Color.web("#818cf8");
    public static final Color PURPLE_100  = Color.web("#ede9fe");
    public static final Color PURPLE_500  = Color.web("#8b5cf6");

    // Slate (Neutrals)
    public static final Color SLATE_50  = Color.web("#f8fafc");
    public static final Color SLATE_100 = Color.web("#f1f5f9");
    public static final Color SLATE_200 = Color.web("#e2e8f0");
    public static final Color SLATE_300 = Color.web("#cbd5e1");
    public static final Color SLATE_400 = Color.web("#94a3b8");
    public static final Color SLATE_500 = Color.web("#64748b");
    public static final Color SLATE_600 = Color.web("#475569");
    public static final Color SLATE_700 = Color.web("#334155");
    public static final Color SLATE_800 = Color.web("#1e293b");
    public static final Color SLATE_900 = Color.web("#0f172a");

    // Gray
    public static final Color GRAY_50  = Color.web("#f9fafb");
    public static final Color GRAY_100 = Color.web("#f3f4f6");
    public static final Color GRAY_200 = Color.web("#e5e7eb");
    public static final Color GRAY_300 = Color.web("#d1d5db");
    public static final Color GRAY_400 = Color.web("#9ca3af");
    public static final Color GRAY_500 = Color.web("#6b7280");
    public static final Color GRAY_600 = Color.web("#4b5563");
    public static final Color GRAY_700 = Color.web("#374151");
    public static final Color GRAY_900 = Color.web("#111827");

    public static final Color WHITE = Color.WHITE;

    // =========================================================
    // SECTION 2: TYPOGRAPHY
    // =========================================================

    public static final int FONT_XS   = 10;
    public static final int FONT_SM   = 11;
    public static final int FONT_BASE = 13;
    public static final int FONT_MD   = 14;
    public static final int FONT_LG   = 16;
    public static final int FONT_XL   = 18;
    public static final int FONT_2XL  = 20;
    public static final int FONT_3XL  = 24;
    public static final int FONT_4XL  = 28;
    public static final int FONT_5XL  = 32;
    public static final int FONT_6XL  = 38;

    public static Font font(int size) {
        return Font.font("Inter", size);
    }

    public static Font fontBold(int size) {
        return Font.font("Inter", FontWeight.BOLD, size);
    }

    public static Font fontSemiBold(int size) {
        return Font.font("Segoe UI Semibold", FontWeight.BOLD, size);
    }

    // =========================================================
    // SECTION 3: SPACING
    // =========================================================

    public static final int SPACE_1  = 4;
    public static final int SPACE_2  = 8;
    public static final int SPACE_3  = 12;
    public static final int SPACE_4  = 16;
    public static final int SPACE_5  = 20;
    public static final int SPACE_6  = 24;
    public static final int SPACE_7  = 28;
    public static final int SPACE_8  = 32;
    public static final int SPACE_10 = 40;
    public static final int SPACE_12 = 48;
    public static final int SPACE_16 = 64;

    // =========================================================
    // SECTION 4: BORDER RADIUS (as CSS strings)
    // =========================================================

    public static final int RADIUS_SM   = 6;
    public static final int RADIUS_MD   = 8;
    public static final int RADIUS_LG   = 10;
    public static final int RADIUS_XL   = 12;
    public static final int RADIUS_2XL  = 14;
    public static final int RADIUS_3XL  = 16;
    public static final int RADIUS_4XL  = 18;
    public static final int RADIUS_FULL = 999;

    // =========================================================
    // SECTION 5: SHADOWS
    // =========================================================

    public static DropShadow shadowSm() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(15, 23, 42, 0.06));
        s.setRadius(8);
        s.setOffsetY(2);
        return s;
    }

    public static DropShadow shadowMd() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(15, 23, 42, 0.08));
        s.setRadius(20);
        s.setOffsetY(8);
        return s;
    }

    public static DropShadow shadowLg() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(15, 23, 42, 0.08));
        s.setRadius(30);
        s.setOffsetY(10);
        return s;
    }

    public static DropShadow shadowXl() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(15, 23, 42, 0.08));
        s.setRadius(34);
        s.setOffsetY(16);
        return s;
    }

    public static DropShadow shadow2xl() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(0, 0, 0, 0.25));
        s.setRadius(60);
        s.setOffsetY(25);
        return s;
    }

    public static DropShadow shadowEmerald() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(5, 150, 105, 0.18));
        s.setRadius(18);
        s.setOffsetY(8);
        return s;
    }

    public static DropShadow shadowEmeraldHover() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(5, 150, 105, 0.24));
        s.setRadius(24);
        s.setOffsetY(12);
        return s;
    }

    public static DropShadow shadowEmeraldHeader() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(5, 150, 105, 0.18));
        s.setRadius(40);
        s.setOffsetY(18);
        return s;
    }

    public static DropShadow shadowAmber() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(217, 119, 6, 0.30));
        s.setRadius(12);
        s.setOffsetY(4);
        return s;
    }

    public static DropShadow shadowAmberHover() {
        DropShadow s = new DropShadow();
        s.setColor(Color.rgb(217, 119, 6, 0.40));
        s.setRadius(16);
        s.setOffsetY(6);
        return s;
    }

    // =========================================================
    // SECTION 6: GRADIENTS
    // =========================================================

    public static LinearGradient gradientEmeraldPrimary() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, EMERALD_600),
            new Stop(1, EMERALD_700));
    }

    public static LinearGradient gradientEmeraldHero() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, EMERALD_600),
            new Stop(0.5, EMERALD_500),
            new Stop(1, TEAL_500));
    }

    public static LinearGradient gradientEmeraldLight() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, EMERALD_50),
            new Stop(1, BLUE_50));
    }

    public static LinearGradient gradientAmberEscrow() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, AMBER_600),
            new Stop(1, AMBER_700));
    }

    public static LinearGradient gradientSlateCard() {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, WHITE),
            new Stop(1, SLATE_50));
    }

    public static LinearGradient gradientDecisionBuy() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ecfeff")),
            new Stop(1, EMERALD_100));
    }

    public static LinearGradient gradientDecisionSell() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#fff1f2")),
            new Stop(1, RED_100));
    }

    public static LinearGradient gradientDecisionHold() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, SLATE_50),
            new Stop(1, SLATE_200));
    }

    // =========================================================
    // SECTION 7: CSS STYLE HELPERS
    // =========================================================

    /** Convert Color to CSS hex string */
    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255));
    }

    /** Convert Color to rgba CSS string */
    public static String toRgba(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255),
            alpha);
    }

    /** Emerald gradient as CSS inline style */
    public static String cssGradientEmerald() {
        return "linear-gradient(to bottom right, #059669, #047857)";
    }

    /** Amber gradient as CSS inline style */
    public static String cssGradientAmber() {
        return "linear-gradient(to bottom right, #d97706, #b45309)";
    }

    /** Hero gradient as CSS inline style */
    public static String cssGradientHero() {
        return "linear-gradient(to bottom right, #059669, #10b981, #14b8a6)";
    }

    // =========================================================
    // SECTION 8: COMPONENT BUILDERS
    // =========================================================

    /**
     * KPI Card — matches the web app stat card exactly.
     * Left border colored, animated counter, icon circle.
     */
    public static VBox createKpiCard(String label, String value, String sub, Color accentColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(SPACE_5));
        card.setStyle(String.format(
            "-fx-background-color: white;" +
            "-fx-background-radius: %d;" +
            "-fx-border-radius: %d;" +
            "-fx-border-color: %s %s %s %s;" +
            "-fx-border-width: 1 1 1 3;",
            RADIUS_3XL, RADIUS_3XL,
            toHex(SLATE_200), toHex(SLATE_200), toHex(SLATE_200), toHex(accentColor)
        ));
        card.setEffect(shadowSm());

        // Label
        Label lbl = new Label(label.toUpperCase());
        lbl.setFont(fontBold(FONT_XS));
        lbl.setTextFill(SLATE_400);

        // Value
        Label val = new Label(value);
        val.setFont(fontBold(FONT_4XL));
        val.setTextFill(SLATE_800);

        // Sub
        Label subLbl = new Label(sub);
        subLbl.setFont(font(FONT_XS));
        subLbl.setTextFill(SLATE_400);

        card.getChildren().addAll(lbl, val, subLbl);

        // Hover lift animation
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-2);
            card.setEffect(shadowMd());
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setEffect(shadowSm());
        });

        return card;
    }

    /**
     * Hero Banner — emerald gradient with white text.
     */
    public static StackPane createHeroBanner(String title, String subtitle, String roleBadge) {
        StackPane hero = new StackPane();
        hero.setPadding(new Insets(SPACE_6));
        hero.setStyle(
            "-fx-background-color: " + cssGradientHero() + ";" +
            "-fx-background-radius: " + RADIUS_4XL + ";" +
            "-fx-border-radius: " + RADIUS_4XL + ";"
        );
        hero.setEffect(shadowEmeraldHeader());

        VBox content = new VBox(SPACE_2);
        content.setAlignment(Pos.CENTER_LEFT);

        // Role badge
        Label badge = new Label(roleBadge.toUpperCase());
        badge.setFont(fontBold(FONT_SM));
        badge.setTextFill(WHITE);
        badge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.15);" +
            "-fx-border-color: rgba(255,255,255,0.30);" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 999;" +
            "-fx-border-radius: 999;" +
            "-fx-padding: 5 12 5 12;"
        );

        Label titleLbl = new Label(title);
        titleLbl.setFont(fontBold(FONT_4XL));
        titleLbl.setTextFill(WHITE);

        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setFont(font(FONT_MD));
        subtitleLbl.setTextFill(Color.rgb(255, 255, 255, 0.85));

        content.getChildren().addAll(badge, titleLbl, subtitleLbl);
        hero.getChildren().add(content);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);

        return hero;
    }

    /**
     * Primary Button — emerald gradient.
     */
    public static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setFont(fontBold(FONT_BASE));
        btn.setTextFill(WHITE);
        btn.setStyle(
            "-fx-background-color: " + cssGradientEmerald() + ";" +
            "-fx-background-radius: " + RADIUS_LG + ";" +
            "-fx-border-radius: " + RADIUS_LG + ";" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-cursor: hand;" +
            "-fx-background-insets: 0;"
        );
        btn.setEffect(shadowEmerald());

        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #047857, #065f46);" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setEffect(shadowEmeraldHover());
            btn.setTranslateY(-1);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: " + cssGradientEmerald() + ";" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setEffect(shadowEmerald());
            btn.setTranslateY(0);
        });

        return btn;
    }

    /**
     * Secondary Button — slate style.
     */
    public static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setFont(fontBold(FONT_BASE));
        btn.setTextFill(SLATE_700);
        btn.setStyle(
            "-fx-background-color: #f8fafc;" +
            "-fx-border-color: #cbd5e1;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: " + RADIUS_LG + ";" +
            "-fx-border-radius: " + RADIUS_LG + ";" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-cursor: hand;" +
            "-fx-background-insets: 0;"
        );

        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: #e2e8f0;" +
                "-fx-border-color: #94a3b8;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setTextFill(SLATE_900);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setTextFill(SLATE_700);
        });

        return btn;
    }

    /**
     * Amber/Escrow Button.
     */
    public static Button createAmberButton(String text) {
        Button btn = new Button(text);
        btn.setFont(fontBold(FONT_BASE));
        btn.setTextFill(WHITE);
        btn.setStyle(
            "-fx-background-color: " + cssGradientAmber() + ";" +
            "-fx-background-radius: " + RADIUS_LG + ";" +
            "-fx-border-radius: " + RADIUS_LG + ";" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-cursor: hand;" +
            "-fx-background-insets: 0;"
        );
        btn.setEffect(shadowAmber());

        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #b45309, #92400e);" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setEffect(shadowAmberHover());
            btn.setTranslateY(-2);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: " + cssGradientAmber() + ";" +
                "-fx-background-radius: " + RADIUS_LG + ";" +
                "-fx-border-radius: " + RADIUS_LG + ";" +
                "-fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand;" +
                "-fx-background-insets: 0;"
            );
            btn.setEffect(shadowAmber());
            btn.setTranslateY(0);
        });

        return btn;
    }

    /**
     * Status Badge Label.
     */
    public static Label createStatusBadge(String status) {
        Label badge = new Label(status.toUpperCase());
        badge.setFont(fontBold(FONT_SM));
        badge.setPadding(new Insets(5, 12, 5, 12));

        switch (status.toUpperCase()) {
            case "APPROVED":
                badge.setTextFill(Color.web("#15803d"));
                badge.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 999; -fx-border-radius: 999;");
                break;
            case "PENDING":
            case "IN_PROGRESS":
                badge.setTextFill(Color.web("#92400e"));
                badge.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 999; -fx-border-radius: 999;");
                break;
            case "REJECTED":
                badge.setTextFill(Color.web("#991b1b"));
                badge.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 999; -fx-border-radius: 999;");
                break;
            case "SUBMITTED":
                badge.setTextFill(Color.web("#1e40af"));
                badge.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 999; -fx-border-radius: 999;");
                break;
            case "DRAFT":
            default:
                badge.setTextFill(SLATE_600);
                badge.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 999; -fx-border-radius: 999;");
                break;
        }

        return badge;
    }

    /**
     * Generic Card VBox with shadow and hover lift.
     */
    public static VBox createCard() {
        VBox card = new VBox(SPACE_3);
        card.setPadding(new Insets(SPACE_5));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: " + RADIUS_3XL + ";" +
            "-fx-border-radius: " + RADIUS_3XL + ";" +
            "-fx-border-color: #f1f5f9;" +
            "-fx-border-width: 1;"
        );
        card.setEffect(shadowSm());

        card.setOnMouseEntered(e -> {
            card.setEffect(shadowMd());
            card.setTranslateY(-2);
        });
        card.setOnMouseExited(e -> {
            card.setEffect(shadowSm());
            card.setTranslateY(0);
        });

        return card;
    }

    /**
     * Animated counter — animates a label from 0 to target value over 900ms.
     */
    public static void animateCounter(Label label, double targetValue, String suffix) {
        final double[] current = {0};
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(900), e -> {
                label.setText(String.format("%.0f%s", targetValue, suffix));
            })
        );

        // Manual frame-by-frame animation using cubic ease-out
        int frames = 60;
        for (int i = 1; i <= frames; i++) {
            final int frame = i;
            double progress = (double) i / frames;
            // Cubic ease-out: 1 - (1-p)^3
            double eased = 1 - Math.pow(1 - progress, 3);
            double value = targetValue * eased;
            timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(900.0 * i / frames),
                    new KeyValue(label.textProperty(),
                        String.format("%.0f%s", value, suffix)))
            );
        }

        timeline.play();
    }

    /**
     * Apply hover lift effect to any Region.
     */
    public static void applyHoverLift(Region node, double liftAmount) {
        DropShadow normalShadow = shadowSm();
        DropShadow hoverShadow = shadowMd();
        node.setEffect(normalShadow);

        node.setOnMouseEntered(e -> {
            node.setTranslateY(-liftAmount);
            node.setEffect(hoverShadow);
        });
        node.setOnMouseExited(e -> {
            node.setTranslateY(0);
            node.setEffect(normalShadow);
        });
    }

    /**
     * Apply the GreenLedger front office theme to a node's inline style.
     * Use for containers that need the page background gradient.
     */
    public static String pageBackgroundStyle() {
        return "-fx-background-color: #f8fafc;";
    }

    /**
     * Sidebar style string.
     */
    public static String sidebarStyle() {
        return "-fx-background-color: #0f172a;" +
               "-fx-pref-width: 240;" +
               "-fx-min-width: 240;" +
               "-fx-max-width: 240;" +
               "-fx-padding: 24 18 24 18;";
    }
}
