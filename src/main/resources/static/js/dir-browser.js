let currentBrowserPath = '/';
let parentBrowserPath = '/';
let targetInputElement = null;

function openDirectoryBrowser(targetInputId) {
    targetInputElement = document.getElementById(targetInputId);
    const initialPath = (targetInputElement && targetInputElement.value.trim().startsWith('/'))
        ? targetInputElement.value.trim()
        : '/srv';

    new bootstrap.Modal(document.getElementById('dirBrowserModal')).show();
    loadDirectory(initialPath);
}

function loadDirectory(path) {
    const spinner = document.getElementById('dirSpinner');
    const container = document.getElementById('dirListContainer');
    const emptyMsg = document.getElementById('emptyDirMsg');
    const pathField = document.getElementById('browserCurrentPath');

    spinner.classList.remove('d-none');
    container.innerHTML = '';
    emptyMsg.classList.add('d-none');

    fetch(`/api/fs/browse?path=${encodeURIComponent(path)}`)
        .then(res => res.json())
        .then(res => {
            if (!res.success) {
                alert('Ошибка: ' + res.message);
                return;
            }
            const data = res.data;
            currentBrowserPath = data.currentPath;
            parentBrowserPath = data.parentPath;
            pathField.value = currentBrowserPath;

            if (!data.directories || data.directories.length === 0) {
                emptyMsg.classList.remove('d-none');
            } else {
                data.directories.forEach(dir => {
                    const item = document.createElement('a');
                    item.href = 'javascript:void(0)';
                    item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center py-2';
                    item.innerHTML = `
                        <span><i class="fas fa-folder text-warning me-2"></i><strong>${dir.name}</strong></span>
                        <i class="fas fa-chevron-right text-muted small"></i>
                    `;
                    item.onclick = () => loadDirectory(dir.fullPath);
                    container.appendChild(item);
                });
            }
        })
        .catch(err => {
            console.error(err);
            alert('Не удалось загрузить структуру каталогов');
        })
        .finally(() => {
            spinner.classList.add('d-none');
        });
}

document.addEventListener('DOMContentLoaded', () => {
    const btnGoUp = document.getElementById('btnGoUp');
    if (btnGoUp) {
        btnGoUp.addEventListener('click', () => {
            if (currentBrowserPath !== '/') loadDirectory(parentBrowserPath);
        });
    }

    const btnRefreshDir = document.getElementById('btnRefreshDir');
    if (btnRefreshDir) {
        btnRefreshDir.addEventListener('click', () => loadDirectory(currentBrowserPath));
    }

    const btnSelectCurrentDir = document.getElementById('btnSelectCurrentDir');
    if (btnSelectCurrentDir) {
        btnSelectCurrentDir.addEventListener('click', () => {
            if (targetInputElement) targetInputElement.value = currentBrowserPath;
            const modalEl = document.getElementById('dirBrowserModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
        });
    }

    const btnCreateDir = document.getElementById('btnCreateDir');
    if (btnCreateDir) {
        btnCreateDir.addEventListener('click', () => {
            const input = document.getElementById('newDirInput');
            const name = input.value.trim();
            if (!name) return;

            fetch(`/api/fs/mkdir?parentPath=${encodeURIComponent(currentBrowserPath)}&name=${encodeURIComponent(name)}`, {
                method: 'POST'
            })
            .then(res => res.json())
            .then(res => {
                if (!res.success) {
                    alert('Ошибка: ' + res.message);
                } else {
                    input.value = '';
                    loadDirectory(currentBrowserPath);
                }
            })
            .catch(err => console.error(err));
        });
    }
});