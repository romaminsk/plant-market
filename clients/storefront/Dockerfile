FROM node:24-alpine AS deps
RUN corepack enable
WORKDIR /repo
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY clients/storefront/package.json clients/storefront/package.json
COPY clients/admin/package.json clients/admin/package.json
RUN pnpm install --frozen-lockfile --filter @plant-market/storefront...

FROM deps AS build
COPY clients/storefront ./clients/storefront
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_SITE_URL
ARG AUTH_SERVICE_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL \
    AUTH_SERVICE_URL=$AUTH_SERVICE_URL
RUN pnpm --filter @plant-market/storefront build

FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0
COPY --from=build /repo/clients/storefront/.next/standalone ./
COPY --from=build /repo/clients/storefront/.next/static ./clients/storefront/.next/static
COPY --from=build /repo/clients/storefront/public ./clients/storefront/public
USER node
CMD ["node", "clients/storefront/server.js"]
