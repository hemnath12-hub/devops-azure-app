document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const dropzoneArea = document.getElementById('dropzone-area');
  const fileInput = document.getElementById('file-input');
  const selectedFileLabel = document.getElementById('selected-file-label');
  const uploadForm = document.getElementById('upload-form');
  const tagsInput = document.getElementById('tags-input');
  const btnUpload = document.getElementById('btn-upload');
  const searchInput = document.getElementById('search-input');
  const btnRefresh = document.getElementById('btn-refresh');
  const documentsTbody = document.getElementById('documents-tbody');
  const toast = document.getElementById('toast');

  // Badge & Stat Elements
  const valBuildNum = document.getElementById('val-build-num');
  const valGitSha = document.getElementById('val-git-sha');
  const valEnv = document.getElementById('val-env');
  const valStorageProvider = document.getElementById('val-storage-provider');
  const statTotalDocs = document.getElementById('stat-total-docs');
  const statTotalSize = document.getElementById('stat-total-size');
  const statStorageLoc = document.getElementById('stat-storage-loc');
  const statUptime = document.getElementById('stat-uptime');

  let currentSelectedFile = null;

  // Initialize
  fetchSystemInfo();
  fetchStorageStats();
  fetchDocuments();

  // Periodic Refresh every 10s
  setInterval(() => {
    fetchStorageStats();
    fetchSystemInfo();
  }, 10000);

  // Drag and Drop Handling
  ['dragenter', 'dragover'].forEach(eventName => {
    dropzoneArea.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzoneArea.classList.add('drag-over');
    });
  });

  ['dragleave', 'drop'].forEach(eventName => {
    dropzoneArea.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzoneArea.classList.remove('drag-over');
    });
  });

  dropzoneArea.addEventListener('drop', (e) => {
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelected(e.dataTransfer.files[0]);
    }
  });

  dropzoneArea.addEventListener('click', () => {
    fileInput.click();
  });

  fileInput.addEventListener('change', () => {
    if (fileInput.files && fileInput.files.length > 0) {
      handleFileSelected(fileInput.files[0]);
    }
  });

  function handleFileSelected(file) {
    currentSelectedFile = file;
    selectedFileLabel.textContent = `Selected: ${file.name} (${formatBytes(file.size)})`;
    selectedFileLabel.style.color = 'var(--azure-light)';
  }

  // Upload Submission
  uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!currentSelectedFile) {
      showToast('Please select or drop a file first.', 'error');
      return;
    }

    btnUpload.disabled = true;
    btnUpload.innerHTML = 'Encrypting & Storing...';

    const formData = new FormData();
    formData.append('file', currentSelectedFile);
    if (tagsInput.value.trim()) {
      formData.append('tags', tagsInput.value.trim());
    }

    try {
      const response = await fetch('/api/v1/documents/upload', {
        method: 'POST',
        body: formData
      });

      const data = await response.json();
      if (response.ok && data.success) {
        showToast(`Document "${data.document.filename}" stored in vault!`, 'success');
        uploadForm.reset();
        currentSelectedFile = null;
        selectedFileLabel.textContent = 'Supports PDF, DOCX, Images, Configs up to 50MB';
        selectedFileLabel.style.color = 'var(--text-dim)';
        fetchDocuments();
        fetchStorageStats();
      } else {
        showToast(data.message || 'Failed to upload document.', 'error');
      }
    } catch (err) {
      showToast('Error uploading document: ' + err.message, 'error');
    } finally {
      btnUpload.disabled = false;
      btnUpload.innerHTML = `
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
        Upload to Vault
      `;
    }
  });

  // Fetch Documents
  async function fetchDocuments() {
    const search = searchInput.value.trim();
    let url = '/api/v1/documents';
    if (search) {
      url += `?search=${encodeURIComponent(search)}`;
    }

    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error('Failed to fetch documents');
      const docs = await res.json();
      renderDocumentsTable(docs);
    } catch (err) {
      console.warn('API offline or error:', err);
      renderDemoPlaceholderIfEmpty();
    }
  }

  function renderDocumentsTable(docs) {
    if (!docs || docs.length === 0) {
      documentsTbody.innerHTML = `
        <tr class="empty-row">
          <td colspan="7">No documents found. Drag & drop a file to test the vault!</td>
        </tr>
      `;
      return;
    }

    documentsTbody.innerHTML = docs.map(doc => {
      const uploadedDate = new Date(doc.uploadedAt).toLocaleString();
      const tagsHtml = doc.tags && doc.tags.length > 0 
        ? doc.tags.map(t => `<span class="tag-badge">${escapeHtml(t)}</span>`).join('')
        : '<span style="color: var(--text-dim); font-size: 0.75rem;">None</span>';

      return `
        <tr>
          <td>
            <div class="doc-name">
              <div class="doc-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
              </div>
              <div>
                <div>${escapeHtml(doc.filename)}</div>
                <div style="font-size: 0.75rem; color: var(--text-dim); font-family: monospace;">ID: ${doc.id}</div>
              </div>
            </div>
          </td>
          <td>
            <span class="doc-hash" title="${doc.checksumSha256}">${doc.checksumSha256}</span>
          </td>
          <td>${formatBytes(doc.sizeBytes)}</td>
          <td>${tagsHtml}</td>
          <td>
            <span class="badge-pill ${doc.storageProvider === 'AZURE_BLOB_STORAGE' ? 'azure-pill' : ''}">
              ${doc.storageProvider}
            </span>
          </td>
          <td style="color: var(--text-muted); font-size: 0.8rem;">${uploadedDate}</td>
          <td>
            <div class="action-btn-group">
              <button class="btn-icon" onclick="downloadDoc('${doc.id}')" title="Download">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
              </button>
              <button class="btn-icon btn-delete" onclick="deleteDoc('${doc.id}')" title="Delete">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  // System & Stats Telemetry
  async function fetchSystemInfo() {
    try {
      const res = await fetch('/api/v1/system/info');
      if (res.ok) {
        const info = await res.json();
        valBuildNum.textContent = `Build #${info.buildNumber || '104'}`;
        valGitSha.textContent = (info.gitCommit || 'a8f23bc').substring(0, 7);
        valEnv.textContent = info.environment || 'Production';
        valStorageProvider.textContent = info.azureStorageEnabled ? 'Azure Blob' : 'Local Fallback';
      }
    } catch (err) {
      console.debug('System info endpoint offline, using defaults.');
    }
  }

  async function fetchStorageStats() {
    try {
      const res = await fetch('/api/v1/documents/stats');
      if (res.ok) {
        const stats = await res.json();
        statTotalDocs.textContent = stats.totalDocuments;
        statTotalSize.textContent = stats.formattedSize;
        statStorageLoc.textContent = `Location: ${stats.storageLocation}`;
        statUptime.textContent = `Uptime: ${formatUptime(stats.uptimeSeconds)}`;
      }
    } catch (err) {
      console.debug('Stats offline, using defaults.');
    }
  }

  // Global window functions for actions
  window.downloadDoc = (id) => {
    window.location.href = `/api/v1/documents/${id}/download`;
  };

  window.deleteDoc = async (id) => {
    if (!confirm('Are you sure you want to delete this document from the vault?')) return;
    try {
      const res = await fetch(`/api/v1/documents/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showToast('Document deleted.', 'success');
        fetchDocuments();
        fetchStorageStats();
      } else {
        showToast('Failed to delete document.', 'error');
      }
    } catch (err) {
      showToast('Error deleting: ' + err.message, 'error');
    }
  };

  // Search filter
  searchInput.addEventListener('input', () => {
    fetchDocuments();
  });

  btnRefresh.addEventListener('click', () => {
    fetchDocuments();
    fetchStorageStats();
    showToast('Vault refreshed.', 'success');
  });

  // Utilities
  function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  function formatUptime(seconds) {
    if (!seconds) return '0s';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function showToast(message, type = 'success') {
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    setTimeout(() => {
      toast.className = 'toast';
    }, 3500);
  }

  function renderDemoPlaceholderIfEmpty() {
    documentsTbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="7">Vault Ready • Waiting for document upload or backend connection.</td>
      </tr>
    `;
  }
});
