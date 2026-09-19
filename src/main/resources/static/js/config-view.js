/**
 * JS-логика для страницы просмотра конфига (smb.conf)
 */
function openRestoreModal(filename, date) {
    const targetFile = document.getElementById('restoreFilename');
    const targetDate = document.getElementById('restoreDate');
    const modalEl = document.getElementById('restoreModal');
    
    if (targetFile && targetDate && modalEl) {
        targetFile.value = filename;
        targetDate.textContent = date;
        new bootstrap.Modal(modalEl).show();
    }
}