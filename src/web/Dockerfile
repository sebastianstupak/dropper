# Multi-stage Dockerfile for Dropper Next.js app

# Stage 1: Dependencies
FROM node:22-alpine AS deps
RUN corepack enable && corepack prepare pnpm@10.28.2 --activate
WORKDIR /app

# Copy package files
COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

# Stage 2: Builder
FROM node:22-alpine AS builder
RUN corepack enable && corepack prepare pnpm@10.28.2 --activate
WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build the application
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# Stage 3: Runner (for self-hosted)
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy built files
COPY --from=builder /app/out ./out
COPY --from=builder /app/public ./public

# Install serve using npm (comes with Node.js)
RUN npm install -g serve

USER nextjs

EXPOSE 3000

ENV PORT=3000

CMD ["serve", "-s", "out", "-l", "3000"]

# Stage 4: Export (for GitHub Pages)
FROM scratch AS export
COPY --from=builder /app/out /
