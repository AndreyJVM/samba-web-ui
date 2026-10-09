import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import { useTranslation } from "../lib/i18n";', 'import { useTranslation } from "../lib/i18n";\nimport { FileManager } from "../components/FileManager";')

# Replace the whole file manager block
idx = content.find('<div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">')
if idx != -1:
    content = content[:idx] + '<FileManager />\n    </div>\n  );\n}\n'

# We still have `fetchFiles`, `currentPath`, etc. But since I can't easily remove them safely, I will just let them be, OR carefully remove them.
# Let's remove them safely.
content = re.sub(r'const \[currentPath, setCurrentPath\] = useState\("/"\);\n', '', content)
content = re.sub(r'const \[browseData, setBrowseData\] = useState<BrowseResult \| null>\(null\);\n', '', content)
content = re.sub(r'const \[filesLoading, setFilesLoading\] = useState\(false\);\n', '', content)
content = re.sub(r'const \[isCreatingDir, setIsCreatingDir\] = useState\(false\);\n', '', content)
content = re.sub(r'const \[newDirName, setNewDirName\] = useState\(""\);\n', '', content)
content = re.sub(r'const navigateTo = .*?;\n', '', content)

# But `fetchFiles` still uses `setBrowseData`. I can modify `fetchFiles` to only fetch disk usage!
old_fetchFiles = '''  const fetchFiles = async (path: string) => {
    setFilesLoading(true);
    try {
      const [browseRes, diskRes] = await Promise.all([
        api.get<BrowseResult>(`/api/fs/browse?path=${encodeURIComponent(path)}`).catch(() => null),
        api.get<DiskUsage>(`/api/fs/disk-usage?path=${encodeURIComponent(path)}`).catch(() => null)
      ]);
      if (browseRes) setBrowseData(browseRes);
      if (diskRes) setDiskUsage(diskRes);
    } catch (err: any) {
      toastError("FS Error", err.message);
    } finally {
      setFilesLoading(false);
    }
  };'''

new_fetchFiles = '''  const fetchFiles = async () => {
    try {
      const diskRes = await api.get<DiskUsage>(`/api/fs/disk-usage?path=/`).catch(() => null);
      if (diskRes) setDiskUsage(diskRes);
    } catch (err: any) {
    }
  };'''
content = content.replace(old_fetchFiles, new_fetchFiles)

content = re.sub(r'  useEffect\(\(\) => \{\n    fetchFiles\(currentPath\);\n  \}, \[currentPath\]\);\n', '  useEffect(() => {\n    fetchFiles();\n  }, []);\n', content)

# Also remove handleCreateDirectory
content = re.sub(r'  const handleCreateDirectory = async \(e: React.FormEvent\) => \{.*?\n  \};\n', '', content, flags=re.DOTALL)

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/FileManager.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = re.sub(r'const result = e.target\?.result as string;\n\s*const base64 = result.split.*?\n', '', content)
with open('frontend/src/components/FileManager.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
