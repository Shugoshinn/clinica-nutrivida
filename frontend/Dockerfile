# Etapa 1: Compilación de Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Etapa 2: Servidor web ligero con Nginx
FROM nginx:alpine
# Copia los archivos compilados. 
# Nota: Si tu proyecto no se llama "clinica-nutrivida", cambia el nombre en la ruta de abajo.
COPY --from=build /app/dist/clinica-nutrivida/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]