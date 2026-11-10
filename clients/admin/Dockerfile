FROM node:24-alpine AS build
RUN corepack enable
WORKDIR /repo
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY clients/admin/package.json clients/admin/package.json
COPY clients/storefront/package.json clients/storefront/package.json
RUN pnpm install --frozen-lockfile --filter @plant-market/admin...
COPY clients/admin ./clients/admin
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN pnpm --filter @plant-market/admin build

FROM nginx:1.29-alpine
COPY infra/staging/admin-nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/clients/admin/dist /usr/share/nginx/html
