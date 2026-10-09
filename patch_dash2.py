import re

with open('frontend/src/pages/DashboardPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import { useTranslation } from "../lib/i18n";', 'import { useTranslation } from "../lib/i18n";\nimport { FileManager } from "../components/FileManager";')

# Replace the whole file manager block
pattern = r'<div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">.*?</div>\s*</div>\s*</div>\s*</div>'

new_ui = '''<FileManager />
    </div>
  );
}'''

# wait, it's safer to just split by `<div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">`
idx = content.find('<div className="bg-surface rounded-lg border border-border shadow-sm-subtle flex flex-col overflow-hidden">')
if idx != -1:
    content = content[:idx] + '<FileManager />\n    </div>\n  );\n}\n'

with open('frontend/src/pages/DashboardPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
