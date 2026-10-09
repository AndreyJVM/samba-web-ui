import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('interface DirectoryItem { name: string; fullPath: string; }', '')

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/components/FileManager.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('reader.onload = async (e) => {', 'reader.onload = async () => {')
with open('frontend/src/components/FileManager.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

