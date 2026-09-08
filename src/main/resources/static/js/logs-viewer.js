document.addEventListener('DOMContentLoaded', () => {
    const logTerminal = document.getElementById('logTerminal');
    const refreshBtn = document.getElementById('refreshBtn');
    const linesSelect = document.getElementById('linesSelect');
    const autoRefreshSwitch = document.getElementById('autoRefreshSwitch');
    const lastUpdated = document.getElementById('lastUpdated');
    const scrollBottomBtn = document.getElementById('scrollBottomBtn');

    if (!logTerminal) return;

    let refreshTimer = null;

    function scrollToBottom() {
        logTerminal.scrollTop = logTerminal.scrollHeight;
    }

    scrollToBottom();

    function fetchLogs() {
        const lines = linesSelect ? linesSelect.value : 100;
        const icon = refreshBtn ? refreshBtn.querySelector('i') : null;
        if (icon) icon.classList.add('fa-spin');

        fetch(`/api/logs/raw?lines=${lines}`)
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data !== undefined) {
                    logTerminal.textContent = res.data;
                    const now = new Date();
                    if (lastUpdated) lastUpdated.textContent = 'Обновлено: ' + now.toLocaleTimeString();
                    if (autoRefreshSwitch && autoRefreshSwitch.checked) {
                        scrollToBottom();
                    }
                }
            })
            .catch(err => console.error('Ошибка обновления логов:', err))
            .finally(() => {
                if (icon) icon.classList.remove('fa-spin');
            });
    }

    if (refreshBtn) refreshBtn.addEventListener('click', fetchLogs);
    if (linesSelect) linesSelect.addEventListener('change', fetchLogs);
    if (scrollBottomBtn) scrollBottomBtn.addEventListener('click', scrollToBottom);

    if (autoRefreshSwitch) {
        autoRefreshSwitch.addEventListener('change', (e) => {
            if (e.target.checked) {
                fetchLogs();
                refreshTimer = setInterval(fetchLogs, 3000);
            } else {
                if (refreshTimer) clearInterval(refreshTimer);
            }
        });
    }
});