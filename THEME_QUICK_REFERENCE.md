# GreenLedger Theme - Quick Reference Card

## 🎨 Color Codes

### Admin Backoffice (Blue)
```
Primary:     #0ea5e9  (Sky-500)
Hover:       #0284c7  (Sky-600)
Success:     #168260  (Green)
Warning:     #c2842d  (Amber)
Danger:      #c44343  (Red)
Background:  #f5f7fa  (Gray-50)
Text:        #161d2c  (Dark)
```

### Front Office (Green)
```
Primary:     #059669  (Emerald-600)
Hover:       #047857  (Emerald-700)
Background:  #f0fdf4  (Emerald-50)
Text:        #0f172a  (Slate-900)
```

---

## 📦 CSS Files

```
/themes/admin-backoffice.css  → Admin (Blue theme)
/themes/app-base.css          → Front Office (Green theme)
```

---

## 🏗️ Layout Classes

```xml
<!-- Sidebar (300px) -->
<VBox styleClass="workspace-sidebar">

<!-- Main Content -->
<VBox styleClass="workspace-main">

<!-- Page Header -->
<VBox styleClass="page-header">

<!-- Page Content -->
<VBox styleClass="page-content">
```

---

## 🎯 Component Classes

### Buttons
```xml
<Button styleClass="ui-btn-primary"/>    <!-- Blue/Green -->
<Button styleClass="ui-btn-secondary"/>  <!-- Gray -->
<Button styleClass="ui-btn-success"/>    <!-- Green -->
<Button styleClass="ui-btn-danger"/>     <!-- Red -->
<Button styleClass="ui-btn-ghost"/>      <!-- Transparent -->

<!-- Sizes -->
<Button styleClass="ui-btn-primary btn-lg"/>  <!-- Large -->
<Button styleClass="ui-btn-primary btn-sm"/>  <!-- Small -->
<Button styleClass="ui-btn-primary btn-xs"/>  <!-- Extra Small -->
```

### Cards
```xml
<VBox styleClass="ui-card"/>        <!-- Standard -->
<VBox styleClass="office-card"/>    <!-- Enhanced -->
<VBox styleClass="metric-card"/>    <!-- KPI -->
```

### Navigation
```xml
<Button styleClass="workspace-nav-link"/>              <!-- Normal -->
<Button styleClass="workspace-nav-link is-active"/>    <!-- Active -->
<Label styleClass="workspace-nav-section-label"/>      <!-- Section -->
```

### Badges
```xml
<!-- Status -->
<Label styleClass="badge badge-primary"/>
<Label styleClass="badge badge-success"/>
<Label styleClass="badge badge-warning"/>
<Label styleClass="badge badge-danger"/>

<!-- Priority -->
<Label styleClass="badge priority-critical"/>  <!-- Red -->
<Label styleClass="badge priority-high"/>      <!-- Orange -->
<Label styleClass="badge priority-medium"/>    <!-- Yellow -->
<Label styleClass="badge priority-low"/>       <!-- Blue -->

<!-- AI Recommendations -->
<Label styleClass="badge ai-recommend"/>  <!-- Green -->
<Label styleClass="badge ai-verify"/>     <!-- Yellow -->
<Label styleClass="badge ai-reject"/>     <!-- Red -->
```

### Forms
```xml
<TextField styleClass="text-field"/>
<PasswordField styleClass="password-field"/>
<TextArea styleClass="text-area"/>
<ComboBox styleClass="combo-box"/>
<DatePicker styleClass="date-picker"/>
```

### Tables
```xml
<TableView styleClass="table-view">
    <columns>
        <TableColumn text="ID"/>
        <TableColumn text="Name"/>
    </columns>
</TableView>
```

---

## 📝 Typography

```xml
<Label styleClass="page-title"/>       <!-- 24px, 800 -->
<Label styleClass="section-title"/>    <!-- 18px, 700 -->
<Label styleClass="small-text"/>       <!-- 11px -->
<Label styleClass="large-text"/>       <!-- 16px -->
<Label styleClass="muted-text"/>       <!-- Gray color -->
```

---

## 🔧 Utilities

### Spacing
```xml
<VBox styleClass="spacing-sm">  <!-- 8px -->
<VBox styleClass="spacing-md">  <!-- 16px -->
<VBox styleClass="spacing-lg">  <!-- 24px -->
```

### Padding
```xml
<VBox styleClass="padding-sm">  <!-- 8px -->
<VBox styleClass="padding-md">  <!-- 16px -->
<VBox styleClass="padding-lg">  <!-- 24px -->
```

### Alignment
```xml
<Label styleClass="text-center"/>
<Label styleClass="text-left"/>
<Label styleClass="text-right"/>
```

---

## 🎨 Admin-Specific

### Wallet Values
```xml
<Label styleClass="deficit-value"/>   <!-- Red -->
<Label styleClass="at-risk-value"/>   <!-- Orange -->
<Label styleClass="healthy-value"/>   <!-- Green -->
```

### Fraud Scores
```xml
<Label styleClass="fraud-score-high"/>    <!-- Red -->
<Label styleClass="fraud-score-medium"/>  <!-- Orange -->
<Label styleClass="fraud-score-low"/>     <!-- Green -->
```

---

## 💻 Java Code

### Load Theme
```java
// Admin theme
scene.getStylesheets().add(
    getClass().getResource("/themes/admin-backoffice.css").toExternalForm()
);

// Front Office theme
scene.getStylesheets().add(
    getClass().getResource("/themes/app-base.css").toExternalForm()
);
```

### Dark Mode
```java
// Enable
scene.getRoot().getStyleClass().add("dark-theme");

// Disable
scene.getRoot().getStyleClass().remove("dark-theme");

// Toggle
if (root.getStyleClass().contains("dark-theme")) {
    root.getStyleClass().remove("dark-theme");
} else {
    root.getStyleClass().add("dark-theme");
}
```

---

## 📐 Sidebar Structure

```xml
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
    </VBox>
    
    <!-- Bottom Actions -->
    <VBox styleClass="workspace-side-actions">
        <Button styleClass="workspace-side-link" text="Settings"/>
        <Button styleClass="workspace-side-link" text="Logout"/>
    </VBox>
</VBox>
```

---

## 📊 Metric Card

```xml
<VBox styleClass="metric-card">
    <Label styleClass="metric-label" text="TOTAL USERS"/>
    <Label styleClass="metric-value" text="1,234"/>
    <Label styleClass="metric-change positive" text="+12.5%"/>
</VBox>
```

---

## 🎯 Complete Admin Layout

```xml
<HBox stylesheets="@../themes/admin-backoffice.css">
    <!-- Sidebar (300px) -->
    <VBox styleClass="workspace-sidebar">
        <!-- Brand, Nav, Actions -->
    </VBox>
    
    <!-- Main Content -->
    <VBox styleClass="workspace-main" HBox.hgrow="ALWAYS">
        <!-- Page Header -->
        <VBox styleClass="page-header">
            <Label styleClass="page-title" text="Dashboard"/>
            <Label styleClass="page-subtitle" text="Overview"/>
        </VBox>
        
        <!-- Page Content -->
        <VBox styleClass="page-content">
            <!-- Metrics, Tables, Charts -->
        </VBox>
    </VBox>
</HBox>
```

---

## 🔍 Troubleshooting

### Styles Not Working?
```java
// Check if CSS is loaded
System.out.println(
    getClass().getResource("/themes/admin-backoffice.css")
);

// Verify file location
// Should be: src/main/resources/themes/admin-backoffice.css
```

### Wrong Colors?
```
Admin = admin-backoffice.css (Blue)
Front Office = app-base.css (Green)
```

### Sidebar Not Showing?
```xml
<VBox styleClass="workspace-sidebar" 
      minWidth="300" 
      prefWidth="300" 
      maxWidth="300"/>
```

---

## 📱 Responsive

```java
// Adapt to screen size
stage.widthProperty().addListener((obs, oldVal, newVal) -> {
    double width = newVal.doubleValue();
    if (width < 760) {
        sidebar.setVisible(false);  // Mobile
    } else if (width < 1100) {
        sidebar.setPrefWidth(240);  // Tablet
    } else {
        sidebar.setPrefWidth(300);  // Desktop
    }
});
```

---

## ✅ Quick Checklist

- [ ] CSS file in `/themes/` folder
- [ ] Load CSS in Scene or FXML
- [ ] Use correct style classes
- [ ] Test buttons, cards, tables
- [ ] Verify sidebar width (300px)
- [ ] Check colors (blue for admin)
- [ ] Test dark mode (optional)

---

## 📚 Documentation

- **THEME_GUIDE.md** - Full documentation
- **THEME_IMPLEMENTATION_SUMMARY.md** - Implementation guide
- **admin-backoffice.css** - Admin stylesheet
- **app-base.css** - Front office stylesheet

---

**Print this page for quick reference while coding!**

**Version**: 1.0 | **Date**: May 8, 2026
