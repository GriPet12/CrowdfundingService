<template>
  <div class="upload-container">
    <h1>Upload Image</h1>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <form @submit.prevent="uploadImage" class="upload-form">
      <div class="file-input-container">
        <label for="file-input" class="file-label">
          {{ fileName || 'Choose an image file' }}
        </label>
        <input
            id="file-input"
            type="file"
            @change="onFileChange"
            accept="image/*"
            class="file-input"
        />
      </div>

      <div class="preview" v-if="imagePreview">
        <img :src="imagePreview" alt="Preview" class="image-preview" />
      </div>

      <button
          type="submit"
          :disabled="isUploading || !file"
          class="upload-button"
      >
        <span v-if="isUploading">Uploading...</span>
        <span v-else>Upload</span>
      </button>
    </form>

    <div v-if="uploadSuccess" class="success-message">
      Image uploaded successfully!
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      file: null,
      fileName: '',
      imagePreview: null,
      isUploading: false,
      error: null,
      uploadSuccess: false
    };
  },
  methods: {
    onFileChange(e) {
      const selectedFile = e.target.files[0];
      if (!selectedFile) return;

      this.file = selectedFile;
      this.fileName = selectedFile.name;
      this.error = null;
      this.uploadSuccess = false;

      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(selectedFile);
    },

    async uploadImage() {
      if (!this.file) {
        this.error = 'Please select a file first';
        return;
      }

      this.isUploading = true;
      this.error = null;
      this.uploadSuccess = false;

      const formData = new FormData();
      formData.append('image', this.file);

      try {
        await axios.post('http://localhost:8081/images', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        this.uploadSuccess = true;
        this.file = null;
        this.fileName = '';
        this.imagePreview = null;
      } catch (err) {
        console.error('Upload error:', err);

        if (err.response?.status === 500) {
          this.error = 'Server error: The file might be too large or in an unsupported format';
        } else if (err.response) {
          this.error = `Error (${err.response.status}): ${err.response.data || 'Unknown error'}`;
        } else if (err.request) {
          this.error = 'Network error: Server is not responding';
        } else {
          this.error = `Upload failed: ${err.message}`;
        }
      } finally {
        this.isUploading = false;
      }
    }
  }
};
</script>

<style scoped>
.upload-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.file-input-container {
  position: relative;
}

.file-input {
  position: absolute;
  top: 0;
  left: 0;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.file-label {
  display: block;
  padding: 10px 15px;
  background-color: #f1f1f1;
  border: 1px dashed #ccc;
  border-radius: 4px;
  text-align: center;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview {
  text-align: center;
  margin: 15px 0;
}

.image-preview {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
}

.upload-button {
  padding: 10px 15px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.upload-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error-message {
  padding: 10px;
  margin: 15px 0;
  background-color: #ffebee;
  color: #c62828;
  border-radius: 4px;
  border-left: 4px solid #c62828;
}

.success-message {
  padding: 10px;
  margin: 15px 0;
  background-color: #e8f5e9;
  color: #2e7d32;
  border-radius: 4px;
  border-left: 4px solid #2e7d32;
}
</style>