# How to Publish to JetBrains Marketplace

To publish your plugin to the official JetBrains Marketplace, follow these steps:

1.  **Build the Distribution**:
    - Run the following command in the project root:
      ```bash
      ./gradlew buildPlugin
      ```
    - The generated plugin ZIP file will be located at `build/distributions/DocumentConverter-0.0.1.zip`.

2.  **Prepare for Marketplace**:
    - Ensure your `plugin.xml` has a unique `<id>`, `<name>`, and a detailed `<description>`.
    - Provide a high-quality icon (`src/main/resources/META-INF/pluginIcon.svg`).

3.  **Upload to Marketplace**:
    - Sign in to [JetBrains Marketplace](https://plugins.jetbrains.com/) with your JetBrains Account.
    - Click on your profile and select **Upload plugin**.
    - Drag and drop the ZIP file generated in step 1.
    - Fill in the required metadata (category, tags, etc.) and submit for review.

4.  **Automated Publishing (Recommended)**:
    - Generate a **Permanent Token** in your Marketplace profile.
    - Set the `ORG_GRADLE_PROJECT_intellijPublishToken` environment variable or add it to `gradle.properties`.
    - Run:
      ```bash
      ./gradlew publishPlugin
      ```