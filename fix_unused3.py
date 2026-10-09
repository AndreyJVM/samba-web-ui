import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Folder, ArrowUp, FolderPlus, ChevronRight', '')
content = content.replace('interface BrowseResult { currentPath: string; parentPath: string | null; directories: DirectoryItem[]; }', '')
content = content.replace('const { info, error: toastError, success } = useToast();', 'const { info, error: toastError } = useToast();')

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/FileManager.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('const result = e.target?.result as string;\n', '')
with open('frontend/src/components/FileManager.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

