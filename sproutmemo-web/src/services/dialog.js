// Custom Dialog Service with Premium UI/UX Overlay Modals and Keyboard Accessibility

export function showAlert(message, title = '系统提示') {
  return new Promise((resolve) => {
    // Create overlay
    const overlay = document.createElement('div');
    overlay.className = 'custom-dialog-overlay';
    
    overlay.innerHTML = `
      <div class="custom-dialog-box">
        <div class="custom-dialog-header">
          <span class="custom-dialog-icon">ℹ️</span>
          <span class="custom-dialog-title">${title}</span>
        </div>
        <div class="custom-dialog-body">${message}</div>
        <div class="custom-dialog-footer">
          <button class="btn btn-primary custom-dialog-ok">确定</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);
    
    // Animate entrance
    setTimeout(() => overlay.classList.add('active'), 10);

    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        e.preventDefault();
        close();
      }
    };
    // Use capture phase (true) to intercept before any component ESC key handlers run
    window.addEventListener('keydown', handleEsc, true);

    const close = () => {
      window.removeEventListener('keydown', handleEsc, true);
      overlay.classList.remove('active');
      setTimeout(() => {
        overlay.remove();
        resolve();
      }, 200);
    };

    overlay.querySelector('.custom-dialog-ok').addEventListener('click', close);
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) close();
    });
  });
}

export function showConfirm(message, title = '确认操作') {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = 'custom-dialog-overlay';
    
    overlay.innerHTML = `
      <div class="custom-dialog-box">
        <div class="custom-dialog-header">
          <span class="custom-dialog-icon">❓</span>
          <span class="custom-dialog-title">${title}</span>
        </div>
        <div class="custom-dialog-body">${message}</div>
        <div class="custom-dialog-footer" style="display: flex; gap: 12px; justify-content: flex-end;">
          <button class="btn btn-secondary custom-dialog-cancel" style="width: auto;">取消</button>
          <button class="btn btn-danger custom-dialog-confirm" style="width: auto;">确认</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);
    
    setTimeout(() => overlay.classList.add('active'), 10);

    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        e.preventDefault();
        finish(false);
      }
    };
    window.addEventListener('keydown', handleEsc, true);

    const finish = (result) => {
      window.removeEventListener('keydown', handleEsc, true);
      overlay.classList.remove('active');
      setTimeout(() => {
        overlay.remove();
        resolve(result);
      }, 200);
    };

    overlay.querySelector('.custom-dialog-confirm').addEventListener('click', () => finish(true));
    overlay.querySelector('.custom-dialog-cancel').addEventListener('click', () => finish(false));
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) finish(false);
    });
  });
}

/**
 * Three-button Confirm Modal for Edit close intercept
 * Resolves to: 'save', 'discard', or 'cancel'
 */
export function showThreeButtonConfirm(message, title = '确认操作') {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = 'custom-dialog-overlay';
    
    overlay.innerHTML = `
      <div class="custom-dialog-box" style="max-width: 460px;">
        <div class="custom-dialog-header">
          <span class="custom-dialog-icon">❓</span>
          <span class="custom-dialog-title">${title}</span>
        </div>
        <div class="custom-dialog-body">${message}</div>
        <div class="custom-dialog-footer" style="display: flex; gap: 10px; justify-content: flex-end;">
          <button class="btn btn-secondary custom-dialog-btn-cancel" style="width: auto; font-size: 13.5px; font-weight: 700; height: 38px; padding: 0 16px;">取消</button>
          <button class="btn btn-danger custom-dialog-btn-discard" style="width: auto; font-size: 13.5px; font-weight: 700; height: 38px; padding: 0 16px;">放弃修改</button>
          <button class="btn btn-primary custom-dialog-btn-save" style="width: auto; font-size: 13.5px; font-weight: 700; height: 38px; padding: 0 16px; background-color: var(--primary); border-color: var(--primary); color: white;">保存修改</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);
    
    setTimeout(() => overlay.classList.add('active'), 10);

    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        e.preventDefault();
        finish('cancel');
      }
    };
    window.addEventListener('keydown', handleEsc, true);

    const finish = (result) => {
      window.removeEventListener('keydown', handleEsc, true);
      overlay.classList.remove('active');
      setTimeout(() => {
        overlay.remove();
        resolve(result);
      }, 200);
    };

    overlay.querySelector('.custom-dialog-btn-save').addEventListener('click', () => finish('save'));
    overlay.querySelector('.custom-dialog-btn-discard').addEventListener('click', () => finish('discard'));
    overlay.querySelector('.custom-dialog-btn-cancel').addEventListener('click', () => finish('cancel'));
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) finish('cancel');
    });
  });
}

export function showPrompt(message, defaultValue = '', title = '输入信息') {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = 'custom-dialog-overlay';
    
    overlay.innerHTML = `
      <div class="custom-dialog-box">
        <div class="custom-dialog-header">
          <span class="custom-dialog-icon">✏️</span>
          <span class="custom-dialog-title">${title}</span>
        </div>
        <div class="custom-dialog-body" style="display: flex; flex-direction: column; gap: 12px;">
          <label style="font-weight: 500; font-size: 14px; color: var(--text-main);">${message}</label>
          <input type="text" id="custom-prompt-input" class="form-control" value="${defaultValue}" style="margin-bottom: 0;" />
        </div>
        <div class="custom-dialog-footer" style="display: flex; gap: 12px; justify-content: flex-end;">
          <button class="btn btn-secondary custom-dialog-cancel" style="width: auto;">取消</button>
          <button class="btn btn-primary custom-dialog-submit" style="width: auto;">确认</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);
    
    // Focus input
    const inputEl = overlay.querySelector('#custom-prompt-input');
    setTimeout(() => {
      overlay.classList.add('active');
      inputEl.focus();
      inputEl.select();
    }, 10);

    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        e.preventDefault();
        finish(null);
      }
    };
    window.addEventListener('keydown', handleEsc, true);

    const finish = (value) => {
      window.removeEventListener('keydown', handleEsc, true);
      overlay.classList.remove('active');
      setTimeout(() => {
        overlay.remove();
        resolve(value);
      }, 200);
    };

    overlay.querySelector('.custom-dialog-submit').addEventListener('click', () => {
      finish(inputEl.value);
    });
    overlay.querySelector('.custom-dialog-cancel').addEventListener('click', () => {
      finish(null);
    });
    inputEl.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') finish(inputEl.value);
    });
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) finish(null);
    });
  });
}
