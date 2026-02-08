# Dropper Web Deployment Guide

This guide covers deploying the Dropper landing page to various platforms.

## Table of Contents
- [GitHub Pages](#github-pages)
- [Docker](#docker)
- [Vercel](#vercel)
- [Manual Deployment](#manual-deployment)

---

## GitHub Pages

### Automatic Deployment

The site automatically deploys to GitHub Pages when you push to `main`:

```bash
git add .
git commit -m "Update landing page"
git push origin main
```

The GitHub Action will:
1. Build the Next.js static export
2. Upload to GitHub Pages
3. Deploy to: `https://[username].github.io/minecraft-mod-versioning-example/`

### Setup (One-time)

1. **Enable GitHub Pages** in repository settings:
   - Go to Settings → Pages
   - Source: GitHub Actions
   - Save

2. **Configure base path** (if needed):
   Edit `.github/workflows/deploy-web.yml`:
   ```yaml
   env:
     NEXT_PUBLIC_BASE_PATH: /your-repo-name
   ```

### Local Test

Test the static export locally:

```bash
cd src/web
pnpm build
pnpm add -g serve
serve -s out
```

Visit http://localhost:3000

---

## Docker

### Build and Run

**Build the image:**
```bash
cd src/web
docker build -t dropper-web .
```

**Run the container:**
```bash
docker run -p 3000:3000 dropper-web
```

Visit http://localhost:3000

### Docker Compose

**Start services:**
```bash
cd src/web
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f
```

**Stop services:**
```bash
docker-compose down
```

### Multi-stage Builds

The Dockerfile has multiple targets:

1. **deps** - Install dependencies only
2. **builder** - Build the application
3. **runner** - Production server (default)
4. **export** - Static files only

**Build specific stage:**
```bash
docker build --target export -t dropper-web:export .
```

### Docker Hub / GitHub Container Registry

**Tag and push:**
```bash
# Tag
docker tag dropper-web ghcr.io/[username]/dropper-web:latest

# Push to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u [username] --password-stdin
docker push ghcr.io/[username]/dropper-web:latest
```

---

## Vercel

### Quick Deploy

1. **Connect GitHub repository** to Vercel
2. **Configure project**:
   - Framework: Next.js
   - Root Directory: `src/web`
   - Build Command: `pnpm build`
   - Output Directory: `.next`

3. **Deploy**:
   - Auto-deploys on every push to `main`
   - Preview deployments for PRs

### Environment Variables

Set in Vercel dashboard:
```
NEXT_PUBLIC_BASE_PATH=
NODE_ENV=production
```

### CLI Deployment

```bash
# Install Vercel CLI
pnpm add -g vercel

# Deploy
cd src/web
vercel --prod
```

---

## Manual Deployment

### Build Static Files

```bash
cd src/web
pnpm build
```

Output in `out/` directory.

### Deploy to Any Static Host

The `out/` directory contains static HTML/CSS/JS. Upload to:

- **AWS S3 + CloudFront**
- **Netlify**: Drag & drop `out/` folder
- **Cloudflare Pages**: Connect Git or upload
- **Azure Static Web Apps**
- **Any web server**: Nginx, Apache, etc.

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name dropper.example.com;
    root /var/www/dropper/out;

    location / {
        try_files $uri $uri/ $uri.html =404;
    }

    # Enable gzip
    gzip on;
    gzip_types text/css application/javascript image/svg+xml;

    # Cache static assets
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## CI/CD Pipeline

### GitHub Actions Workflows

**`.github/workflows/deploy-web.yml`**
- Triggers: Push to `main`, manual dispatch
- Deploys to GitHub Pages

**`.github/workflows/build-docker.yml`**
- Triggers: Push to `main`, tags, manual dispatch
- Builds and pushes Docker image to GHCR

### Workflow Features

✅ **Caching** - pnpm store cached for faster builds
✅ **Artifacts** - Build outputs uploaded
✅ **Docker layer caching** - GitHub Actions cache
✅ **Multi-stage builds** - Optimized image sizes
✅ **Automatic versioning** - Semantic versioning from tags

---

## Environment Variables

### Build Time

Set in `.env.local` or CI/CD:

```bash
# Base path for routing (GitHub Pages)
NEXT_PUBLIC_BASE_PATH=/repo-name

# Analytics IDs (optional)
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
NEXT_PUBLIC_PLAUSIBLE_DOMAIN=dropper.dev
```

### Runtime (Docker)

Set in `docker-compose.yml` or docker run:

```bash
docker run -p 3000:3000 \
  -e NODE_ENV=production \
  -e PORT=3000 \
  dropper-web
```

---

## Troubleshooting

### Images Not Loading

**Problem**: Images show 404 on GitHub Pages

**Solution**: Update `next.config.js`:
```js
images: {
  unoptimized: true,
}
```

### Base Path Issues

**Problem**: Links broken on GitHub Pages

**Solution**: Set base path in workflow:
```yaml
env:
  NEXT_PUBLIC_BASE_PATH: /repo-name
```

Update links to use base path:
```tsx
<Link href={`${process.env.NEXT_PUBLIC_BASE_PATH || ''}/about`}>
```

### Docker Build Fails

**Problem**: EACCES permission errors

**Solution**: Check Dockerfile user permissions:
```dockerfile
RUN chown -R nextjs:nodejs /app
USER nextjs
```

### Out of Memory

**Problem**: Build fails with heap out of memory

**Solution**: Increase Node memory:
```bash
NODE_OPTIONS="--max-old-space-size=4096" pnpm build
```

Or in Dockerfile:
```dockerfile
ENV NODE_OPTIONS="--max-old-space-size=4096"
```

---

## Performance Optimization

### Static Export

✅ Pre-rendered HTML for instant loads
✅ No server required
✅ Can be served from CDN
✅ Perfect Lighthouse scores

### Docker Optimization

✅ Multi-stage builds (small final image)
✅ Layer caching for fast rebuilds
✅ Alpine Linux base (minimal size)
✅ Non-root user for security

### CDN & Caching

Configure cache headers:
```js
// next.config.js
headers: async () => [
  {
    source: '/:path*',
    headers: [
      {
        key: 'Cache-Control',
        value: 'public, max-age=31536000, immutable',
      },
    ],
  },
]
```

---

## Monitoring

### Health Checks

Docker health check included:
```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:3000"]
  interval: 30s
```

### Analytics

Add to `layout.tsx`:
```tsx
// Google Analytics
<Script src="https://www.googletagmanager.com/gtag/js?id=GA_ID" />

// Plausible
<Script defer data-domain="dropper.dev" src="https://plausible.io/js/script.js" />
```

---

## Security

### Content Security Policy

Add to `next.config.js`:
```js
headers: async () => [
  {
    source: '/:path*',
    headers: [
      {
        key: 'Content-Security-Policy',
        value: "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'",
      },
    ],
  },
]
```

### Docker Security

✅ Non-root user
✅ Minimal base image
✅ No unnecessary packages
✅ Read-only filesystem (optional)

---

## Rollback

### GitHub Pages

Revert to previous deployment:
```bash
git revert HEAD
git push origin main
```

### Docker

Roll back to previous image:
```bash
docker pull ghcr.io/[username]/dropper-web:v1.0.0
docker-compose up -d
```

---

## Support

- **Documentation**: See `CLAUDE.md` for development guide
- **Issues**: https://github.com/[username]/dropper/issues
- **CI Logs**: Check GitHub Actions tab

**Deployment Status**: Check workflow badge in README
