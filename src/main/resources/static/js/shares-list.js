/**
 * Скрипт для страницы списка шар: удаление и мониторинг заполненности дисков
 */

// Модальное окно подтверждения удаления
function openDeleteShareModal(name) {
    const target = document.getElementById('deleteShareTarget');
    const form = document.getElementById('deleteShareForm');
    if (target) target.textContent = name;
    if (form) form.action = '/shares/delete/' + encodeURIComponent(name);

    const modalEl = document.getElementById('deleteShareModal');
    if (modalEl) {
        new bootstrap.Modal(modalEl).show();
    }
}

// Асинхронная подгрузка свободного места на дисках
document.addEventListener('DOMContentLoaded', () => {
    const widgets = document.querySelectorAll('.disk-usage-widget');

    widgets.forEach(widget => {
        const path = widget.getAttribute('data-path');
        if (!path) return;

        fetch(`/api/fs/disk-usage?path=${encodeURIComponent(path)}`)
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    const data = res.data;

                    // Цветовой индикатор: <75% зеленый, 75-90% желтый, >=90% красный
                    let colorClass = 'bg-success';
                    if (data.usePercent >= 90) {
                        colorClass = 'bg-danger';
                    } else if (data.usePercent >= 75) {
                        colorClass = 'bg-warning';
                    }

                    widget.innerHTML = `
                        <div class="d-flex justify-content-between align-items-center mb-1" style="font-size: 0.75rem;">
                            <span class="text-secondary" title="Точка монтирования: ${data.mountPoint}">
                                <i class="fas fa-hdd me-1"></i>${data.mountPoint}
                            </span>
                            <span class="fw-bold">${data.usePercent}%</span>
                        </div>
                        <div class="progress" style="height: 5px;">
                            <div class="progress-bar ${colorClass}" role="progressbar" style="width: ${data.usePercent}%"></div>
                        </div>
                        <div class="d-flex justify-content-between text-muted mt-1" style="font-size: 0.7rem;">
                            <span>Свободно: <strong>${data.available}</strong></span>
                            <span>Всего: ${data.total}</span>
                        </div>
                    `;
                } else {
                    widget.innerHTML = `<span class="text-danger" style="font-size: 0.7rem;"><i class="fas fa-exclamation-triangle"></i> ${res.message || 'Ошибка'}</span>`;
                }
            })
            .catch(() => {
                widget.innerHTML = `<span class="text-muted" style="font-size: 0.75rem;"><i class="fas fa-wifi-slash"></i> Ошибка опроса</span>`;
            });
    });
});