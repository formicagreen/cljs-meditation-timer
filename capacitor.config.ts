import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'dev.shantiram.meditation-timer',
  appName: 'Meditation timer',
  webDir: 'public',
  server: process.env.SERVER_URL ? {
    url: process.env.SERVER_URL,
    cleartext: true
  } : {}
};

export default config;