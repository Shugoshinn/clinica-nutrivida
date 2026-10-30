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
					// Vite 7 tipa el proxy sin los eventos de EventEmitter; se declara la firma usada
					const emitter = proxy as unknown as {
						on(event: 'proxyReq', listener: (proxyRequest: { removeHeader(name: string): void }) => void): void;
					};
					emitter.on('proxyReq', (proxyRequest) => proxyRequest.removeHeader('origin'));
				}
			}
		}
	}
});
