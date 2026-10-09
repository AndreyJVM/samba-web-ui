import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Clean up imports and unused states
content = re.sub(r'import \{.*?\} from "lucide-react";', 'import { FileStack, Power } from "lucide-react";', content)
content = re.sub(r'const \[currentPath, setCurrentPath\].*?;\n', '', content)
content = re.sub(r'const \[browseData, setBrowseData\].*?;\n', '', content)
content = re.sub(r'const \[filesLoading, setFilesLoading\].*?;\n', '', content)
content = re.sub(r'const \[isCreatingDir, setIsCreatingDir\].*?;\n', '', content)
content = re.sub(r'const \[newDirName, setNewDirName\].*?;\n', '', content)
content = re.sub(r'const navigateTo = .*?;\n', '', content)
content = re.sub(r'const handleCreateDirectory = .*?\n  \};\n', '', content, flags=re.DOTALL)
content = re.sub(r'const fetchFiles = .*?\n  \};\n', '', content, flags=re.DOTALL)

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/FileManager.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('import { FilePane, BrowseResult, DirectoryItem } from "./ui/file-pane";', 'import { FilePane, BrowseResult } from "./ui/file-pane";')
content = content.replace("const base64 = result.split(',')[1];", "")
with open('frontend/src/components/FileManager.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/ui/file-pane.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('import React, { useState, useEffect }', 'import React, { useState }')
content = content.replace('import { api } from "../../lib/api";\n', '')
content = content.replace('export function FilePane({ path, onNavigate, browseData, loading, selectedItems, onToggleSelect, onSelectAll, onCreateDir, onUploadFile, onDrop, onAction, title }: FilePaneProps) {', 'export function FilePane({ onNavigate, browseData, loading, selectedItems, onToggleSelect, onSelectAll, onCreateDir, onUploadFile, onDrop, onAction }: FilePaneProps) {')
with open('frontend/src/components/ui/file-pane.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
