import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import { useTranslation } from "../lib/i18n";', 'import { useTranslation } from "../lib/i18n";\nimport { FileManager } from "../components/FileManager";')

# We need to remove the existing file manager state and UI
# states to remove: currentPath, browseData, filesLoading, isCreatingDir, newDirName, fetchFiles, handleCreateDirectory
# But let's just replace the UI first

pattern = r'<div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">.*?<div className="min-h-\[240px\]">.*?</div>\s*</div>'

new_ui = '<FileManager />'

content = re.sub(pattern, new_ui, content, flags=re.DOTALL)

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
