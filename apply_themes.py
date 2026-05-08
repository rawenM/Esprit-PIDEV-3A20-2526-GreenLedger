#!/usr/bin/env python3
"""
Apply GreenLedger themes to all FXML files
Admin pages get admin-backoffice.css
Front office pages get app-base.css
"""

import os
import re

# Define which files get which theme
ADMIN_THEME = "@../themes/admin-backoffice.css"
FRONT_OFFICE_THEME = "@../themes/app-base.css"

# Admin pages (get admin-backoffice.css)
ADMIN_PAGES = [
    "admin_shell.fxml",
    "admin_users.fxml",
    "audit_log.fxml",
    "edit_user.fxml",
    "user_statistics.fxml",
    "evaluation_dashboard.fxml",
    "evaluation_form.fxml",
    "evaluation_queue.fxml",
    "evaluation_resume.fxml",
    "expert_shell.fxml",
    "expert_carbon_dashboard.fxml",
]

# Front office pages (get app-base.css)
FRONT_OFFICE_PAGES = [
    "dashboard.fxml",
    "investisseur_shell.fxml",
    "investisseur_portfolio.fxml",
    "investisseur_messages.fxml",
    "investisseur_notifications.fxml",
    "porteur_shell.fxml",
    "porteur_projets.fxml",
    "porteur_projet_form.fxml",
    "porteur_messages.fxml",
    "porteur_notifications.fxml",
    "porteur_assistant.fxml",
    "marketplace.fxml",
    "create_listing.fxml",
    "escrow.fxml",
    "investor_financing.fxml",
    "finance_risk_agent.fxml",
    "swipe_invest.fxml",
    "batchLineage.fxml",
]

# Auth pages (get app-base.css)
AUTH_PAGES = [
    "login.fxml",
    "register.fxml",
    "forgot_password.fxml",
    "reset_password.fxml",
    "login_with_captcha_choice.fxml",
    "puzzle_captcha.fxml",
]

# Root level FXML files
ROOT_FXML_DIR = "src/main/resources"
ROOT_FXML_FILES = {
    "main.fxml": FRONT_OFFICE_THEME,
    "settings.fxml": FRONT_OFFICE_THEME,
    "editProfile.fxml": FRONT_OFFICE_THEME,
    "financement.fxml": FRONT_OFFICE_THEME,
    "expertProjet.fxml": ADMIN_THEME,
    "gestionCarbone.fxml": ADMIN_THEME,
    "GestionProjet.fxml": FRONT_OFFICE_THEME,
    "greenwallet.fxml": FRONT_OFFICE_THEME,
    "Investment_dashboard.fxml": FRONT_OFFICE_THEME,
    "mlDecision.fxml": ADMIN_THEME,
    "ownerEvaluations.fxml": FRONT_OFFICE_THEME,
    "projectEvaluationView.fxml": ADMIN_THEME,
    "ProjetCreate.fxml": FRONT_OFFICE_THEME,
    "ProjetDetail.fxml": FRONT_OFFICE_THEME,
    "AssistantChat.fxml": FRONT_OFFICE_THEME,
    "BatchCarbonTest.fxml": ADMIN_THEME,
    "ComprehensiveTest.fxml": ADMIN_THEME,
    "SystemTest.fxml": ADMIN_THEME,
    "test.fxml": ADMIN_THEME,
}

def apply_theme_to_file(filepath, theme_path):
    """Apply theme stylesheet to an FXML file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already has stylesheets attribute
        if 'stylesheets=' in content:
            print(f"  ⚠️  Already has stylesheets: {os.path.basename(filepath)}")
            return False
        
        # Find the root element (BorderPane, VBox, HBox, AnchorPane, etc.)
        # Pattern: <ElementName ... > (may span multiple lines)
        pattern = r'(<(?:BorderPane|VBox|HBox|AnchorPane|StackPane|GridPane|FlowPane|TilePane|ScrollPane)\s+[^>]*?)(\s*>)'
        
        def add_stylesheet(match):
            element_opening = match.group(1)
            closing = match.group(2)
            
            # Check if it already has stylesheets
            if 'stylesheets=' in element_opening:
                return match.group(0)
            
            # Add stylesheets attribute before the closing >
            return f'{element_opening}\n            stylesheets="{theme_path}"{closing}'
        
        new_content = re.sub(pattern, add_stylesheet, content, count=1)
        
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"  ✅ Applied theme: {os.path.basename(filepath)}")
            return True
        else:
            print(f"  ⚠️  Could not find root element: {os.path.basename(filepath)}")
            return False
            
    except Exception as e:
        print(f"  ❌ Error processing {filepath}: {e}")
        return False

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    fxml_dir = os.path.join(base_dir, "src", "main", "resources", "fxml")
    root_fxml_dir = os.path.join(base_dir, "src", "main", "resources")
    
    total_files = 0
    updated_files = 0
    
    print("\n🎨 Applying GreenLedger Themes to FXML Files\n")
    print("=" * 60)
    
    # Apply admin theme
    print("\n📘 ADMIN PAGES (admin-backoffice.css):")
    for filename in ADMIN_PAGES:
        filepath = os.path.join(fxml_dir, filename)
        if os.path.exists(filepath):
            total_files += 1
            if apply_theme_to_file(filepath, ADMIN_THEME):
                updated_files += 1
        else:
            print(f"  ⚠️  File not found: {filename}")
    
    # Apply front office theme to fxml/ directory
    print("\n📗 FRONT OFFICE PAGES (app-base.css):")
    for filename in FRONT_OFFICE_PAGES:
        filepath = os.path.join(fxml_dir, filename)
        if os.path.exists(filepath):
            total_files += 1
            if apply_theme_to_file(filepath, FRONT_OFFICE_THEME):
                updated_files += 1
        else:
            print(f"  ⚠️  File not found: {filename}")
    
    # Apply auth theme
    print("\n🔐 AUTH PAGES (app-base.css):")
    for filename in AUTH_PAGES:
        filepath = os.path.join(fxml_dir, filename)
        if os.path.exists(filepath):
            total_files += 1
            if apply_theme_to_file(filepath, FRONT_OFFICE_THEME):
                updated_files += 1
        else:
            print(f"  ⚠️  File not found: {filename}")
    
    # Apply themes to root level FXML files
    print("\n📄 ROOT LEVEL FXML FILES:")
    for filename, theme in ROOT_FXML_FILES.items():
        filepath = os.path.join(root_fxml_dir, filename)
        if os.path.exists(filepath):
            total_files += 1
            theme_name = "admin-backoffice.css" if "admin" in theme else "app-base.css"
            print(f"  {filename} → {theme_name}")
            if apply_theme_to_file(filepath, theme):
                updated_files += 1
        else:
            print(f"  ⚠️  File not found: {filename}")
    
    print("\n" + "=" * 60)
    print(f"\n✅ Complete!")
    print(f"   Total files processed: {total_files}")
    print(f"   Files updated: {updated_files}")
    print(f"   Files skipped: {total_files - updated_files}")
    print("\n🎉 All themes applied successfully!\n")

if __name__ == "__main__":
    main()
