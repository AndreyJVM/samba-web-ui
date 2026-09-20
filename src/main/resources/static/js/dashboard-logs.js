function loadDashboardLogs() {
    fetch('/api/logs/raw?lines=200')
        .then(res => res.json())
        .then(data => {
            const terminal = document.getElementById('logTerminal');
            if(data.success && data.data) {
                const logs = data.data.split('\n');
                const errors = logs.filter(line => line.match(/error|fail|warn|panic/i));
                
                if(errors.length > 0) {
                    terminal.textContent = errors.join('\n');
                    document.getElementById('lastUpdated').textContent = `Найдено: ${errors.length}`;
                } else {
                    terminal.textContent = "Ошибок и предупреждений в логе не обнаружено ✅";
                    document.getElementById('lastUpdated').textContent = "ОК";
                }
            } else {
                terminal.textContent = "Не удалось загрузить логи.";
            }
        })
        .catch(e => {
            document.getElementById('logTerminal').textContent = "Ошибка сети при загрузке логов.";
        });
}

document.addEventListener('DOMContentLoaded', loadDashboardLogs);
setInterval(loadDashboardLogs, 10000); // 10s refresh