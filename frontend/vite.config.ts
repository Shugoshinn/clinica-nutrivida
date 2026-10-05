import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
	plugins: [react()],
	server: {
		proxy: {
			'/api': {
				target: 'http://api-nutrivida-v2.us-east-1.elasticbeanstalk.com',
				changeOrigin: true,
				configure: (proxy) => {
					proxy.on('proxyReq', (proxyRequest) => proxyRequest.removeHeader('origin'));
				}
			}
		}
	}
});
