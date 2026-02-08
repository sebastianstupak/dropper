rootProject.name = "dropper"

// Include CLI module
include("src:cli")
project(":src:cli").name = "cli"

// Note: src/web is a Next.js project with its own package.json
// and will be built separately with npm/yarn
