```markdown
# Design System Document

## 1. Overview & Creative North Star: "The Financial Sanctuary"

This design system is built upon the concept of **The Financial Sanctuary**. Most financial applications are high-stress environments—cluttered with rigid grids, aggressive alerts, and clinical data tables. We are moving in the opposite direction. 

Our goal is to transform "money management" into a mindful, editorial experience. We achieve this by prioritizing **negative space over borders**, **tonal depth over shadows**, and **asymmetrical layouts** that guide the eye naturally rather than forcing it through a grid. The look is soft but professional, utilizing a "Paper & Glass" aesthetic that feels tactile, premium, and intentional.

---

## 2. Colors: Tonal Atmosphere

Our palette moves away from digital vibrancy toward organic, muted tones. 

### Core Palette
*   **Primary (`#506356`):** Our core Sage. It is used sparingly for high-intent actions and primary brand moments.
*   **Surface (`#FAF9F6`):** The "Warm Off-White" foundation. It provides a soft, paper-like quality that reduces eye strain.
*   **On-Surface (`#2F3430`):** Our "Charcoal." High legibility without the harshness of pure black.

### The "No-Line" Rule
**Standard 1px borders are strictly prohibited.** To define sections, use background color shifts.
*   Place a `surface-container-low` section on a `surface` background to create a subtle "pressed" or "inset" feel.
*   Use `surface-container-highest` only for the smallest, most elevated interactive elements.

### The "Glass & Gradient" Rule
To prevent the UI from feeling flat or "template-like," apply a **Signature Glow**:
*   **Main CTAs:** Use a subtle linear gradient from `primary` to `primary_dim` (Top-Left to Bottom-Right).
*   **Floating Elements:** Use `surface_container_lowest` at 80% opacity with a `12px` backdrop blur. This creates a "Frosted Sage" effect where background data softly bleeds through the foreground UI.

---

## 3. Typography: Editorial Authority

We use **Manrope** exclusively. Its geometric yet humanist qualities provide the "Professional Minimalist" balance required for financial trust.

*   **Display (Large/Med/Small):** Used for total balances and monthly summaries. Letter-spacing should be set to `-0.02em` to create a tight, editorial impact.
*   **Headlines:** Used for page titles. We encourage **Asymmetric Placement**—titles should often sit slightly offset or with generous top-padding to create "breathing room."
*   **Body (Large/Med/Small):** Set with a generous line-height (1.6) to ensure financial logs never feel cramped.
*   **Labels:** Always uppercase with `0.05em` tracking for a sophisticated, "utility-chic" look.

---

## 4. Elevation & Depth: Tonal Layering

Traditional box-shadows are messy. We communicate hierarchy through physical stacking.

### The Layering Principle
Think of the UI as sheets of fine stationery.
1.  **Base:** `surface` (The desk)
2.  **Section:** `surface-container-low` (The folder)
3.  **Active Card:** `surface-container-lowest` (The paper)

### Ambient Shadows
If an element must float (e.g., a Modal or FAB), use a **Sage-Tinted Shadow**:
*   **Color:** `on_surface` at 6% opacity.
*   **Blur:** `24px` to `48px`. 
*   **Offset:** `y: 8px`.
This mimics soft, natural sunlight rather than a digital drop shadow.

### The "Ghost Border" Fallback
If a boundary is required for accessibility (e.g., input fields), use the `outline_variant` token at **15% opacity**. It should be felt, not seen.

---

## 5. Components: Soft & Purposeful

### Buttons & Chips
*   **Primary Button:** Rounded `lg` (1rem). No border. Gradient fill (Sage to Sage-Dim).
*   **Secondary/Tertiary:** High-transparency `surface-variant`.
*   **Functional Chips:** For Income/Expense, use `secondary_container` (Muted Green) and `error_container` (Muted Red). Text should remain high-contrast on top of these desaturated fills.

### Cards & Lists
*   **Forbid Dividers:** Never use a horizontal line to separate transactions. Use the **Spacing Scale** (vertical white space) or a subtle hover state shift to `surface-container-high`.
*   **Financial Cards:** Use `surface_container_lowest` with a `xl` (1.5rem) corner radius. This large radius communicates "softness" and "safety."

### Input Fields
*   **State:** Default state should have no background, only a "Ghost Border" at the bottom. 
*   **Focus State:** Transition to a `surface-container-low` background with a subtle 2px `primary` underline.

### The "Pulse" Transaction List (Custom Component)
Instead of a table, use an editorial list. The date is a `label-md` floating to the left, while the transaction description uses `title-md`. The amount should be `display-sm` for immediate recognition, utilizing the desaturated functional colors.

---

## 6. Do’s and Don’ts

### Do:
*   **Embrace White Space:** If a screen feels "empty," it is likely working.
*   **Use Tonal Shifts:** Change backgrounds to indicate a change in context.
*   **Use Roundedness:** Stick to `xl` for large containers and `md` for small interactive elements to maintain a "soft" personality.

### Don't:
*   **Don't use #000000:** It breaks the calming "Financial Sanctuary" atmosphere.
*   **Don't use 1px Solid Borders:** They create "visual noise" that fatigues the user.
*   **Don't use Pure Red/Green:** Always use the desaturated `error` and `secondary` tokens provided. Financial data should be informative, not alarming.
*   **Don't Align Everything to the Left:** Experiment with centered "Display" values and staggered headlines to create a premium, custom feel.

---

*This design system is a living document intended to guide the creation of a calm, professional, and sophisticated financial tool. Every pixel should contribute to the user's peace of mind.*```