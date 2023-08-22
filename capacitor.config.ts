import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'dev.dagnorberg.timer',
  appName: 'Dhyāna',
  webDir: 'public',
  server: process.env.SERVER_URL ? {
    url: process.env.SERVER_URL,
    cleartext: true
  } : {}
};

export default config;