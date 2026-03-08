/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                'sr-orange-start': '#FF9500',
                'sr-orange-end': '#FFD60A',
                'sr-black': '#050505',
            }
        },
    },
    plugins: [],
}
