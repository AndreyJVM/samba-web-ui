/**
 * JS-логика для страницы списка пользователей
 */
function openDeleteUserModal(username) {
    const targetEl = document.getElementById('deleteTargetUser');
    const inputEl = document.getElementById('deleteUsernameInput');
    const modalEl = document.getElementById('deleteUserModal');
    
    if (targetEl && inputEl && modalEl) {
        targetEl.textContent = username;
        inputEl.value = username;
        new bootstrap.Modal(modalEl).show();
    }
}