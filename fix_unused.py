import re

with open('frontend/src/components/ui/autocomplete.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace("import { ChevronDown } from 'lucide-react';\n", "")
with open('frontend/src/components/ui/autocomplete.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('frontend/src/pages/groups/GroupsPage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('import { AutocompleteInput } from "../../components/ui/autocomplete";\n', '')
with open('frontend/src/pages/groups/GroupsPage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
