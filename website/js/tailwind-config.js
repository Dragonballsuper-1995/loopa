tailwind.config = {
    theme: {
        extend: {
            colors: {
                loopBase: '#0F0E0C',
                loopSurface: '#1A1915',
                loopRaised: '#242320',
                loopAmber: '#E8A87C',
                loopAmberStrong: '#D4845A',
                loopAmberSubtle: '#2A1F17',
                textPrimary: '#F0EDE8',
                textSecondary: '#A09990',
                textMuted: '#5C574F',
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
