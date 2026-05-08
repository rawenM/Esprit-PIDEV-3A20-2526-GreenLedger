# GreenLedger Theme & Design System Guide

## 📋 Overview

This guide explains how to apply the GreenLedger design system to your JavaFX application, including both the **Front Office** (green/teal theme) and **Admin Backoffice** (blue theme).

---

## 🎨 Color Palettes

### Front Office Theme (Green/Teal)
```css
Primary: #059669 (Emerald-600)
Primary Hover: #047857 (Emerald-700)
Success: #10b981 (Emerald-500)
Background: #f0fdf4 (Emerald-50)
Surface: #ffffff (White)
Text: #0f172a (Slate-900)
Text Muted: #64748b (Slate-500)
```

### Admin Backoffice Theme (Blue)
```css
Primary: #0ea5e9 (Sky-500)
Primary Hover: #0284c7 (Sky-600)
Success: #168260 (Green)
Warning: #c2842d (Amber)
Danger: #c44343 (Red)
Background: #f5f7fa (Gray-50)
Surface: #ffffff (White)
Surface-2: #ecf1f7 (Gray-100)
Text: #161d2c (Dark)
Text Muted: #5f6b81 (Gray-500)
Border: #d0dae8 (Gray-300)
```

### Dark Theme Support
```css
Background: #0b111a (Dark)
Surface: #111827 (Gray-900)
Surface-2: #182234 (Gray-800)
Text: #f0f4f8 (Light)
Text Muted: #9ca3af (Gray-400)
Border: #374151 (Gray-700)
Primary: #22c5d2 (Cyan)
Success: #10b981 (Emerald)
Warning: #f59e0b (Orange)
Danger: #f87171 (Red)
```

---

## 📁 CSS Files Structure

```
src/main/resources/themes/
├── app-base.css              # Front Office theme (Green/Teal)
├── admin-backoffice.css      # Admin Backoffice theme (Blue)
└── dark-theme.css            # Dark mode overrides (optional)
```

---

## 🚀 Quick Start

### 1. Apply Theme to Your Application

In your main JavaFX class:

```java
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Load your root layout
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        
        Scene scene = new Scene(root, 1400, 900);
        
        // Apply theme based on user role
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isAdmin()) {
            // Admin Backoffice theme (Blue)
            scene.getStylesheets().add(
                getClass().getResource("/themes/admin-backoffice.css").toExternalForm()
            );
        } else {
            // Front Office theme (Green)
            scene.getStylesheets().add(
                getClass().getResource("/themes/app-base.css").toExternalForm()
            );
        }
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("GreenLedger");
        primaryStage.show();
    }
}
```

### 2. Apply Theme in FXML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="Controllers.AdminUsersController"
            stylesheets="@../themes/admin-backoffice.css">
    
    <!-- Your content here -->
    
</BorderPane>
```

---

## 🏗️ Layout Structure

### Admin Backoffice Layout

```xml
<HBox>
    <!-- Sidebar (300px) -->
    <VBox styleClass="workspace-sidebar">
        <!-- Brand -->
        <HBox styleClass="workspace-brand">
            <Label styleClass="workspace-brand-mark" text="GL"/>
            <VBox>
                <Label styleClass="workspace-brand-title" text="GreenLedger"/>
                <Label styleClass="workspace-brand-subtitle" text="BACK OFFICE"/>
            </VBox>
        </HBox>
        
        <!-- Navigation -->
        <VBox styleClass="workspace-nav">
            <Label styleClass="workspace-nav-section-label" text="OVERVIEW"/>
            <Button styleClass="workspace-nav-link is-active" text="Dashboard"/>
            <Button styleClass="workspace-nav-link" text="Users"/>
            
            <Label styleClass="workspace-nav-section-label" text="MANAGEMENT"/>
            <Button styleClass="workspace-nav-link" text="Projects"/>
            <Button styleClass="workspace-nav-link" text="Wallets"/>
        </VBox>
        
        <!-- Bottom Actions -->
        <VBox styleClass="workspace-side-actions">
            <Button styleClass="workspace-side-link" text="Settings"/>
            <Button styleClass="workspace-side-link" text="Logout"/>
        </VBox>
    </VBox>
    
    <!-- Main Content -->
    <VBox styleClass="workspace-main" HBox.hgrow="ALWAYS">
        <!-- Page Header -->
        <VBox styleClass="page-header">
            <Label styleClass="page-title" text="User Management"/>
            <Label styleClass="page-subtitle" text="Manage users, roles, and permissions"/>
        </VBox>
        
        <!-- Page Content -->
        <VBox styleClass="page-content">
            <!-- Your content here -->
        </VBox>
    </VBox>
</HBox>
```

---

## 🎯 Component Classes

### Buttons

```xml
<!-- Primary Button (Blue for Admin, Green for Front Office) -->
<Button styleClass="ui-btn-primary" text="Save"/>

<!-- Secondary Button -->
<Button styleClass="ui-btn-secondary" text="Cancel"/>

<!-- Ghost Button -->
<Button styleClass="ui-btn-ghost" text="Learn More"/>

<!-- Success Button -->
<Button styleClass="ui-btn-success" text="Approve"/>

<!-- Danger Button -->
<Button styleClass="ui-btn-danger" text="Delete"/>

<!-- Button Sizes -->
<Button styleClass="ui-btn-primary btn-lg" text="Large"/>
<Button styleClass="ui-btn-primary" text="Default"/>
<Button styleClass="ui-btn-primary btn-sm" text="Small"/>
<Button styleClass="ui-btn-primary btn-xs" text="Extra Small"/>
```

### Cards

```xml
<!-- Standard Card -->
<VBox styleClass="ui-card">
    <Label text="Card Title"/>
    <Label text="Card content goes here"/>
</VBox>

<!-- Office Card (Enhanced) -->
<VBox styleClass="office-card">
    <Label text="Enhanced Card"/>
</VBox>

<!-- Metric Card (KPI) -->
<VBox styleClass="metric-card">
    <Label styleClass="metric-label" text="TOTAL USERS"/>
    <Label styleClass="metric-value" text="1,234"/>
    <Label styleClass="metric-change positive" text="+12.5%"/>
</VBox>
```

### Form Inputs

```xml
<!-- Text Field -->
<TextField styleClass="text-field" promptText="Enter your name"/>

<!-- Password Field -->
<PasswordField styleClass="password-field" promptText="Enter password"/>

<!-- Text Area -->
<TextArea styleClass="text-area" promptText="Enter description"/>

<!-- ComboBox -->
<ComboBox styleClass="combo-box" promptText="Select option"/>

<!-- Date Picker -->
<DatePicker styleClass="date-picker"/>
```

### Tables

```xml
<TableView styleClass="table-view">
    <columns>
        <TableColumn text="ID" styleClass="table-column"/>
        <TableColumn text="Name" styleClass="table-column"/>
        <TableColumn text="Status" styleClass="table-column"/>
    </columns>
</TableView>
```

### Badges

```xml
<!-- Status Badges -->
<Label styleClass="badge badge-primary" text="Primary"/>
<Label styleClass="badge badge-success" text="Success"/>
<Label styleClass="badge badge-warning" text="Warning"/>
<Label styleClass="badge badge-danger" text="Danger"/>
<Label styleClass="badge badge-info" text="Info"/>

<!-- Priority Badges -->
<Label styleClass="badge priority-critical" text="CRITICAL"/>
<Label styleClass="badge priority-high" text="HIGH"/>
<Label styleClass="badge priority-medium" text="MEDIUM"/>
<Label styleClass="badge priority-low" text="LOW"/>

<!-- AI Recommendation Badges -->
<Label styleClass="badge ai-recommend" text="RECOMMENDED"/>
<Label styleClass="badge ai-verify" text="VERIFY"/>
<Label styleClass="badge ai-reject" text="REJECT"/>
```

---

## 🎨 Typography

### Font Families

```css
Primary: 'Inter', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif
Display: 'Sora' (for headings - optional)
Monospace: 'Space Grotesk' (for code - optional)
```

### Font Sizes

```xml
<!-- Page Title -->
<Label styleClass="page-title" text="Dashboard"/>  <!-- 24px, 800 weight -->

<!-- Section Title -->
<Label styleClass="section-title" text="Overview"/>  <!-- 18px, 700 weight -->

<!-- Body Text -->
<Label text="Regular text"/>  <!-- 14px, 400 weight -->

<!-- Small Text -->
<Label styleClass="small-text" text="Small text"/>  <!-- 11px -->

<!-- Large Text -->
<Label styleClass="large-text" text="Large text"/>  <!-- 16px -->

<!-- Muted Text -->
<Label styleClass="muted-text" text="Muted text"/>  <!-- Gray color -->
```

---

## 📊 Charts & Visualizations

### Line Chart

```xml
<LineChart styleClass="chart">
    <xAxis>
        <CategoryAxis label="Date"/>
    </xAxis>
    <yAxis>
        <NumberAxis label="Value"/>
    </yAxis>
</LineChart>
```

### Bar Chart

```xml
<BarChart styleClass="chart">
    <xAxis>
        <CategoryAxis label="Category"/>
    </xAxis>
    <yAxis>
        <NumberAxis label="Count"/>
    </yAxis>
</BarChart>
```

---

## 🔧 Utility Classes

### Spacing

```xml
<!-- Spacing -->
<VBox styleClass="spacing-sm">  <!-- 8px spacing -->
<VBox styleClass="spacing-md">  <!-- 16px spacing -->
<VBox styleClass="spacing-lg">  <!-- 24px spacing -->

<!-- Padding -->
<VBox styleClass="padding-sm">  <!-- 8px padding -->
<VBox styleClass="padding-md">  <!-- 16px padding -->
<VBox styleClass="padding-lg">  <!-- 24px padding -->
```

### Alignment

```xml
<Label styleClass="text-center" text="Centered"/>
<Label styleClass="text-left" text="Left aligned"/>
<Label styleClass="text-right" text="Right aligned"/>
```

---

## 🌓 Dark Mode Support

### Enable Dark Mode

```java
// In your controller or main class
public void enableDarkMode() {
    Scene scene = stage.getScene();
    scene.getRoot().getStyleClass().add("dark-theme");
}

public void disableDarkMode() {
    Scene scene = stage.getScene();
    scene.getRoot().getStyleClass().remove("dark-theme");
}
```

### Toggle Dark Mode

```java
@FXML
private void handleToggleDarkMode() {
    Parent root = stage.getScene().getRoot();
    if (root.getStyleClass().contains("dark-theme")) {
        root.getStyleClass().remove("dark-theme");
    } else {
        root.getStyleClass().add("dark-theme");
    }
}
```

---

## 🎯 Admin-Specific Components

### Wallet Supervision

```xml
<!-- Deficit Value (Red) -->
<Label styleClass="deficit-value" text="-1,234.56"/>

<!-- At-Risk Value (Orange) -->
<Label styleClass="at-risk-value" text="45.23"/>

<!-- Healthy Value (Green) -->
<Label styleClass="healthy-value" text="1,234.56"/>
```

### Fraud Scoring

```xml
<!-- High Fraud Score (Red) -->
<Label styleClass="fraud-score-high" text="0.85"/>

<!-- Medium Fraud Score (Orange) -->
<Label styleClass="fraud-score-medium" text="0.45"/>

<!-- Low Fraud Score (Green) -->
<Label styleClass="fraud-score-low" text="0.15"/>
```

---

## 📱 Responsive Design

### Breakpoints (for reference)

```css
Desktop: > 1100px
Tablet: 760px - 1100px
Mobile: < 760px
```

### Adaptive Layouts

```java
// In your controller
@FXML
public void initialize() {
    // Listen for window resize
    stage.widthProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal.doubleValue() < 760) {
            // Mobile layout
            sidebar.setVisible(false);
            mainContent.setPrefWidth(newVal.doubleValue());
        } else if (newVal.doubleValue() < 1100) {
            // Tablet layout
            sidebar.setPrefWidth(240);
        } else {
            // Desktop layout
            sidebar.setPrefWidth(300);
        }
    });
}
```

---

## 🎨 Custom Styling Examples

### Gradient Backgrounds

```css
/* In your custom CSS */
.custom-gradient {
    -fx-background-color: linear-gradient(to bottom right, #0ea5e9, #0284c7);
}
```

### Glassmorphism Effect

```xml
<VBox styleClass="fo-glass">
    <!-- Frosted glass effect -->
</VBox>
```

### Custom Shadows

```css
.custom-shadow {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);
}
```

---

## 🔍 Troubleshooting

### Issue 1: Styles Not Applied

**Problem**: CSS classes not working

**Solution**:
```java
// Ensure CSS is loaded
scene.getStylesheets().add(
    getClass().getResource("/themes/admin-backoffice.css").toExternalForm()
);

// Check if file exists
System.out.println(getClass().getResource("/themes/admin-backoffice.css"));
```

### Issue 2: Colors Not Showing

**Problem**: CSS variables not working in JavaFX

**Solution**: JavaFX doesn't support CSS custom properties. Use direct colors:
```css
/* Instead of: */
-fx-background-color: var(--primary);

/* Use: */
-fx-background-color: #0ea5e9;
```

### Issue 3: Fonts Not Loading

**Problem**: Custom fonts not displaying

**Solution**:
```java
// Load custom fonts
Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Regular.ttf"), 14);
Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Bold.ttf"), 14);
```

---

## 📚 Best Practices

### 1. Consistent Spacing
Use the spacing utility classes (spacing-sm, spacing-md, spacing-lg) for consistent layouts.

### 2. Color Usage
- Use `ui-btn-primary` for main actions
- Use `ui-btn-secondary` for secondary actions
- Use `ui-btn-danger` for destructive actions

### 3. Typography Hierarchy
- Page Title (24px) → Section Title (18px) → Body (14px) → Small (11px)

### 4. Card Usage
- Use `ui-card` for standard content containers
- Use `metric-card` for KPI displays
- Use `office-card` for enhanced admin panels

### 5. Table Styling
- Always use `table-view` class
- Keep column headers concise
- Use badges for status columns

---

## 🎯 Complete Example

### Admin Dashboard

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<HBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="Controllers.AdminDashboardController"
      stylesheets="@../themes/admin-backoffice.css">
    
    <!-- Sidebar -->
    <VBox styleClass="workspace-sidebar">
        <HBox styleClass="workspace-brand">
            <Label styleClass="workspace-brand-mark" text="GL"/>
            <VBox>
                <Label styleClass="workspace-brand-title" text="GreenLedger"/>
                <Label styleClass="workspace-brand-subtitle" text="BACK OFFICE"/>
            </VBox>
        </HBox>
        
        <VBox styleClass="workspace-nav">
            <Label styleClass="workspace-nav-section-label" text="OVERVIEW"/>
            <Button styleClass="workspace-nav-link is-active" text="Dashboard"/>
            <Button styleClass="workspace-nav-link" text="Users"/>
            <Button styleClass="workspace-nav-link" text="Projects"/>
        </VBox>
    </VBox>
    
    <!-- Main Content -->
    <VBox styleClass="workspace-main" HBox.hgrow="ALWAYS">
        <VBox styleClass="page-header">
            <Label styleClass="page-title" text="Dashboard"/>
            <Label styleClass="page-subtitle" text="Overview of your platform"/>
        </VBox>
        
        <VBox styleClass="page-content">
            <!-- Metrics Row -->
            <HBox styleClass="spacing-md">
                <VBox styleClass="metric-card">
                    <Label styleClass="metric-label" text="TOTAL USERS"/>
                    <Label styleClass="metric-value" text="1,234"/>
                    <Label styleClass="metric-change positive" text="+12.5%"/>
                </VBox>
                
                <VBox styleClass="metric-card">
                    <Label styleClass="metric-label" text="ACTIVE PROJECTS"/>
                    <Label styleClass="metric-value" text="567"/>
                    <Label styleClass="metric-change positive" text="+8.3%"/>
                </VBox>
                
                <VBox styleClass="metric-card">
                    <Label styleClass="metric-label" text="TOTAL CREDITS"/>
                    <Label styleClass="metric-value" text="89,234"/>
                    <Label styleClass="metric-change negative" text="-2.1%"/>
                </VBox>
            </HBox>
            
            <!-- Table -->
            <VBox styleClass="ui-card">
                <Label styleClass="section-title" text="Recent Users"/>
                <TableView styleClass="table-view">
                    <columns>
                        <TableColumn text="ID"/>
                        <TableColumn text="Name"/>
                        <TableColumn text="Email"/>
                        <TableColumn text="Status"/>
                    </columns>
                </TableView>
            </VBox>
        </VBox>
    </VBox>
</HBox>
```

---

## 📖 Additional Resources

- [JavaFX CSS Reference](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/doc-files/cssref.html)
- [Inter Font](https://fonts.google.com/specimen/Inter)
- [Tailwind CSS Colors](https://tailwindcss.com/docs/customizing-colors) (for reference)

---

**Version**: 1.0  
**Last Updated**: May 8, 2026  
**Author**: Kiro AI Assistant
