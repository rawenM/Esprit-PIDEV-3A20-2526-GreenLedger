#!/usr/bin/env python3
"""Fix all dark sidebars and dark panels in front-office FXML files."""
import re, os

BASE = r"c:\Users\Mega-PC\Desktop\Pi_Dev\src\main\resources"

# White sidebar replacement patterns
DARK_SIDEBAR_BG   = r'-fx-background-color:#0F172A'
WHITE_SIDEBAR_BG  = '-fx-background-color:#ffffff; -fx-border-color:#e5e7eb; -fx-border-width:0 1 0 0'

DARK_BRAND_BORDER = r'-fx-border-color:rgba\(255,255,255,0\.1\);-fx-border-width:0 0 1 0'
WHITE_BRAND_BORDER= '-fx-border-color:#e5e7eb;-fx-border-width:0 0 1 0'

DARK_SECTION_LBL  = r'-fx-text-fill:rgba\(255,255,255,0\.25\)'
WHITE_SECTION_LBL = '-fx-text-fill:#9ca3af'

DARK_NAV_BTN      = r'-fx-text-fill:rgba\(255,255,255,0\.65\)'
WHITE_NAV_BTN     = '-fx-text-fill:#374151'

DARK_ACTIVE_BTN   = r'-fx-background-color:#059669;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:700;-fx-background-radius:8;-fx-padding:11 14;-fx-cursor:hand;-fx-border-width:0;-fx-background-insets:0;-fx-effect:dropshadow\(gaussian,rgba\(5,150,105,0\.35\),12,0,0,3\);'
WHITE_ACTIVE_BTN  = '-fx-background-color:#f0fdf4;-fx-text-fill:#059669;-fx-font-size:12px;-fx-font-weight:700;-fx-background-radius:8;-fx-padding:11 14;-fx-cursor:hand;-fx-border-width:0 0 0 3;-fx-border-color:#059669;-fx-background-insets:0;-fx-effect:none;'

DARK_PROFILE_BG   = r'-fx-background-color:#070F1A'
WHITE_PROFILE_BG  = '-fx-background-color:#f9fafb'

DARK_PROFILE_BORDER = r'-fx-border-color:rgba\(255,255,255,0\.1\);-fx-border-width:1 0 0 0'
WHITE_PROFILE_BORDER= '-fx-border-color:#e5e7eb;-fx-border-width:1 0 0 0'

DARK_PROFILE_NAME = r'-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:700'
WHITE_PROFILE_NAME= '-fx-text-fill:#111827;-fx-font-size:12px;-fx-font-weight:700'

DARK_PROFILE_TYPE = r'-fx-text-fill:rgba\(16,185,129,0\.6\)'
WHITE_PROFILE_TYPE= '-fx-text-fill:#059669'

DARK_PROFILE_ROLE = r'-fx-text-fill:rgba\(16,185,129,0\.75\)'
WHITE_PROFILE_ROLE= '-fx-text-fill:#059669'

DARK_APP_NAME     = r'-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:800'
WHITE_APP_NAME    = '-fx-text-fill:#064e3b;-fx-font-size:14px;-fx-font-weight:800'

DARK_PROFIL_BTN   = r'-fx-background-color:#1E293B;-fx-text-fill:rgba\(255,255,255,0\.8\)'
WHITE_PROFIL_BTN  = '-fx-background-color:#f3f4f6;-fx-text-fill:#374151'

DARK_AVATAR       = r'-fx-background-color:#059669;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:800;-fx-background-radius:20'
WHITE_AVATAR      = '-fx-background-color:#dcfce7;-fx-text-fill:#059669;-fx-font-size:12px;-fx-font-weight:800;-fx-background-radius:20'

# Files with dark sidebars to fix
SIDEBAR_FILES = [
    os.path.join(BASE, "fxml", "porteur_projets.fxml"),
    os.path.join(BASE, "fxml", "porteur_projet_form.fxml"),
    os.path.join(BASE, "fxml", "swipe_invest.fxml"),
    os.path.join(BASE, "fxml", "evaluation_dashboard.fxml"),
    os.path.join(BASE, "fxml", "evaluation_form.fxml"),
    os.path.join(BASE, "financement.fxml"),
]

# Fix expert_carbon_dashboard.fxml dark header
EXPERT_CARBON = os.path.join(BASE, "fxml", "expert_carbon_dashboard.fxml")

# Fix wrong stylesheets (admin-backoffice.css → app-base.css) for expert pages
WRONG_CSS_FILES = [
    os.path.join(BASE, "fxml", "expert_shell.fxml"),
    os.path.join(BASE, "fxml", "evaluation_dashboard.fxml"),
    os.path.join(BASE, "fxml", "evaluation_form.fxml"),
    os.path.join(BASE, "fxml", "expert_carbon_dashboard.fxml"),
]

def fix_sidebar(content):
    """Apply all sidebar light-theme replacements."""
    # Sidebar container background
    content = re.sub(DARK_SIDEBAR_BG, WHITE_SIDEBAR_BG, content)
    # Brand section border
    content = re.sub(DARK_BRAND_BORDER, WHITE_BRAND_BORDER, content)
    # Section labels
    content = re.sub(DARK_SECTION_LBL, WHITE_SECTION_LBL, content)
    # Inactive nav button text
    content = re.sub(DARK_NAV_BTN, WHITE_NAV_BTN, content)
    # Active nav button (green solid → light green with border)
    content = re.sub(DARK_ACTIVE_BTN, WHITE_ACTIVE_BTN, content)
    # Profile footer background
    content = re.sub(DARK_PROFILE_BG, WHITE_PROFILE_BG, content)
    # Profile footer border
    content = re.sub(DARK_PROFILE_BORDER, WHITE_PROFILE_BORDER, content)
    # Profile name text
    content = re.sub(DARK_PROFILE_NAME, WHITE_PROFILE_NAME, content)
    # Profile type text
    content = re.sub(DARK_PROFILE_TYPE, WHITE_PROFILE_TYPE, content)
    # Role subtitle (rgba green)
    content = re.sub(DARK_PROFILE_ROLE, WHITE_PROFILE_ROLE, content)
    # App name text
    content = re.sub(DARK_APP_NAME, WHITE_APP_NAME, content)
    # Profil button
    content = re.sub(DARK_PROFIL_BTN, WHITE_PROFIL_BTN, content)
    # Avatar badge
    content = re.sub(DARK_AVATAR, WHITE_AVATAR, content)
    return content

def fix_wrong_css(content):
    """Replace admin-backoffice.css with app-base.css for front-office pages."""
    return content.replace(
        'stylesheets="@../themes/admin-backoffice.css"',
        'stylesheets="@../themes/app-base.css"'
    )

updated = []

# Fix sidebars
for fpath in SIDEBAR_FILES:
    if not os.path.exists(fpath):
        print(f"  ⚠️  Not found: {fpath}")
        continue
    with open(fpath, 'r', encoding='utf-8') as f:
        original = f.read()
    fixed = fix_sidebar(original)
    if fixed != original:
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"  ✅ Fixed sidebar: {os.path.basename(fpath)}")
        updated.append(fpath)
    else:
        print(f"  ℹ️  No changes needed: {os.path.basename(fpath)}")

# Fix expert_carbon_dashboard dark header
if os.path.exists(EXPERT_CARBON):
    with open(EXPERT_CARBON, 'r', encoding='utf-8') as f:
        content = f.read()
    # Replace dark header background
    fixed = content.replace(
        '-fx-background-color: #1F2937;',
        '-fx-background-color: #ffffff;'
    ).replace(
        '-fx-text-fill: white;',
        '-fx-text-fill: #0f172a;'
    ).replace(
        'style="-fx-text-fill: #D1D5DB;"',
        'style="-fx-text-fill: #374151;"'
    )
    if fixed != content:
        with open(EXPERT_CARBON, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"  ✅ Fixed dark header: expert_carbon_dashboard.fxml")
        updated.append(EXPERT_CARBON)

# Fix wrong CSS references
for fpath in WRONG_CSS_FILES:
    if not os.path.exists(fpath):
        continue
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()
    fixed = fix_wrong_css(content)
    if fixed != content:
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"  ✅ Fixed CSS ref: {os.path.basename(fpath)}")
        if fpath not in updated:
            updated.append(fpath)

# Also fix porteur_projet_form.fxml right panel (dark live preview)
FORM_FILE = os.path.join(BASE, "fxml", "porteur_projet_form.fxml")
if os.path.exists(FORM_FILE):
    with open(FORM_FILE, 'r', encoding='utf-8') as f:
        content = f.read()
    # Replace dark right panel
    fixed = content.replace(
        '-fx-background-color:#111827;-fx-border-color:#1F2937;-fx-border-width:0 0 0 1;',
        '-fx-background-color:#f8fafc;-fx-border-color:#e5e7eb;-fx-border-width:0 0 0 1;'
    ).replace(
        '-fx-background-color:#1A2E26;-fx-border-color:#2D5F3F;',
        '-fx-background-color:#f0fdf4;-fx-border-color:#bbf7d0;'
    ).replace(
        '-fx-background-color:#1F2937;-fx-border-color:#374151;',
        '-fx-background-color:#ffffff;-fx-border-color:#e5e7eb;'
    ).replace(
        '-fx-background-color:#1E3A5F;-fx-border-color:#2563EB;',
        '-fx-background-color:#eff6ff;-fx-border-color:#bfdbfe;'
    ).replace(
        '-fx-background:#111827;-fx-background-color:#111827;',
        '-fx-background:#f8fafc;-fx-background-color:#f8fafc;'
    )
    # Fix text colors in right panel
    fixed = fixed.replace(
        '-fx-text-fill:#6EE7B7;',
        '-fx-text-fill:#059669;'
    ).replace(
        '-fx-text-fill:white;-fx-font-size:42px;',
        '-fx-text-fill:#059669;-fx-font-size:42px;'
    ).replace(
        '-fx-text-fill:white;-fx-font-size:28px;',
        '-fx-text-fill:#0f172a;-fx-font-size:28px;'
    ).replace(
        '-fx-text-fill:rgba(255,255,255,0.4);',
        '-fx-text-fill:#94a3b8;'
    ).replace(
        '-fx-text-fill:rgba(255,255,255,0.35);',
        '-fx-text-fill:#94a3b8;'
    ).replace(
        '-fx-text-fill:rgba(255,255,255,0.08);',
        '-fx-text-fill:#f1f5f9;'
    ).replace(
        '-fx-background-color:rgba(255,255,255,0.08);',
        '-fx-background-color:#f1f5f9;'
    ).replace(
        '-fx-text-fill:#9CA3AF;',
        '-fx-text-fill:#6b7280;'
    ).replace(
        '"Analyse en temps reel"\n                     style="-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:white;',
        '"Analyse en temps reel"\n                     style="-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#0f172a;'
    ).replace(
        '-fx-text-fill:#93C5FD;',
        '-fx-text-fill:#2563eb;'
    ).replace(
        '-fx-background-color:rgba(37,99,235,0.3);-fx-text-fill:#93C5FD;',
        '-fx-background-color:#dbeafe;-fx-text-fill:#2563eb;'
    )
    if fixed != content:
        with open(FORM_FILE, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"  ✅ Fixed dark right panel: porteur_projet_form.fxml")
        if FORM_FILE not in updated:
            updated.append(FORM_FILE)

print(f"\n✅ Done! Updated {len(updated)} files.")
