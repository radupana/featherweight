import eslint from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ["**/*.ts"],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      "quotes": ["error", "double"],
      "indent": ["error", 2],
    },
  },
  {
    ignores: [
      "lib/**/*",
      "generated/**/*",
      "jest.config.js",
      "**/*.test.ts",
    ],
  }
);
