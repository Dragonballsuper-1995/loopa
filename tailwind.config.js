/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ["./website/**/*.{html,js}"],
    theme: {
        extend: {
            colors: {
                loopBase: '#0C0B09',          // deepened — warm near-black
                loopSurface: '#1C1A17',        // warmer mid-dark
                loopRaised: '#2A2823',         // more contrast vs surface
                loopAmber: '#E8A87C',
                loopAmberStrong: '#D4845A',
                loopAmberSubtle: '#2A1F17',
                loopTerracotta: '#C47A5A',     // secondary accent — decorative / non-CTA
                textPrimary: '#F0EDE8',
                textSecondary: '#A09990',
                textMuted: '#7A746D',          // raised from #5C574F for readability
                loopSuccess: '#7AB87A',
                loopError: '#C87070',
                // Named border tokens — mirror Android Loopa.Border / BorderMd / BorderStrong (Color.kt)
                loopBorderSubtle:  'rgba(240,237,232,0.071)',  // 0x12 alpha — hairline dividers
                loopBorderDefault: 'rgba(240,237,232,0.122)',  // 0x1F alpha — card borders
                loopBorderStrong:  'rgba(240,237,232,0.200)',  // 0x33 alpha — focus rings
            },
            fontFamily: {
                sans: ['"DM Sans"', 'sans-serif'],
            },
            borderRadius: {
                'sm': '6px',
                'md': '10px',
                'lg': '14px',
                'xl': '20px',
                '2xl': '28px',
            },
            boxShadow: {
                'amber': '0 0 0 1px rgba(232,168,124,0.4)',
            }
        }
    }
};
