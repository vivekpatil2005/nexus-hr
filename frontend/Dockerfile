# ─── Stage 1: Build ───
FROM node:22-alpine AS builder
WORKDIR /app

# Copy package descriptors first to cache package installation
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# ─── Stage 2: Web Server ───
FROM nginx:stable-alpine

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Document port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
