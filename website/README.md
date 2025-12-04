# Featherweight Website

Landing page and legal documents for the Featherweight weightlifting tracking app.

## Tech Stack

- **Astro 5** - Static site generator
- **Tailwind CSS 4** - Utility-first CSS
- **DaisyUI 5** - Component library with custom theme
- **View Transitions** - Smooth page navigation

## Development

```bash
# Install dependencies
pnpm install

# Start dev server at localhost:4321
pnpm dev

# Build for production
pnpm build

# Preview production build
pnpm preview
```

## Structure

```
website/
├── src/
│   ├── components/      # Astro components
│   ├── config/          # Site configuration
│   ├── layouts/         # Page layouts
│   ├── pages/           # Routes
│   └── styles/          # Global CSS with Tailwind
├── public/              # Static assets
└── dist/                # Build output
```

## Pages

- `/` - Landing page with features, FAQ, and email signup
- `/privacy-policy` - Privacy policy (required for Google Play)
- `/terms` - Terms of service

## Deployment

Automatically deployed to GitHub Pages via GitHub Actions when changes are pushed to `main` in the `website/` directory.

Custom domain: `featherweight.app`

## DNS Configuration

Required DNS records for custom domain:

| Type  | Name | Value              |
|-------|------|--------------------|
| A     | @    | 185.199.108.153    |
| A     | @    | 185.199.109.153    |
| A     | @    | 185.199.110.153    |
| A     | @    | 185.199.111.153    |
| CNAME | www  | radupana.github.io |
