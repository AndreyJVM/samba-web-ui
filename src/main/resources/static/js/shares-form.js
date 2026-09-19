/**
 * JS-логика для формы редактирования/создания шары
 */
document.addEventListener("DOMContentLoaded", function() {
    const validUsersEl = document.getElementById('validUsers');
    if (!validUsersEl) return;
    
    const rawValidUsers = validUsersEl.value.trim();
    if (!rawValidUsers) return;

    // Читаем текущих (из конфига)
    const entries = rawValidUsers.split(',').map(e => e.trim()).filter(e => e.length > 0);
    const customEntries = [];

    // Бежим по загруженным юзерам
    entries.forEach(entry => {
        const checkbox = document.querySelector(`.valid-users-checkbox[value="${entry}"]`);
        if (checkbox) {
            checkbox.checked = true;
        } else {
            customEntries.push(entry); // Иначe (AD доменные)
        }
    });

    // Заполняем поле
    if (customEntries.length > 0) {
        document.getElementById('customUsers').value = customEntries.join(', ');
    }
});

function collectValidUsers() {
    const checkboxes = document.querySelectorAll('.valid-users-checkbox:checked');
    const localSelected = Array.from(checkboxes).map(cb => cb.value); // Локальные или группы
    
    const customInputString = document.getElementById('customUsers').value.trim();
    const customSelected = customInputString ? customInputString.split(',').map(e => e.trim()).filter(e => e.length > 0) : [];
    
    const allSelected = [...localSelected, ...customSelected];
    
    const validUsersEl = document.getElementById('validUsers');
    if (validUsersEl) {
        validUsersEl.value = allSelected.join(', ');
    }
}