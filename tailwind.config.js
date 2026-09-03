/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ["./website/**/*.{html,js}"],
    theme: {
        extend: {
            colors: {
                loopBase: '#060608',           // Ultra-deep obsidian canvas
                loopSurface: '#101014',        // Deep surface for cards and panels
                loopRaised: '#181820',         // Elevated cards / active states
                loopGlass: 'rgba(16, 16, 22, 0.75)',
                loopAmber: '#FF5A36',          // Electric Sunset Coral / Primary CTA
                loopAmberStrong: '#E04826',    // Active / pressed
                loopAmberSubtle: '#26120E',    // Subtle chip tint
                loopTerracotta: '#E88A6E',     // Secondary warm accent
                textPrimary: '#FFFFFF',        // Pure white high-contrast text
                textSecondary: '#A3A3B2',      // Refined neutral gray
                textMuted: '#686878',          // Muted meta text
                loopSuccess: '#4ADE80',        // Watched green
                loopError: '#F87171',          // Error red
                accentCyan: '#00D2FF',         // Tech specs / anime badge
                accentGold: '#FFB800',         // Star ratings / awards
                accentPurple: '#8A4FFF',       // AI Concierge
                // Named border tokens
                loopBorderSubtle:  'rgba(255, 255, 255, 0.07)',
                loopBorderDefault: 'rgba(255, 255, 255, 0.12)',
                loopBorderStrong:  'rgba(255, 255, 255, 0.22)',
            },
            fontFamily: {
                sans: ['"Plus Jakarta Sans"', '"DM Sans"', 'sans-serif'],
                display: ['"Outfit"', '"Plus Jakarta Sans"', 'sans-serif'],
            },
            borderRadius: {
                'sm': '8px',
                'md': '12px',
                'lg': '16px',
                'xl': '24px',
                '2xl': '32px',
            },
            boxShadow: {
                'amber': '0 0 24px rgba(255, 90, 54, 0.35)',
                'glass': '0 1px 0 0 rgba(255, 255, 255, 0.08) inset, 0 16px 40px -8px rgba(0, 0, 0, 0.7)',
                'card-hover': '0 20px 48px -10px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 90, 54, 0.25)',
            }
        }
    }
};
