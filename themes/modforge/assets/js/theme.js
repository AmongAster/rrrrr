document.addEventListener('DOMContentLoaded', () => {
  const copyTarget = document.querySelector('[data-copy-server]');

  if (copyTarget) {
    copyTarget.addEventListener('click', async () => {
      const value = copyTarget.getAttribute('data-copy-server');

      if (!value || !navigator.clipboard) {
        return;
      }

      await navigator.clipboard.writeText(value);
      const previous = copyTarget.innerHTML;
      copyTarget.innerHTML = '<i class="bi bi-check2"></i> copied';
      setTimeout(() => {
        copyTarget.innerHTML = previous;
      }, 1800);
    });
  }
});
