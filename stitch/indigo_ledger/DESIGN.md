# Design System Documentation: The Financial Editorial

## 1. Overview & Creative North Star
**The Creative North Star: "The Sovereign Ledger"**

This design system moves away from the cluttered, "utility-first" look of traditional finance apps. Instead, it adopts a **High-End Editorial** aesthetic. We treat personal finance data not as a chore, but as a premium publication. 

The system breaks the rigid "Android template" look through **intentional asymmetry**, **tonal depth layering**, and a **"No-Line" philosophy**. By prioritizing negative space and sophisticated surface transitions over borders and dividers, we create a sense of calm authority and digital "breathing room." The interface should feel like a bespoke financial concierge—authoritative, minimalist, and deeply intentional.

---

## 2. Colors & Surface Architecture

The palette is anchored in an authoritative **Indigo Blue**, supported by high-chroma semantic colors for financial health. However, the premium feel is established in the neutrals.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders for sectioning or containment. Boundaries must be defined solely through:
1.  **Background Color Shifts:** (e.g., a `surface-container-low` section sitting on a `surface` background).
2.  **Subtle Tonal Transitions:** Using depth to imply edges rather than lines.

### Surface Hierarchy & Nesting
We treat the UI as a physical stack of fine paper. Depth is achieved by nesting `surface-container` tiers:
*   **Background (`#f8f9fa`):** The canvas.
*   **Surface-Container-Low (`#f3f4f5`):** Secondary content areas or "wells."
*   **Surface-Container-Lowest (`#ffffff`):** The "Hero" cards that should pop against the background.

### The "Glass & Gradient" Rule
To elevate AI features and main CTAs:
*   **AI Elements:** Use `secondary_container` with a `backdrop-blur` (12px–20px) to create a "Frosted Cyan" glass effect.
*   **Signature Textures:** Main CTAs must use a subtle linear gradient (Top-Left to Bottom-Right) transitioning from `primary` (#3525cd) to `primary_container` (#4f46e5). This adds "soul" and prevents the UI from looking digitally flat.

---

## 3. Typography: The Editorial Scale

We use a dual-font approach to balance character with readability. **Manrope** provides a geometric, modern authority for headers, while **Inter/Pretendard** handles high-density data.

| Level | Font | Size | Weight | Intent |
| :--- | :--- | :--- | :--- | :--- |
| **Display-LG** | Manrope | 3.5rem | 700 | Large balance statements. |
| **Headline-MD** | Manrope | 1.75rem | 600 | Page titles & Section headers. |
| **Title-MD** | Inter | 1.125rem | 500 | Card titles and sub-sections. |
| **Body-LG** | Inter | 1rem | 400 | Standard transaction descriptions. |
| **Label-MD** | Inter | 0.75rem | 600 | Overline text and category tags. |

**Editorial Note:** Use **Asymmetric Headlines**. Don't feel forced to center everything. Large Display text should be left-aligned with significant padding to create an "Editorial Margin."

---

## 4. Elevation & Depth: Tonal Layering

We reject the standard Material 2 "Drop Shadow." We use **Ambient Softness**.

*   **The Layering Principle:** Depth is "stacked." Place a `surface-container-lowest` (#FFFFFF) card atop a `surface-container-low` (#F3F4F5) section. The delta in hex value creates a natural lift.
*   **Ambient Shadows:** For floating Action Buttons (FABs) or Modal Sheets, use a shadow with a blur of 32px and 4% opacity. The shadow color must be a tinted version of `primary` (Indigo) rather than black, mimicking natural light refraction.
*   **The "Ghost Border" Fallback:** Only if accessibility requires it, use a `outline-variant` at **15% opacity**. Never use a 100% opaque border.
*   **Glassmorphism:** Use `surface_bright` with 80% opacity and a 16px blur for top app bars to allow content to "ghost" through as the user scrolls.

---

## 5. Components

### Cards & Lists
*   **Rule:** Forbid the use of divider lines. 
*   **Implementation:** Separate list items using **Vertical White Space** (16dp). For high-density transaction lists, use alternating background tints (`surface` vs `surface-container-low`) rather than lines.
*   **Roundedness:** Cards use **xl (1.5rem)** corners to feel approachable.

### Buttons
*   **Primary:** Indigo Gradient (Primary to Primary-Container), **full (9999px)** rounding.
*   **Secondary:** Glassmorphic Cyan for AI-driven actions, or `surface-container-high` for standard secondary tasks.
*   **State:** On press, scale the button down to **96%** rather than just changing the color.

### Input Fields
*   **Style:** "Plinth" style. No bottom line, no box. Use a `surface-container-highest` background with **md (0.75rem)** rounding.
*   **Focus:** The background shifts to `primary_fixed` with a subtle Indigo "Ghost Border."

### Specialized Components: The AI Insight Card
*   **Visual:** Uses a `secondary_fixed_dim` background with a subtle "noise" texture overlay and a `backdrop-blur`. This distinguishes AI-generated financial advice from standard user-logged data.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use extreme white space. If you think there’s enough padding, add 8dp more.
*   **Do** use "Inter" for all numbers. Manrope is for words; Inter's tabularized numbers are for financial clarity.
*   **Do** nest containers to show hierarchy (e.g., White card on a Light Gray section).

### Don't:
*   **Don't** use 1px dividers. Ever.
*   **Don't** use pure black (#000000) for text. Use `on_surface` (#191c1d) to maintain the high-end editorial feel.
*   **Don't** use standard Material 3 "Elevation shadows" (1, 2, 3, 4). Use the Tonal Layering system defined in Section 4.
*   **Don't** cram data. If a screen feels full, it's time to use a "drill-down" pattern or a progressive disclosure carousel.