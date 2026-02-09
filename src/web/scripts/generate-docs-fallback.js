/**
 * Generates fallback docs.json if it doesn't exist
 * In production, docs should be generated from CLI with: dropper docs --output=src/web/public/docs.json
 */

const fs = require('fs')
const path = require('path')

const docsPath = path.join(__dirname, '..', 'public', 'docs.json')

// Check if docs.json exists
if (!fs.existsSync(docsPath)) {
  console.log('⚠️  docs.json not found, generating fallback...')
  console.log('ℹ️  To generate real docs, run: dropper docs --output=src/web/public/docs.json')

  const fallbackDocs = {
    version: "1.0.0",
    commands: [
      {
        name: "init",
        description: "Initialize a new multi-loader Minecraft mod project",
        usage: "dropper init <PROJECT_NAME>",
        arguments: [
          {
            name: "PROJECT_NAME",
            description: "Name of the project directory to create",
            required: true
          }
        ],
        options: [
          { name: "--name", description: "Mod display name", required: false },
          { name: "--author", description: "Mod author", required: false }
        ],
        examples: [
          "dropper init my-mod",
          "dropper init my-mod --name \"My Awesome Mod\""
        ]
      },
      {
        name: "build",
        description: "Build your mod for all configured loaders and versions",
        usage: "dropper build [OPTIONS]",
        arguments: [],
        options: [
          { name: "--all", description: "Build for all versions and loaders", required: false }
        ],
        examples: ["dropper build", "dropper build --all"]
      }
    ]
  }

  fs.writeFileSync(docsPath, JSON.stringify(fallbackDocs, null, 2))
  console.log('✅ Fallback docs.json created')
} else {
  console.log('✅ docs.json found')
}
