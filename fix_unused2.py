import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'  useEffect\(\(\) => \{\n    fetchFiles\(currentPath\);\n  \}, \[currentPath\]\);\n', '', content)
content = re.sub(r'interface DirectoryItem \{ name: string; fullPath: string; \}\n', '', content)
content = re.sub(r'interface BrowseResult \{ currentPath: string; parentPath: string \| null; directories: DirectoryItem\[\]; \}\n', '', content)
content = re.sub(r'  const \[diskUsage, setDiskUsage\] = useState<DiskUsage \| null>\(null\);\n', '', content)
content = content.replace('const { info, error: toastError, success } = useToast();', 'const { info, error: toastError } = useToast();')
content = re.sub(r'interface DiskUsage \{ total: string; used: string; available: string; usePercent: number; mountPoint: string; \}\n', '', content)

# Remove disk usage rendering
content = re.sub(r'<div className="bg-surface rounded-lg border border-border shadow-sm-subtle px-5 py-4 flex items-center justify-between gap-6 shrink-0">.*?</div>', '', content, flags=re.DOTALL)

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/FileManager.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('const result = e.target?.result as string;\n        \n        const formData = new FormData();', 'const formData = new FormData();')
with open('frontend/src/components/FileManager.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
