# Meditation timer

Meditation timer app built using ClojureScript, shadow-cljs, Reagent, Tailwind and Capacitor.
Currently targeting ios but could be extended for web and android.

## Development

See this page:
https://capacitorjs.com/docs/getting-started/environment-setup

You also need to Java for building clojurescript.

To test in the browser, run `npm run dev:web`.  

To test on device, run `npm run dev:web` then `npm run dev:ios` then try to navigate whatever build errors xcode decides to throw at you.

Production builds are created automatically with the xcode ci cloud tool.